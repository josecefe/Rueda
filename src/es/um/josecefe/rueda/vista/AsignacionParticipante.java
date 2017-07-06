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
package es.um.josecefe.rueda.vista;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author josec
 */
public class AsignacionParticipante {

    private final StringProperty participante = new SimpleStringProperty();
    private final StringProperty ida = new SimpleStringProperty();
    private final StringProperty vuelta = new SimpleStringProperty();
    private final BooleanProperty conductor = new SimpleBooleanProperty();

    public AsignacionParticipante() {
        // Constructor por defecto
    }

    public AsignacionParticipante(String participante, String ida, String vuelta, boolean conductor) {
        this.participante.set(participante);
        this.ida.set(ida);
        this.vuelta.set(vuelta);
        this.conductor.set(conductor);
    }

    public String getParticipante() {
        return participante.get();
    }

    public void setParticipante(String value) {
        participante.set(value);
    }

    public StringProperty participanteProperty() {
        return participante;
    }

    public String getIda() {
        return ida.get();
    }

    public void setIda(String value) {
        ida.set(value);
    }

    public StringProperty idaProperty() {
        return ida;
    }

    public String getVuelta() {
        return vuelta.get();
    }

    public void setVuelta(String value) {
        vuelta.set(value);
    }

    public StringProperty vueltaProperty() {
        return vuelta;
    }

    public boolean isConductor() {
        return conductor.get();
    }

    public void setConductor(boolean value) {
        conductor.set(value);
    }

    public BooleanProperty conductorProperty() {
        return conductor;
    }

}
