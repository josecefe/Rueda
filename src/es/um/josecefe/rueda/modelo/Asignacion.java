/*
 * Copyright (C) 2016 josec
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
package es.um.josecefe.rueda.modelo;

import static java.util.stream.Collectors.toList;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

/**
 *
 * @author josec
 */
public class Asignacion {

    private final ObjectProperty<Dia> dia = new SimpleObjectProperty<>();
    private final ListProperty<Participante> conductores = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Pair<Participante, Lugar>> peida = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Pair<Participante, Lugar>> pevuelta = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final IntegerProperty coste = new ReadOnlyIntegerWrapper();

    public Asignacion() {
    }

    public Asignacion(Dia d, AsignacionDia asignacionDia) {
        dia.set(d);
        conductores.addAll(asignacionDia.getConductores());
        peida.addAll(asignacionDia.getPeIda().entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(toList()));
        pevuelta.addAll(asignacionDia.getPeVuelta().entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(toList()));
        coste.set(asignacionDia.getCoste());
    }

    public Dia getDia() {
        return dia.get();
    }

    public void setDia(Dia value) {
        dia.set(value);
    }

    public ObjectProperty<Dia> diaProperty() {
        return dia;
    }

    public ObservableList<Participante> getConductores() {
        return conductores.get();
    }

    public void setConductores(ObservableList<Participante> value) {
        conductores.set(value);
    }

    public ListProperty<Participante> participantesProperty() {
        return conductores;
    }

    public ObservableList<Pair<Participante, Lugar>> getPeIda() {
        return peida.get();
    }

    public void setPeIda(ObservableList<Pair<Participante, Lugar>> value) {
        peida.set(value);
    }

    public ListProperty<Pair<Participante, Lugar>> peIdaProperty() {
        return peida;
    }

    public ObservableList<Pair<Participante, Lugar>> getPeVuelta() {
        return pevuelta.get();
    }

    public void setPeVuelta(ObservableList<Pair<Participante, Lugar>> value) {
        pevuelta.set(value);
    }

    public ListProperty<Pair<Participante, Lugar>> peVueltaProperty() {
        return pevuelta;
    }

    public int getCoste() {
        return coste.get();
    }

    public void setCoste(int value) {
        coste.set(value);
    }

    public IntegerProperty costeProperty() {
        return coste;
    }
}
