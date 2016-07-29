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
package es.um.josecefe.rueda;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 *
 * @author josec
 */
public final class EstadisticasGA implements Estadisticas {
    private final AtomicLong generaciones = new AtomicLong();
    private final long ti = System.currentTimeMillis();
    private final DoubleAdder generados = new DoubleAdder();
    private double tiempo = 0;
    private final int numGeneraciones;
    private int fitness;

    public EstadisticasGA(int numGeneraciones) {
        this.numGeneraciones = numGeneraciones;
    }

    @Override
    public EstadisticasGA updateTime() {
        tiempo = (System.currentTimeMillis() - ti) / 1000.0;
        return this;
    }
    
    @Override
    public EstadisticasGA setFitness(int aptitud) {
        fitness = aptitud;
        return this;
    }

    @Override
    public int getFitness() {
        return fitness;
    }

    @Override
    public String toString() {
        final double porcentajeCompletado = (double) generaciones.get()* 100.0 / numGeneraciones ;
        return String.format("t=%,.2f s, Fitness=%,d, Generación nº %,d (%,.0f gen/s), Individuos Generados=%,.0f (%,.0f g/s), Completado=%.3f%% (est. fin=%g s = %,.3f horas = %,.3f dias = %,.3f años)",
                tiempo, fitness, generaciones.get(), generaciones.get() / tiempo, generados.sum(), generados.sum() / tiempo, 
                porcentajeCompletado, (tiempo / porcentajeCompletado) * 100.0, (tiempo / porcentajeCompletado) / 36.0, (tiempo / porcentajeCompletado) / 864.0, (tiempo / porcentajeCompletado) / 315360.0);
    }

    long incGeneracion() {
        return generaciones.incrementAndGet();
    }

    void addGenerados(double nGenerados) {
        generados.add(nGenerados);
    }

    void setGeneracion(long generation) {
        generaciones.set(generation);
    }
}
