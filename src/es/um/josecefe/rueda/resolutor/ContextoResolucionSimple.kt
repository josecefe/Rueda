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
import es.um.josecefe.rueda.util.combinations
import java.util.*
import kotlin.math.max

/**
 * @author josecefe
 */
internal class ContextoResolucionSimple(horarios: Set<Horario>) {
    val dias: List<Dia> = horarios.map { it.dia }.distinct().sorted()
    val participantes: List<Participante> = horarios.map { it.participante }.distinct().sorted()
    val solucionesCandidatas: MutableMap<Dia, List<AsignacionDiaSimple>>
    val mapaParticipantesSoluciones: MutableMap<Dia, Map<Set<Participante>, AsignacionDiaSimple>>
    val coefConduccion: Map<Participante, Double> //Que tanto por 1 supone que use el coche cada conductor
    val diasFijos: Map<Participante, MutableSet<Dia>>
    val minCond: Int
    val mapParticipanteDias: Map<Participante, Set<Dia>>
    val totalPosiblesSoluciones: Long
    private val solucionesNivel: LongArray

    init {
        // Y aquí ajustamos el coeficiente que nos mide su grado de participación (gente que solo va parte de los días)
        val vecesParticipa = horarios.groupingBy { it.participante }.eachCount()
        val maxVecesParticipa = vecesParticipa.values.max() ?: 0
        coefConduccion = participantes.associateWith { maxVecesParticipa.toDouble() / (vecesParticipa[it]?.toDouble() ?: 0.0) - 0.001 }
        // Vamos a trabajar día a día
        //Para parelizar: solucionesCandidatas = dias.stream().collect(toMap(ind -> ind, d -> {
        solucionesCandidatas = LinkedHashMap()
        mapaParticipantesSoluciones = HashMap()
        for (d: Dia in dias) {
            val solucionesDia = mutableListOf<AsignacionDiaSimple>() //Para paralelizar usar Vector en luar de ArrayList
            val mapaParticipantesAsignacionDia = mutableMapOf<Set<Participante>, AsignacionDiaSimple>() //Para paralelizar usar ConcurrentHashMap
            val horariosDia = horarios.filter { it.dia == d }.toSet()
            val participanteHorario = horariosDia.associateBy { it.participante }
            //val participantesDia = horariosDia.map { it.participante }.sortedBy { it.nombre }

            // Para cada hora de entrada, obtenemos los conductores disponibles
            val entradaConductor = horariosDia.filter { it.coche }.groupBy({ it.entrada }, { it.participante })
            // Para comprobar, vemos los participantes, sus entradas y salidas
            val nParticipantesIda = horariosDia.groupingBy { it.entrada }.eachCount()

            val nParticipantesVuelta = horariosDia.groupingBy { it.salida }.eachCount()
            // Generamos todas las posibilidades y a ver cuales sirven...
            val conductoresDia = entradaConductor.keys.map { key ->
                SubSets(entradaConductor.getValue(key), 1, entradaConductor.getValue(key).size).toList()
            }
            val combinarConductoresDia = Combinador(conductoresDia)

            //for (List<Set<Participante>> condDia : combinarConductoresDia) {
            for (condDia in combinarConductoresDia) {
                val selCond = condDia.flatten().toSortedSet()
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                val plazasIda = selCond.map { participanteHorario[it] }.groupBy { it!!.entrada }.mapValues { it -> it.value.sumBy { it!!.participante.plazasCoche } }
                val plazasVuelta = selCond.map { participanteHorario[it] }.groupBy { it!!.salida }.mapValues { it -> it.value.sumBy { it!!.participante.plazasCoche } }

                if (nParticipantesIda.entries.all { e -> plazasIda[e.key] ?: 0 >= e.value }
                        && nParticipantesVuelta.entries.all { e -> (plazasVuelta[e.key] ?: 0) >= e.value }) {

                    val asignacionDiaSimple = AsignacionDiaSimple(selCond)
                    solucionesDia.add(asignacionDiaSimple)
                    mapaParticipantesAsignacionDia[selCond] = asignacionDiaSimple //Para acelerar la busqueda después
                }
            }
            solucionesDia.sort()
            solucionesCandidatas[d] = solucionesDia
            mapaParticipantesSoluciones[d] = mapaParticipantesAsignacionDia
        }

        /* Averiguamos los días fijos de cada participantes (aquellos días que forzosamente tiene que llevarse el coche) */
        val diasFijosl: MutableMap<Participante, MutableSet<Dia>> = mutableMapOf()
        for ((dia, solDia) in solucionesCandidatas) {
            val conductores: MutableSet<Participante> = participantes.toMutableSet()
            for (asignacion: AsignacionDia in solDia) conductores.retainAll(asignacion.conductores)
            for (c in conductores) {
                var d = diasFijosl[c]
                if (d == null) {
                    d = HashSet()
                    diasFijosl[c] = d
                }
                d.add(dia)
            }
        }

        diasFijos = diasFijosl.toMap()
        /* Averiguamos cual es el minimo de veces que se debe llevar el coche cada uno a partir del máximo de dias fijos */
        minCond = max(1, diasFijos.values.map { it.size }.max()?.toInt() ?: 0)

        mapParticipanteDias = LinkedHashMap<Participante, Set<Dia>>()
        for (i in participantes.indices) {
            if (participantes[i].plazasCoche > 0) {
                val p: Participante = participantes[i]
                val setDias = horarios.filter { h -> h.coche && h.participante == p }.map { h -> h.dia }.toSet()
                mapParticipanteDias[p] = setDias
            }
        }

        var posiblesSoluciones = 0L
        solucionesNivel = LongArray(dias.size - minCond + 1)

        for (nivel in minCond..dias.size) {
            var comb: Long = 1
            for ((key, value) in mapParticipanteDias) {
                val numVecesCond = max(1,
                        (nivel.toFloat() / coefConduccion.getValue(key) + 0.5f).toInt()) //Para minorar adecuadamente el nivel usando el coeficiente de conductor
                val porDiasFijos = (diasFijos[key]?.size ?: 0).toLong()
                val combinations = combinations(max(1, value.size.toLong() - porDiasFijos),
                        max(0, numVecesCond - porDiasFijos))
                comb *= combinations
            }
            solucionesNivel[nivel - minCond] = comb
            posiblesSoluciones += comb
        }
        totalPosiblesSoluciones = posiblesSoluciones
    }

    fun getSolucionesNivel(nivel: Int) = solucionesNivel[nivel - minCond]
}