/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import javafx.beans.property.IntegerProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import java.util.stream.Stream

/**
 * @author josec
 */
class DatosRueda {
    val diasProperty: ListProperty<Dia> = SimpleListProperty(FXCollections.observableArrayList<Dia>())
    val lugaresProperty: ListProperty<Lugar> = SimpleListProperty(FXCollections.observableArrayList())
    val participantesProperty: ListProperty<Participante> = SimpleListProperty(FXCollections.observableArrayList<Participante>())
    val horariosProperty: ListProperty<Horario> = SimpleListProperty(FXCollections.observableArrayList())
    val asignacionProperty: ListProperty<Asignacion> = SimpleListProperty(FXCollections.observableArrayList())
    val costeAsignacionProperty: IntegerProperty = SimpleIntegerProperty()
    /**
     * Obtiene el último mensaje de validación producido por la última llamada a validar
     *
     * @return mensaje de validación si ésta fue negativa, en blanco si fue positiva
     */
    var mensajeValidacion: String? = null
        private set

    var dias: List<Dia>
        get() = diasProperty.get()
        set(value) {
            diasProperty.clear()
            diasProperty.addAll(value)
        }

    var lugares: List<Lugar>
        get() = lugaresProperty.get()
        set(value) {
            lugaresProperty.clear()
            lugaresProperty.addAll(value)
        }

    var participantes: List<Participante>
        get() = participantesProperty.get()
        set(value) {
            participantesProperty.clear()
            participantesProperty.addAll(value)
        }

    var horarios: List<Horario>
        get() = horariosProperty.get()
        set(value) {
            horariosProperty.clear()
            horariosProperty.addAll(value)
        }

    var asignacion: List<Asignacion>
        get() = asignacionProperty.get()
        set(value) {
            asignacionProperty.clear()
            asignacionProperty.addAll(value)
        }

    var costeAsignacion: Int
        get() = costeAsignacionProperty.get()
        set(value) = costeAsignacionProperty.set(value)

    fun reemplazar(nuevosDatosRueda: DatosRueda) {
        asignacionProperty.clear()
        horariosProperty.clear()
        participantesProperty.clear()
        lugaresProperty.clear()
        diasProperty.clear()
        diasProperty.addAll(nuevosDatosRueda.dias)
        lugaresProperty.addAll(nuevosDatosRueda.lugares)
        participantesProperty.addAll(nuevosDatosRueda.participantes)
        horariosProperty.addAll(nuevosDatosRueda.horarios)
        asignacionProperty.addAll(nuevosDatosRueda.asignacion)
        costeAsignacionProperty.set(nuevosDatosRueda.costeAsignacion)
    }

    fun poblarDesdeHorarios(horariosBase: Set<Horario>) {
        asignacionProperty.clear()
        horariosProperty.clear()
        participantesProperty.clear()
        lugaresProperty.clear()
        diasProperty.clear()
        costeAsignacionProperty.set(0)

        // Creamos las demas cosas a partir de solo los horarios de entrada
        horariosProperty.addAll(FXCollections.observableArrayList(horariosBase))
        // Dias
        horariosProperty.stream().map{ it.dia}.distinct().sorted().collect({ diasProperty },
                { c, e -> c.add(e) }) { c, ce -> c.addAll(ce) }
        // Participantes
        horariosProperty.stream().map{ it.participante }.distinct().sorted().collect(
                { participantesProperty }, { c, e -> c.add(e) }) { c, ce -> c.addAll(ce) }
        // Y a partir de los participantes, los Lugares
        participantesProperty.stream().map{ it.puntosEncuentro }.flatMap{ it.stream() }.distinct().collect({ lugaresProperty },
                { c, e -> c.add(e) }) { c, ce -> c.addAll(ce) }
    }

    fun setSolucion(resolver: Map<Dia, AsignacionDia>?, costeTotal: Int) {
        asignacionProperty.clear()
        resolver?.entries?.stream()?.map { entry -> Asignacion(entry.key, entry.value) }?.collect(
                { asignacionProperty },
                { c, e -> c.add(e) }) { c, ce -> c.addAll(ce) }
        costeAsignacion = costeTotal
    }

    /**
     * Permite comprobar la coherencia de los datos actuales. Si el estado es
     * incoherente, indica la causa a través de getMensajeValidacion
     *
     * @return true si el estado de los datos permiten ejecutar la resolución
     * (son coherentes), false en otro caso (ver getMensajeValidacion)
     */
    fun validar(): Boolean {
        if (horarios.any { it.participante?.puntosEncuentro?.isEmpty() ?: true }) {
            mensajeValidacion = "Hay participantes sin lugares de encuentro definidos"
            return false
        }
        mensajeValidacion = ""
        return true
    }
}
