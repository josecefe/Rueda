/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javafx.beans.property.*
import javafx.collections.FXCollections

/**
 * @author josec
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@id")
class Asignacion(dia: Dia, conductores: List<Participante>, peIda: List<Pair<Participante, Lugar>>,
                 peVuelta: List<Pair<Participante, Lugar>>, coste: Int) : Comparable<Asignacion> {

    private val diaProp: SimpleObjectProperty<Dia> = SimpleObjectProperty()
    private val conductoresProp: SimpleListProperty<Participante> = SimpleListProperty(FXCollections.observableArrayList<Participante>())
    private val peidaProp: SimpleListProperty<Pair<Participante, Lugar>> = SimpleListProperty(FXCollections.observableArrayList<Pair<Participante, Lugar>>())
    private val pevueltaProp: SimpleListProperty<Pair<Participante, Lugar>> = SimpleListProperty(FXCollections.observableArrayList<Pair<Participante, Lugar>>())
    private val costeProp = SimpleIntegerProperty()

    init {
        diaProp.set(dia)
        conductoresProp.addAll(conductores)
        peidaProp.addAll(peIda)
        pevueltaProp.addAll(peVuelta)
        costeProp.set(coste)
    }

    constructor(dia: Dia, asignacionDia: AsignacionDia) : this(dia, asignacionDia.conductores.toList(),
            asignacionDia.peIda.entries.map { Pair(it.key, it.value) },
            asignacionDia.peVuelta.entries.map { Pair(it.key, it.value) }, asignacionDia.coste)

    var dia: Dia
        get() = diaProp.get()
        set(value) = diaProp.set(value)

    fun diaProperty(): ObjectProperty<Dia> = diaProp

    var conductores: List<Participante>
        get() = conductoresProp.get()
        set(value) {
            conductoresProp.clear()
            conductoresProp.addAll(value)
        }

    fun participantesProperty(): ListProperty<Participante> = conductoresProp

    var peIda: List<Pair<Participante, Lugar>>
        get() = peidaProp.get()
        set(value) {
            peidaProp.clear()
            peidaProp.addAll(value)
        }

    fun peIdaProperty(): ListProperty<Pair<Participante, Lugar>> = peidaProp

    var peVuelta: List<Pair<Participante, Lugar>>
        get() = pevueltaProp.get()
        set(value) {
            pevueltaProp.clear()
            pevueltaProp.addAll(value)
        }

    fun peVueltaProperty(): ListProperty<Pair<Participante, Lugar>> = pevueltaProp

    var coste: Int
        get() = costeProp.get()
        set(value) = costeProp.set(value)

    fun costeProperty(): IntegerProperty = costeProp

    override fun compareTo(other: Asignacion): Int {
        return dia.compareTo(other.dia)
    }
}
