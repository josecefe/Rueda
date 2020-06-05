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
data class AsignacionDiaSimple(
        override val conductores: Set<Participante> = emptySet(),
        override val coste: Int = 0
) : Comparable<AsignacionDiaSimple>, AsignacionDia {

    override val peIda: Map<Participante, Lugar>
        get() = conductores.associateWith { it.puntosEncuentro[0] }

    override val peVuelta: Map<Participante, Lugar>
        get() = peIda

    override fun compareTo(other: AsignacionDiaSimple) = (conductores.size * 1000 + coste).compareTo(other.conductores.size * 1000 + other.coste)
}