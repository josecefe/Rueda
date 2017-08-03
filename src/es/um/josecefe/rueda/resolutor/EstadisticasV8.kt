/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import org.apache.commons.lang3.time.DurationFormatUtils

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.DoubleAdder

/**
 * @author josec
 */
class EstadisticasV8 : Estadisticas() {
    var expandidos = AtomicLong()
    private var descartados = DoubleAdder()
    var terminales = DoubleAdder()
    private var generados = DoubleAdder()
    var totalPosiblesSoluciones: Double = 0.0

    override fun inicia(): Estadisticas {
        expandidos.set(0)
        descartados.reset()
        terminales.reset()
        generados.reset()
        return super.inicia()
    }

    override fun toString(): String {
        val lPorcentajeArbol = completado * 100.0
        return String.format("t=%s s, C=%,d NE=%,d, %,.0f NE/s, NG=%,.0f, %,.0f NG/s, SG=%,.0f, NP=%g, CMPL=%.3f%%, ETR=%s",
                tiempoString, fitness, expandidos.get(), expandidos.get() * 1000.0 / tiempo, generados.sum(), generados.sum() * 1000.0 / tiempo,
                terminales.sum(), descartados.sum(), lPorcentajeArbol,
                DurationFormatUtils.formatDurationHMS((tiempo / lPorcentajeArbol * 100.0).toLong()))
    }

    fun incExpandidos(): Long {
        return expandidos.incrementAndGet()
    }

    fun addGenerados(nGenerados: Double) {
        generados.add(nGenerados)
    }

    fun addTerminales(nTerminales: Double) {
        terminales.add(nTerminales)
    }

    fun addDescartados(nDescartados: Double) {
        descartados.add(nDescartados)
    }

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     *
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    override val completado: Double
        get() = (descartados.sum() + terminales.sum()) / totalPosiblesSoluciones
}
