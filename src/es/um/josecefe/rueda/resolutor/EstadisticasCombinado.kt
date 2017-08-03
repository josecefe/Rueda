/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import org.apache.commons.lang3.time.DurationFormatUtils

/**
 * @author josec
 */
class EstadisticasCombinado(primera: Estadisticas, segunda: Estadisticas) : Estadisticas() {

    init {
        progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)))
    }

    var primera = primera
        set(value) {
            field = value
            progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)))
        }

    var segunda = segunda
        set(value) {
            field = value
            progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)))
        }

    override val completado: Double
        get() = primera.completado * 0.099 + segunda.completado * 0.901

    override fun toString(): String {
        return String.format("t=%s s, C=%,d Cmpl.=%.3f%% (ETR=%s)",
                tiempoString, fitness, progresoProperty().get() * 100.0,
                DurationFormatUtils.formatDurationHMS((tiempo / progresoProperty().get()).toLong()))
    }

    override var tiempo: Long
        get() = primera.tiempo + segunda.tiempo
        set(value) {
            super.tiempo = value
        }

    override var fitness: Int = 0
        get() = Math.min(primera.fitness, segunda.fitness)

    override fun inicia(): Estadisticas {
        primera.inicia()
        segunda.inicia()
        return super.inicia()
    }
}
