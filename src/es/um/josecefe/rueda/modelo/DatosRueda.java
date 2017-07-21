/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author josec
 */
public class DatosRueda {

    private final ListProperty<Dia> dias = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Lugar> lugares = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Participante> participantes = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Horario> horarios = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Asignacion> asignaciones = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final IntegerProperty costeAsignacion = new SimpleIntegerProperty();
    private String mensajeValidacion;

    public List<Dia> getDias() {
        return dias.get();
    }

    public void setDias(List<Dia> value) {
        dias.clear();
        dias.addAll(value);
    }

    public ListProperty<Dia> diasProperty() {
        return dias;
    }

    public List<Lugar> getLugares() {
        return lugares.get();
    }

    public void setLugares(List<Lugar> value) {
        lugares.clear();
        lugares.addAll(value);
    }

    public ListProperty<Lugar> lugaresProperty() {
        return lugares;
    }

    public List<Participante> getParticipantes() {
        return participantes.get();
    }

    public void setParticipantes(List<Participante> value) {
        participantes.clear();
        participantes.addAll(value);
    }

    public ListProperty<Participante> participantesProperty() {
        return participantes;
    }

    public List<Horario> getHorarios() {
        return horarios.get();
    }

    public void setHorarios(List<Horario> value) {
        horarios.clear();
        horarios.addAll(value);
    }

    public ListProperty<Horario> horariosProperty() {
        return horarios;
    }

    public List<Asignacion> getAsignacion() {
        return asignaciones.get();
    }

    public void setAsignacion(List<Asignacion> value) {
        asignaciones.clear();
        asignaciones.addAll(value);
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
        costeAsignacion.set(nuevosDatosRueda.getCosteAsignacion());
    }

    public void poblarDesdeHorarios(Set<Horario> horariosBase) {
        asignaciones.clear();
        horarios.clear();
        participantes.clear();
        lugares.clear();
        dias.clear();
        costeAsignacion.set(0);

        // Creamos las demas cosas a partir de solo los horarios de entrada
        horarios.addAll(FXCollections.observableArrayList(horariosBase));
        // Dias
        horarios.stream().map(Horario::getDia).distinct().sorted().collect(() -> dias, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
        // Participantes
        horarios.stream().map(Horario::getParticipante).distinct().sorted().collect(() -> participantes, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
        // Y a partir de los participantes, los Lugares
        participantes.stream().map(Participante::getPuntosEncuentro).flatMap(List::stream).distinct().collect(() -> lugares, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
    }

    public void setSolucion(Map<Dia, ? extends AsignacionDia> resolver, int costeTotal) {
        asignaciones.clear();
        if (resolver != null) {
            resolver.entrySet().stream().map(entry -> new Asignacion(entry.getKey(), entry.getValue())).collect(() -> asignaciones, (c, e) -> c.add(e), (c, ce) -> c.addAll(ce));
        }
        setCosteAsignacion(costeTotal);
    }

    /**
     * Permite comprobar la coherencia de los datos actuales. Si el estado es
     * incoherente, indica la causa a través de getMensajeValidacion
     *
     * @return true si el estado de los datos permiten ejecutar la resolución
     * (son coherentes), false en otro caso (ver getMensajeValidacion)
     */
    public boolean validar() {
        if (getHorarios().stream().map(Horario::getParticipante).anyMatch(p -> p.getPuntosEncuentro().size() < 1)) {
            mensajeValidacion = "Hay participantes sin lugares de encuentro definidos";
            return false;
        }
        mensajeValidacion = "";
        return true;

    }

    /**
     * Obtiene el último mensaje de validación producido por la última llamada a validar
     *
     * @return mensaje de validación si ésta fue negativa, en blanco si fue positiva
     */
    public String getMensajeValidacion() {
        return mensajeValidacion;
    }
}
