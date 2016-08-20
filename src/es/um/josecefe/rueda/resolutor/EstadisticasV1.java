/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

/**
 *
 * @author josec
 */
public final class EstadisticasV1 extends Estadisticas {
    long expandidos = 0;
    double descartados = 0, terminales = 0, generados = 0, totalPosiblesSoluciones;

    public EstadisticasV1() {
        this.totalPosiblesSoluciones = totalPosiblesSoluciones;
    }

    void setTotalPosiblesSoluciones(double totalPosiblesSoluciones) {
        this.totalPosiblesSoluciones = totalPosiblesSoluciones;
    }
    
    EstadisticasV1 acumular(EstadisticasV1 otro) {
        expandidos += otro.expandidos;
        generados += otro.generados;
        descartados += otro.descartados;
        terminales += otro.terminales;
        return this;
    }
    
    @Override
    public String toString() {
        final double porcentajeArbol = getCompletado() * 100.0;
        return String.format("t=%,.2f s, C=%,d NE=%,d (%,.0f ne/s), NG=%,.0f (%,.0f ng/s), SG=%,.0f, NP=%g, Completado=%.3f%% (est. fin=%g s = %,.3f horas = %,.3f dias = %,.3f años)",
                tiempo, fitness, expandidos, expandidos / tiempo, generados, generados / tiempo,
                terminales, descartados, porcentajeArbol, (tiempo / porcentajeArbol) * 100.0,
                (tiempo / porcentajeArbol) / 36.0, (tiempo / porcentajeArbol) / 864.0, (tiempo / porcentajeArbol) / 315360.0);
    }

    long incExpandidos() {
        return ++expandidos;
    }

    void addGenerados(double nGenerados) {
        generados += nGenerados;
    }

    void addTerminales(double nTerminales) {
        terminales += nTerminales;
    }

    void addDescartados(double nDescartados) {
        descartados += nDescartados;
    }

    /**
     * Tanto por uno de la cantidad del arbol de posibles soluciones explorado
     * @return Valor entre 0 y 1 indicando el tanto por uno explorado
     */
    public double getCompletado() {
        return (descartados + terminales) / totalPosiblesSoluciones;
    }
}
