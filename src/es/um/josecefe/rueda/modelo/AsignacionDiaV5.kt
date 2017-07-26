/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

import java.util.*

/**
 * @author josecefe
 */
class AsignacionDiaV5
/**
 * Constructor normal
 *
 * @param participantes
 * @param conductores
 * @param puntoEncuentroIda
 * @param puntoEncuentroVuelta
 * @param coste
 */(private val participantes: Array<Participante> = emptyArray(), conductores: Set<Participante> = emptySet(),
    puntoEncuentroIda: Map<Participante, Lugar> = emptyMap(),
    puntoEncuentroVuelta: Map<Participante, Lugar> = emptyMap(),
    override var coste: Int = 0) : Comparable<AsignacionDiaV5>, AsignacionDia {
    val conductoresArray: BooleanArray = BooleanArray(participantes.size)
    override var peIda: Map<Participante, Lugar> = puntoEncuentroIda
    override var peVuelta: Map<Participante, Lugar> = puntoEncuentroVuelta
    private val numConductores: Int

    override val conductores: Set<Participante>
        get() = (0..conductoresArray.lastIndex)
                .filter { conductoresArray[it] }
                .map { participantes[it] }
                .toSet()

    override fun compareTo(other: AsignacionDiaV5): Int = Integer.compare(numConductores * 1000 + coste,
            other.numConductores * 1000 + other.coste)

    override fun hashCode(): Int = ((3 * 67 + Objects.hashCode(this.conductoresArray)) * 67 + Objects.hashCode(
            this.peIda)) * 67 + Objects.hashCode(this.peVuelta)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other1 = other as AsignacionDiaV5
        if (this.conductoresArray != other1.conductoresArray) {
            return false
        }
        return if (this.peIda != other1.peIda) {
            false
        } else this.peVuelta == other1.peVuelta
    }

    override fun toString(): String {
        return "AsignacionDia{ coste=$coste, conductores=$conductores, peIda=$peIda, peVuelta=$peVuelta}"
    }

    init {
        for (i in participantes.indices)
            this.conductoresArray[i] = conductores.contains(participantes[i])
        this.numConductores = conductores.size
    }


}
