/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import org.apache.commons.lang3.time.DurationFormatUtils

class EstadisticasV7 : Estadisticas() {
    var expandidos: Long = 0
        private set
    var descartados = 0.0
        private set
    var terminales = 0.0
        private set
    var generados = 0.0
        private set
    var totalPosiblesSoluciones: Double = 0.0

    override fun inicia(): Estadisticas {
        expandidos = 0L
        generados = 0.0
        terminales = 0.0
        descartados = 0.0
        return super.inicia()
    }

    override fun toString(): String {
        val porcentajeArbol = completado * 100.0
        return String.format("t=%s s, C=%,d NE=%,d, %,.0f NE/s, NG=%,.0f, %,.0f NG/s, SG=%,.0f, NP=%g, CMPL=%.3f%%, ETR=%s",
                tiempoString, fitness, expandidos, expandidos * 1000.0 / tiempo, generados, generados * 1000.0 / tiempo,
                terminales, descartados, porcentajeArbol, DurationFormatUtils.formatDurationHMS((tiempo / porcentajeArbol * 100).toLong()))
    }

    fun incExpandidos(): Long {
        return ++expandidos
    }

    fun addGenerados(nGenerados: Double) {
        generados += nGenerados
    }

    fun addTerminales(nTerminales: Double) {
        terminales += nTerminales
    }

    fun addDescartados(nDescartados: Double) {
        descartados += nDescartados
    }

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     *
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    override val completado: Double
        get() = (descartados + terminales) / totalPosiblesSoluciones
}
