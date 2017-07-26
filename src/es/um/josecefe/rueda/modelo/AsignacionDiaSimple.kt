/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo

/**
 * @author josecefe
 */
class AsignacionDiaSimple : Comparable<AsignacionDiaSimple>, AsignacionDia {
    override var conductores: Set<Participante> = emptySet()
    override var peIda: Map<Participante, Lugar> = emptyMap()
    override var peVuelta: Map<Participante, Lugar> = emptyMap()
    override var coste: Int = 0

    /**
     * Constructor pensado para la persistencia
     */
    internal constructor() {}

    /**
     * Constructor normal
     *
     * @param conductores
     * @param puntoEncuentroIda
     * @param puntoEncuentroVuelta
     * @param coste
     */
    constructor(conductores: Set<Participante>, puntoEncuentroIda: Map<Participante, Lugar>, puntoEncuentroVuelta: Map<Participante, Lugar>, coste: Int) {
        this.conductores = conductores.toSet()
        this.peIda = puntoEncuentroIda.toMap()
        this.peVuelta = puntoEncuentroVuelta.toMap()
        this.coste = coste
    }

    override fun compareTo(other: AsignacionDiaSimple) = Integer.compare(conductores.size * 1000 + coste, other.conductores.size * 1000 + other.coste)

    override fun hashCode() = ((201 + conductores.hashCode()) * 67 + peIda.hashCode()) * 67 + peVuelta.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (other !is AsignacionDia) {
            return false
        }
        if (this.conductores != other.conductores) {
            return false
        }
        return if (this.peIda != other.peIda) {
            false
        } else this.peVuelta == other.peVuelta
    }

    override fun toString(): String {
        return "AsignacionDia{ coste=$coste, conductores=$conductores, peIda=$peIda, peVuelta=$peVuelta}"
    }
}
