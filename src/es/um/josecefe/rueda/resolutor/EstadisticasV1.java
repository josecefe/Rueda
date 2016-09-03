/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author josec
 */
public final class EstadisticasV1 extends Estadisticas {
    protected long expandidos = 0;
    protected double descartados = 0, terminales = 0, generados = 0;
    protected double totalPosiblesSoluciones;

    public EstadisticasV1() {
    }

    @Override
    public Estadisticas inicia() {
        descartados = terminales = generados = expandidos = 0L;
        return super.inicia(); 
    }

    
    protected void setTotalPosiblesSoluciones(double totalPosiblesSoluciones) {
        this.totalPosiblesSoluciones = totalPosiblesSoluciones;
    }
    
    protected EstadisticasV1 acumular(EstadisticasV1 otro) {
        expandidos += otro.expandidos;
        generados += otro.generados;
        descartados += otro.descartados;
        terminales += otro.terminales;
        return this;
    }
    
    @Override
    public String toString() {
        final double porcentajeArbol = getCompletado() * 100.0;
        return String.format("t=%s s, C=%,d NE=%,d (%,.0f NE/s), NG=%,.0f (%,.0f NG/s), SG=%,.0f, NP=%g, Completado=%.3f%% (ETA=%s)",
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
