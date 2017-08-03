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
 * Esta clase contiene un generador de subconjuntos a partir de uno dado
 *
 * @param <T>
 * @author josecefe@um.es
</T> */
class SubSets<T>(conjunto: Collection<T>, minSize: Int, maxSize: Int) : Iterable<Set<T>> {
    private val base: List<T> = conjunto.toList()
    private val size: Int = conjunto.size
    private val minSize: Int
    private val maxSize: Int

    init {
        this.minSize = Math.min(this.size, minSize)
        this.maxSize = if (maxSize > this.size) this.size else maxSize
    }

    private fun generarElemento(bitElement: Int): Set<T> {
        var bit = bitElement
        val res = LinkedHashSet<T>()
        var i = 0
        while (bit > 0) {
            if (bit % 2 == 1) {
                res.add(base[i])
            }
            bit = bit.ushr(1)
            i++
        }
        return res
    }

    override fun iterator(): Iterator<Set<T>> {
        return SubSetsIterator()
    }

    private inner class SubSetsIterator : Iterator<Set<T>> {
        private val ultimo = 1 shl size
        private var actual = 0

        override fun hasNext(): Boolean {
            if (maxSize < size || minSize != 0) {
                var c = Integer.bitCount(actual)
                while ((c > maxSize || c < minSize) && actual < ultimo) {
                    c = Integer.bitCount(++actual)
                }
            } // Avanzamos hasta llegar a uno valido
            return actual < ultimo
        }

        override fun next(): Set<T> {
            if (maxSize < size || minSize != 0) {
                var c = Integer.bitCount(actual)
                while ((c > maxSize || c < minSize) && actual < ultimo) {
                    c = Integer.bitCount(++actual)
                }
            } // Avanzamos hasta llegar a uno valido
            return generarElemento(actual++)
        }
    }
}
