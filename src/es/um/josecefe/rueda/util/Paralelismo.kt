/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.util

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

inline fun <T, R> Iterable<T>.pmap(
        numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
        exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
        crossinline transform: (T) -> R): List<R> {

    // default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
    val defaultSize = if (this is Collection<*>) this.size else 10
    val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

    for (item in this) {
        exec.submit { destination.add(transform(item)) }
    }

    exec.shutdown()
    exec.awaitTermination(1, TimeUnit.DAYS)

    return ArrayList<R>(destination)
}

inline fun <T> Iterable<T>.pforEach(
        numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
        exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
        crossinline transform: (T) -> Unit): Unit {

    for (item in this) exec.submit { transform(item) }

    exec.shutdown()
    exec.awaitTermination(1, TimeUnit.DAYS)
}