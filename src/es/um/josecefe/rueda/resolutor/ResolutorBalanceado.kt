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
import es.um.josecefe.rueda.util.summarizingInt
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicStampedReference
import kotlin.math.max
import kotlin.math.roundToInt

class ResolutorBalanceado : Resolutor() {
    companion object {
        private const val DEBUG = true
        private const val ESTADISTICAS = true
        private const val CADA_EXPANDIDOS_EST = 1000000
        private const val UMBRAL_PARALELO = 1000
    }

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

        val contexto = ContextoResolucionExahustivo(horarios)

        /* Fin inicialización */

        if (ESTADISTICAS) {
            estGlobal.totalPosiblesSoluciones = 0.0
            estGlobal.actualizaProgreso()
            if (DEBUG) println("Tiempo inicializar = ${estGlobal.tiempoString}")
        }

        val solucionRef = AtomicStampedReference(solucionFinal, Int.MAX_VALUE)
        // SECUENCIAL: var mejorCoste = Double.POSITIVE_INFINITY

        //Vamos a ir buscando soluciones de menos veces al tope de veces
        for (nivel in contexto.minCond..contexto.dias.size) {
            if (solucionRef.stamp < Int.MAX_VALUE) {
                //SECUENCIAL: if (!mejorCoste.isInfinite()) {
                if (ESTADISTICAS) estGlobal.actualizaProgreso()
                if (DEBUG) println("** No examinamos el nivel $nivel al tener ya una solución de un nivel anterior **")
                break
            } // Hemos terminado

            if (!continuar) {
                if (DEBUG) println("*** No vamos a examinar el nivel $nivel al haberse interrumpido la resolución ***")
                break
            }

            if (DEBUG) println("* Iniciando examen del nivel $nivel con un total de ${contexto.getSolucionesNivel(
                    nivel)} en este nivel *")
            if (ESTADISTICAS) estGlobal.totalPosiblesSoluciones += contexto.getSolucionesNivel(nivel)

            val listSubcon: MutableList<List<Set<Dia>>> = ArrayList(contexto.mapParticipanteDias.size)
            for ((key, value) in contexto.mapParticipanteDias) {
                val numVecesCond = max(1, (nivel.toDouble() / contexto.coefConduccion.getValue(key)).roundToInt()) - (contexto.diasFijos[key]?.size ?: 0)//Para minorar adecuadamente el valor de i usando el coeficiente de conductor
                val diasCambiantes: Set<Dia> = if (contexto.diasFijos[key] != null) value.minus(
                        contexto.diasFijos.getValue(key)) else value
                listSubcon.add(SubSets(diasCambiantes, numVecesCond, numVecesCond).map {
                    if (contexto.diasFijos[key] != null) contexto.diasFijos.getValue(key).plus(it) else it
                }.toList())
            }
            val combinaciones: Combinador<Set<Dia>> = Combinador(listSubcon)
            if (DEBUG) println("   ---> Soluciones a generar del nivel $nivel: ${combinaciones.size}")
            var contador = 0L
            val valida = AtomicLong()

            runBlocking {
                val deferreds: MutableList<Deferred<Unit>> = mutableListOf()
                for (c in combinaciones) {
                    if (DEBUG) contador++
                    if (!continuar) break
                    deferreds.add(async {

                        val asignacion: MutableMap<Dia, MutableSet<Participante>> = mutableMapOf()

                        val participaIt = contexto.mapParticipanteDias.keys.iterator()
                        for (e: Set<Dia> in c) {
                            val p = participaIt.next()

                            for (dia in e) {
                                var participantesSet = asignacion[dia]
                                if (participantesSet == null) {
                                    participantesSet = HashSet()
                                    asignacion[dia] = participantesSet
                                }
                                participantesSet.add(p)
                            }
                        }
                        if (asignacion.size < contexto.solucionesCandidatas.size) {
                            if (ESTADISTICAS) {
                                estGlobal.incExpandidos()
                                estGlobal.addDescartados(1.0)
                            }
                            return@async //continue
                        }
                        val solCand = mutableMapOf<Dia, AsignacionDiaExahustiva>()
                        for ((dia, participantesDia) in asignacion) {
                            val sol = contexto.mapaParticipantesSoluciones[dia]?.get(
                                    participantesDia) ?: break // Aquí se queda en blanco, luego no sirve
                            solCand[dia] = sol
                        }

                        // Para ver si es valida bastaria con ver si hay solución en cada día
                        if (solCand.size == contexto.solucionesCandidatas.size) {
                            if (DEBUG) valida.incrementAndGet()
                            if (ESTADISTICAS) estGlobal.addTerminales(1.0)

                            val posibilidades = Combinador(solCand.map { (dia, asignaciones) ->
                                asignaciones.otrosPosiblesLugares.map {
                                    Pair(dia, it)
                                }
                            })
                            for (posible in posibilidades) {
                                val t = contexto.participantes.associateWith { participaIt1 ->
                                    posible.mapNotNull {
                                        if (it.second[participaIt1] != null) Triple(it.first,
                                                it.second[participaIt1]?.first,
                                                it.second[participaIt1]?.second) else null
                                    }
                                }
                                val tp = t.mapValues { (participante, value) ->
                                    value.sumBy { (dia, ida, vuelta) ->
                                        (participante.puntosEncuentro.indexOf(
                                                ida) + participante.puntosEncuentro.indexOf(
                                                vuelta)) * (if (solCand[dia]!!.conductores.contains(
                                                participante)) PESO_LUGAR_CONDUCTOR else PESO_LUGAR_PASAJERO)
                                    }
                                }
                                val dif = tp.values.summarizingInt()
                                val apt = ((dif.avg + dif.max) * 1000.0).toInt()
                                var costeAnt = solucionRef.stamp

                                if (apt < costeAnt) {
                                    val iteradorPosible = posible.iterator()
                                    val solNueva = solCand.mapValues { (_, asignacionCompleta) ->
                                        AsignacionDiaExahustiva(asignacionCompleta.conductores,
                                                listOf(iteradorPosible.next().second), dif.sum)
                                    }
                                    var solAnt: Map<Dia, AsignacionDia>
                                    do {
                                        costeAnt = solucionRef.stamp
                                        solAnt = solucionRef.reference
                                    } while (apt < costeAnt && !solucionRef.compareAndSet(solAnt, solNueva, costeAnt,
                                            apt))
                                    if (apt < costeAnt) {
                                        if (DEBUG) println(
                                                "---> Encontrada una mejora: Coste anterior = $costeAnt, nuevo coste = ${solucionRef.stamp}, sol = ${solucionRef.reference}")
                                        if (ESTADISTICAS) {
                                            estGlobal.fitness = solucionRef.stamp
                                            estGlobal.actualizaProgreso()
                                        }
                                    } else {
                                        if (DEBUG) println(
                                                "---> Descartada una mejora por concurrencia: Coste anterior = $costeAnt, nuevo coste = ${solucionRef.stamp}, sol = ${solucionRef.reference}")
                                    }
                                }
                            }

                        } else {
                            if (ESTADISTICAS) estGlobal.addDescartados(1.0)
                        }
                        if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS_EST == 0L) {
                            estGlobal.actualizaProgreso()
                            if (DEBUG) println(estGlobal)
                        }
                    })
                    if (deferreds.size > UMBRAL_PARALELO) {
                        deferreds.forEach { it.await() }
                        deferreds.clear()
                    }
                }
                if (deferreds.isNotEmpty()) {
                    deferreds.forEach { it.await() }
                    deferreds.clear()
                }
            }
            if (DEBUG) println(
                    "   ---> $contador combinaciones generadas en este nivel, de las cuales $valida han sido validas")
        }

        solucionFinal = solucionRef.reference

        //Estadisticas finales
        if (ESTADISTICAS) {
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

    override val estadisticas: Estadisticas
        get() = estGlobal

    override var estrategia
        get() = Estrategia.EQUILIBRADO
        set(@Suppress("UNUSED_PARAMETER") value) { /* Nada */
        }
}