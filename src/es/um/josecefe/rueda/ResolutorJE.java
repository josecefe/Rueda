/**
 *
 */
package es.um.josecefe.rueda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toMap;
import org.jenetics.Crossover;
import org.jenetics.GaussianMutator;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.MeanAlterer;
import org.jenetics.MonteCarloSelector;
import org.jenetics.Mutator;
import org.jenetics.Optimize;
import org.jenetics.Phenotype;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.TournamentSelector;
import org.jenetics.engine.Codec;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.engine.EvolutionStatistics;

/**
 * Implementa la resolución del problema de la Rueda mediante el empleo de un
 * algoritmo genético.
 *
 * @author josec
 *
 */
public class ResolutorJE implements Resolutor {

    // Depuración y estadisticas
    private final static boolean DEBUG = true;
    private final static boolean ESTADISTICAS = true;

    // Algoritmo genetico
    private final static int TAM_POBLACION_DEF = 200;
    private final static double PROB_MUTACION_DEF = 0.015;
    private final static int TAM_TORNEO_DEF = 2;
    private final static int N_GENERACIONES_DEF = 10000;
    private final static int TIEMPO_MAXIMO_DEF = Integer.MAX_VALUE;
    private final static int OBJ_APTITUD_DEF = 0;
    private final static int TAM_ELITE_DEF = 100;
    private final static int TAM_EXTRANJERO_DEF = TAM_ELITE_DEF;
    private final static int MAX_ESTANCADO_DEF = N_GENERACIONES_DEF / 10;

    // Variables del algoritmo
    private final Set<Horario> horarios;
    private final Dia[] dias;
    private final Participante[] participantes;
    private final float[] coefConduccion;
    private Map<Dia, List<AsignacionDiaV5>> solCandidatasDiarias;
    private Map<Dia, AsignacionDia> solucionFinal;
    private int[] tamanosNivel;
    private double totalPosiblesSoluciones;
    private final boolean[] participantesConCoche;
    private int tamPoblacion;
    private double probMutacion;
    private int numGeneraciones;
    private long tiempoMaximo;
    private int objAptitud;
    private EstadisticasGA estGlobal;
    private int tournamentSize;
    private int tamElite;
    private int tamExtranjero;
    private int maxEstancado;
    private AsignacionDiaV5[][] solCanDiaMatrix;

    public ResolutorJE(Set<Horario> horarios) {
        this(horarios, TAM_POBLACION_DEF, PROB_MUTACION_DEF, N_GENERACIONES_DEF, TIEMPO_MAXIMO_DEF, OBJ_APTITUD_DEF, TAM_TORNEO_DEF, TAM_ELITE_DEF, TAM_EXTRANJERO_DEF, MAX_ESTANCADO_DEF);
    }

    public ResolutorJE(Set<Horario> horarios, int tamPoblacion, double probMutacion, int nGeneraciones, long maxTime, int objAptitud, int tamTorneo, int tamElite, int tamExtranjero, int maxEstancado) {
        this.horarios = horarios;
        this.tamPoblacion = tamPoblacion;
        this.probMutacion = probMutacion;
        this.numGeneraciones = nGeneraciones;
        this.tiempoMaximo = maxTime;
        this.objAptitud = objAptitud;
        this.tournamentSize = tamTorneo;
        this.tamElite = tamElite;
        this.tamExtranjero = tamExtranjero;
        this.maxEstancado = maxEstancado;

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
            coefConduccion[i] = (float) (float) maxVecesParticipa / vecesParticipa.get(participantes[i]) - 0.001f; // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }
    }

    private void inicializa() {
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
                final Set<Participante> selCond = condDia.stream().flatMap(e -> e.stream()).collect(toSet());
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                Map<Integer, Integer> plazasIda = selCond.stream()
                        .map(e -> participanteHorario.get(e)).collect(
                        groupingBy(Horario::getEntrada, summingInt(h -> h.getParticipante().getPlazasCoche())));
                Map<Integer, Integer> plazasVuelta = selCond.stream()
                        .map(e -> participanteHorario.get(e))
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
                                .collect(groupingBy(p -> participanteHorario.get(p).getEntrada(), groupingBy(p -> lugaresIda.get(p), summingInt(Participante::getPlazasCoche))));

                        Map<Integer, Map<Lugar, Integer>> plazasDisponiblesVuelta = selCond.stream()
                                .collect(groupingBy(p -> participanteHorario.get(p).getSalida(), groupingBy(p -> lugaresVuelta.get(p), summingInt(Participante::getPlazasCoche))));
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

        //Convertir el mapa de solucionesCandidatasDiarias en un array multidimensional
        solCanDiaMatrix = new AsignacionDiaV5[dias.length][];
        IntStream.range(0, dias.length).forEach(i -> solCanDiaMatrix[i] = solCandidatasDiarias.get(dias[i]).toArray(new AsignacionDiaV5[0]));

        if (ESTADISTICAS) {
            tamanosNivel = solCandidatasDiarias.keySet().stream().mapToInt(k -> solCandidatasDiarias.get(k).size()).toArray();
            totalPosiblesSoluciones = IntStream.of(tamanosNivel).mapToDouble(i -> (double) i).reduce(1.0, (a, b) -> a * b);
            System.out.println("Nº de posibles soluciones: " + IntStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                    + totalPosiblesSoluciones);
        }
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

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver() {
        if (horarios.isEmpty()) {
            return Collections.emptyMap();
        }

        long ti = System.currentTimeMillis(); // Tiempo inicial
        inicializa();

        if (ESTADISTICAS && DEBUG) {
            System.out.format("Tiempo inicializar =%,.2f s\n", (System.currentTimeMillis() - ti) / 1000.0);
        }

        if (ESTADISTICAS) {
            estGlobal = new EstadisticasGA(numGeneraciones);
        }
        // Algoritmo GENETICO de JENETICS

        // The problem domain encoder/decoder.
        final Codec<int[], IntegerGene> codec = Codec.of(
                Genotype.of(IntStream.range(0, dias.length).mapToObj(d -> IntegerChromosome.of(0, solCanDiaMatrix[d].length - 1)).collect(toList())),
                gt -> gt.stream().mapToInt(g -> g.getGene().intValue()).toArray()
        );

        final EvolutionStatistics<Integer, ?> statistics = EvolutionStatistics.ofNumber();

        final Engine<IntegerGene, Integer> engine = Engine
                .builder(this::calculaAptitud, codec)
                .alterers(
                        new MeanAlterer<>(0.25),
                        new Mutator<>(0.1))
                .populationSize(tamPoblacion)
                //.selector(new MonteCarloSelector<>())
                .selector(new RouletteWheelSelector<>())
                .optimize(Optimize.MINIMUM)
                //.offspringSelector(new RouletteWheelSelector<>())
                //.survivorsSelector(new TournamentSelector<>(3))
                .minimizing()
                .build();

        final Phenotype<IntegerGene, Integer> pt = engine.stream()
                .limit(numGeneraciones)
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());

        final int[] mejor = codec.decoder().apply(pt.getGenotype());
        solucionFinal = getSolucion(mejor);
        System.out.println(statistics);
        System.out.format("Resultado fitness=%d, genes=%s\n", pt.getFitness(), solucionFinal);

        //Estadisticas finales
        if (ESTADISTICAS && DEBUG) {
            estGlobal.setGeneracion(pt.getGeneration());
            estGlobal.addGenerados((engine.getOffspringCount() + engine.getSurvivorsCount()) * pt.getGeneration());
            estGlobal.setFitness((int) pt.getFitness()).updateTime();
            System.out.println("====================");
            System.out.println("Estadísticas finales");
            System.out.println("====================");
            System.out.println(estGlobal);
            System.out.println("Solución final=" + solucionFinal);
            System.out.println("-----------------------------------------------");
        }

        return solucionFinal;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucionFinal;
    }

    @Override
    public Optional<Estadisticas> getEstadisticas() {
        return Optional.ofNullable(estGlobal);
    }

    int calculaAptitud(int[] genes) {
        Map<Participante, Integer> vecesCoche
                = IntStream.range(0, genes.length).mapToObj(d -> solCanDiaMatrix[d][genes[d]].getConductores()).flatMap(Collection::stream).collect(Collectors.groupingBy(Function.identity(), reducing(0, e -> 1, Integer::sum)));
        IntSummaryStatistics est = IntStream.range(0, participantes.length).filter(i -> participantesConCoche[i]).map(i -> {
            final int vecesCorregido = Math.round(vecesCoche.getOrDefault(participantes[i], 0) * coefConduccion[i]);
            return vecesCorregido;
        }).summaryStatistics();
        return (est.getMax() * 1000 + (est.getMax() - est.getMin()) * 100 + IntStream.range(0, genes.length).mapToObj(d -> getAsignacionDia(d, genes)).mapToInt(AsignacionDia::getCoste).sum());
    }

    AsignacionDiaV5 getAsignacionDia(int dia, int[] genes) {
        return solCanDiaMatrix[dia][genes[dia]];
    }

    Map<Dia, AsignacionDia> getSolucion(int[] genes) {
        Map<Dia, AsignacionDia> sol = new HashMap<>(genes.length);
        for (int d = 0; d < dias.length; d++) {
            sol.put(dias[d], getAsignacionDia(d, genes));
        }
        return sol;
    }
}
