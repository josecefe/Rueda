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
import kotlin.streams.toList

internal class CombinadorTest {
    private fun preparacion(): Combinador<Set<Char>> {
        val iteratorList = ArrayList<Iterable<Set<Char>>>(10)
        val sets = SubSets(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2).toList()
        for (i in 0..8) {
            iteratorList.add(sets)
        }
        return Combinador(iteratorList)
    }

    @Test
    operator fun iterator() {
        println("Esperando 10 segundillos para dar tiempo a conectar...")
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Empezando...")
        val combinador = preparacion()
        var count: Long = 0
        val tiempoInicial = System.currentTimeMillis()
        for (characterList in combinador) {
            if (++count % 20000000L == 0L)
                println(" ... combinaciones hasta ahora: " + count + ", tiempo (ms)=" + (System.currentTimeMillis() - tiempoInicial) + ", ultima generada = " + characterList)
        }
        println("Total combinaciones: " + count + ", tiempo total (ms)=" + (System.currentTimeMillis() - tiempoInicial))
    }

    @Test
    fun stream() {
        val conjunto1 = Arrays.asList('a', 'b', 'c')
        val conjunto2 = Arrays.asList('A', 'B')
        val conjunto3 = Arrays.asList('1', '2', '3', '4')
        val iteratorList = Arrays.asList<Iterable<Char>>(conjunto1, conjunto2, conjunto3)
        val combinador = Combinador(iteratorList)
        val collect = combinador.stream().toList()
        println("Combinaciones: " + collect + ", total combinaciones = " + collect.size + " = " + conjunto1.size + " * " + conjunto2.size + " * " + conjunto3.size)
    }
}