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
import java.util.*

/**
 * @author josec
 *
 *
 * SQL: CREATE TABLE participante ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT
 * NULL, "nombre" TEXT NOT NULL, "plazasCoche" INTEGER NOT NULL, "residencia"
 * INTEGER REFERENCES lugar(id) )
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@id")
class Participante
/**
 * Constructor de la clase que debe usarse cuando se crea un objeto de este
 * tipo, reservando el constructor por defecto para labores de persistencia
 *
 * @param nombre          Nombre a mostrar del participante
 * @param plazasCoche     Nº de ocupantes del vehículo, incluido el conductor, 0 significa que no tiene coche
 * @param puntosEncuentro Lista con orden que contiene los lugares de encuentro admitidos
 * por el participante por orden de preferencia
 */
(
        /**
         * Devuelve el id del participante.
         *
         * @return el id
         */
        nombre: String = "", plazasCoche: Int = 0,
        puntosEncuentro: List<Lugar> = emptyList()) : Comparable<Participante> {
    private val nombreProperty: StringProperty
    private val plazasCocheProperty: IntegerProperty
    /*
     * SQL: CREATE TABLE punto_encuentro ( "participante" INTEGER NOT
	 * NULL REFERENCES participante(id) ON DELETE CASCADE, "lugar" INTEGER NOT
	 * NULL REFERENCES lugar(id) ON DELETE CASCADE, "orden" INTEGER NOT NULL,
	 * PRIMARY KEY (participante, lugar, orden) );
	 *
     */
    private val puntosEncuentroProp: ListProperty<Lugar>

    init {
        this.nombreProperty = SimpleStringProperty(nombre)
        this.plazasCocheProperty = SimpleIntegerProperty(plazasCoche) // Incluido el conductor
        this.puntosEncuentroProp = SimpleListProperty(FXCollections.observableArrayList(puntosEncuentro))
    }

    /**
     * @return the nombre
     */
    var nombre: String
        get() = nombreProperty.get()
        set(nombre) = this.nombreProperty.set(nombre)

    /**
     * @return the plazasCoche
     */
    var plazasCoche: Int
        get() = plazasCocheProperty.get()
        set(plazasCoche) = this.plazasCocheProperty.set(plazasCoche)

    /**
     * @return the puntosEncuentro
     */
    var puntosEncuentro: MutableList<Lugar>
        get() = ArrayList(puntosEncuentroProp)
        set(puntosEncuentro) {
            this.puntosEncuentroProp.clear()
            this.puntosEncuentroProp.addAll(puntosEncuentro)
        }

    fun setId(@Suppress("UNUSED_PARAMETER") i: Int) = Unit

    fun nombreProperty(): StringProperty = nombreProperty

    fun plazasCocheProperty(): IntegerProperty = plazasCocheProperty

    fun puntosEncuentroProperty(): ListProperty<Lugar> = puntosEncuentroProp

    override fun toString(): String {
        return nombreProperty.get()
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
     */
    override fun compareTo(other: Participante): Int {
        return nombre.compareTo(other.nombre)
    }
}
