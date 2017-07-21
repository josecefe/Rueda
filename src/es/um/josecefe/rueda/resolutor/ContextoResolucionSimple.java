/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * @author josecefe
 */
class ContextoResolucionSimple {
    final List<Dia> dias;
    //Map<Dia, Integer> diasIndex;
    final List<Participante> participantes;
    //Map<Participante, Integer> participantesIndex;
    final Map<Dia, List<AsignacionDiaSimple>> solucionesCandidatas;
    final HashMap<Dia,HashMap<Set<Participante>,AsignacionDiaSimple>> mapaParticipantesSoluciones;
    final Map<Participante, Double> coefConduccion; //Que tanto por 1 supone que use el coche cada conductor

    ContextoResolucionSimple(final Set<Horario> horarios) {
        //Construimos la lista de Dias (usamos un lista para tener un orden)
        Set<Dia> diasSet = new TreeSet<>(); //Lo queremos ordenado
        for (Horario horario : horarios) {
            diasSet.add(horario.getDia());
        }
        dias = new ArrayList<>(diasSet); // Podría ser un array si lo vemos necesario

        //Construimos ahora la lista de conductores (también usamos una lista por lo mismo que en dias
        Set<Participante> participanteSet = new TreeSet<>(); //Lo queremos ordenado
        for (Horario horario : horarios) {
            Participante participante = horario.getParticipante();
            participanteSet.add(participante);
        }
        participantes = new ArrayList<>(participanteSet);

        // Y aquí ajustamos el coeficiente que nos mide su grado de participación (gente que solo va parte de los días)
        Map<Participante, Long> vecesParticipa = horarios.stream().collect(groupingBy(Horario::getParticipante, counting()));
        long maxVecesParticipa = vecesParticipa.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        coefConduccion = new LinkedHashMap<>(participantes.size());
        for (Participante p : participantes) {
            coefConduccion.put(p, (double) maxVecesParticipa / vecesParticipa.get(p) - 0.001); // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }

        // Vamos a trabajar día a día
        //Para parelizar: solucionesCandidatas = dias.stream().collect(toMap(ind -> ind, d -> {
        solucionesCandidatas = new LinkedHashMap<>();
        mapaParticipantesSoluciones = new HashMap<>();
        for (Dia d : dias) {
            ArrayList<AsignacionDiaSimple> solucionesDia = new ArrayList<>();
            HashMap<Set<Participante>,AsignacionDiaSimple> mapaParticipantesAsignacionDia = new HashMap<>();
            Set<Horario> horariosDia = horarios.stream().filter(h -> h.getDia() == d).collect(toSet());
            Map<Participante, Horario> participanteHorario = horariosDia.stream().collect(toMap(Horario::getParticipante, Function.identity()));
            List<Participante> participantesDia = horariosDia.stream().map(Horario::getParticipante).sorted().collect(toList());
            final int nParticipantesDia = participantesDia.size();

            // Para cada hora de entrada, obtenemos los conductores disponibles
            Map<Integer, Set<Participante>> entradaConductor = horariosDia.stream().filter(Horario::isCoche)
                    .collect(Collectors.groupingBy(Horario::getEntrada, Collectors.mapping(Horario::getParticipante, toSet())));
            // Para comprobar, vemos los participantes, sus entradas y salidas
            Map<Integer, Long> nParticipantesIda = horariosDia.stream()
                    .collect(Collectors.groupingBy(Horario::getEntrada, Collectors.mapping(Horario::getParticipante, Collectors.counting())));
            Map<Integer, Long> nParticipantesVuelta = horariosDia.stream()
                    .collect(Collectors.groupingBy(Horario::getSalida, Collectors.mapping(Horario::getParticipante, Collectors.counting())));
            // Generamos todas las posibilidades y a ver cuales sirven...
            List<Iterable<Set<Participante>>> conductoresDia = entradaConductor.keySet().stream()
                    .map(key -> new SubSets<>(entradaConductor.get(key), 1, entradaConductor.get(key).size())).collect(Collectors.toList());
            Combinador<Set<Participante>> combinarConductoresDia = new Combinador<>(conductoresDia);

            for (List<Set<Participante>> condDia : combinarConductoresDia) {
                final Set<Participante> selCond = condDia.stream().flatMap(Collection::stream).collect(toSet());
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro
                Map<Integer, Integer> plazasIda = selCond.stream()
                        .map(participanteHorario::get).collect(
                                Collectors.groupingBy(Horario::getEntrada, Collectors.summingInt(h -> h.getParticipante().getPlazasCoche())));
                Map<Integer, Integer> plazasVuelta = selCond.stream()
                        .map(participanteHorario::get)
                        .collect(Collectors.groupingBy(Horario::getSalida, Collectors.summingInt(h -> h.getParticipante().getPlazasCoche())));

                if (nParticipantesIda.entrySet().stream().allMatch(e -> plazasIda.getOrDefault(e.getKey(), 0) >= e.getValue())
                        && nParticipantesVuelta.entrySet().stream().allMatch(e -> plazasVuelta.getOrDefault(e.getKey(), 0) >= e.getValue())) {

                    // Obtenemos la lista de posibles lugares teniendo en cuenta quien es el conductor
                    List<Iterable<Lugar>> posiblesLugares = participantesDia.stream().map(Participante::getPuntosEncuentro).
                            collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                    posiblesLugares.addAll(selCond.stream().sorted().map(Participante::getPuntosEncuentro).
                            collect(ArrayList::new, ArrayList::add, ArrayList::addAll));

                    int mejorCoste = Integer.MAX_VALUE;
                    Map<Participante, Lugar> mejorLugaresIda = null, mejorLugaresVuelta = null;

                    for (List<Lugar> selLugares : new Combinador<>(posiblesLugares)) {
                        Map<Participante, Lugar> lugaresIda, lugaresVuelta;
                        lugaresIda = new HashMap<>();
                        lugaresVuelta = new HashMap<>();
                        Iterator<Lugar> il = selLugares.subList(nParticipantesDia, selLugares.size()).iterator();
                        for (int i = 0; i < nParticipantesDia; i++) {
                            lugaresIda.put(participantesDia.get(i), selLugares.get(i));
                            lugaresVuelta.put(participantesDia.get(i), selCond.contains(participantesDia.get(i))
                                    ? il.next() : selLugares.get(i));
                        }
                        Map<Integer, Map<Lugar, Integer>> plazasDisponiblesIda = selCond.stream()
                                .collect(Collectors.groupingBy(p -> participanteHorario.get(p).getEntrada(), Collectors.groupingBy(lugaresIda::get, Collectors.summingInt(Participante::getPlazasCoche))));

                        Map<Integer, Map<Lugar, Integer>> plazasDisponiblesVuelta = selCond.stream()
                                .collect(Collectors.groupingBy(p -> participanteHorario.get(p).getSalida(), Collectors.groupingBy(lugaresVuelta::get, Collectors.summingInt(Participante::getPlazasCoche))));
                        // Para comprobar, vemos los participantes, sus entradas y salidas
                        Map<Integer, Map<Lugar, Long>> plazasNecesariasIda = horariosDia.stream()
                                .collect(Collectors.groupingBy(Horario::getEntrada, Collectors.groupingBy(h -> lugaresIda.get(h.getParticipante()), Collectors.counting())));

                        Map<Integer, Map<Lugar, Long>> plazasNecesariasVuelta = horariosDia.stream()
                                .collect(Collectors.groupingBy(Horario::getSalida, Collectors.groupingBy(h -> lugaresVuelta.get(h.getParticipante()), Collectors.counting())));

                        if (plazasNecesariasIda.entrySet().stream().allMatch(e -> e.getValue().entrySet().stream().allMatch(ll -> ll.getValue() <= plazasDisponiblesIda.get(e.getKey()).getOrDefault(ll.getKey(), 0)))
                                && plazasNecesariasVuelta.entrySet().stream().allMatch(e -> e.getValue().entrySet().stream().allMatch(ll -> ll.getValue() <= plazasDisponiblesVuelta.get(e.getKey()).getOrDefault(ll.getKey(), 0)))) {
                            // Calculamos coste
                            int coste = participantesDia.stream().mapToInt(
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
                        AsignacionDiaSimple asignacionDiaSimple = new AsignacionDiaSimple(selCond, mejorLugaresIda, mejorLugaresVuelta, mejorCoste);
                        solucionesDia.add(asignacionDiaSimple);
                        mapaParticipantesAsignacionDia.put(selCond, asignacionDiaSimple); //Para acelerar la busqueda después
                    }
                }
            }
            solucionesDia.sort(null); //Ordenamos la soluciones parciales tras el barajado
            solucionesCandidatas.put(d, solucionesDia);
            mapaParticipantesSoluciones.put(d, mapaParticipantesAsignacionDia);
        }
    }
}
