/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.util

import java.util.*

/**
 * Esta clase contiene un generador de todas las combinaciones resultantes del producto cartesiano
 * a partir de los conjuntos dados como entrada.
 *
 * @author josecefe@um.es
 * @param <T>
</T> */
class Combinador<out T>(private val conjuntos: List<List<T>>) : Iterable<List<T>> {

    override fun iterator(): Iterator<List<T>> {
        return CombinadorIterator()
    }

    public val size: Int
        get() = conjuntos.map { it.size }.reduce { a, b -> a * b }

    private inner class CombinadorIterator : Iterator<List<T>> {
        private val actual = conjuntos.map { it.iterator() }.toMutableList()
        private val ultimo = actual.map { it.next() }.toMutableList()
        private var hayMas = true

        override fun hasNext(): Boolean {
            return hayMas
        }

        override fun next(): List<T> {
            val a = actual.listIterator()
            var act: Iterator<T>?
            val res = ArrayList(ultimo)

            hayMas = false
            while (a.hasNext()) {
                act = a.next()
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
