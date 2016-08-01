/**
 *
 */
package es.um.josecefe.rueda;

import static java.lang.String.format;
import java.time.Duration;
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
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.Well44497b;
import org.jenetics.Alterer;
import org.jenetics.GaussianMutator;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.MeanAlterer;
import org.jenetics.Mutator;
import org.jenetics.Optimize;
import org.jenetics.Phenotype;
import org.jenetics.Population;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.TournamentSelector;
import org.jenetics.engine.Codec;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.engine.EvolutionStatistics;
import org.jenetics.engine.limit;
import static org.jenetics.internal.math.random.indexes;
import org.jenetics.internal.util.Equality;
import org.jenetics.internal.util.Hash;
import org.jenetics.internal.util.IntRef;
import org.jenetics.util.RandomRegistry;

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
    private final static int TAM_POBLACION_DEF = 20;
    private final static double PROB_MUTACION_DEF = 0.25;
    private final static double PROB_MEJORA_DEF = 0.1;
    private final static int TAM_TORNEO_DEF = 2;
    private final static int N_GENERACIONES_DEF = 200;
    private final static int TIEMPO_MAXIMO_DEF = 500;
    private final static int OBJ_APTITUD_DEF = 0;
    private final static int TAM_ELITE_DEF = 4;
    private final static int MAX_ESTANCADO_DEF = N_GENERACIONES_DEF / 5;

    // Optimizador por busqueda local
    final private static int MAX_ITER_LOCAL_SEARCH_DEF = 100; //Multiplicar por nº de dias, originalmente 300
    final private static double STOP_FITNESS_DEF = 0;
    final private static boolean ACTIVE_CMA_DEF = true;
    final private static int DIAGONAL_ONLY_DEF = 0;
    final private static int CHECK_FEASABLE_COUNT_DEF = 1;
    private final static boolean ESTADISTICAS_OPT_LOCAL_DEF = true;

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
    private double probMejora;
    private int numGeneraciones;
    private long tiempoMaximo;
    private int objAptitud;
    private EstadisticasGA estGlobal;
    private int tamTorneo;
    private int tamElite;
    private int maxEstancado;
    private AsignacionDiaV5[][] solCanDiaMatrix;

    public ResolutorJE(Set<Horario> horarios) {
        this(horarios, TAM_POBLACION_DEF, PROB_MUTACION_DEF, PROB_MEJORA_DEF, N_GENERACIONES_DEF, TIEMPO_MAXIMO_DEF, OBJ_APTITUD_DEF, TAM_TORNEO_DEF, TAM_ELITE_DEF, MAX_ESTANCADO_DEF);
    }

    public ResolutorJE(Set<Horario> horarios, int tamPoblacion, double probMutacion, double probMejora, int nGeneraciones, long maxTime, int objAptitud, int tamTorneo, int tamElite, int maxEstancado) {
        this.horarios = horarios;

        dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        participantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);
        
        
        this.tamPoblacion = tamPoblacion * dias.length;
        this.probMutacion = probMutacion;
        this.probMejora = probMejora;
        this.numGeneraciones = nGeneraciones * dias.length;
        this.tiempoMaximo = maxTime * dias.length;
        this.objAptitud = objAptitud;
        this.tamTorneo = tamTorneo;
        this.tamElite = tamElite * dias.length;
        this.maxEstancado = maxEstancado * dias.length;
        
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

        //Podemos intentar obtener una mejora de los individuos mediante la busqueda directa con optimizador directo
        CMAESOptimizer optimizadorLocal
                = new CMAESOptimizer(MAX_ITER_LOCAL_SEARCH_DEF * dias.length, STOP_FITNESS_DEF, ACTIVE_CMA_DEF, DIAGONAL_ONLY_DEF, CHECK_FEASABLE_COUNT_DEF, new Well44497b(), ESTADISTICAS_OPT_LOCAL_DEF, new SimpleValueChecker(Double.NEGATIVE_INFINITY, 0.0001));

        double[] range = new double[dias.length]; // Para el parámetro sigma
        double[] lB = new double[dias.length]; // Lo dejamos a 0, indice minimo del array
        double[] uB = new double[dias.length]; //Este hay que inicializarlo al tamaño máximo del array de suluciones - 1
        for (int i = 0; i < dias.length; i++) {
            uB[i] = Math.nextDown(solCanDiaMatrix[i].length); // Limite superior (la variable va ser el indice del array)
            range[i] = uB[i] / 3; // El parametro sigma debe ser 1/3 del espacio de busqueda
        }
        OptimizationData sigma = new CMAESOptimizer.Sigma(range);
        OptimizationData popSize = new CMAESOptimizer.PopulationSize((int) (4 + Math.floor(3 * Math.log(dias.length))));
        SimpleBounds bounds = new SimpleBounds(lB, uB);
        ObjectiveFunction objective = new ObjectiveFunction(this::calculaAptitudBL);
        final MaxEval maxEvaluations = new MaxEval(MAX_ITER_LOCAL_SEARCH_DEF * dias.length);

        // Algoritmo GENETICO de JENETICS
        // The problem domain encoder/decoder.
        final Codec<int[], IntegerGene> codec = Codec.of(
                Genotype.of(IntStream.range(0, dias.length).mapToObj(d -> IntegerChromosome.of(0, solCanDiaMatrix[d].length - 1)).collect(toList())),
                gt -> gt.toSeq().stream().mapToInt(g -> g.getGene().intValue()).toArray()
        );

        final EvolutionStatistics<Integer, ?> statistics = EvolutionStatistics.ofNumber();

        final Engine<IntegerGene, Integer> engine = Engine
                .builder(this::calculaAptitud, codec)
                .offspringFraction(1.0 - (double) tamElite / tamPoblacion)
                .alterers(
                        new MeanAlterer<>(probMutacion),
                        new Mutator<>(probMutacion),
                        new GaussianMutator<>(probMutacion),
                        new MejoraPorBusqueda((double[] genes) -> optimizadorLocal.optimize(new InitialGuess(genes), objective, GoalType.MINIMIZE, bounds, sigma, popSize, maxEvaluations).getPoint(), probMejora)
                )
                .populationSize(tamPoblacion)
                //.selector(new MonteCarloSelector<>())
                //.selector(new RouletteWheelSelector<>())
                .optimize(Optimize.MINIMUM)
                .offspringSelector(new RouletteWheelSelector<>())
                .survivorsSelector(new TournamentSelector<>(tamTorneo))
                .minimizing()
                .build();

        final Phenotype<IntegerGene, Integer> pt = engine.stream()
                .limit(limit.bySteadyFitness(maxEstancado))
                .limit(limit.byExecutionTime(Duration.ofSeconds(tiempoMaximo)))
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
        return (est.getMax() * 1000 + (est.getMax() - est.getMin()) * 100 + IntStream.range(0, genes.length).mapToObj(d -> getAsignacionDia(d, genes[d])).mapToInt(AsignacionDia::getCoste).sum());
    }

    int calculaAptitudBL(double[] genes) {
        Map<Participante, Integer> vecesCoche
                = IntStream.range(0, genes.length).mapToObj(d -> solCanDiaMatrix[d][(int) genes[d]].getConductores()).flatMap(Collection::stream).collect(Collectors.groupingBy(Function.identity(), reducing(0, e -> 1, Integer::sum)));
        IntSummaryStatistics est = IntStream.range(0, participantes.length).filter(i -> participantesConCoche[i]).map(i -> {
            final int vecesCorregido = Math.round(vecesCoche.getOrDefault(participantes[i], 0) * coefConduccion[i]);
            return vecesCorregido;
        }).summaryStatistics();
        return (est.getMax() * 1000 + (est.getMax() - est.getMin()) * 100 + IntStream.range(0, genes.length).mapToObj(d -> getAsignacionDia(d, (int) genes[d])).mapToInt(AsignacionDia::getCoste).sum());
    }

    AsignacionDiaV5 getAsignacionDia(int dia, int valGen) {
        return solCanDiaMatrix[dia][valGen];
    }
//
//    AsignacionDiaV5 getAsignacionDia(int dia, double[] genes) {
//        return solCanDiaMatrix[dia][(int)genes[dia]];
//    }

    Map<Dia, AsignacionDia> getSolucion(int[] genes) {
        Map<Dia, AsignacionDia> sol = new HashMap<>(genes.length);
        for (int d = 0; d < dias.length; d++) {
            sol.put(dias[d], getAsignacionDia(d, genes[d]));
        }
        return sol;
    }

    public static class MejoraPorBusqueda implements Alterer<IntegerGene, Integer> {

        private static final double PROB_MEJORA_DEF = 0.01;

        private final double _probability;
        private final Function<double[], double[]> optimizador;

        public MejoraPorBusqueda(Function<double[], double[]> optimizador) {
            this._probability = PROB_MEJORA_DEF;
            this.optimizador = optimizador;
        }

        public MejoraPorBusqueda(Function<double[], double[]> optimizador, double probMejora) {
            this._probability = probMejora;
            this.optimizador = optimizador;
        }

        @Override
        public int alter(Population<IntegerGene, Integer> population, long generation) {
            assert population != null : "Not null is guaranteed from base class.";

            final IntRef alterations = new IntRef(0);

            indexes(RandomRegistry.getRandom(), population.size(), _probability).forEach((int i) -> {
                final Phenotype<IntegerGene, Integer> pt = population.get(i);

                final Genotype<IntegerGene> gt = pt.getGenotype();
                final double[] genesOriginal = gt.stream().mapToDouble(g -> g.getGene().doubleValue()).toArray();
                double[] genes = optimizador.apply(genesOriginal);

                List<IntegerChromosome> pepe = IntStream.range(0, genes.length).mapToObj(ind -> IntegerChromosome.of(gt.get(ind, 0).newInstance((int) genes[ind]))).collect(toList());
                Genotype<IntegerGene> genotipo = Genotype.of(pepe);
                final Phenotype<IntegerGene, Integer> mpt = pt.newInstance(genotipo, generation);
                if (mpt.compareTo(pt) < 0) {
                    population.set(i, mpt);
                }
            });
            return alterations.value;
        }

        @Override
        public int hashCode() {
            return Hash.of(getClass()).and(super.hashCode()).value();
        }

        @Override
        public boolean equals(final Object obj) {
            return Equality.of(this, obj).test(super::equals);
        }

        @Override
        public String toString() {
            return format("%s[p=%f]", getClass().getSimpleName(), _probability);
        }
    }
}
