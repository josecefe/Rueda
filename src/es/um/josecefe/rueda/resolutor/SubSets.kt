/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Esta clase contiene un generador de subconjuntos a partir de uno dado
 *
 * @param <T>
 * @author josecefe@um.es
</T> */
class SubSets<T>(conjunto: Collection<T>, minSize: Int, maxSize: Int) : Iterable<Set<T>> {
    private val base: List<T>
    private val size: Int
    private val minSize: Int
    private val maxSize: Int

    init {
        this.base = conjunto.toList()
        this.size = conjunto.size
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

    override fun spliterator(): Spliterator<Set<T>> {
        return SubSetsSpliterator(0, 1 shl size)
    }

    fun stream(): Stream<Set<T>> {
        return StreamSupport.stream(spliterator(), false)
    }

    fun parallelStream(): Stream<Set<T>> {
        return StreamSupport.stream(spliterator(), true)
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

    private inner class SubSetsSpliterator internal constructor(private var actual: Int // current index, advanced on split or traversal
                                                                , private val ultimo: Int // one past the greatest index
    ) : Spliterator<Set<T>> {

        override fun forEachRemaining(action: Consumer<in Set<T>>) {
            while (actual < ultimo) {
                if (maxSize < size || minSize != 0) {
                    var c = Integer.bitCount(actual)
                    while ((c > maxSize || c < minSize) && actual < ultimo) {
                        c = Integer.bitCount(++actual)
                    }
                } // Avanzamos hasta llegar a uno valido
                if (actual < ultimo)
                    action.accept(generarElemento(actual))
                actual++
            }
        }

        override fun tryAdvance(action: Consumer<in Set<T>>): Boolean {
            if (maxSize < size || minSize != 0) {
                var c = Integer.bitCount(actual)
                while ((c > maxSize || c < minSize) && actual < ultimo) {
                    c = Integer.bitCount(++actual)
                }
            } // Avanzamos hasta llegar a uno valido
            if (actual < ultimo) {
                action.accept(generarElemento(actual++))
                return true
            } else
            // cannot advance
                return false
        }

        override fun trySplit(): Spliterator<Set<T>>? {
            val lo = actual // divide range in half
            val mid = (lo + ultimo).ushr(1) and 1.inv() // force midpoint to be even
            if (lo < mid) { // split out left half
                actual = mid // reset this Spliterator's origin
                return SubSetsSpliterator(lo, mid)
            } else
            // too small to split
                return null
        }

        override fun estimateSize(): Long {
            return ((ultimo - actual) / (1 shl size - (maxSize - minSize))).toLong()
        }

        /**
         * Si no se limita el conjunto, sabemos su tamaño y el de sus partes
         */
        override fun characteristics(): Int {
            return if (minSize == 0 && maxSize == size) Spliterator.SIZED or Spliterator.SUBSIZED or Spliterator.IMMUTABLE or Spliterator.DISTINCT else Spliterator.IMMUTABLE or Spliterator.DISTINCT
        }

    }
}
