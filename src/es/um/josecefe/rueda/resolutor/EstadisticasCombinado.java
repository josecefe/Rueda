/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * @author josec
 */
public class EstadisticasCombinado extends Estadisticas {

    protected Estadisticas primera;
    protected Estadisticas segunda;

    public EstadisticasCombinado(Estadisticas primera, Estadisticas segunda) {
        this.primera = primera;
        this.segunda = segunda;
        progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)));
    }

    @Override
    public double getCompletado() {
        return primera.getCompletado() * 0.099 + segunda.getCompletado() * 0.901;
    }

    @Override
    public String toString() {
        return String.format("t=%s s, C=%,d Cmpl.=%.3f%% (ETR=%s)",
                getTiempoString(), getFitness(), progresoProperty().get() * 100.0,
                DurationFormatUtils.formatDurationHMS((long) (getTiempo() / progresoProperty().get())));
    }

    public void setPrimera(Estadisticas estadisticas) {
        primera = estadisticas;
        progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)));
    }

    public void setSegunda(Estadisticas estadisticas) {
        segunda = estadisticas;
        progreso.bind(primera.progresoProperty().multiply(0.099).add(segunda.progresoProperty().multiply(0.901)));
    }

    @Override
    public long getTiempo() {
        return primera.getTiempo() + segunda.getTiempo();
    }

    @Override
    public int getFitness() {
        return Math.min(primera.getFitness(), segunda.getFitness());
    }

    @Override
    public Estadisticas inicia() {
        primera.inicia();
        segunda.inicia();
        return super.inicia();
    }
}
