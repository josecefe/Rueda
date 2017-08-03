/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.AsignacionDiaV5
import es.um.josecefe.rueda.modelo.Dia
import java.util.*

/**
 * Nodo
 * Clase que se usa por diversor Resolutores como nodo del árbol de busqueda.
 *
 * @author josecefe
 */
internal class Nodo : Comparable<Nodo> {
    companion object {
        private const val DEBUG = true
    }

    private val padre: Nodo?
    private val eleccion: AsignacionDiaV5?
    private val vecesConductor: ByteArray
    val nivel: Int
    private val costeAcumulado: Int
    val cotaInferior: Int
    val cotaSuperior: Int
    private val contexto: ContextoResolucion
    var costeEstimado: Int = 0
        private set

    constructor(contexto: ContextoResolucion) {
        this.contexto = contexto
        padre = null
        eleccion = null
        vecesConductor = ByteArray(contexto.participantes.size)
        cotaInferior = 0
        costeAcumulado = cotaInferior
        cotaSuperior = Integer.MAX_VALUE
        costeEstimado = cotaSuperior
        nivel = -1
    }

    private constructor(padre: Nodo, nuevaAsignacion: AsignacionDiaV5, contexto: ContextoResolucion) {
        this.contexto = contexto
        this.padre = padre
        nivel = padre.nivel + 1
        eleccion = nuevaAsignacion
        costeAcumulado = padre.costeAcumulado + nuevaAsignacion.coste
        vecesConductor = Arrays.copyOf(padre.vecesConductor, padre.vecesConductor.size)
        val conductores = nuevaAsignacion.conductoresArray
        var maximo = 0
        var maxCS = 0
        var maxCI = 0
        var total = 0
        var totalMinRes = 0
        var totalMaxRes = 0
        var minimo = Integer.MAX_VALUE
        var minCI = Integer.MAX_VALUE
        var minCS = Integer.MAX_VALUE
        val terminal = contexto.dias.size == nivel + 1
        //int sum = 0;
        //nuevaAsignacion.getConductores().stream().forEachOrdered(ic -> ++vecesConductor[ic]);
        for (i in vecesConductor.indices) {
            if (contexto.participantesConCoche[i]) {
                //sum = vecesConductor[i];
                if (conductores[i]) {
                    ++vecesConductor[i]
                }
                total += vecesConductor[i].toInt()
                val vecesConductorVirt = (vecesConductor[i].toFloat() * contexto.coefConduccion[i] + 0.5f).toInt()
                if (vecesConductorVirt > maximo) {
                    maximo = vecesConductorVirt
                }
                if (vecesConductorVirt < minimo) {
                    minimo = vecesConductorVirt
                }
                if (!terminal) {
                    val vecesConductorVirtCS = (((vecesConductor[i] + contexto.maxVecesCondDia[contexto.ordenExploracionDias[nivel + 1]][i]) * contexto.coefConduccion[i]) + 0.5f).toInt()
                    if (vecesConductorVirtCS > maxCS) {
                        maxCS = vecesConductorVirtCS
                    }
                    if (vecesConductorVirtCS < minCS) {
                        minCS = vecesConductorVirtCS
                    }
                    val vecesConductorVirtCI = (((vecesConductor[i] + contexto.minVecesCondDia[contexto.ordenExploracionDias[nivel + 1]][i]) * contexto.coefConduccion[i]) + 0.5f).toInt()
                    if (vecesConductorVirtCI > maxCI) {
                        maxCI = vecesConductorVirtCI
                    }
                    if (vecesConductorVirtCI < minCI) {
                        minCI = vecesConductorVirtCI
                    }
                    if (contexto.estrategia == Resolutor.Estrategia.MINCONDUCTORES) {
                        totalMaxRes += contexto.maxVecesCondDia[contexto.ordenExploracionDias[nivel + 1]][i]
                        totalMinRes += contexto.minVecesCondDia[contexto.ordenExploracionDias[nivel + 1]][i]
                    }
                }
            }
        }
        if (terminal) {
            //Es terminal
            if (contexto.estrategia == Resolutor.Estrategia.EQUILIBRADO) {
                cotaInferior = maximo * PESO_MAXIMO_VECES_CONDUCTOR + (maximo - minimo) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado
                cotaSuperior = cotaInferior
            } else {
                // estrategia == Estrategia.MINCONDUCTORES
                cotaInferior = maximo * PESO_MAXIMO_VECES_CONDUCTOR + total * PESO_TOTAL_CONDUCTORES + (maximo - minimo) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado
                cotaSuperior = cotaInferior
            }
        } else if (contexto.estrategia == Resolutor.Estrategia.EQUILIBRADO) {
            cotaInferior = maxCI * PESO_MAXIMO_VECES_CONDUCTOR + (maxCI - minCS) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.mejorCosteDia[contexto.ordenExploracionDias[nivel + 1]]
            cotaSuperior = maxCS * PESO_MAXIMO_VECES_CONDUCTOR + (maxCS - minCI) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.peorCosteDia[contexto.ordenExploracionDias[nivel + 1]] + 1 // Añadimos el 1 para evitar un bug que nos haría perder una solución
        } else {
            // estrategia == Estrategia.MINCONDUCTORES
            cotaInferior = maxCI * PESO_MAXIMO_VECES_CONDUCTOR + (total + totalMinRes) * PESO_TOTAL_CONDUCTORES + (maxCI - minCS) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.mejorCosteDia[contexto.ordenExploracionDias[nivel + 1]]
            cotaSuperior = maxCS * PESO_MAXIMO_VECES_CONDUCTOR + (total + totalMaxRes) * PESO_TOTAL_CONDUCTORES + (maxCS - minCI) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.peorCosteDia[contexto.ordenExploracionDias[nivel + 1]] + 1 // Añadimos el 1 para evitar un bug que nos haría perder una solución
        }
        if (DEBUG && (cotaInferior < padre.cotaInferior || cotaSuperior > padre.cotaSuperior || cotaSuperior < cotaInferior)) {
            System.err.println("*****************\n**** ¡LIADA!:\n --> Padre=$padre\n --> Hijo=${this}")
        }
        calculaCosteEstimado()
    }

    fun calculaCosteEstimado() {
        costeEstimado = (contexto.pesoCotaInferiorNum * cotaInferior + cotaSuperior * (contexto.pesoCotaInferiorDen - contexto.pesoCotaInferiorNum)) / contexto.pesoCotaInferiorDen
    }

    private val dia: Dia?
        get() = if (nivel >= 0) contexto.dias[contexto.ordenExploracionDias[nivel]] else null

    val solucion: MutableMap<Dia, AsignacionDiaV5>
        get() {
            val solucion: MutableMap<Dia, AsignacionDiaV5> = padre?.solucion ?: HashMap()
            if (nivel >= 0) {
                solucion.put(dia!!, eleccion!!)
            }
            return solucion
        }

    fun generaHijos(): List<Nodo> = contexto.solucionesCandidatas[contexto.dias[contexto.ordenExploracionDias[nivel + 1]]]!!.map { generaHijo(it) }

    private fun generaHijo(solDia: AsignacionDiaV5): Nodo = Nodo(this, solDia, contexto)

    override fun compareTo(other: Nodo): Int {
        return costeEstimado - other.costeEstimado
    }

    override fun hashCode(): Int {
        return Objects.hash(this.eleccion, padre, nivel)
    }

    override fun equals(other: Any?): Boolean = this === other || (other != null && other is Nodo && nivel == other.nivel && eleccion == other.eleccion && padre == other.padre)

    override fun toString(): String = String.format("Nodo{nivel=%d, estimado=%,d, inferior=%,d, superior=%,d}", nivel, costeEstimado, cotaInferior, cotaSuperior)

}
