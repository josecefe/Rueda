/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.*
import es.um.josecefe.rueda.util.Combinador
import es.um.josecefe.rueda.util.SubSets
import java.util.*

/**
 * @author josec
 */
internal class ContextoResolucion (horarios: Set<Horario>) {
    val dias: List<Dia>
    //Map<Dia, Integer> diasIndex;
    val participantes: List<Participante>
    //Map<Participante, Integer> participantesIndex;
    val solucionesCandidatas: MutableMap<Dia, List<AsignacionDiaV5>>
    private val mapaParticipantesSoluciones: MutableMap<Dia, Map<Set<Participante>, AsignacionDiaV5>>
    val coefConduccion: FloatArray  //Que tanto por 1 supone que use el coche cada conductor
    var participantesConCoche: BooleanArray
    var ordenExploracionDias: IntArray
    var maxVecesCondDia: Array<IntArray>
    var minVecesCondDia: Array<IntArray>
    var peorCosteDia: IntArray
    var mejorCosteDia: IntArray
    var pesoCotaInferiorNum: Int = 0 //Numerador de la fracción al ponderar la cota inferior respecto de la cota superior (el resto) para calcular el coste estimado
    var pesoCotaInferiorDen: Int = 0 //Denominador de la fracción al ponderar las cotas inferior y superior para calcular el coste estimado
    var estrategia: Resolutor.Estrategia = Resolutor.Estrategia.EQUILIBRADO

    init {
        //Construimos la lista de Dias (usamos un lista para tener un orden)
        val diasSet = mutableSetOf<Dia>() //Lo queremos ordenado
        horarios.mapTo(diasSet) { it.dia }
        dias = ArrayList(diasSet) // Podría ser un array si lo vemos necesario

        //Construimos ahora la lista de conductores (también usamos una lista por lo mismo que en dias
        val participanteSet = TreeSet<Participante>() //Lo queremos ordenado
        horarios.mapTo(participanteSet) { it.participante }
        participantes = ArrayList(participanteSet)

        // Y aquí ajustamos el coeficiente que nos mide su grado de participación (gente que solo va parte de los días)
        val vecesParticipa = horarios.groupingBy { it.participante }.eachCount()
        val maxVecesParticipa = vecesParticipa.values.max() ?: 0
        coefConduccion = FloatArray(participantes.size)
        for ((i, p) in participantes.withIndex()) {
            coefConduccion[i] = maxVecesParticipa.toFloat() / (vecesParticipa[p] ?: 0).toFloat() - 0.001f // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }

        participantesConCoche = BooleanArray(participantes.size)
        for (i in participantes.indices)
            participantesConCoche[i] = participantes[i].plazasCoche > 0

        maxVecesCondDia = Array(dias.size) { IntArray(participantes.size) }
        minVecesCondDia = Array(dias.size) { IntArray(participantes.size) }

        peorCosteDia = IntArray(dias.size)

        mejorCosteDia = IntArray(dias.size)

        Arrays.fill(mejorCosteDia, Integer.MAX_VALUE)

        // Vamos a trabajar día a día
        //Para parelizar: solucionesCandidatas = dias.stream().collect(toMap(ind -> ind, d -> {
        solucionesCandidatas = LinkedHashMap()
        mapaParticipantesSoluciones = HashMap()
        for ((indDia, d) in dias.withIndex()) {
            val solucionesDia = Vector<AsignacionDiaV5>()
            val mapaParticipantesAsignacionDia = HashMap<Set<Participante>, AsignacionDiaV5>() //ConcurrentHashMap
            val horariosDia = horarios.filter { it.dia == d }.toSet()
            val participanteHorario = horariosDia.associate { Pair(it.participante, it) }
            val participantesDia = horariosDia.map { it.participante }.sortedBy { it.nombre }

            // Para cada hora de entrada, obtenemos los conductores disponibles
            val entradaConductor = horariosDia.filter { it.coche }.groupBy({ it.entrada }, { it.participante })
            // Para comprobar, vemos los participantes, sus entradas y salidas
            val nParticipantesIda = horariosDia.groupingBy { it.entrada }.eachCount()

            val nParticipantesVuelta = horariosDia.groupingBy { it.salida }.eachCount()
            // Generamos todas las posibilidades y a ver cuales sirven...
            val conductoresDia = entradaConductor.keys.map { key ->
                SubSets(entradaConductor[key]!!, 1, entradaConductor[key]!!.size)
            }
            val combinarConductoresDia = Combinador(conductoresDia)

            for (condDia in combinarConductoresDia) {
                val selCond = condDia.flatMap { it }.toSet()
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                val plazasIda = selCond.map { participanteHorario[it] }.groupBy { it!!.entrada }.mapValues { it.value.sumBy { it!!.participante.plazasCoche } }
                val plazasVuelta = selCond.map { participanteHorario[it] }.groupBy { it!!.salida }.mapValues { it.value.sumBy { it!!.participante.plazasCoche } }

                if (nParticipantesIda.entries.all { e -> (plazasIda[e.key] ?: 0) >= e.value }
                        && nParticipantesVuelta.entries.all { e -> (plazasVuelta[e.key] ?: 0) >= e.value }) {

                    // Obtenemos la lista de posibles lugares teniendo en cuenta quien es el conductor
                    val posiblesLugares = participantesDia.map{ it.puntosEncuentro }.plus(selCond.map{ it.puntosEncuentro })

                    var mejorCoste = Integer.MAX_VALUE
                    var mejorLugaresIda: Map<Participante, Lugar>? = null
                    var mejorLugaresVuelta: Map<Participante, Lugar>? = null

                    for (selLugares in Combinador(posiblesLugares)) {
                        val lugaresIda: MutableMap<Participante, Lugar>
                        val lugaresVuelta: MutableMap<Participante, Lugar>
                        lugaresIda = HashMap()
                        lugaresVuelta = HashMap()
                        val il = selLugares.subList(participantesDia.size, selLugares.size).iterator()
                        for (i in participantesDia.indices) {
                            lugaresIda.put(participantesDia[i], selLugares[i])
                            lugaresVuelta.put(participantesDia[i], if (selCond.contains(participantesDia[i]))
                                il.next()
                            else
                                selLugares[i])
                        }

                        val plazasDisponiblesIda: Map<Int, Map<Lugar, Int>> = selCond.groupBy { participanteHorario[it]!!.entrada }.mapValues { it.value.groupBy { lugaresIda[it]!! }.mapValues { it.value.sumBy { it.plazasCoche } } }
                                //.stream().collect(groupingBy({ p -> participanteHorario[p]!!.entrada }, groupingBy({ lugaresIda[it] }, summingInt({ it.plazasCoche }))))

                        val plazasDisponiblesVuelta = selCond.groupBy { participanteHorario[it]!!.salida }.mapValues { it.value.groupBy { lugaresVuelta[it]!! }.mapValues { it.value.sumBy { it.plazasCoche } } }
                                //.stream().collect(groupingBy({ p -> participanteHorario[p]!!.salida }, groupingBy({ lugaresVuelta[it]!! }, summingInt({ it.plazasCoche }))))
                        // Para comprobar, vemos los participantes, sus entradas y salidas
                        val plazasNecesariasIda = horariosDia.groupBy{ it.entrada }.mapValues { it.value.groupingBy { lugaresIda[it.participante]!! }.eachCount() }

                        val plazasNecesariasVuelta = horariosDia.groupBy{ it.salida }.mapValues { it.value.groupingBy { lugaresVuelta[it.participante]!! }.eachCount() }

                        if (plazasNecesariasIda.entries.all { e -> e.value.entries.all { ll -> ll.value <= (plazasDisponiblesIda[e.key]!![ll.key] ?: 0) } }
                                && plazasNecesariasVuelta.entries.all { e -> e.value.entries.all { ll -> ll.value <= (plazasDisponiblesVuelta[e.key]!![ll.key] ?: 0) } }) {
                            // Calculamos coste
                            val coste = participantesDia.sumBy { e -> e.puntosEncuentro.indexOf(lugaresIda[e]) + e.puntosEncuentro.indexOf(lugaresVuelta[e]) }

                            if (coste < mejorCoste) {
                                mejorCoste = coste
                                mejorLugaresIda = lugaresIda
                                mejorLugaresVuelta = lugaresVuelta
                            }
                        }
                    }
                    if (mejorCoste < Integer.MAX_VALUE) {
                        // Tenemos una solución válida
                        solucionesDia.add(AsignacionDiaV5(participantes.toTypedArray(), selCond, mejorLugaresIda!!, mejorLugaresVuelta!!, mejorCoste))
                        // Actualizamos estadisticas
                        if (peorCosteDia[indDia] < mejorCoste) {
                            peorCosteDia[indDia] = mejorCoste
                        }
                        if (mejorCosteDia[indDia] > mejorCoste) {
                            mejorCosteDia[indDia] = mejorCoste
                        }
                        // Actualizamos veces conductor
                        for (i in participantes.indices) {
                            if (selCond.contains(participantes[i])) {
                                maxVecesCondDia[indDia][i] = 1
                            } else {
                                minVecesCondDia[indDia][i] = 0
                            }
                        }
                    }
                }
            }
            Collections.shuffle(solucionesDia) //Barajamos las soluciones parciales para dar cierta aleatoridad a igualdad de coste
            Collections.sort(solucionesDia) //Ordenamos la soluciones parciales tras el barajado
            solucionesCandidatas.put(d, ArrayList(solucionesDia))
            mapaParticipantesSoluciones.put(d, HashMap(mapaParticipantesAsignacionDia))
        }

        //Vamos a ver cual sería el mejor orden para ir explorando los días, empezando por el que menos soluciones posibles ofrece
        if (ORDEN_IMPORTA) {
            ordenExploracionDias = solucionesCandidatas.entries.sortedByDescending { it.value.size }.map { e -> dias.indexOf(e.key) }.toTypedArray().toIntArray()
        } else {
            ordenExploracionDias = (0 until solucionesCandidatas.size).toList().toIntArray()
        }


        // Cambio de indirección: para calcular acumulados vamos a tener en cuenta el orden de exploración
        for (i in peorCosteDia.size - 2 downTo 0) {
            peorCosteDia[ordenExploracionDias[i]] += peorCosteDia[ordenExploracionDias[i + 1]]
            mejorCosteDia[ordenExploracionDias[i]] += mejorCosteDia[ordenExploracionDias[i + 1]]
            for (j in 0 until minVecesCondDia[ordenExploracionDias[i]].size) {
                minVecesCondDia[ordenExploracionDias[i]][j] += minVecesCondDia[ordenExploracionDias[i + 1]][j]
                maxVecesCondDia[ordenExploracionDias[i]][j] += maxVecesCondDia[ordenExploracionDias[i + 1]][j]
            }
        }
    }

    companion object {
        private const val ORDEN_IMPORTA = true
    }
}