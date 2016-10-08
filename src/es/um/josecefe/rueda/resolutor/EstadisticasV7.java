/*
 * Copyright (C) 2016 José Ceferino Ortega
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

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author josec
 */
public final class EstadisticasV7 extends Estadisticas {
    protected long expandidos = 0;
    protected double descartados = 0, terminales = 0, generados = 0;
    protected double totalPosiblesSoluciones;

    public EstadisticasV7() {
    }

    @Override
    public Estadisticas inicia() {
        descartados = terminales = generados = expandidos = 0L;
        return super.inicia(); 
    }

    
    protected void setTotalPosiblesSoluciones(double totalPosiblesSoluciones) {
        this.totalPosiblesSoluciones = totalPosiblesSoluciones;
    }
    
    protected EstadisticasV7 acumular(EstadisticasV7 otro) {
        expandidos += otro.expandidos;
        generados += otro.generados;
        descartados += otro.descartados;
        terminales += otro.terminales;
        return this;
    }
    
    @Override
    public String toString() {
        final double porcentajeArbol = getCompletado() * 100.0;
        return String.format("t=%s s, C=%,d NE=%,d, %,.0f NE/s, NG=%,.0f, %,.0f NG/s, SG=%,.0f, NP=%g, CMPL=%.3f%%, ETR=%s",
                getTiempoString(), fitness, expandidos, expandidos * 1000.0 / tiempo, generados, generados * 1000.0 / tiempo,
                terminales, descartados, porcentajeArbol, DurationFormatUtils.formatDurationHMS((long)((tiempo / porcentajeArbol) * 100)));
    }

    protected long incExpandidos() {
        return ++expandidos;
    }

    protected void addGenerados(double nGenerados) {
        generados += nGenerados;
    }

    protected void addTerminales(double nTerminales) {
        terminales += nTerminales;
    }

    protected void addDescartados(double nDescartados) {
        descartados += nDescartados;
    }

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    @Override
    public double getCompletado() {
        return (descartados + terminales) / totalPosiblesSoluciones;
    }
}