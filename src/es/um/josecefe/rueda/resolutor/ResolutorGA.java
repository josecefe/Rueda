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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static es.um.josecefe.rueda.resolutor.Pesos.*;
import static java.util.stream.Collectors.*;

/**
 * Implementa la resolución del problema de la Rueda mediante el empleo de un
 * algoritmo genético.
 *
 * @author josecefe
 */
public class ResolutorGA extends Resolutor {

    private final static boolean DEBUG = false;
    private final static boolean ESTADISTICAS = true;
    private final static int IMP_CADA_CUANTAS_GEN = 100;

    private final static int TAM_POBLACION_DEF = 1000;
    private final static double PROB_MUTACION_DEF = 0.015;
    private final static int TAM_TORNEO_DEF = 5;
    private final static int N_GENERACIONES_DEF = 1000;
    private final static int TIEMPO_MAXIMO_DEF = Integer.MAX_VALUE;
    private final static int OBJ_APTITUD_DEF = 0;
    private final static int TAM_ELITE_DEF = 5;
    private final static int TAM_EXTRANJERO_DEF = TAM_ELITE_DEF;
    private final static int MAX_ESTANCADO_DEF = N_GENERACIONES_DEF / 10;

    protected Estrategia estrategia = Estrategia.EQUILIBRADO;

    private Set<Horario> horarios;
    private Dia[] dias;
    private Participante[] participantes;
    private float[] coefConduccion;
    private Map<Dia, List<AsignacionDiaV5>> solCandidatasDiarias;
    private Map<Dia, AsignacionDia> solucionFinal;
    private boolean[] participantesConCoche;
    private int tamPoblacion;
    private double probMutacion;
    private int numGeneraciones;
    private long tiempoMaximo;
    private int objAptitud;
    private EstadisticasGA estGlobal = new EstadisticasGA();
    private int tournamentSize;
    private int tamElite;
    private int tamExtranjero;
    private int maxEstancado;

    public ResolutorGA() {
        this(TAM_POBLACION_DEF, PROB_MUTACION_DEF, N_GENERACIONES_DEF, TIEMPO_MAXIMO_DEF, OBJ_APTITUD_DEF, TAM_TORNEO_DEF, TAM_ELITE_DEF, TAM_EXTRANJERO_DEF, MAX_ESTANCADO_DEF);
    }

    public ResolutorGA(int tamPoblacion, double probMutacion, int nGeneraciones, long maxTime, int objAptitud, int tamTorneo, int tamElite, int tamExtranjero, int maxEstancado) {
        this.tamPoblacion = tamPoblacion;
        this.probMutacion = probMutacion;
        this.numGeneraciones = nGeneraciones;
        this.tiempoMaximo = maxTime;
        this.objAptitud = objAptitud;
        this.tournamentSize = tamTorneo;
        this.tamElite = tamElite;
        this.tamExtranjero = tamExtranjero;
        this.maxEstancado = maxEstancado;
    }

    public void setProbMutacion(double probMutacion) {
        this.probMutacion = probMutacion;
    }

    public void setTamPoblacion(int tamPoblacion) {
        this.tamPoblacion = tamPoblacion;
    }

    public void setTiempoMaximo(long tiempoMaximo) {
        this.tiempoMaximo = tiempoMaximo;
    }

    public void setNumGeneraciones(int numGeneraciones) {
        this.numGeneraciones = numGeneraciones;
    }

    private void inicializa() {
        continuar = true;
        dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        participantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);
        participantesConCoche = new boolean[participantes.length];
        for (int i = 0; i < participantes.length; i++) {
            participantesConCoche[i] = participantes[i].getPlazasCoche() > 0;
        }

        Map<Participante, Long> vecesParticipa = horarios.stream().collect(groupingBy(Horario::getParticipante, counting()));
        int maxVecesParticipa = (int) vecesParticipa.values().stream().mapToLong(Long::longValue).max().orElse(0);

        coefConduccion = new float[participantes.length];
        for (int i = 0; i < coefConduccion.length; i++) {
            coefConduccion[i] = (float) maxVecesParticipa / vecesParticipa.get(participantes[i]) - 0.001f; // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }
        solucionFinal = null;

        // Vamos a trabajar día a día
        solCandidatasDiarias = IntStream.range(0, dias.length).parallel().boxed().collect(toConcurrentMap(ind -> dias[ind], indDia -> {
            //for (int indDia = 0; indDia < dias.length; indDia++) {
            Dia d = dias[indDia];
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
                    posiblesLugares.addAll(selCond.stream().map(Participante::getPuntosEncuentro).
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
                            int coste = Stream.of(participantesDia).mapToInt(e -> e.getPuntosEncuentro().indexOf(lugaresIda.get(e)) + e.getPuntosEncuentro().indexOf(lugaresVuelta.get(e))).sum();// + selCond.size()*2;

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
                    }
                }
            }
            return solucionesDia;
        }));

        if (ESTADISTICAS) {
            int[] tamanosNivel = solCandidatasDiarias.keySet().stream().mapToInt(k -> solCandidatasDiarias.get(k).size()).toArray();
            double totalPosiblesSoluciones = IntStream.of(tamanosNivel).mapToDouble(i -> (double) i).reduce(1.0, (a, b) -> a * b);
            if (DEBUG) {
                System.out.println("Nº de posibles soluciones: " + IntStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                        + totalPosiblesSoluciones);
            }
        }
    }

    private Individuo tournamentSelection(List<Individuo> pop, int tamTorneo) {
        // Create a tournament population
        Individuo ind, mejor = null;
        int mejorAdaptacion = Integer.MAX_VALUE;
        // For each place in the tournament get a random individual
        for (int i = 0; i < tamTorneo; i++) {
            ind = pop.get(ThreadLocalRandom.current().nextInt(pop.size()));
            if (ind.getAptitud() < mejorAdaptacion) {
                mejorAdaptacion = ind.getAptitud();
                mejor = ind;
            }
        }
        // Get the fittest
        return mejor;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        this.horarios = horarios;
        if (horarios.isEmpty()) {
            return Collections.emptyMap();
        }

        if (ESTADISTICAS) {
            estGlobal.inicia();
        }

        inicializa();

        if (ESTADISTICAS) {
            estGlobal.setNumGeneraciones(numGeneraciones);
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.getTiempoString());
            }
        }

        List<Individuo> poblacion = Stream.generate(Individuo::new).limit(tamPoblacion).collect(toList());
        Individuo mejor = poblacion.stream().min(Individuo::compareTo).orElseGet(Individuo::new);

        if (DEBUG) {
            System.out.println("Mejor Población Inicial: " + mejor);
            System.out.println("Est. Población Inicial: " + poblacion.stream().mapToInt(Individuo::getAptitud).summaryStatistics());
        }
        int nGen = 0, estancado = 0;
        //AHORA VA EL ESQUEMA DEL GA
        long ti = System.currentTimeMillis();
        while (System.currentTimeMillis() - ti < tiempoMaximo && nGen++ < numGeneraciones && mejor.getAptitud() > objAptitud && continuar) {
            // Paso 1 y 2: Seleccionar padres, combinar y después mutar...
            List<Individuo> elite = null;
            if (tamElite > 1) {
                poblacion.sort(null); // Ordenacion natural
                elite = poblacion.subList(0, tamElite); // Creamos el grupo de elite
            }
            final List<Individuo> antPoblacion = poblacion;
            poblacion = poblacion.parallelStream().map(ind -> ind.cruze(tournamentSelection(antPoblacion, tournamentSize))).map(ind -> ind.mutacion(probMutacion)).collect(toList());

            if (ESTADISTICAS) {
                estGlobal.incGeneracion();
                estGlobal.addGenerados(poblacion.size());
            }

            if (tamElite > 0) {
                poblacion = poblacion.subList(0, tamPoblacion); // Para controlar el tamaño de la población
            }

            // Añadimos el mejor que teníamos hasta ahora a la nueva población
            if (tamElite > 0) {
                if (tamElite == 1) {
                    poblacion.add(mejor);
                } else {
                    if (elite != null) {
                        poblacion.addAll(elite);
                    }
                }
            }

            if (tamExtranjero > 0) {
                poblacion.addAll(Stream.generate(Individuo::new).limit(tamExtranjero).collect(toList()));
            }
            // Paso 3: Elegir al mejor y actualizar estadisticas
            Individuo nuevoMejor = poblacion.stream().min(Individuo::compareTo).orElseGet(Individuo::new);

            if (nuevoMejor.compareTo(mejor) < 0) {
                mejor = nuevoMejor;
                if (ESTADISTICAS) {
                    estGlobal.setFitness(mejor.getAptitud()).actualizaProgreso();
                }
                estancado = 0;
            } else if (++estancado >= maxEstancado) {
                if (DEBUG) {
                    System.out.format("******Estamos estancados durante %,d generaciones, vamos a regenerar la población conservando solo el mejor****\n", estancado);
                }
                poblacion = Stream.generate(Individuo::new).limit(tamPoblacion - 1).collect(toList());
                poblacion.add(mejor);
                maxEstancado += estancado; // Subimos el umbral para no regenerar en exceso
                estancado = 0;
            }

            if (ESTADISTICAS && nGen % IMP_CADA_CUANTAS_GEN == 0) {
                estGlobal.actualizaProgreso();
                if (DEBUG) {
                    System.out.format("Generación %d -> mejor=%s\n", nGen, mejor);
                    System.out.println(" - Est. población: " + poblacion.stream().mapToInt(Individuo::getAptitud).summaryStatistics());
                    System.out.println(" - Est. algoritmo:" + estGlobal);
                }
            }
        }

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(mejor.getAptitud()).actualizaProgreso();
            if (DEBUG) {
                System.out.println("====================");
                System.out.println("Estadísticas finales");
                System.out.println("====================");
                System.out.println(estGlobal);
                System.out.println("Solución final=" + mejor);
                System.out.println("-----------------------------------------------");
            }
        }
        solucionFinal = mejor.getGenes();
        return solucionFinal;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucionFinal;
    }

    @Override
    public Estadisticas getEstadisticas() {
        return estGlobal;
    }

    /**
     * Fija la estrategia de optimización
     *
     * @param estrategia Tipo de estrategia a seguir para la optimización
     */
    @Override
    public void setEstrategia(Estrategia estrategia) {
        this.estrategia = estrategia;
    }

    final private class Individuo implements Comparable<Individuo> {
        private final Map<Dia, AsignacionDia> genes;
        private final int aptitud;

        Individuo() {
            genes = Stream.of(dias).collect(toMap(Function.identity(),
                    d -> solCandidatasDiarias.get(d).get(ThreadLocalRandom.current().nextInt(solCandidatasDiarias.get(d).size()))));
            aptitud = calculaAptitud(genes);
        }

        //Creamos un individuo con estos genes
        Individuo(Map<Dia, AsignacionDia> genes) {
            this.genes = genes;
            aptitud = calculaAptitud(genes);
        }

        private int calculaAptitud(Map<Dia, AsignacionDia> genes) {
            Map<Participante, Integer> vecesCoche = genes.values().stream().flatMap(a -> a.getConductores().stream()).collect(Collectors.groupingBy(Function.identity(), reducing(0, e -> 1, Integer::sum)));
            IntSummaryStatistics est = IntStream.range(0, participantes.length).filter(i -> participantesConCoche[i]).map(i -> Math.round(vecesCoche.getOrDefault(participantes[i], 0) * coefConduccion[i])).summaryStatistics();
            int apt = est.getMax() * PESO_MAXIMO_VECES_CONDUCTOR + (est.getMax() - est.getMin()) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + genes.values().stream().mapToInt(AsignacionDia::getCoste).sum();
            if (estrategia == Estrategia.MINCONDUCTORES)
                apt += est.getSum() * PESO_TOTAL_CONDUCTORES;

            return apt;
        }

        /**
         * Crea un hijo como resultado del cruce de los genes de este individuo
         * y el otro padre
         *
         * @param otroPadre El otro individuo que sera padre de nuevo individuo,
         *                  aportando sus genes
         * @return nuevo individuo resultado del cruce de los genes de los
         * progenitores
         */
        Individuo cruze(Individuo otroPadre) {
            Map<Dia, AsignacionDia> genesHijo = Stream.of(dias).collect(toMap(Function.identity(),
                    d -> ThreadLocalRandom.current().nextBoolean() ? genes.get(d) : otroPadre.getGenes().get(d)));

            return new Individuo(genesHijo);
        }

        /**
         * Crea una mutación a partir de este individuo
         *
         * @param probMutacionGen valor entre 0 y 1 indicando la probabilidad de
         *                        mutar de cada gen
         * @return
         */
        Individuo mutacion(double probMutacionGen) {
            Map<Dia, AsignacionDia> genesHijo = Stream.of(dias).collect(toMap(Function.identity(),
                    d -> ThreadLocalRandom.current().nextDouble() < probMutacionGen
                            ? solCandidatasDiarias.get(d).get(ThreadLocalRandom.current().nextInt(solCandidatasDiarias.get(d).size()))
                            : genes.get(d)));

            return new Individuo(genesHijo);
        }

        public Map<Dia, AsignacionDia> getGenes() {
            return genes;
        }

        int getAptitud() {
            return aptitud;
        }

        @Override
        public int compareTo(Individuo o) {
            //return Double.compare(getCosteEstimado(), o.getCosteEstimado());
            return Integer.compare(getAptitud(), o.getAptitud());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.genes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Individuo other = (Individuo) obj;
            return Objects.equals(this.genes, other.genes);
        }

        @Override
        public String toString() {
            return String.format("Individuo{aptitud=%,d, genes=%s}", getAptitud(), getGenes());
        }

    }
}
