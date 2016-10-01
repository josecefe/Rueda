/*
 * Copyright (C) 2016 José Ceferino Ortega
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Esta clase contiene un generador de subconjuntos a partir de uno dado
 * 
 * @author josecefe@um.es
 * @param <T>
 *
 */
public class SubSets<T> implements Iterable<Set<T>> {
	private final T[] base;
	private final int size;
	private final int minSize;
	private final int maxSize;

	private Set<T> generarElemento(int bit) {
		Set<T> res = new HashSet<>();
		for (int i = 0; bit > 0; i++) {
			if (bit % 2 == 1) {
				res.add(base[i]);
			}
			bit = bit >>> 1;
		}
		return res;
	}

	private class SubSetsIterator implements Iterator<Set<T>> {
		private int actual = 0;
		private final int ultimo = (1 << size);

		@Override
		public boolean hasNext() {
			if (maxSize < size || minSize != 0)
				for (int c = Integer.bitCount(actual); (c > maxSize || c < minSize)
						&& actual < ultimo; c = Integer.bitCount(++actual))
					; // Avanzamos hasta llegar a uno valido
			return actual < ultimo;
		}

		@Override
		public Set<T> next() {
			if (maxSize < size || minSize != 0)
				for (int c = Integer.bitCount(actual); (c > maxSize || c < minSize)
						&& actual < ultimo; c = Integer.bitCount(++actual))
					; // Avanzamos hasta llegar a uno valido
			return generarElemento(actual++);
		}
	}

	private class SubSetsSpliterator implements Spliterator<Set<T>> {
		private int actual; // current index, advanced on split or traversal
		private final int ultimo; // one past the greatest index

		SubSetsSpliterator(int origen, int ultimo) {
			this.actual = origen;
			this.ultimo = ultimo;
		}

		@Override
		public void forEachRemaining(Consumer<? super Set<T>> action) {
			for (; actual < ultimo; actual++) {
				if (maxSize < size || minSize != 0)
					for (int c = Integer.bitCount(actual); (c > maxSize || c < minSize)
							&& actual < ultimo; c = Integer.bitCount(++actual))
						; // Avanzamos hasta llegar a uno valido
				if (actual < ultimo)
					action.accept(generarElemento(actual));
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super Set<T>> action) {
			if (maxSize < size || minSize != 0)
				for (int c = Integer.bitCount(actual); (c > maxSize || c < minSize)
						&& actual < ultimo; c = Integer.bitCount(++actual))
					; // Avanzamos hasta llegar a uno valido
			if (actual < ultimo) {
				action.accept(generarElemento(actual++));
				return true;
			} else // cannot advance
				return false;
		}

		@Override
		public Spliterator<Set<T>> trySplit() {
			int lo = actual; // divide range in half
			int mid = ((lo + ultimo) >>> 1) & ~1; // force midpoint to be even
			if (lo < mid) { // split out left half
				actual = mid; // reset this Spliterator's origin
				return new SubSetsSpliterator(lo, mid);
			} else // too small to split
				return null;
		}

		@Override
		public long estimateSize() {
			return (long) ((ultimo - actual) / (1 << (size - (maxSize - minSize))));
		}

		/**
		 * Si no se limita el conjunto, sabemos su tamaño y el de sus partes
		 */
		@Override
		public int characteristics() {
			return (minSize == 0 && maxSize == size) ? SIZED | SUBSIZED | IMMUTABLE | DISTINCT : IMMUTABLE | DISTINCT;
		}

	}

	@SuppressWarnings("unchecked")
	public SubSets(Set<T> conjunto) {
		this.base = (T[]) conjunto.toArray();
		this.minSize = 0;
		this.maxSize = this.size = conjunto.size();
	}

	@SuppressWarnings("unchecked")
	public SubSets(Set<T> conjunto, int minSize, int maxSize) {
		this.base = (T[]) conjunto.toArray();
		this.size = conjunto.size();
		this.minSize = Math.min(this.size, minSize);
		this.maxSize = Math.min(this.size, maxSize);
	}

	@Override
	public Iterator<Set<T>> iterator() {
		return new SubSetsIterator();
	}

	@Override
	public Spliterator<Set<T>> spliterator() {
		return new SubSetsSpliterator(0, (1 << size));
	}

	public Stream<Set<T>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	public Stream<Set<T>> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}
}
