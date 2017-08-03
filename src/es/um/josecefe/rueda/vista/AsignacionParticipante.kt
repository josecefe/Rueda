/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.vista

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @author josec
 */
class AsignacionParticipante(participante: String = "", ida: String = "", vuelta: String = "", conductor: Boolean = false) {
    private val participanteProperty: SimpleStringProperty = SimpleStringProperty(participante)
    private val idaProperty: SimpleStringProperty = SimpleStringProperty(ida)
    private val vueltaProperty: SimpleStringProperty = SimpleStringProperty(vuelta)
    private val conductorProperty: SimpleBooleanProperty = SimpleBooleanProperty(conductor)

    var participante: String
        get() = participanteProperty.get()
        set(value) = participanteProperty.set(value)

    fun participanteProperty(): StringProperty = participanteProperty

    var ida: String
        get() = idaProperty.get()
        set(value) = idaProperty.set(value)

    fun idaProperty(): StringProperty = idaProperty

    var vuelta: String
        get() = vueltaProperty.get()
        set(value) = vueltaProperty.set(value)

    fun vueltaProperty(): StringProperty = vueltaProperty

    var isConductor: Boolean
        get() = conductorProperty.get()
        set(value) = conductorProperty.set(value)

    fun conductorProperty(): BooleanProperty = conductorProperty

}
