/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.resolutor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

class SubSetsTest {

    @Test
    void iterator() {
        SubSets<Character> conjuntos = new SubSets<>(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2);
        for (Set<Character> sc : conjuntos) {
            System.out.println(sc.toString());
        }
    }

    @Test
    void spliterator() {
    }

    @Test
    void stream() {
        SubSets<Character> conjuntos = new SubSets<>(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2);
        List<Set<Character>> combinaciones = conjuntos.stream().collect(toList());
        System.out.println("Combinaciones obtenidas="+combinaciones+", total combinaciones: "+combinaciones.size());
    }

    @Test
    void parallelStream() {
    }

}