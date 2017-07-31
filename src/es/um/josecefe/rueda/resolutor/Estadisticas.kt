/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import org.apache.commons.lang3.time.DurationFormatUtils

/**
 * @author josec
 */
abstract class Estadisticas {
    protected val progreso: DoubleProperty = SimpleDoubleProperty(0.0)
    open var fitness: Int = 0

    private var ti: Long = 0 // Para el tiempo inicial
    open var tiempo: Long = 0
        protected set

    open fun inicia(): Estadisticas {
        ti = System.currentTimeMillis() // Tiempo inicial
        fitness = Integer.MAX_VALUE
        if (!progreso.isBound)
            progreso.set(0.0)
        return this
    }

    fun actualizaProgreso(): Estadisticas {
        tiempo = System.currentTimeMillis() - ti
        if (!progreso.isBound)
            progreso.set(completado)
        return this
    }

    val tiempoString: String
        get() = DurationFormatUtils.formatDurationHMS(tiempo)

    fun progresoProperty(): DoubleProperty {
        return progreso
    }

    /**
     * Indica el grado de completitud de la exploración de soluciones del algoritmo
     *
     * @return valor entre 0 y 1 indicando el grado de completitud
     */
    abstract val completado: Double
}
