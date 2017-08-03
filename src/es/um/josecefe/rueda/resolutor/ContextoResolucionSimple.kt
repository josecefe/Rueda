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
import java.util.concurrent.ConcurrentHashMap

/**
 * @author josecefe
 */
internal class ContextoResolucionSimple(horarios: Set<Horario>) {
    val dias: List<Dia>
    //Map<Dia, Integer> diasIndex;
    val participantes: List<Participante>
    //Map<Participante, Integer> participantesIndex;
    val solucionesCandidatas: MutableMap<Dia, List<AsignacionDiaSimple>>
    val mapaParticipantesSoluciones: MutableMap<Dia, Map<Set<Participante>, AsignacionDiaSimple>>
    val coefConduccion: MutableMap<Participante, Double> //Que tanto por 1 supone que use el coche cada conductor

    init {
        //Construimos la lista de Dias (usamos un lista para tener un orden)
        val diasSet = TreeSet<Dia>() //Lo queremos ordenado
        horarios.map { it.dia }.filterNotNull().mapTo(diasSet) { it }
        dias = ArrayList(diasSet) // Podría ser un array si lo vemos necesario

        //Construimos ahora la lista de conductores (también usamos una lista por lo mismo que en dias
        val participanteSet = TreeSet<Participante>() //Lo queremos ordenado
        horarios.map { it.participante }.filterNotNull().mapTo(participanteSet) { it }
        participantes = ArrayList(participanteSet)

        // Y aquí ajustamos el coeficiente que nos mide su grado de participación (gente que solo va parte de los días)
        val vecesParticipa = horarios.groupingBy { it.participante!! }.eachCount()
        val maxVecesParticipa = vecesParticipa.values.max() ?: 0
        coefConduccion = LinkedHashMap(participantes.size)
        for (p in participantes) {
            coefConduccion.put(p, maxVecesParticipa.toDouble() / vecesParticipa.getOrDefault(p, 0).toDouble() - 0.001) // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }

        // Vamos a trabajar día a día
        //Para parelizar: solucionesCandidatas = dias.stream().collect(toMap(ind -> ind, d -> {
        solucionesCandidatas = LinkedHashMap()
        mapaParticipantesSoluciones = HashMap()
        for (d in dias) {
            val solucionesDia = Vector<AsignacionDiaSimple>()
            val mapaParticipantesAsignacionDia = ConcurrentHashMap<Set<Participante>, AsignacionDiaSimple>()
            val horariosDia = horarios.filter { it.dia == d }.toSet()
            val participanteHorario = horariosDia.associate{Pair(it.participante!!, it)  }
            val participantesDia = horariosDia.map{ it.participante!! }.sortedBy { it.nombre }

            // Para cada hora de entrada, obtenemos los conductores disponibles
            val entradaConductor = horariosDia.filter{ it.coche }.groupBy ({ it.entrada}, {it.participante!!})
            // Para comprobar, vemos los participantes, sus entradas y salidas
            val nParticipantesIda = horariosDia.groupingBy { it.entrada }.eachCount()

            val nParticipantesVuelta = horariosDia.groupingBy { it.salida }.eachCount()
            // Generamos todas las posibilidades y a ver cuales sirven...
            val conductoresDia = entradaConductor.keys.map { key ->
                SubSets(entradaConductor[key]!!, 1, entradaConductor[key]!!.size)
            }
            val combinarConductoresDia = Combinador(conductoresDia)

            //for (List<Set<Participante>> condDia : combinarConductoresDia) {
            for (condDia in combinarConductoresDia) {
                val selCond = condDia.flatMap { it }.filterNotNull().toSet()
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                val plazasIda = selCond.map { participanteHorario[it] }.groupBy{ it!!.entrada }.mapValues { it.value.sumBy { it!!.participante!!.plazasCoche }}
                val plazasVuelta = selCond.map { participanteHorario[it] }.groupBy { it!!.salida }.mapValues { it.value.sumBy { it!!.participante!!.plazasCoche }}

                if (nParticipantesIda.entries.all { e -> plazasIda.getOrDefault(e.key, 0) >= e.value }
                        && nParticipantesVuelta.entries.all{ e -> plazasVuelta.getOrDefault(e.key, 0) >= e.value }) {

                    // Obtenemos la lista de posibles lugares teniendo en cuenta quien es el conductor
                    val posiblesLugares = participantesDia.map{ it.puntosEncuentro }.plus(selCond.map{ it.puntosEncuentro })

                    var mejorCoste = Integer.MAX_VALUE
                    var mejorLugaresIda: Map<Participante, Lugar> = emptyMap()
                    var mejorLugaresVuelta: Map<Participante, Lugar> = emptyMap()

                    for (selLugares in Combinador(posiblesLugares)) {
                        val lugaresIda: MutableMap<Participante, Lugar> = HashMap()
                        val lugaresVuelta: MutableMap<Participante, Lugar> = HashMap()
                        val il = selLugares.subList(participantesDia.size, selLugares.size).iterator()
                        for (i in participantesDia.indices) {
                            lugaresIda.put(participantesDia[i], selLugares[i])
                            lugaresVuelta.put(participantesDia[i], if (selCond.contains(participantesDia[i])) il.next() else selLugares[i])
                        }
                        val plazasDisponiblesIda = selCond.groupBy { participanteHorario[it]!!.entrada }.mapValues { it.value.groupBy { lugaresIda[it]!! }.mapValues { it.value.sumBy { it.plazasCoche } } }
                        //.stream().collect(groupingBy({ p -> participanteHorario[p]!!.entrada }, groupingBy({ lugaresIda[it] }, summingInt({ it.plazasCoche }))))

                        val plazasDisponiblesVuelta = selCond.groupBy { participanteHorario[it]!!.salida }.mapValues { it.value.groupBy { lugaresVuelta[it]!! }.mapValues { it.value.sumBy { it.plazasCoche } } }
                        //.stream().collect(groupingBy({ p -> participanteHorario[p]!!.salida }, groupingBy({ lugaresVuelta[it]!! }, summingInt({ it.plazasCoche }))))
                        // Para comprobar, vemos los participantes, sus entradas y salidas
                        val plazasNecesariasIda = horariosDia.groupBy{ it.entrada }.mapValues { it.value.groupingBy { lugaresIda[it.participante]!! }.eachCount() }

                        val plazasNecesariasVuelta = horariosDia.groupBy{ it.salida }.mapValues { it.value.groupingBy { lugaresVuelta[it.participante]!! }.eachCount() }

                        if (plazasNecesariasIda.entries.all { e -> e.value.entries.all { ll -> ll.value <= plazasDisponiblesIda[e.key]!!.getOrDefault(ll.key, 0) } }
                                && plazasNecesariasVuelta.entries.all { e -> e.value.entries.all { ll -> ll.value <= plazasDisponiblesVuelta[e.key]!!.getOrDefault(ll.key, 0) } }) {
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
                        val asignacionDiaSimple = AsignacionDiaSimple(selCond, mejorLugaresIda, mejorLugaresVuelta, mejorCoste)
                        solucionesDia.add(asignacionDiaSimple)
                        mapaParticipantesAsignacionDia.put(selCond, asignacionDiaSimple) //Para acelerar la busqueda después
                    }
                }
            }
            Collections.sort(solucionesDia)
            solucionesCandidatas.put(d, ArrayList(solucionesDia))
            mapaParticipantesSoluciones.put(d, HashMap(mapaParticipantesAsignacionDia))
        }
    }
}
