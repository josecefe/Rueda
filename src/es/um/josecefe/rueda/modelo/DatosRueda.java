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

import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author josec
 */
public class DatosRueda {

    private final ListProperty<Dia> dias = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Lugar> lugares = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Participante> participantes = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Horario> horarios = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Asignacion> asignaciones = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final IntegerProperty costeAsignacion = new SimpleIntegerProperty();

    public ObservableList<Dia> getDias() {
        return dias.get();
    }

    public void setDias(ObservableList<Dia> value) {
        dias.set(value);
    }

    public ListProperty<Dia> diasProperty() {
        return dias;
    }

    public ObservableList<Lugar> getLugares() {
        return lugares.get();
    }

    public void setLugares(ObservableList<Lugar> value) {
        lugares.set(value);
    }

    public ListProperty<Lugar> lugaresProperty() {
        return lugares;
    }

    public ObservableList<Participante> getParticipantes() {
        return participantes.get();
    }

    public void setParticipantes(ObservableList<Participante> value) {
        participantes.set(value);
    }

    public ListProperty<Participante> participantesProperty() {
        return participantes;
    }

    public ObservableList<Horario> getHorarios() {
        return horarios.get();
    }

    public void setHorarios(ObservableList<Horario> value) {
        horarios.set(value);
    }

    public ListProperty<Horario> horariosProperty() {
        return horarios;
    }

    public ObservableList<Asignacion> getAsignacion() {
        return asignaciones.get();
    }

    public void setAsignacion(ObservableList<Asignacion> value) {
        asignaciones.set(value);
    }

    public ListProperty<Asignacion> asignacionProperty() {
        return asignaciones;
    }

    public int getCosteAsignacion() {
        return costeAsignacion.get();
    }

    public void setCosteAsignacion(int value) {
        costeAsignacion.set(value);
    }

    public IntegerProperty costeAsignacionProperty() {
        return costeAsignacion;
    }

    public void reemplazar(DatosRueda nuevosDatosRueda) {
        asignaciones.clear();
        horarios.clear();
        participantes.clear();
        lugares.clear();
        dias.clear();
        dias.addAll(nuevosDatosRueda.getDias());
        lugares.addAll(nuevosDatosRueda.getLugares());
        participantes.addAll(nuevosDatosRueda.getParticipantes());
        horarios.addAll(nuevosDatosRueda.getHorarios());
        asignaciones.addAll(nuevosDatosRueda.getAsignacion());
    }

    public void poblarDesdeHorarios(Set<Horario> horariosBase) {
        asignaciones.clear();
        horarios.clear();
        participantes.clear();
        lugares.clear();
        dias.clear();

        // Creamos las demas cosas a partir de solo los horarios de entrada
        horarios.addAll(FXCollections.observableArrayList(horariosBase));
        // Dias
        horarios.stream().map(Horario::getDia).distinct().sorted().collect(() -> dias, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
        // Participantes
        horarios.stream().map(Horario::getParticipante).distinct().sorted().collect(() -> participantes, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
        // Y a partir de los participantes, los Lugares
        participantes.stream().map(Participante::getPuntosEncuentro).flatMap(List::stream).distinct().collect(() -> lugares, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
    }

    public void setSolucion(Map<Dia, ? extends AsignacionDia> resolver) {
        asignaciones.clear();
        resolver.entrySet().stream().map(entry -> new Asignacion(entry.getKey(), entry.getValue())).collect(() -> asignaciones, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
    }
}
