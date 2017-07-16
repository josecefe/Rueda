package es.um.josecefe.rueda.resolutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class SubSetsTest {

    @Test
    void iterator() {
        SubSets<Character> conjuntos = new SubSets<>(Set.of('L', 'M', 'X', 'J', 'V'), 2, 2);
        for (Set<Character> sc : conjuntos) {
            System.out.println(sc.toString());
        }
    }

    @Test
    void spliterator() {
    }

    @Test
    void stream() {
        SubSets<Character> conjuntos = new SubSets<>(List.of('L', 'M', 'X', 'J', 'V'), 2, 2);
        List<Set<Character>> combinaciones = conjuntos.stream().collect(toList());
        System.out.println("Combinaciones obtenidas="+combinaciones+", total combinaciones: "+combinaciones.size());
    }

    @Test
    void parallelStream() {
    }

}