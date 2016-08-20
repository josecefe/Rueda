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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 *
 * @author josec
 */
public final class EstadisticasV8 extends Estadisticas {
    AtomicLong expandidos = new AtomicLong();
    DoubleAdder descartados = new DoubleAdder();
    DoubleAdder terminales = new DoubleAdder();
    DoubleAdder generados = new DoubleAdder();
    double totalPosiblesSoluciones;

    public EstadisticasV8(){
    }

    void setTotalPosiblesSoluciones(double totalPosiblesSoluciones) {
        this.totalPosiblesSoluciones = totalPosiblesSoluciones;
    }
    
    EstadisticasV8 acumular(EstadisticasV8 otro) {
        expandidos.getAndAdd(otro.expandidos.get());
        generados.add(otro.generados.sum());
        descartados.add(otro.descartados.sum());
        terminales.add(otro.terminales.sum());
        return this;
    }
    
    @Override
    public String toString() {
        double lPorcentajeArbol = getCompletado() * 100.0;
        return String.format("t=%,.2f s, C=%,d, NE=%,d (%,.0f ne/s), NG=%,.0f (%,.0f ng/s), SG=%,.0f, NP=%g, Completado=%.3f%% (est. fin=%g s = %,.3f horas = %,.3f dias = %,.3f años)", 
                tiempo, fitness,expandidos.get(), expandidos.get() / tiempo, generados.sum(), generados.sum() / tiempo, terminales.sum(), descartados.sum(), lPorcentajeArbol,
                (tiempo / lPorcentajeArbol) * 100.0, (tiempo / lPorcentajeArbol) / 36.0, (tiempo / lPorcentajeArbol) / 864.0, (tiempo / lPorcentajeArbol) / 315360.0);
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

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    public double getCompletado() {
        return (descartados.sum() + terminales.sum()) / totalPosiblesSoluciones;
    }
}
