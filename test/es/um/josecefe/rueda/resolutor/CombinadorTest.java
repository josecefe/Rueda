package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CombinadorTest {
    @Test
    void iterator() {
        final List<Iterable<Character>> iteratorList = List.of(Arrays.asList('a', 'b', 'c'), Arrays.asList('A', 'B', 'C'), Arrays.asList('1', '2', '3'));
        Combinador<Character> combinador = new Combinador<Character>(iteratorList);
        for (List<Character> characterList: combinador) {
            System.out.println(characterList);
        }
    }

    @Test
    void stream() {
        List<Character> conjunto1 = Arrays.asList('a', 'b', 'c');
        List<Character> conjunto2 = Arrays.asList('A', 'B');
        List<Character> conjunto3 = Arrays.asList('1', '2', '3', '4');
        final List<Iterable<Character>> iteratorList = List.of(conjunto1, conjunto2, conjunto3);
        Combinador<Character> combinador = new Combinador<Character>(iteratorList);
        Set<List<Character>> collect = combinador.stream().collect(Collectors.toSet());
        System.out.println("Combinaciones: "+collect+", total combinaciones = "+collect.size()+" = "+conjunto1.size()+" * "+conjunto2.size()+" * "+conjunto3.size());
    }


}