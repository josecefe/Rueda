/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

import java.time.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 *
 * @author josec
 */
public abstract class Estadisticas {
    protected int fitness;
    protected long ti; // Para el tiempo inicial
    protected Duration tiempo;
    protected final DoubleProperty progreso = new SimpleDoubleProperty(0);
    
    public Estadisticas setFitness(int aptitud) {
        this.fitness = aptitud;
        return this;
    }
    
    public int getFitness() {
        return fitness;
    }
    
    public Estadisticas iniciaTiempo() {
        ti = System.currentTimeMillis(); // Para el tiempo
        return this;
    }
    
    public Estadisticas actualizaProgreso() {
        tiempo = Duration.ofMillis(System.currentTimeMillis() - ti);
        progreso.set(getCompletado());
        return this;
    }
    
    public Duration getTiempo() {
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
