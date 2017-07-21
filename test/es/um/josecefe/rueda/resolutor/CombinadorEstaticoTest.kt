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

internal class CombinadorEstaticoTest {
    private fun preparacion(): CombinadorEstatico<Set<Char>> {
        val iteratorList = ArrayList<Iterable<Set<Char>>>()
        val sets: List<Set<Char>> = SubSets<Char>(listOf('L', 'M', 'X', 'J', 'V'), 2, 2).toList()
        for (i in 0..9) iteratorList.add(sets)
        return CombinadorEstatico(iteratorList)
    }

    @Test
    operator fun iterator() {
        println("Empezando el test...")
        val combinador = preparacion()
        var count: Long = 0
        val tiempoInicial = System.currentTimeMillis()
        for (characterList in combinador) {
            if (++count % 20000000L == 0L)
                println(" ... combinaciones hasta ahora: $count, velocidad (comb/ms)=${count.toDouble()/(System.currentTimeMillis() - tiempoInicial).toDouble()}, ultima generada = $characterList")
        }
        println("Total combinaciones: $count, tiempo total (ms)=${System.currentTimeMillis() - tiempoInicial}, velocidad (comb/ms)=${count.toDouble()/(System.currentTimeMillis() - tiempoInicial).toDouble()}")
    }

    @Test
    fun stream() {
        val conjunto1 = listOf('a', 'b', 'c')
        val conjunto2 = listOf('A', 'B')
        val conjunto3 = listOf('1', '2', '3', '4')
        val iteratorList = listOf(conjunto1, conjunto2, conjunto3)
        val combinador = CombinadorEstatico<Char>(iteratorList)
        val collect = combinador.toList()
        println("Combinaciones: " + collect + ", total combinaciones = " + collect.size + " = " + conjunto1.size + " * " + conjunto2.size + " * " + conjunto3.size)
    }
}