/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * @author josec
 */
class ContextoResolucion {
    private static final boolean ORDEN_IMPORTA = true;
    Dia[] dias;
    int[] ordenExploracionDias;
    Participante[] participantes;
    Map<Dia, List<AsignacionDiaV5>> solucionesCandidatas;
    double[] coefConduccion; //Que tanto por 1 supone que use el coche cada conductor
    int[][] maxVecesCondDia;
    int[][] minVecesCondDia;
    int[] peorCosteDia;
    int[] mejorCosteDia;
    int pesoCotaInferiorNum; //Numerador de la fracción al ponderar la cota inferior respecto de la cota superior (el resto) para calcular el coste estimado
    int pesoCotaInferiorDen; //Denominador de la fracción al ponderar las cotas inferior y superior para calcular el coste estimado
    Resolutor.Estrategia estrategia = Resolutor.Estrategia.EQUILIBRADO;

    void inicializa(final Set<Horario> horarios) {
        dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        participantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);

        Map<Participante, Long> vecesParticipa = horarios.stream().collect(groupingBy(Horario::getParticipante, counting()));
        long maxVecesParticipa = vecesParticipa.values().stream().mapToLong(Long::longValue).max().orElseThrow(()-> new IllegalArgumentException("Horarios incorrectos"));

        coefConduccion = Stream.of(participantes).mapToDouble(p -> (double) maxVecesParticipa / vecesParticipa.get(p) - 0.001).toArray(); // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren

        maxVecesCondDia = new int[dias.length][participantes.length];
        minVecesCondDia = new int[dias.length][participantes.length];

        peorCosteDia = new int[dias.length];

        mejorCosteDia = new int[dias.length];

        Arrays.fill(mejorCosteDia, Integer.MAX_VALUE);

        // Vamos a trabajar día a día
        solucionesCandidatas = IntStream.range(0, dias.length).parallel().boxed().collect(toConcurrentMap(ind -> dias[ind], indDia -> {
            //for (int indDia = 0; indDia < dias.length; indDia++) {
            Dia d = dias[indDia];
            Arrays.fill(minVecesCondDia[indDia], 1); //El minimo lo ponemos en 1 en ausencia de informacion
            // El array de maximos ya esta incializado a 0 que es lo que necesitamos
            ArrayList<AsignacionDiaV5> solucionesDia = new ArrayList<>();
            Set<Horario> horariosDia = horarios.stream().filter(h -> h.getDia() == d).collect(toSet());
            Map<Participante, Horario> participanteHorario = horariosDia.stream()
                    .collect(toMap(Horario::getParticipante, Function.identity()));
            Participante[] participantesDia = horariosDia.stream().map(Horario::getParticipante).sorted().toArray(Participante[]::new);

            // Para cada hora de entrada, obtenemos los conductores disponibles
            Map<Integer, Set<Participante>> entradaConductor = horariosDia.stream().filter(Horario::isCoche)
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, toSet())));
            // Para comprobar, vemos los participantes, sus entradas y salidas
            Map<Integer, Long> nParticipantesIda = horariosDia.stream()
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, counting())));
            Map<Integer, Long> nParticipantesVuelta = horariosDia.stream()
                    .collect(groupingBy(Horario::getSalida, mapping(Horario::getParticipante, counting())));
            // Generamos todas las posibilidades y a ver cuales sirven...
            List<Iterable<Set<Participante>>> conductoresDia = entradaConductor.keySet().stream()
                    .map(key -> new SubSets<>(entradaConductor.get(key), 1, entradaConductor.get(key).size())).collect(toList());
            Combinador<Set<Participante>> combinarConductoresDia = new Combinador<>(conductoresDia);

            for (List<Set<Participante>> condDia : combinarConductoresDia) {
                final Set<Participante> selCond = condDia.stream().flatMap(Collection::stream).collect(toSet());
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                Map<Integer, Integer> plazasIda = selCond.stream()
                        .map(participanteHorario::get).collect(
                                groupingBy(Horario::getEntrada, summingInt(h -> h.getParticipante().getPlazasCoche())));
                Map<Integer, Integer> plazasVuelta = selCond.stream()
                        .map(participanteHorario::get)
                        .collect(groupingBy(Horario::getSalida, summingInt(h -> h.getParticipante().getPlazasCoche())));

                if (nParticipantesIda.entrySet().stream().allMatch(e -> plazasIda.getOrDefault(e.getKey(), 0) >= e.getValue())
                        && nParticipantesVuelta.entrySet().stream().allMatch(e -> plazasVuelta.getOrDefault(e.getKey(), 0) >= e.getValue())) {

                    // Obtenemos la lista de posibles lugares teniendo en cuenta quien es el conductor
                    List<Iterable<Lugar>> posiblesLugares = Stream.of(participantesDia).map(Participante::getPuntosEncuentro).
                            collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                    posiblesLugares.addAll(selCond.stream().sorted().map(Participante::getPuntosEncuentro).
                            collect(ArrayList::new, ArrayList::add, ArrayList::addAll));

                    int mejorCoste = Integer.MAX_VALUE;
                    Map<Participante, Lugar> mejorLugaresIda = null, mejorLugaresVuelta = null;

                    for (List<Lugar> selLugares : new Combinador<>(posiblesLugares)) {
                        Map<Participante, Lugar> lugaresIda, lugaresVuelta;
                        lugaresIda = new HashMap<>();
                        lugaresVuelta = new HashMap<>();
                        Iterator<Lugar> il = selLugares.subList(participantesDia.length, selLugares.size()).iterator();
                        for (int i = 0; i < participantesDia.length; i++) {
                            lugaresIda.put(participantesDia[i], selLugares.get(i));
                            lugaresVuelta.put(participantesDia[i], selCond.contains(participantesDia[i])
                                    ? il.next() : selLugares.get(i));
                        }
                        Map<Integer, Map<Lugar, Integer>> plazasDisponiblesIda = selCond.stream()
                                .collect(groupingBy(p -> participanteHorario.get(p).getEntrada(), groupingBy(lugaresIda::get, summingInt(Participante::getPlazasCoche))));

                        Map<Integer, Map<Lugar, Integer>> plazasDisponiblesVuelta = selCond.stream()
                                .collect(groupingBy(p -> participanteHorario.get(p).getSalida(), groupingBy(lugaresVuelta::get, summingInt(Participante::getPlazasCoche))));
                        // Para comprobar, vemos los participantes, sus entradas y salidas
                        Map<Integer, Map<Lugar, Long>> plazasNecesariasIda = horariosDia.stream()
                                .collect(groupingBy(Horario::getEntrada, groupingBy(h -> lugaresIda.get(h.getParticipante()), counting())));

                        Map<Integer, Map<Lugar, Long>> plazasNecesariasVuelta = horariosDia.stream()
                                .collect(groupingBy(Horario::getSalida, groupingBy(h -> lugaresVuelta.get(h.getParticipante()), counting())));

                        if (plazasNecesariasIda.entrySet().stream().allMatch(e -> e.getValue().entrySet().stream().allMatch(ll -> ll.getValue() <= plazasDisponiblesIda.get(e.getKey()).getOrDefault(ll.getKey(), 0)))
                                && plazasNecesariasVuelta.entrySet().stream().allMatch(e -> e.getValue().entrySet().stream().allMatch(ll -> ll.getValue() <= plazasDisponiblesVuelta.get(e.getKey()).getOrDefault(ll.getKey(), 0)))) {
                            // Calculamos coste
                            int coste = Stream.of(participantesDia).mapToInt(
                                    p -> (p.getPuntosEncuentro().indexOf(lugaresIda.get(p)) + p.getPuntosEncuentro().indexOf(lugaresVuelta.get(p))) *
                                            (selCond.contains(p) ? Pesos.PESO_LUGAR_CONDUCTOR : Pesos.PESO_LUGAR_PASAJERO)).sum();

                            if (coste < mejorCoste) {
                                mejorCoste = coste;
                                mejorLugaresIda = lugaresIda;
                                mejorLugaresVuelta = lugaresVuelta;
                            }
                        }

                    }
                    if (mejorCoste < Integer.MAX_VALUE) {
                        // Tenemos una solución válida
                        solucionesDia.add(new AsignacionDiaV5(participantes, selCond, mejorLugaresIda, mejorLugaresVuelta, mejorCoste));
                        // Actualizamos estadisticas
                        if (peorCosteDia[indDia] < mejorCoste) {
                            peorCosteDia[indDia] = mejorCoste;
                        }
                        if (mejorCosteDia[indDia] > mejorCoste) {
                            mejorCosteDia[indDia] = mejorCoste;
                        }
                        // Actualizamos veces conductor
                        for (int i = 0; i < participantes.length; i++) {
                            if (selCond.contains(participantes[i])) {
                                maxVecesCondDia[indDia][i] = 1;
                            } else {
                                minVecesCondDia[indDia][i] = 0;
                            }
                        }
                    }
                }
            }
            Collections.shuffle(solucionesDia); //Barajamos las soluciones parciales para dar cierta aleatoridad a igualdad de coste
            solucionesDia.sort(null); //Ordenamos la soluciones parciales tras el barajado
            return solucionesDia;
        }));

        //Vamos a ver cual sería el mejor orden para ir explorando los días, empezando por el que menos soluciones posibles ofrece
        if (ORDEN_IMPORTA) {
            Comparator<Map.Entry<Dia, List<AsignacionDiaV5>>> comparator = Comparator.comparingInt(e -> e.getValue().size());
            ordenExploracionDias = solucionesCandidatas.entrySet().stream().sorted(comparator.reversed()).mapToInt(e -> ArrayUtils.indexOf(dias, e.getKey())).toArray();
        } else {
            ordenExploracionDias = IntStream.range(0,solucionesCandidatas.size()).toArray();
        }


        // Cambio de indirección: para calcular acumulados vamos a tener en cuenta el orden de exploración
        for (int i = peorCosteDia.length - 2; i >= 0; i--) {
            peorCosteDia[ordenExploracionDias[i]] += peorCosteDia[ordenExploracionDias[i + 1]];
            mejorCosteDia[ordenExploracionDias[i]] += mejorCosteDia[ordenExploracionDias[i + 1]];
            for (int j = 0; j < minVecesCondDia[ordenExploracionDias[i]].length; j++) {
                minVecesCondDia[ordenExploracionDias[i]][j] += minVecesCondDia[ordenExploracionDias[i + 1]][j];
                maxVecesCondDia[ordenExploracionDias[i]][j] += maxVecesCondDia[ordenExploracionDias[i + 1]][j];
            }
        }
    }
}
