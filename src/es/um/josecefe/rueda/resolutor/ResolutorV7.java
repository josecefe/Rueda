/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toMap;

/**
 * @author josec
 *
 */
/**
 * Resolutor
 */
public class ResolutorV7 extends Resolutor {

    private final static boolean DEBUG = true;
    private final static boolean ESTADISTICAS = true;
    private final static long CADA_EXPANDIDOS = 100000L;
    private Set<Horario> horarios;
    private Dia[] dias;
    private Participante[] participantes;
    private double[] coefConduccion;
    private Map<Dia, List<AsignacionDiaV5>> solucionesCandidatas;
    private Map<Dia, AsignacionDiaV5> solucionFinal;
    private int[] tamanosNivel;
    private double[] nPosiblesSoluciones;
    private double totalPosiblesSoluciones;
    //private Map<Dia, double[]> estadisticasDia;
    private int[][] maxVecesCondDia;
    private int[][] minVecesCondDia;
    private int[] peorCosteDia;
    private int[] mejorCosteDia;
    private boolean[] participantesConCoche;
    private double pesoCotaInferior;
    private double cotaInferiorCorte;
    private Nodo RAIZ;
    private EstadisticasV1 estGlobal = new EstadisticasV1();

    public ResolutorV7() {

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
        OptionalLong maxVecesParticipa = vecesParticipa.values().stream().mapToLong(Long::longValue).max();

        coefConduccion = Stream.of(participantes).mapToDouble(p -> (double) maxVecesParticipa.getAsLong() / vecesParticipa.get(p) - 0.001).toArray(); // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren

        maxVecesCondDia = new int[dias.length][participantes.length];
        minVecesCondDia = new int[dias.length][participantes.length];

        peorCosteDia = new int[dias.length];

        mejorCosteDia = new int[dias.length];

        Arrays.fill(mejorCosteDia, Integer.MAX_VALUE);

        solucionFinal = null;

        // Vamos a trabajar día a día
        solucionesCandidatas = IntStream.range(0, dias.length).parallel().boxed().collect(toConcurrentMap(ind -> dias[ind], indDia -> {
            //for (int indDia = 0; indDia < dias.length; indDia++) {
            Dia d = dias[indDia];
            Arrays.fill(minVecesCondDia[indDia], 1); //El minimo lo ponemos en 1 en ausencia de informacion
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
            return solucionesDia;
        }));

        if (ESTADISTICAS) {
            tamanosNivel = solucionesCandidatas.keySet().stream().mapToInt(k -> solucionesCandidatas.get(k).size()).toArray();
            totalPosiblesSoluciones = IntStream.of(tamanosNivel).mapToDouble(i -> (double) i).reduce(1.0, (a, b) -> a * b);
            if (DEBUG) {
                System.out.println("Nº de posibles soluciones: " + IntStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                        + totalPosiblesSoluciones);
            }
            double acum = 1.0;
            nPosiblesSoluciones = new double[tamanosNivel.length];

            for (int i = tamanosNivel.length - 1; i >= 0; i--) {
                nPosiblesSoluciones[i] = acum;
                acum = acum * tamanosNivel[i];
            }
        }

        for (int i = peorCosteDia.length - 2; i >= 0; i--) {
            peorCosteDia[i] += peorCosteDia[i + 1];
            mejorCosteDia[i] += mejorCosteDia[i + 1];
            for (int j = 0; j < minVecesCondDia[i].length; j++) {
                minVecesCondDia[i][j] += minVecesCondDia[i + 1][j];
                maxVecesCondDia[i][j] += maxVecesCondDia[i + 1][j];
            }
        }
    }

    @Override
    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios) {
        return resolver(horarios, 0.5);
    }

    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios, double pesoCotaInf) {
        return resolver(horarios, Double.POSITIVE_INFINITY, Math.min(1, Math.max(0, pesoCotaInf)));
    }

    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios, double cotaInfCorte, double pesoCotaInf) {
        this.horarios = horarios;
        if (horarios.isEmpty()) {
            return Collections.emptyMap();
        }

        if (ESTADISTICAS) {
            estGlobal.iniciaTiempo();
        }

        inicializa();

        if (ESTADISTICAS) {
            estGlobal.setTotalPosiblesSoluciones(totalPosiblesSoluciones);
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.getTiempo());
            }
        }
        // Preparamos el algoritmo
        pesoCotaInferior = pesoCotaInf;
        RAIZ = new Nodo();
        Nodo actual = RAIZ;
        Nodo mejor = actual;
        cotaInferiorCorte = cotaInfCorte; //Lo tomamos como cota superior
        ArrayDeque<Nodo> pilaNodosVivos = new ArrayDeque<>(); // Inicialmente es una cola LIFO (pila)
        Collection<Nodo> LNV = pilaNodosVivos;
        Supplier<Nodo> opPull = pilaNodosVivos::removeLast; //Para controlar si es pila o cola, inicialmente pila
        LNV.add(actual);

        // Bucle principal
        do {
            actual = opPull.get();
            if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS == 0L) {
                estGlobal.setFitness((int) (cotaInferiorCorte * 1000)).actualizaProgreso();
                if (DEBUG) {
                    System.out.format("> LNV=%,d, ", LNV.size());
                    System.out.println(estGlobal);
                    System.out.println("-- Trabajando con " + actual);
                }
            }
            if (actual.getCotaInferior() < cotaInferiorCorte) { //Estrategia de poda: si la cotaInferior >= C no seguimos
                if (ESTADISTICAS) {
                    estGlobal.addGenerados(tamanosNivel[actual.getIndiceDia() + 1]);
                }
                if (actual.getIndiceDia() + 2 == dias.length) { // Los hijos son terminales
                    if (ESTADISTICAS) {
                        estGlobal.addTerminales(tamanosNivel[actual.getIndiceDia() + 1]);
                    }
                    Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo); //true para paralelo
                    if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                        if (mejor == RAIZ) {
                            PriorityQueue<Nodo> colaNodosVivos = new PriorityQueue<>(LNV);
                            LNV = colaNodosVivos;
                            opPull = colaNodosVivos::poll;
                            if (DEBUG) {
                                System.out.println("---- CAMBIANDO A COLA DE PRIORIDAD");
                            }
                        }

                        mejor = mejorHijo.get();
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor);
                        }
                        if (mejor.getCosteEstimado() < cotaInferiorCorte) {
                            if (DEBUG) {
                                System.out.format("** Nuevo C: Anterior=%,.3f, Nuevo=%,.3f\n", cotaInferiorCorte, mejor.getCosteEstimado());
                            }
                            final double fC = cotaInferiorCorte = mejor.getCosteEstimado();

                            // Limpiamos la lista de nodos vivos de los que no cumplan...
                            int antes;
                            if (ESTADISTICAS) {
                                estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getIndiceDia()]).sum());
                                estGlobal.setFitness((int) (cotaInferiorCorte * 1000)).actualizaProgreso();
                                antes = LNV.size();
                            }
                            boolean removeIf = LNV.removeIf(n -> n.getCotaInferior() >= fC);
                            if (ESTADISTICAS && DEBUG && removeIf) {
                                System.out.format("** Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size());
                            }
                        }
                    }
                } else { // Es un nodo intermedio
                    final double Corte = cotaInferiorCorte;
                    List<Nodo> lNF = actual.generaHijos(true).filter(n -> n.getCotaInferior() < Corte).collect(toList()); //PARALELO poner true
                    OptionalDouble menorCotaSuperior = lNF.stream().mapToDouble(Nodo::getCotaSuperior).min();
                    if (menorCotaSuperior.isPresent() && menorCotaSuperior.getAsDouble() < cotaInferiorCorte) { // Mejora de C
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,.3f, Nuevo=%,.3f\n", cotaInferiorCorte, menorCotaSuperior.getAsDouble());
                        }
                        cotaInferiorCorte = menorCotaSuperior.getAsDouble(); //Actualizamos C
                        final double fC = cotaInferiorCorte;
                        lNF.removeIf(n -> n.getCotaInferior() >= fC); //Recalculamos lNF
                        // Limpiamos la LNV
                        int antes;
                        if (ESTADISTICAS) {
                            estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getIndiceDia()]).sum());
                            estGlobal.setFitness((int) (cotaInferiorCorte * 1000)).actualizaProgreso();
                            antes = LNV.size();
                        }
                        boolean removeIf = LNV.removeIf(n -> n.getCotaInferior() >= fC);
                        if (ESTADISTICAS && DEBUG && removeIf) {
                            System.out.format("## Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size());
                        }
                    }
                    if (ESTADISTICAS) {
                        estGlobal.addDescartados((tamanosNivel[actual.getIndiceDia() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getIndiceDia() + 1]);
                    }
                    LNV.addAll(lNF);
                }
            } else if (ESTADISTICAS) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.getIndiceDia()]);
            }
        } while (!LNV.isEmpty() && continuar);

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness((int) (cotaInferiorCorte * 1000)).actualizaProgreso();
            if (DEBUG) {
                System.out.println("====================");
                System.out.println("Estadísticas finales");
                System.out.println("====================");
                System.out.println(estGlobal);
                System.out.println("Solución final=" + mejor);
                System.out.println("-----------------------------------------------");
            }
        }
        // Construimos la solución final
        if (mejor.compareTo(RAIZ) < 0) {
            Iterator<AsignacionDiaV5> i = mejor.getEleccion().iterator();
            solucionFinal = Stream.of(dias).collect(toMap(Function.identity(), d -> i.next()));
        }

        return solucionFinal;
    }

    @Override
    public Map<Dia, AsignacionDiaV5> getSolucionFinal() {
        return solucionFinal;
    }

    @Override
    public Estadisticas getEstadisticas() {
        return estGlobal;
    }

    //Function<Nodo, Double> funcionCoste;
    final private class Nodo implements Comparable<Nodo> {

        private final List<AsignacionDiaV5> eleccion;
        private final int vecesConductor[];
        private final int indiceDia;
        private final double costeAcumulado;
        private final double cotaInferior;
        private final double cotaSuperior;
        private final double costeEstimado;
        //private final boolean valida;

        Nodo() {
            eleccion = Collections.emptyList();
            vecesConductor = new int[participantes.length];
            costeAcumulado = cotaInferior = 0;
            costeEstimado = cotaSuperior = Double.POSITIVE_INFINITY;
            indiceDia = -1;
            //valida = false;
        }

        Nodo(Nodo padre, AsignacionDiaV5 nuevaAsignacion) {
            indiceDia = padre.indiceDia + 1;
            eleccion = new ArrayList<>(padre.eleccion.size() + 1);
            eleccion.addAll(padre.eleccion);
            eleccion.add(nuevaAsignacion);
            costeAcumulado = padre.costeAcumulado + nuevaAsignacion.getCoste();
            vecesConductor = Arrays.copyOf(padre.vecesConductor, padre.vecesConductor.length);

            boolean[] conductores = nuevaAsignacion.getConductoresArray();
            long maximo = 0, maxCS = 0, maxCI = 0;
            long minimo = Long.MAX_VALUE, minCI = Long.MAX_VALUE, minCS = Long.MAX_VALUE;
            final boolean terminal = dias.length == indiceDia + 1;
            //int sum = 0;
            //nuevaAsignacion.getConductores().stream().forEachOrdered(ic -> ++vecesConductor[ic]);
            for (int i = 0; i < vecesConductor.length; i++) {
                if (participantesConCoche[i]) {
                    //sum = vecesConductor[i];
                    if (conductores[i]) {
                        ++vecesConductor[i];
                    }
                    long vecesConductorVirt = Math.round(vecesConductor[i] * coefConduccion[i]);

                    if (vecesConductorVirt > maximo) {
                        maximo = vecesConductorVirt;
                    }

                    if (vecesConductorVirt < minimo) {
                        minimo = vecesConductorVirt;
                    }

                    if (!terminal) {
                        long vecesConductorVirtCS = Math.round((vecesConductor[i] + maxVecesCondDia[indiceDia + 1][i]) * coefConduccion[i]);
                        if (vecesConductorVirtCS > maxCS) {
                            maxCS = vecesConductorVirtCS;
                        }
                        if (vecesConductorVirtCS < minCS) {
                            minCS = vecesConductorVirtCS;
                        }

                        long vecesConductorVirtCI = Math.round((vecesConductor[i] + minVecesCondDia[indiceDia + 1][i]) * coefConduccion[i]);
                        if (vecesConductorVirtCI > maxCI) {
                            maxCI = vecesConductorVirtCI;
                        }
                        if (vecesConductorVirtCI < minCI) {
                            minCI = vecesConductorVirtCI;
                        }
                    }
                }
            }
            if (terminal) { //Es terminal
                costeEstimado = cotaSuperior = cotaInferior = maximo + (maximo - minimo) / 10.0 + costeAcumulado / 1000.0;
                //valida = (minimo == maximo);
            } else {
                //cotaInferior = 2 * maxCI - minCS;
                //cotaSuperior = 2 * maxCS - minCI;

                cotaInferior = maxCI + (maxCI - minCS) / 10.0 + (costeAcumulado + mejorCosteDia[indiceDia + 1]) / 1000.0;
                cotaSuperior = maxCS + (maxCS - minCI) / 10.0 + (costeAcumulado + peorCosteDia[indiceDia + 1]) / 1000.0;
                costeEstimado = cotaInferior * pesoCotaInferior + cotaSuperior * (1 - pesoCotaInferior);
//                costeEstimado = cotaInferior * pesoCotaInferior + cotaSuperior*(1-pesoCotaInferior)
//                        + (costeAcumulado + mejorCosteDia[indiceDia + 1] * pesoCotaInferior 
//                        + peorCosteDia[indiceDia + 1] * (1 - pesoCotaInferior)) / 1000.0;

//                if (costeEstimado > cotaSuperior || costeEstimado < cotaInferior) //
//                {
//                    System.out.format("¡¡¡LIADA!!!: nivel=%d, max=%d, min=%d, maxCI=%d, minCI=%d, maxCS=%d, minCS=%d, inferior=%.3f, superior=%.3f, estimado=%.3f",
//                            indiceDia, maximo, minimo, maxCI, minCI, maxCS, minCS, cotaInferior, cotaSuperior, costeEstimado);
//                }
//costeEstimado = 2 * maxCS - minCS + ((costeAcumulado + mejorCosteDia[indiceDia + 1])+(costeAcumulado + peorCosteDia[indiceDia + 1]))/2000.0;
//valida = false;
            }
        }

        double getCotaSuperior() {
            return cotaSuperior;
        }

        double getCotaInferior() {
            return cotaInferior;
        }

        double getCosteEstimado() {
            return costeEstimado;
        }

        List<AsignacionDiaV5> getEleccion() {
            return eleccion;
        }

        int getIndiceDia() {
            return indiceDia;
        }

        Stream<Nodo> generaHijos(boolean paralelo) {
            return (paralelo ? solucionesCandidatas.get(dias[indiceDia + 1]).parallelStream()
                    : solucionesCandidatas.get(dias[indiceDia + 1]).stream()).map(solDia -> new Nodo(this, solDia));
        }

        @Override
        public int compareTo(Nodo o) {
            //return Double.compare(getCosteEstimado(), o.getCosteEstimado());
            return Double.compare(costeEstimado, o.costeEstimado);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.eleccion);
            hash = 23 * hash + this.indiceDia;
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
            final Nodo other = (Nodo) obj;
            if (this.indiceDia != other.indiceDia) {
                return false;
            }
            return Objects.equals(this.eleccion, other.eleccion);
        }

        @Override
        public String toString() {
            return String.format("Nodo{nivel=%d, estimado=%.3f, inferior=%.3f, superior=%.3f, eleccion=%s}", indiceDia, getCosteEstimado(), getCotaInferior(), getCotaSuperior(), eleccion);
        }
    }
}
