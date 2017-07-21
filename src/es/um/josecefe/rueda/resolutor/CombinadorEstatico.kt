/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Esta clase contiene un generador de todas las combinaciones resultantes del producto cartesiano
 * a partir de los conjuntos dados como entrada.
 *
 * @author josecefe@um.es
 */
class CombinadorEstatico<T>(private val conjuntos: List<Iterable<T>>) : Iterable<List<T>> {

    override fun iterator(): Iterator<List<T>> {
        return CombinadorIterator()
    }

    fun stream(): Stream<List<T>> {
        return StreamSupport.stream(spliterator(), false)
    }

    fun parallelStream(): Stream<List<T>> {
        return StreamSupport.stream(spliterator(), true)
    }

    private inner class CombinadorIterator : Iterator<List<T>> {
        private val actual: MutableList<Iterator<T>> = conjuntos.map { c -> c.iterator() }.toMutableList() //Para saber en que elemento estamos...
        private val ultimo: MutableList<T> = actual.map { it.next() }.toMutableList()
        private var hayMas = true

        override fun hasNext() = hayMas

        override fun next(): List<T> {
            val a = actual.listIterator()
            val res = ultimo.toList()

            hayMas = false
            while (a.hasNext()) {
                var act = a.next()
                if (!act.hasNext()) {
                    act = conjuntos[a.previousIndex()].iterator()
                    ultimo[a.previousIndex()] = act.next()
                    a.set(act)
                } else {
                    ultimo[a.previousIndex()] = act.next()
                    hayMas = true
                    break
                }
            }

            return res
        }
    }
}
