/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.AsignacionDia
import es.um.josecefe.rueda.modelo.Dia
import es.um.josecefe.rueda.modelo.Horario
import java.util.concurrent.atomic.AtomicInteger

/**
 * ResolutorV8
 *
 *
 * Versión paralela del resolutor -> No implementa LNV, sino que sigue una
 * estrategia no guiada, solo podada (la estimación es innecesaria)
 *
 * @author josecefe
 */
class ResolutorV8 : ResolutorAcotado() {
    private var contexto : ContextoResolucion? = null
    private var tamanosNivel : IntArray? = null
    private var nPosiblesSoluciones : DoubleArray? = null
    private val estGlobal = EstadisticasV7()
    override var solucionFinal: Map<Dia, AsignacionDia> = emptyMap()
        private set
    private var cotaInferiorCorte: AtomicInteger? = null
    private var ultMilisEst: Long = 0 // La ultima vez que se hizo estadística

    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia> {
        return resolver(horarios, Integer.MAX_VALUE - 1)
    }

    override fun resolver(horarios: Set<Horario>, cotaInfCorte: Int): Map<Dia, AsignacionDia> {
        if (horarios.isEmpty()) {
            return emptyMap()
        }

        if (ESTADISTICAS) {
            estGlobal.inicia()
        }

        continuar = true

        val contexto1=ContextoResolucion(horarios)
        contexto = contexto1
        contexto1.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF
        contexto1.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF

        solucionFinal = emptyMap()

        tamanosNivel = contexto1.ordenExploracionDias.map { i -> contexto1.solucionesCandidatas[contexto1.dias[i]]!!.size }.toIntArray()
        val totalPosiblesSoluciones = tamanosNivel!!.map { i -> i.toDouble() }.fold(1.0) { a, b -> a * b }
        nPosiblesSoluciones = DoubleArray(tamanosNivel!!.size)
        if (ESTADISTICAS) {
            if (DEBUG) {
                println("Nº de posibles soluciones: " + tamanosNivel!!.map { it.toDouble().toString() }.joinToString(" * ") + " = "
                        + totalPosiblesSoluciones)
            }
            var acum = 1.0

            for (i in tamanosNivel!!.indices.reversed()) {
                nPosiblesSoluciones!![i] = acum
                acum *= tamanosNivel!![i]
            }
        }


        if (ESTADISTICAS) {
            estGlobal.totalPosiblesSoluciones = totalPosiblesSoluciones
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.tiempoString)
            }
        }

        // Preparamos el algoritmo
        val actual = Nodo(contexto1)
        var mejor = actual
        cotaInferiorCorte = AtomicInteger(cotaInfCorte + 1) //Lo tomamos como cota superior

        mejor = branchAndBound(actual, mejor) ?: mejor //Cogemos el nuevo mejor

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.fitness = mejor.costeEstimado
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println("====================")
                println("Estadísticas finales")
                println("====================")
                println(estGlobal)
                println("Solución final=" + mejor)
                println("-----------------------------------------------")
            }
        }
        // Construimos la solución final
        if (mejor.costeEstimado < cotaInfCorte) {
            solucionFinal = mejor.solucion
        } else {
            solucionFinal = emptyMap()
        }

        return solucionFinal
    }

    private fun branchAndBound(actual: Nodo, mejorPadre: Nodo): Nodo? {
        var mejor = mejorPadre
        if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS_EST == 0L && System.currentTimeMillis() - ultMilisEst > CADA_MILIS_EST) {
            ultMilisEst = System.currentTimeMillis()
            estGlobal.fitness = cotaInferiorCorte!!.get()
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println(estGlobal)
                println("-- Trabajando con " + actual)
            }
        }
        if (actual.cotaInferior < cotaInferiorCorte!!.get() && continuar) { //Estrategia de poda: si la cotaInferior >= C no seguimos
            if (ESTADISTICAS) {
                estGlobal.addGenerados(tamanosNivel!![actual.nivel + 1].toDouble())
            }
            if (actual.nivel + 2 == contexto!!.dias.size) { // Los hijos son terminales
                if (ESTADISTICAS) {
                    estGlobal.addTerminales(tamanosNivel!![actual.nivel + 1].toDouble())
                }
                val mejorHijo = actual.generaHijos().min()

                if (mejorHijo!=null && mejorHijo < mejor) {
                    mejor = mejorHijo
                    var cota: Int
                    do {
                        cota = cotaInferiorCorte!!.get()
                    } while (mejor.costeEstimado < cota && !cotaInferiorCorte!!.compareAndSet(cota, mejor.costeEstimado))
                    if (mejor.costeEstimado < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.fitness = mejor.costeEstimado
                            estGlobal.actualizaProgreso()
                        }
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor)
                            System.out.format("** Nuevo (nueva solución) C: Anterior=%,d, Nuevo=%,d\n", cota, mejor.costeEstimado)
                        }
                    }
                }
            } else { // Es un nodo intermedio
                val lNF = actual.generaHijos().filter { n -> n.cotaInferior < cotaInferiorCorte!!.get() }.toMutableList()
                val menorCotaSuperior = lNF.map{ it.cotaSuperior }.min()
                if (menorCotaSuperior != null) {
                    var cota: Int
                    do {
                        cota = cotaInferiorCorte!!.get()
                    } while (menorCotaSuperior< cota && !cotaInferiorCorte!!.compareAndSet(cota, menorCotaSuperior))
                    if (menorCotaSuperior < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.fitness = menorCotaSuperior
                            estGlobal.actualizaProgreso()
                        }
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cota, menorCotaSuperior)
                        }
                    }
                    lNF.removeAll { n -> n.cotaInferior >= cotaInferiorCorte!!.get() } //Recalculamos lNF
                    // Limpiamos la LNV
                }
                if (ESTADISTICAS) {
                    estGlobal.addDescartados((tamanosNivel!![actual.nivel + 1] - lNF.size) * nPosiblesSoluciones!![actual.nivel + 1])
                }
                val mejorAhora = mejor
                val mejorHijo = lNF.sorted().map { n -> branchAndBound(n, mejorAhora) }.filterNotNull().min()
                if (mejorHijo!=null && mejorHijo < mejor) { // Tenemos un hijo que mejora
                    mejor = mejorHijo
                }
            }
        } else if (ESTADISTICAS) {
            if (continuar) {
                estGlobal.addDescartados(nPosiblesSoluciones!![actual.nivel])
            }
        }

        return mejor
    }

    override val estadisticas: Estadisticas
        get() = estGlobal

    override var estrategia = Estrategia.EQUILIBRADO

    companion object {

        private const val DEBUG = false

        //private const val PARALELO = false

        private const val ESTADISTICAS = true
        private const val CADA_EXPANDIDOS_EST = 1000
        private const val CADA_MILIS_EST = 1000

        private const val PESO_COTA_INFERIOR_NUM_DEF = 1
        private const val PESO_COTA_INFERIOR_DEN_DEF = 2
    }
}
