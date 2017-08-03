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
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

/**
 * ResolutorV7
 *
 *
 * Se trata de un resolutor que aplica la tecnica de Ramificación y poda (B&B).
 *
 * @author josecefe
 */
class ResolutorV7 : ResolutorAcotado() {
    override var estrategia = Estrategia.EQUILIBRADO
    private val estGlobal = EstadisticasV7()

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

        var ultMilisEst: Long = 0 // La ultima vez que se hizo estadística

        continuar = true

        val contex = ContextoResolucion(horarios)
        contex.estrategia = estrategia

        solucionFinal = emptyMap()

        val tamanosNivel= IntStream.of(*contex.ordenExploracionDias).map { i -> contex.solucionesCandidatas[contex.dias[i]]!!.size }.toArray()
        val totalPosiblesSoluciones = IntStream.of(*tamanosNivel!!).mapToDouble { i -> i.toDouble() }.reduce(1.0) { a, b -> a * b }
        val nPosiblesSoluciones = DoubleArray(tamanosNivel.size)

        if (ESTADISTICAS) {

            if (DEBUG) {
                println("Nº de posibles soluciones: " + IntStream.of(*tamanosNivel).mapToObj{java.lang.Double.toString(it.toDouble())}.collect(Collectors.joining(" * ")) + " = "
                        + totalPosiblesSoluciones)
            }
            var acum = 1.0

            for (i in tamanosNivel.indices.reversed()) {
                nPosiblesSoluciones[i] = acum
                acum *= tamanosNivel[i]
            }
            estGlobal.totalPosiblesSoluciones = totalPosiblesSoluciones
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.tiempoString)
            }
        }
        // Preparamos el algoritmo
        val RAIZ = Nodo(contex)
        var actual = RAIZ
        var mejor = actual
        var cotaInferiorCorte = if (cotaInfCorte < Integer.MAX_VALUE) cotaInfCorte + 1 else cotaInfCorte
        var LNV: MutableCollection<Nodo>
        var opPull: Supplier<Nodo>
        contex.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF_INI //Primero buscamos en profundidad
        contex.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF_INI //Primero buscamos en profundidad
        if (CON_CAMBIO_DE_ESTRATEGIA) {
            val pilaNodosVivos = ArrayDeque<Nodo>() // Inicialmente es una cola LIFO (pila)
            LNV = pilaNodosVivos
            opPull = Supplier { pilaNodosVivos.removeLast() } //Para controlar si es pila o cola, inicialmente pila
        } else {
            val colaNodosVivos = PriorityQueue<Nodo>()
            LNV = colaNodosVivos
            opPull = Supplier { colaNodosVivos.poll() }
        }
        LNV.add(actual)

        // Bucle principal
        do {
            actual = opPull.get()
            if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS == 0L && System.currentTimeMillis() - ultMilisEst > CADA_MILIS_EST) {
                ultMilisEst = System.currentTimeMillis()
                estGlobal.fitness = cotaInferiorCorte
                estGlobal.actualizaProgreso()
                if (DEBUG) {
                    System.out.format("> LNV=%,d, ", LNV.size)
                    println(estGlobal)
                    println("-- Trabajando con " + actual)
                }
            }
            if (actual.cotaInferior < cotaInferiorCorte) { //Estrategia de poda: si la cotaInferior >= C no seguimos
                if (ESTADISTICAS) {
                    estGlobal.addGenerados(tamanosNivel[actual.nivel + 1].toDouble())
                }
                if (actual.nivel + 2 == contex.dias.size) { // Los hijos son terminales
                    if (ESTADISTICAS) {
                        estGlobal.addTerminales(tamanosNivel[actual.nivel + 1].toDouble())
                    }
                    val mejorHijo = actual.generaHijos(PARALELO).min()
                    if (mejorHijo!=null && mejorHijo < mejor) {
                        if (mejor === RAIZ) {
                            // Cambiamos los pesos
                            contex.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF_FIN //Después buscamos más equilibrado
                            contex.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF_FIN //Después buscamos más equilibrado
                            LNV.forEach{ it.calculaCosteEstimado() }// Hay que forzar el calculo de nuevo de los costes de los nodos
                            val colaNodosVivos: PriorityQueue<Nodo> = PriorityQueue(LNV.size)
                            colaNodosVivos.addAll(LNV)
                            if (DEBUG) {
                                println("---- ACTUALIZANDO LA LNV POR CAMBIO DE PESOS")
                            }
                            opPull = Supplier { colaNodosVivos.poll() }
                            LNV = colaNodosVivos
                        }

                        mejor = mejorHijo
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor)
                        }
                        if (mejor.costeEstimado < cotaInferiorCorte) {
                            if (DEBUG) {
                                System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cotaInferiorCorte, mejor.costeEstimado)
                            }
                            cotaInferiorCorte = mejor.costeEstimado
                            val fC = cotaInferiorCorte

                            // Limpiamos la lista de nodos vivos de los que no cumplan...
                            val antes = LNV.size
                            if (ESTADISTICAS) {
                                estGlobal.addDescartados(LNV.filter { n -> n.cotaInferior >= fC }.map { n -> nPosiblesSoluciones[n.nivel] }.sum())
                                estGlobal.fitness = cotaInferiorCorte
                                estGlobal.actualizaProgreso()
                            }
                            val removeIf = LNV.removeIf { n -> n.cotaInferior >= fC }
                            if (ESTADISTICAS && DEBUG && removeIf) {
                                System.out.format("** Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size)
                            }
                        }
                    }
                } else { // Es un nodo intermedio
                    val Corte = cotaInferiorCorte
                    val lNF = actual.generaHijos(PARALELO).filter { n -> n.cotaInferior < Corte }.toMutableList()
                    val menorCotaSuperior = lNF.map{ it.cotaSuperior }.min()
                    if (menorCotaSuperior!=null && menorCotaSuperior < cotaInferiorCorte) { // Mejora de C
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cotaInferiorCorte, menorCotaSuperior)
                        }
                        cotaInferiorCorte = menorCotaSuperior //Actualizamos C
                        val fC = cotaInferiorCorte
                        lNF.removeAll { n -> n.cotaInferior >= fC } //Recalculamos lNF
                        // Limpiamos la LNV
                        val antes = LNV.size
                        if (ESTADISTICAS) {
                            estGlobal.addDescartados(LNV.filter { n -> n.cotaInferior >= fC }.map { n -> nPosiblesSoluciones[n.nivel] }.sum())
                            estGlobal.fitness = cotaInferiorCorte
                            estGlobal.actualizaProgreso()
                        }
                        val removeIf = LNV.removeAll { n -> n.cotaInferior >= fC }
                        if (ESTADISTICAS && DEBUG && removeIf) {
                            System.out.format("## Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size)
                        }
                    }
                    if (ESTADISTICAS) {
                        estGlobal.addDescartados((tamanosNivel[actual.nivel + 1] - lNF.size) * nPosiblesSoluciones[actual.nivel + 1])
                    }
                    LNV.addAll(lNF)
                }
            } else if (ESTADISTICAS) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.nivel])
            }
        } while (!LNV.isEmpty() && continuar)

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.fitness = cotaInferiorCorte
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

    override var solucionFinal: Map<Dia, AsignacionDia> = emptyMap()
        private set

    override val estadisticas: Estadisticas
        get() = estGlobal

    companion object {
        private const val DEBUG = false
        private const val PARALELO = false
        private const val ESTADISTICAS = true
        private const val CADA_EXPANDIDOS = 1000
        private const val CADA_MILIS_EST = 1000
        private const val CON_CAMBIO_DE_ESTRATEGIA = false
        private const val PESO_COTA_INFERIOR_NUM_DEF_INI = 1
        private const val PESO_COTA_INFERIOR_DEN_DEF_INI = 8
        private const val PESO_COTA_INFERIOR_NUM_DEF_FIN = 1
        private const val PESO_COTA_INFERIOR_DEN_DEF_FIN = 2
    }
}
