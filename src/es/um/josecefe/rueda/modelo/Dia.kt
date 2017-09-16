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
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @author josecefe
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@id")
class Dia(descripcion: String = "Día Desconocido", val orden: Int = 1) : Comparable<Dia> {
    private val descriptionProperty: SimpleStringProperty = SimpleStringProperty(descripcion)
    var descripcion: String
        get() = descriptionProperty.get()
        set(value) = descriptionProperty.set(value)

    fun setId(@Suppress("UNUSED_PARAMETER") i: Int) = Unit

    fun descriptionProperty(): StringProperty = descriptionProperty

    override fun compareTo(other: Dia): Int = orden.compareTo(other.orden)

    override fun toString(): String = descripcion
}
