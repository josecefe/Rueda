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
public final class EstadisticasV1 implements Estadisticas {

    long ti = System.currentTimeMillis(); // Para el tiempo
    long expandidos = 0;
    double descartados = 0, terminales = 0, generados = 0, tiempo = 0, totalPosiblesSoluciones;
    private int fitness;

    public EstadisticasV1(double totalPosiblesSoluciones) {
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
    public EstadisticasV1 updateTime() {
        tiempo = (System.currentTimeMillis() - ti) / 1000.0;
        return this;
    }
    
    @Override
    public EstadisticasV1 setFitness(int mejor) {
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
        final double porcentajeArbol = (descartados + terminales) / totalPosiblesSoluciones * 100.0;
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
}
