/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.modelo

/**
 * @author josec
 */
data class DatosRueda(
        val dias: List<Dia> = emptyList(),
        val lugares: List<Lugar> = emptyList(),
        val participantes: List<Participante> = emptyList(),
        val horarios: List<Horario> = emptyList(),
        var asignacion: List<Asignacion> = emptyList(),
        var costeAsignacion: Int = 0
) {

    private var mensajeValidacion = ""

    fun validar(): Boolean {
        if (horarios.any { it.participante.puntosEncuentro.isEmpty() }) {
            mensajeValidacion = "Hay participantes sin lugares de encuentro definidos"
            return false
        }
        mensajeValidacion = ""
        return true
    }

    fun setSolucion(resolver: Map<Dia, AsignacionDia>, costeTotal: Int) {
        asignacion = resolver.entries.map { entry -> Asignacion(entry.key, entry.value) }
        costeAsignacion = costeTotal
    }
}