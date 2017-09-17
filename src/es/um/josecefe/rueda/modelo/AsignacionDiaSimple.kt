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
        val otrosPosiblesLugares: List<Map<Participante, Pair<Lugar, Lugar>>> = emptyList(),
        override val coste: Int = 0
) : Comparable<AsignacionDiaSimple>, AsignacionDia {

    override val peIda: Map<Participante, Lugar>
        get() = otrosPosiblesLugares[0].mapValues { (_, par) -> par.first }

    override val peVuelta: Map<Participante, Lugar>
        get() = otrosPosiblesLugares[0].mapValues { (_, par) -> par.second }

    override fun compareTo(other: AsignacionDiaSimple) = Integer.compare(conductores.size * 1000 + coste, other.conductores.size * 1000 + other.coste)
}
