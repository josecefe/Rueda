/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author josec
 */
public abstract class Estadisticas {
    protected int fitness;
    protected long ti; // Para el tiempo inicial
    protected long tiempo;
    protected final DoubleProperty progreso = new SimpleDoubleProperty(0);
    
    public Estadisticas setFitness(int aptitud) {
        this.fitness = aptitud;
        return this;
    }
    
    public int getFitness() {
        return fitness;
    }
    
    public Estadisticas inicia() {
        ti = System.currentTimeMillis(); // Tiempo inicial
        fitness = Integer.MAX_VALUE;
        if (!progreso.isBound())
            progreso.set(0);
        return this;
    }
    
    public Estadisticas actualizaProgreso() {
        tiempo = System.currentTimeMillis() - ti;
        if (!progreso.isBound())
            progreso.set(getCompletado());
        return this;
    }
    
    public String getTiempoString() {
        return DurationFormatUtils.formatDurationHMS(getTiempo());
    }
    
    public long getTiempo() {
        return tiempo;
    }
    
    public DoubleProperty progresoProperty() {
        return progreso;
    }

    /**
     * Indica el grado de completitud de la exploración de soluciones del algoritmo
     * @return valor entre 0 y 1 indicando el grado de completitud
     */
    public abstract double getCompletado();
}
