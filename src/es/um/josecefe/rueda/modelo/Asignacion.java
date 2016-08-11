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

import com.sun.javafx.collections.ObservableMapWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SetProperty;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

/**
 *
 * @author josec
 */
public class Asignacion {

    private final ObjectProperty<Dia> dia = new ReadOnlyObjectWrapper<>();
    private final SetProperty<Participante> conductores = new ReadOnlySetWrapper<>();
    private final MapProperty<Participante, Lugar> peida = new ReadOnlyMapWrapper<>();
    private final MapProperty<Participante, Lugar> pevuelta = new ReadOnlyMapWrapper<>();
    private final IntegerProperty coste = new ReadOnlyIntegerWrapper();

    public Asignacion() {
    }
    
    public Asignacion(Dia d, AsignacionDia asignacionDia) {
        dia.set(d);
        conductores.set(new ObservableSetWrapper<>(asignacionDia.getConductores()));
        peida.set(new ObservableMapWrapper<>(asignacionDia.getPeIda()));
        pevuelta.set(new ObservableMapWrapper<>(asignacionDia.getPeVuelta()));
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

    public ObservableSet<Participante> getConductores() {
        return conductores.get();
    }

    public void setConductores(ObservableSet<Participante> value) {
        conductores.set(value);
    }

    public SetProperty<Participante> participantesProperty() {
        return conductores;
    }

    public ObservableMap<Participante, Lugar> getPeIda() {
        return peida.get();
    }

    public void setPeIda(ObservableMap<Participante, Lugar> value) {
        peida.set(value);
    }

    public MapProperty<Participante, Lugar> peIdaProperty() {
        return peida;
    }

    public ObservableMap<Participante, Lugar> getPeVuelta() {
        return pevuelta.get();
    }

    public void setPeVuelta(ObservableMap<Participante, Lugar> value) {
        pevuelta.set(value);
    }

    public MapProperty<Participante, Lugar> peVueltaProperty() {
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
