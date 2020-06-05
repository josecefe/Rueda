/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.util

import java.util.*


tailrec fun factorial(n: Long, a: Long = 1): Long = if (n <= 1) a else factorial(n - 1, a * n)

fun permutations(n: Long, r: Long): Long = factorial(n) / factorial(n - r)

fun combinations(n: Long, r: Long): Long = permutations(n, r) / factorial(r)

fun <T> cartesianProduct(lists: List<List<T>>): List<List<T>> {
    val resultLists = ArrayList<List<T>>()
    if (lists.isEmpty()) {
        resultLists.add(emptyList())
        return resultLists
    } else {
        val firstList = lists[0]
        val remainingLists = cartesianProduct(lists.subList(1, lists.size))
        for (condition in firstList) {
            for (remainingList in remainingLists) {
                val resultList = ArrayList<T>()
                resultList.add(condition)
                resultList.addAll(remainingList)
                resultLists.add(resultList)
            }
        }
    }
    return resultLists
}