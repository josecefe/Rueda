/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * SQL: CREATE TABLE "dia" ( "id" INTEGER PRIMARY KEY NOT NULL, "descripcion"
 * TEXT );
 *
 * @author josecefe
 */
class Dia(descripcion: String = "Día Desconocido") : Comparable<Dia> {
    private val descriptionProperty: SimpleStringProperty = SimpleStringProperty(descripcion)
    var descripcion: String
        get() = descriptionProperty.get()
        set(value) = descriptionProperty.set(value)

    fun descriptionProperty(): StringProperty = descriptionProperty

    override fun compareTo(other: Dia): Int = descripcion.compareTo(other.descripcion)

    override fun toString(): String = descripcion
}
