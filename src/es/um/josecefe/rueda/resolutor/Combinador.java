/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Esta clase contiene un generador de todas las combinaciones resultantes del producto cartesiano
 * a partir de los conjuntos dados como entrada.
 *
 * @author josecefe@um.es
 * @param <T>
 */
public class Combinador<T> implements Iterable<List<T>> {
    private final List<Iterable<T>> conjuntos;

    public Combinador(List<Iterable<T>> conjuntos) {
        this.conjuntos = conjuntos;
    }

    @Override
    public Iterator<List<T>> iterator() {
        return new CombinadorIterator();
    }

    public Stream<List<T>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<List<T>> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    private class CombinadorIterator implements Iterator<List<T>> {
        private List<Iterator<T>> actual = conjuntos.stream().map(c -> c.iterator()).collect(Collectors.toList()); //Para saber en que elemento estamos...
        private List<T> ultimo = actual.stream().map(Iterator<T>::next).collect(Collectors.toList());
        private boolean hayMas = true;

        @Override
        public boolean hasNext() {
            return hayMas;
        }

        @Override
        public List<T> next() {
            ListIterator<Iterator<T>> a = actual.listIterator();
            Iterator<T> act = null;
            List<T> res = new ArrayList<>(ultimo);

            hayMas = false;
            while (a.hasNext()) {
                act = a.next();
                if (!act.hasNext()) {
                    act = conjuntos.get(a.previousIndex()).iterator();
                    ultimo.set(a.previousIndex(), act.next());
                    a.set(act);
                } else {
                    ultimo.set(a.previousIndex(), act.next());
                    hayMas = true;
                    break;
                }
            }

            return res;
        }
    }
}
