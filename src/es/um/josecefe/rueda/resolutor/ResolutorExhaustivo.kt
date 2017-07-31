/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.*
import es.um.josecefe.rueda.util.combinations
import java.util.*
import java.util.concurrent.atomic.AtomicStampedReference
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

private const val DEBUG = true
private const val ESTADISTICAS = true
private const val CADA_EXPANDIDOS_EST = 1000

class ResolutorExhaustivo : Resolutor() {
    private val estGlobal = EstadisticasV8()
    override var solucionFinal: Map<Dia, AsignacionDia> = emptyMap()
        private set

    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia> {
        solucionFinal = emptyMap()

        if (horarios.isEmpty()) {
            return solucionFinal
        }

        if (ESTADISTICAS) {
            estGlobal.inicia()
        }

        /* Inicialización */
        continuar = true

        val contexto = ContextoResolucionSimple(horarios)

        val diasFijos: HashMap<Participante, MutableSet<Dia>> = HashMap()
        for ((dia, solDia) in contexto.solucionesCandidatas) {
            val conductores: MutableSet<Participante> = contexto.participantes.toMutableSet()
            for (asignacion: AsignacionDia in solDia) conductores.retainAll(asignacion.conductores)
            conductores.forEach { c -> diasFijos.computeIfAbsent(c, { HashSet() }).add(dia) }
        }
        val minCond = Math.max(1, diasFijos.values.map { it.size }.max()?.toInt() ?: 0)

        val mapParticipanteDias = LinkedHashMap<Participante, Set<Dia>>()
        for (i in contexto.participantes.indices) {
            if (contexto.participantes[i].plazasCoche > 0) {
                val p: Participante = contexto.participantes[i]
                val setDias = horarios.filter { h -> h.coche && h.participante == p }.map { h -> h.dia }.filterNotNull().toSet()
                mapParticipanteDias[p] = setDias
            }
        }

        val vacio = emptySet<Dia>().toMutableSet()

        var totalPosiblesSoluciones = 0.0
        if (ESTADISTICAS) {
            for (i in minCond..contexto.dias.size) {
                var comb = 1.0
                if (DEBUG) print("Para $i veces conducir por participante tenemos ")
                for ((key, value) in mapParticipanteDias) {
                    val numVecesCond = Math.max(1, (i.toFloat() / contexto.coefConduccion[key]!! + 0.5f).toInt()) //Para minorar adecuadamente el valor de i usando el coeficiente de conductor
                    val porDiasFijos = diasFijos.getOrDefault(key, vacio).size.toLong()
                    //if (porDiasFijos >= numVecesCond) continue
                    val combinations = combinations(Math.max(1, value.size.toLong() - porDiasFijos), Math.max(1, numVecesCond - porDiasFijos))
                    if (DEBUG) print(" * $combinations")
                    comb *= combinations
                }
                if (DEBUG) println(" = $comb")
                totalPosiblesSoluciones += comb
            }
            if (DEBUG) println("Nº total de posibles soluciones: $totalPosiblesSoluciones")
        }
        /* Fin inicialización */

        if (ESTADISTICAS) {
            estGlobal.totalPosiblesSoluciones = totalPosiblesSoluciones
            estGlobal.actualizaProgreso()
            if (DEBUG) println("Tiempo inicializar = ${estGlobal.tiempoString}")
        }

        val solucionRef = AtomicStampedReference(solucionFinal, Int.MAX_VALUE)

        //Vamos a ir buscando soluciones de menos veces al tope de veces
        bucledias@ for (i in minCond..contexto.dias.size) {
            if (solucionRef.stamp < Int.MAX_VALUE) {
                if (ESTADISTICAS) {
                    estGlobal.terminales.add(estGlobal.expandidos.toDouble() - estGlobal.terminales.toDouble())
                    estGlobal.addDescartados(totalPosiblesSoluciones - estGlobal.expandidos.get())
                }

                break
            } // Hemos terminado

            if (DEBUG) println("*** Probando soluciones de nivel $i...")
            if (!continuar) break
            val listSubcon: MutableList<Iterable<Set<Dia>>> = ArrayList(mapParticipanteDias.size)
            for ((key, value) in mapParticipanteDias) {
                val numVecesCond = Math.max(1, Math.round(i.toDouble() / contexto.coefConduccion[key]!!).toInt()) - diasFijos.getOrDefault(key, emptySet<Dia>().toMutableSet()).size //Para minorar adecuadamente el valor de i usando el coeficiente de conductor
                val diasCambiantes: Set<Dia> = if (diasFijos[key] != null) value.minus(diasFijos[key]!!) else value
                listSubcon.add(SubSets(diasCambiantes, numVecesCond, numVecesCond).map { if (diasFijos[key] != null) diasFijos[key]!!.plus(it) else it }.toList())
            }
            val combinaciones: Combinador<Set<Dia>> = Combinador(listSubcon)

            //TODO: Habría que ver las implicaciones del paralelismos, especialmente en las variables compartidas
            combinaciones.parallelStream().forEach { c ->
                if (!continuar) return@forEach
                val asignacion: MutableMap<Dia, MutableSet<Participante>> = HashMap()

                val participaIt = mapParticipanteDias.keys.iterator()
                for (e: Set<Dia> in c) {
                    val p = participaIt.next()
                    e.map { asignacion.computeIfAbsent(it) { HashSet() } }.forEach { it.add(p) }
                }
                if (asignacion.size < contexto.solucionesCandidatas.size) {
                    if (ESTADISTICAS) estGlobal.incExpandidos()
                    return@forEach
                }
                val solCand = HashMap<Dia, AsignacionDiaSimple>()
                for ((key, value) in asignacion) {
                    val sol = contexto.mapaParticipantesSoluciones.get(key)?.get(value)

                    if (sol == null) {
                        if (ESTADISTICAS) estGlobal.incExpandidos()
                        return@forEach // Aquí se queda en blanco, luego no sirve
                    }

                    solCand[key] = sol
                }

                // Para ver si es valida bastaria con ver si hay solución en cada día
                if (solCand.size == contexto.solucionesCandidatas.size) {
                    val apt = calculaAptitud(solCand, i)
                    var costeAnt: Int
                    var solAnt: Map<Dia, AsignacionDia>
                    do {
                        costeAnt = solucionRef.stamp
                        solAnt = solucionRef.reference
                    } while (apt < costeAnt && !solucionRef.compareAndSet(solAnt, solCand, costeAnt, apt))
                    if (apt < costeAnt) {
                        if (DEBUG) println("---> Encontrada una mejora: Coste anterior = $costeAnt, nuevo coste = ${solucionRef.stamp}, sol = ${solucionRef.reference}")
                        if (ESTADISTICAS) {
                            estGlobal.fitness = solucionRef.stamp; estGlobal.actualizaProgreso()
                        }
                    }
                }
                if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS_EST == 0L) {
                    estGlobal.terminales.add(estGlobal.expandidos.toDouble() - estGlobal.terminales.toDouble())
                    estGlobal.actualizaProgreso()
                    if (DEBUG) println(estGlobal)
                }
            }
        }

        solucionFinal = solucionRef.reference

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.fitness = solucionRef.stamp
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println("====================")
                println("Estadísticas finales")
                println("====================")
                println(estGlobal)
                println("Solución final=$solucionFinal")
                println("-----------------------------------------------")
            }
        }

        return solucionFinal
    }

    private fun calculaAptitud(sol: Map<Dia, AsignacionDia>, vecesCond: Int) = vecesCond * PESO_MAXIMO_VECES_CONDUCTOR + sol.values.map { it.coste }.sum()

    override val estadisticas: Estadisticas
        get() = estGlobal

    override var estrategia
        get() = Resolutor.Estrategia.EQUILIBRADO
        set(value) { /* Nada */}
}
