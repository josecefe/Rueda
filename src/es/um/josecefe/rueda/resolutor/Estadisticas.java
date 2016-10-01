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
