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

/**
 * Representa un lugar
 *
 *
 * SQL: CREATE TABLE "lugar" ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 * "nombre" TEXT NOT NULL, "latitud" REAL, "longitud" REAL, "direccion" TEXT,
 * "poblacion" TEXT, "cp" TEXT );
 *
 * @author josec
 */
/**
 * Crea un lugar. Este construtor es el que debe usarse para crear un nuevo
 * objeto del tipo Lugar. Se debe reservar el constructor por defecto para
 * labores de persistencia.
 *
 * @param nombre
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@id")
class Lugar(nombre: String = "Lugar Desconocido") : Comparable<Lugar> {

    private val nombreProperty: SimpleStringProperty = SimpleStringProperty(nombre)

    /**
     * @return the nombre
     */
    var nombre: String
        get() = nombreProperty.get()
        set(nombre) = this.nombreProperty.set(nombre)

    /**
     * Propiedad JavaFX siguiendo sus convenciones
     */
    fun nombreProperty(): SimpleStringProperty = nombreProperty

    override fun toString(): String = nombre

    override fun compareTo(other: Lugar): Int = nombre.compareTo(other.nombre)
}

