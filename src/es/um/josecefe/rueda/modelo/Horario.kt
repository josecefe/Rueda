/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import javafx.beans.property.*

/**
 * SQL: CREATE TABLE "horario" ( "participante" INTEGER NOT NULL REFERENCES
 * participante(id) ON DELETE CASCADE, "dia" INTEGER NOT NULL REFERENCES dia(id)
 * ON DELETE CASCADE, "entrada" INTEGER NOT NULL, "salida" INTEGER NOT NULL,
 * "coche" INTEGER NOT NULL DEFAULT (1), PRIMARY KEY (participante,dia) );
 *
 * @author josecefe
 */
class Horario
/**
 * Crea una porción de horario que corresponde un participante y un día
 * dados
 *
 * @param participante Participante al que pertenece esta parte del horario
 * @param dia          Día al que se refiere esta parte
 * @param entrada      Hora de entrada del participante ese dia
 * @param salida       Hora de salida del participante ese dia
 * @param coche        Indica si ese día podría compartir su coche
 */
(participante: Participante? = null, dia: Dia? = null, entrada: Int = 0, salida: Int = 0, coche: Boolean = false) : Comparable<Horario> {

    private val participanteProperty: ObjectProperty<Participante>
    private val diaProperty: ObjectProperty<Dia>
    private val entradaProperty: IntegerProperty
    private val salidaProperty: IntegerProperty
    private val cocheProperty: BooleanProperty

    init {
        this.participanteProperty = SimpleObjectProperty(participante)
        this.diaProperty = SimpleObjectProperty(dia)
        this.entradaProperty = SimpleIntegerProperty(entrada)
        this.salidaProperty = SimpleIntegerProperty(salida)
        this.cocheProperty = SimpleBooleanProperty(coche)
    }

    /**
     * Permite fijar el participante de esta entrada en el horario Nota: Este
     * método se incluye para la persistencia
     */
    var participante: Participante
        get() = participanteProperty.get()
        set(participante) = this.participanteProperty.set(participante)

    fun participanteProperty() = participanteProperty

    /**
     * Permite fija el dia de esta entrada de horario. Perticipante + Dia no se
     * pueden repetir Nota: Este método se incluye para la persistencia
     *
     */
    var dia: Dia
        get() = diaProperty.get()
        set(dia) = this.diaProperty.set(dia)

    fun diaProperty() = diaProperty
    /**
     * Permite fijar la hora de entrada (entendida como un entero, usualmente
     * empezando por 1 para primera hora, 2 para la segunda, etc.) Nota: Este
     * método se incluye para la persistencia
     */
    var entrada: Int
        get() = entradaProperty.get()
        set(entrada) = this.entradaProperty.set(entrada)

    fun entradaProperty() = entradaProperty

    /**
     * Permite fijar la hora de salida (entendida como un entero, usualmente
     * empezando por 1 para primera hora, 2 para la segunda, etc.) Nota: Este
     * método se incluye para la persistencia
     */
    var salida: Int
        get() = salidaProperty.get()
        set(salida) = this.salidaProperty.set(salida)

    fun salidaProperty() = salidaProperty

    var coche: Boolean
        get() = cocheProperty.get()
        set(coche) = this.cocheProperty.set(coche)

    fun cocheProperty() = cocheProperty

    override fun toString(): String = "Horario [participante=$participante, dia=$dia, entrada=$entrada, salida=$salida, coche=$coche]"

    override fun hashCode(): Int {
        val prime = 31
        var result = 100
        result = prime * result + dia.hashCode()
        result = prime * result + participante.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean =
            (this === other) || (other != null && other is Horario && dia == other.dia && participante == other.participante)

    /* (non-Javadoc)
	 * @see java.lang.comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Horario): Int {
        var res = dia.compareTo(other.dia)
        if (res == 0) res = participante.compareTo(other.participante)

        return res
    }
}
