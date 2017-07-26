/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * SQL: CREATE TABLE "dia" ( "id" INTEGER PRIMARY KEY NOT NULL, "descripcion"
 * TEXT );
 *
 * @author josecefe
 */
class Dia
/**
 * El Identificador debe ser un número único a partir de 1. Los valores por defecto son para la persistencia
 *
 * @param id          Número único de identificación a partir del 1
 * @param descripcion Descripción del día en texto
 */(var id: Int = 0, descripcion: String = "") : Comparable<Dia> {
    val descriptionProperty: SimpleStringProperty
    var descripcion: String
        get() = descriptionProperty.get()
        set(value) = descriptionProperty.set(value)

    override fun compareTo(other: Dia): Int = Integer.compare(id, other.id)

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    override fun toString(): String = descripcion

    init {
        this.descriptionProperty = SimpleStringProperty(descripcion)
    }
}
