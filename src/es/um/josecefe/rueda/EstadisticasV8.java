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
public final class EstadisticasV8 implements Estadisticas {
    
    AtomicLong expandidos = new AtomicLong();
    long ti = System.currentTimeMillis();
    DoubleAdder descartados = new DoubleAdder();
    DoubleAdder terminales = new DoubleAdder();
    DoubleAdder generados = new DoubleAdder();
    double tiempo = 0;
    double nSoluciones;
    private int fitness;

    public EstadisticasV8(double nSolucionesTotal) {
        this.nSoluciones = nSolucionesTotal;
    }

    EstadisticasV8 acumular(EstadisticasV8 otro) {
        expandidos.getAndAdd(otro.expandidos.get());
        generados.add(otro.generados.sum());
        descartados.add(otro.descartados.sum());
        terminales.add(otro.terminales.sum());
        return this;
    }

    @Override
    public EstadisticasV8 updateTime() {
        tiempo = (System.currentTimeMillis() - ti) / 1000.0;
        return this;
    }
    
    @Override
    public EstadisticasV8 setFitness(int mejor) {
        fitness = mejor;
        return this;
    }

    @Override
    public int getFitness() {
        return fitness;
    }

    public double getTiempo() {
        return tiempo;
    }

    @Override
    public String toString() {
        final double porcentajeArbol = (descartados.sum() + terminales.sum()) / nSoluciones * 100.0;
        return String.format("t=%,.2f s, C=%,d, NE=%,d (%,.0f ne/s), NG=%,.0f (%,.0f ng/s), SG=%,.0f, NP=%g, Completado=%.3f%% (est. fin=%g s = %,.3f horas = %,.3f dias = %,.3f años)", 
                tiempo, fitness,expandidos.get(), expandidos.get() / tiempo, generados.sum(), generados.sum() / tiempo, terminales.sum(), descartados.sum(), porcentajeArbol,
                (tiempo / porcentajeArbol) * 100.0, (tiempo / porcentajeArbol) / 36.0, (tiempo / porcentajeArbol) / 864.0, (tiempo / porcentajeArbol) / 315360.0);
    }

    long incExpandidos() {
        return expandidos.incrementAndGet();
    }

    void addGenerados(double nGenerados) {
        generados.add(nGenerados);
    }

    void addTerminales(double nTerminales) {
        terminales.add(nTerminales);
    }

    void addDescartados(double nDescartados) {
        descartados.add(nDescartados);
    }
    
}
