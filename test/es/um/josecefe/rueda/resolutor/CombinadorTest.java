/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.resolutor;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CombinadorTest {
    @NotNull
    private Combinador<Set<Character>> preparacion() {
        final List<Iterable<Set<Character>>> iteratorList = new ArrayList<>(10);
        List<Set<Character>> sets = new SubSets<>(Arrays.asList('L', 'M', 'X', 'J', 'V'), 2, 2).stream().collect(Collectors.toList());
        for (int i = 0; i < 9; i++) {
            iteratorList.add(sets);
        }
        return new Combinador<>(iteratorList);
    }

    @Test
    void iterator() {
        System.out.println("Esperando 10 segundillos para dar tiempo a conectar...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Empezando...");
        Combinador<Set<Character>> combinador = preparacion();
        long count = 0, tiempoInicial = System.currentTimeMillis();
        for (List<Set<Character>> characterList : combinador) {
            if (++count % 20000000L == 0L)
                System.out.println(" ... combinaciones hasta ahora: " + count + ", tiempo (ms)=" + (System.currentTimeMillis() - tiempoInicial) + ", ultima generada = " + characterList);
        }
        System.out.println("Total combinaciones: " + count + ", tiempo total (ms)=" + (System.currentTimeMillis() - tiempoInicial));
    }

    @Test
    void stream() {
        List<Character> conjunto1 = Arrays.asList('a', 'b', 'c');
        List<Character> conjunto2 = Arrays.asList('A', 'B');
        List<Character> conjunto3 = Arrays.asList('1', '2', '3', '4');
        final List<Iterable<Character>> iteratorList = Arrays.asList(conjunto1, conjunto2, conjunto3);
        Combinador<Character> combinador = new Combinador<Character>(iteratorList);
        Set<List<Character>> collect = combinador.stream().collect(Collectors.toSet());
        System.out.println("Combinaciones: " + collect + ", total combinaciones = " + collect.size() + " = " + conjunto1.size() + " * " + conjunto2.size() + " * " + conjunto3.size());
    }
}