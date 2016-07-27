/**
 *
 */
package es.um.josecefe.rueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.InvalidRepresentationException;
import org.apache.commons.math3.genetics.MutationPolicy;
import org.apache.commons.math3.genetics.OnePointCrossover;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.StoppingCondition;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.Well44497b;

/**
 * Implementa la resolución del problema de la Rueda mediante el empleo de un
 * algoritmo genético.
 *
 * @author josec
 *
 */
public class ResolutorDS implements Resolutor {

    // Depuración y estadisticas
    private final static boolean DEBUG = true;
    private final static boolean ESTADISTICAS = true;
    private final static int IMP_CADA_CUANTAS_GEN = 100;

    // Algoritmo genetico
    private final static int TAM_POBLACION_DEF = 1000;
    private final static double PROB_MUTACION_DEF = 0.015;
    private final static int TAM_TORNEO_DEF = 2;
    private final static int N_GENERACIONES_DEF = 10000;
    private final static int TIEMPO_MAXIMO_DEF = Integer.MAX_VALUE;
    private final static int OBJ_APTITUD_DEF = 0;
    private final static int TAM_ELITE_DEF = 100;
    private final static int TAM_EXTRANJERO_DEF = TAM_ELITE_DEF;
    private final static int MAX_ESTANCADO_DEF = N_GENERACIONES_DEF / 10;

    // Optimizador local
    final private static int MAX_ITER_LOCAL_SEARCH_DEF = 300; //Multiplicar por nº de dias
    final private static double STOP_FITNESS_DEF = 0;
    final private static boolean ACTIVE_CMA_DEF = true;
    final private static int DIAGONAL_ONLY_DEF = 0;
    final private static int CHECK_FEASABLE_COUNT_DEF = 1;
    private final static boolean ESTADISTICAS_OPT_LOCAL_DEF = false;

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

    public ResolutorDS(Set<Horario> horarios) {
        this(horarios, TAM_POBLACION_DEF, PROB_MUTACION_DEF, N_GENERACIONES_DEF, TIEMPO_MAXIMO_DEF, OBJ_APTITUD_DEF, TAM_TORNEO_DEF, TAM_ELITE_DEF, TAM_EXTRANJERO_DEF, MAX_ESTANCADO_DEF);
    }

    public ResolutorDS(Set<Horario> horarios, int tamPoblacion, double probMutacion, int nGeneraciones, long maxTime, int objAptitud, int tamTorneo, int tamElite, int tamExtranjero, int maxEstancado) {
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
        Arrays.fill(uB, Math.nextDown(1.0)); // Rellenamos con el nº más cercano a 1 por debajo
        Arrays.fill(range, 1.0 / 3.0); // El parametro sigma debe ser 1/3 del espacio de busqueda
//        for (int i = 0; i < dias.length; i++) {
//            uB[i] = solCanDiaMatrix[i].length - 1; // Limite superior (la variable va ser el indice del array)
//            range[i] = uB[i] / 3; // El parametro sigma debe ser 1/3 del espacio de busqueda
//        }

//        OptimizationData sigma = new CMAESOptimizer.Sigma(range);
//        OptimizationData popSize = new CMAESOptimizer.PopulationSize((int) (4 + Math.floor(3 * Math.log(dias.length))));
//        SimpleBounds bounds = new SimpleBounds(new double[dias.length], uB);
//        ObjectiveFunction objective = new ObjectiveFunction(this::calculaAptitud);
//        final MaxEval maxEvaluations = new MaxEval(MAX_ITER_LOCAL_SEARCH_DEF * dias.length);
        // El genetico de Apache Commons Math
        GeneticAlgorithm ga = new GeneticAlgorithm(new OnePointCrossover<>(), 1, new RandomMutation(), probMutacion, new TournamentSelection(TAM_TORNEO_DEF));

// stopping condition
        StoppingCondition stopCond = new FixedGenerationCount(numGeneraciones);

        List<Chromosome> poblacion = Stream.generate(this::nuevoIndividuo).limit(tamPoblacion).collect(toList());
//        poblacion = poblacion.stream().map(i -> optimizadorLocal.optimize(new InitialGuess(i.getGenes()), objective,
//                GoalType.MINIMIZE, bounds, sigma, popSize, maxEvaluations)).map(p -> new Individuo(p.getPoint())).collect(toList());

        // initial population
        Population initial = new ElitisticListPopulation(poblacion, tamPoblacion, (double) tamElite / tamPoblacion);

        if (DEBUG) {
            System.out.println("Mejor Población Inicial después inicializar: " + initial.getFittestChromosome());
        }

        // run the algorithm
        Population finalPopulation = ga.evolve(initial, stopCond);

        // best chromosome from the final population
        Chromosome mejor = finalPopulation.getFittestChromosome();

        //Estadisticas finales
        if (ESTADISTICAS && DEBUG) {
            estGlobal.setFitness((int) mejor.getFitness()).updateTime();
            System.out.println("====================");
            System.out.println("Estadísticas finales");
            System.out.println("====================");
            System.out.println(estGlobal);
            System.out.println("Solución final=" + mejor);
            System.out.println("-----------------------------------------------");
        }
        solucionFinal = ((Individuo) mejor).getSolucion();
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

    int calculaAptitud(List<Double> genes) {
        Map<Participante, Integer> vecesCoche
                = IntStream.range(0, genes.size()).mapToObj(d -> solCanDiaMatrix[d][(int) (genes.get(d) * solCanDiaMatrix[d].length)].getConductores()).flatMap(Collection::stream).collect(Collectors.groupingBy(Function.identity(), reducing(0, e -> 1, Integer::sum)));
        IntSummaryStatistics est = IntStream.range(0, participantes.length).filter(i -> participantesConCoche[i]).map(i -> {
            final int vecesCorregido = Math.round(vecesCoche.getOrDefault(participantes[i], 0) * coefConduccion[i]);
            return vecesCorregido;
        }).summaryStatistics();
        return -(est.getMax() * 1000 + (est.getMax() - est.getMin()) * 100 + IntStream.range(0, genes.size()).mapToObj(d -> getAsignacionDia(d, genes)).mapToInt(AsignacionDia::getCoste).sum());
    }

    AsignacionDiaV5 getAsignacionDia(int dia, List<Double> genes) {
        return solCanDiaMatrix[dia][(int) (genes.get(dia) * solCanDiaMatrix[dia].length)];
    }

    Individuo nuevoIndividuo() {
        Double[] genes = new Double[dias.length];
        for (int i = 0; i < dias.length; i++) {
            genes[i] = ThreadLocalRandom.current().nextDouble();
        }
        return new Individuo(genes);
    }

    final private class Individuo extends AbstractListChromosome<Double> {

        //Creamos un individuo con estos genes
        Individuo(Double[] genes) {
            super(genes);
        }

        private Individuo(List<Double> genes) {
            super(genes);
        }

        public Map<Dia, AsignacionDia> getSolucion() {
            Map<Dia, AsignacionDia> sol = new HashMap<>(getRepresentation().size());
            for (int d = 0; d < dias.length; d++) {
                sol.put(dias[d], getAsignacionDia(d, getRepresentation()));
            }
            return sol;
        }

        @Override
        protected boolean isSame(Chromosome another) {
            if (another == null) {
                return false;
            }
            if (another.getClass() != this.getClass()) {
                return false;
            }
            return this.getRepresentation().equals(((Individuo) another).getRepresentation());
        }

        @Override
        public double fitness() {
            return calculaAptitud(getRepresentation());
        }

        public int getAptitud() {
            return (int) getFitness();
        }

        @Override
        protected void checkValidity(List<Double> chromosomeRepresentation) throws InvalidRepresentationException {
            //Nada
        }

        @Override
        public AbstractListChromosome<Double> newFixedLengthChromosome(List<Double> chromosomeRepresentation) {
            return new Individuo(chromosomeRepresentation);
        }

        @Override
        public List<Double> getRepresentation() {
            return super.getRepresentation();
        }

    }

    public class RandomMutation implements MutationPolicy {

        /**
         * {@inheritDoc}
         *
         * @throws MathIllegalArgumentException if <code>original</code> is not
         * a {@link RandomKey} instance
         */
        @Override
        public Chromosome mutate(final Chromosome original) throws MathIllegalArgumentException {
            if (!(original instanceof Individuo)) {
                throw new RuntimeException("Las clases no cuadran");
            }

            Individuo originalRk = (Individuo) original;
            List<Double> repr = originalRk.getRepresentation();
            int rInd = GeneticAlgorithm.getRandomGenerator().nextInt(repr.size());

            List<Double> newRepr = new ArrayList<>(repr);
            newRepr.set(rInd, GeneticAlgorithm.getRandomGenerator().nextDouble());

            return originalRk.newFixedLengthChromosome(newRepr);
        }

    }

}
