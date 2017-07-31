/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.resolutor

import org.junit.jupiter.api.Test
import java.util.*

internal class SubSetsTest {

    @Test
    operator fun iterator() {
        val conjuntos = SubSets(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2)
        for (sc in conjuntos) {
            println(sc.toString())
        }
    }

    @Test
    fun spliterator() {
    }

    @Test
    fun stream() {
        val conjuntos = SubSets(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2)
        val combinaciones = conjuntos.toList()
        println("Combinaciones obtenidas=" + combinaciones + ", total combinaciones: " + combinaciones.size)
    }

    @Test
    fun parallelStream() {
    }

}