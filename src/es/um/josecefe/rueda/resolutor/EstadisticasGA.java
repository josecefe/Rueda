/*
 * Copyright (C) 2016 Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author josec
 */
public final class EstadisticasGA extends Estadisticas {
    private final AtomicLong generaciones = new AtomicLong();
    private final DoubleAdder generados = new DoubleAdder();
    private int numGeneraciones;

    public EstadisticasGA() {
        
    }

    void setNumGeneraciones(int numGeneraciones) {
        this.numGeneraciones = numGeneraciones;
    }

    @Override
    public String toString() {
        final double porcentajeCompletado = getCompletado() * 100.0;
        return String.format("t=%s, Fitness=%,d, Generación nº %,d (%,.0f gen/s), Individuos Generados=%,.0f (%,.0f g/s), Completado=%.3f%% (ETA=%s)",
                getTiempoString(), fitness, generaciones.get(), generaciones.get() * 1000.0 / tiempo, generados.sum(), generados.sum() * 1000.0 / tiempo, 
                porcentajeCompletado, DurationFormatUtils.formatDurationHMS((long)((tiempo / porcentajeCompletado) * 100.0)));
    }

    long incGeneracion() {
        return generaciones.incrementAndGet();
    }

    EstadisticasGA addGenerados(double nGenerados) {
        generados.add(nGenerados);
        return this;
    }

    EstadisticasGA setGeneracion(long generation) {
        generaciones.set(generation);
        return this;
    }
    
    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    public double getCompletado() {
        return (double) generaciones.get() / numGeneraciones;
    }
}
