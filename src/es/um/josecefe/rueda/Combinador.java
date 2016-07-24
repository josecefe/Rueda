/**
 * (C) 2016 José Ceferino Ortega Carretero
 */
package es.um.josecefe.rueda;

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
 *
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

	private class CombinadorIterator implements Iterator<List<T>> {
		private List<Iterator<T>> actual = conjuntos.stream().map(c -> c.iterator()).collect(Collectors.toList()); //Para saber en que elemento estamos...
		private List<T> ultimo=actual.stream().map(Iterator<T>::next).collect(Collectors.toList());
		private boolean hayMas = true;
				
		@Override
		public boolean hasNext() {
			return hayMas;
		}
		
		@Override
		public List<T> next() {
			ListIterator<Iterator<T>> a = actual.listIterator();
			Iterator<T> act=null;
			List<T> res = new ArrayList<>(ultimo);

			hayMas=false;
			while (a.hasNext()) {
				act = a.next();
				if (!act.hasNext()) {
					act=conjuntos.get(a.previousIndex()).iterator();
					ultimo.set(a.previousIndex(), act.next());
					a.set(act);
				} else {
					ultimo.set(a.previousIndex(), act.next());
					hayMas=true;
					break;
				}
			}
			
			return res;
		}
	}
/*
	private class CombinadorSpliterator implements Spliterator<Set<T>> {
		private int actual; // current index, advanced on split or traversal
		private final int ultimo; // one past the greatest index

		CombinadorSpliterator(int origen, int ultimo) {
			this.actual = origen;
			this.ultimo = ultimo;
		}

		public void forEachRemaining(Consumer<? super Set<T>> action) {
			for (; actual < ultimo; actual++) {
				while (contarBits(actual) > maxSize && actual < ultimo)
					actual++;
				if (actual < ultimo)
					action.accept(generarElemento(actual));
			}
		}

		public boolean tryAdvance(Consumer<? super Set<T>> action) {
			while (contarBits(actual) > maxSize && actual < ultimo)
				actual++;
			if (actual < ultimo) {
				action.accept(generarElemento(actual++));
				return true;
			} else // cannot advance
				return false;
		}

		public Spliterator<Set<T>> trySplit() {
			int lo = actual; // divide range in half
			int mid = ((lo + ultimo) >>> 1) & ~1; // force midpoint to be even
			if (lo < mid) { // split out left half
				actual = mid; // reset this Spliterator's origin
				return new CombinadorSpliterator(lo, mid);
			} else // too small to split
				return null;
		}

		public long estimateSize() {
			return (long) ((ultimo - actual) / (1 << (size - maxSize)));
		}

		public int characteristics() {
			return ORDERED | SIZED | IMMUTABLE | SUBSIZED;
		}

	}
	
	@Override
	public Spliterator<Set<T>> spliterator() {
		return new CombinadorSpliterator(0, (1 << size));
	}

	public Stream<Set<T>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	public Stream<Set<T>> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}

*/
	
	public Stream<List<T>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
}
