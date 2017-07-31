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
class EstadisticasGA : Estadisticas() {
    private val generaciones = AtomicLong()
    private val generados = DoubleAdder()
    var numGeneraciones: Int = 0

    override fun inicia(): Estadisticas {
        generaciones.set(0)
        generados.reset()
        return super.inicia()
    }

    override fun toString(): String {
        val porcentajeCompletado = completado * 100.0
        return String.format("t=%s, C=%,d, G=%,d (%,.0f gen/s), IG=%,.0f (%,.0f g/s), Cmpl.=%.3f%% (ETR=%s)",
                tiempoString, fitness, generaciones.get(), generaciones.get() * 1000.0 / tiempo, generados.sum(), generados.sum() * 1000.0 / tiempo,
                porcentajeCompletado, DurationFormatUtils.formatDurationHMS((tiempo / porcentajeCompletado * 100.0).toLong()))
    }

    fun incGeneracion(): Long {
        return generaciones.incrementAndGet()
    }

    fun addGenerados(nGenerados: Double): EstadisticasGA {
        generados.add(nGenerados)
        return this
    }

    fun setGeneracion(generation: Long): EstadisticasGA {
        generaciones.set(generation)
        return this
    }

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     *
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    override val completado: Double
        get() = generaciones.get().toDouble() / numGeneraciones
}
