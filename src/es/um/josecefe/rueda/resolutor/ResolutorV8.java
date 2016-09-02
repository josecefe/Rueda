/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toMap;
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
public class ResolutorV8 extends Resolutor {

    private final static boolean DEBUG = false;
    private final static boolean ESTADISTICAS = true;
    private final static long CADA_EXPANDIDOS = 1000000L;
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
    private AtomicInteger cotaInferiorCorte;
    private Nodo RAIZ;
    private EstadisticasV8 estGlobal = new EstadisticasV8();

    public ResolutorV8() {

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
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        return resolver(horarios, 0.5);
    }

    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, double pesoCotaInf) {
        return resolver(horarios, Integer.MAX_VALUE, Math.min(1, Math.max(0, pesoCotaInf)));
    }

    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, int cotaInfCorte, double pesoCotaInf) {
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
                System.out.format("Tiempo inicializar =%s\n", estGlobal.getTiempoString());
            }
        }

        // Preparamos el algoritmo
        pesoCotaInferior = pesoCotaInf;
        RAIZ = new Nodo();
        Nodo actual = RAIZ;
        Nodo mejor = actual;
        cotaInferiorCorte = new AtomicInteger(cotaInfCorte); //Lo tomamos como cota superior

        mejor = branchAndBound(actual, mejor).orElse(mejor); //Cogemos el nuevo mejor

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(mejor.getCosteEstimado());
            estGlobal.actualizaProgreso();
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

    private Optional<Nodo> branchAndBound(Nodo actual, Nodo mejorPadre) {
        Nodo mejor = mejorPadre;
        //Estadisticas estGlobal = new EstadisticasV8(nSoluciones);
        if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS == 0L) {
            estGlobal.setFitness(cotaInferiorCorte.get());
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.println(estGlobal);
                System.out.println("-- Trabajando con " + actual);
            }
        }
        if (actual.getCotaInferior() < cotaInferiorCorte.get() && continuar) { //Estrategia de poda: si la cotaInferior >= C no seguimos
            if (ESTADISTICAS) {
                estGlobal.addGenerados(tamanosNivel[actual.getIndiceDia() + 1]);
            }
            if (actual.getIndiceDia() + 2 == dias.length) { // Los hijos son terminales
                if (ESTADISTICAS) {
                    estGlobal.addTerminales(tamanosNivel[actual.getIndiceDia() + 1]);
                }
                Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo); //true para paralelo

                if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                    mejor = mejorHijo.get();
                    int cota;
                    do {
                        cota = cotaInferiorCorte.get();
                    } while (mejor.getCosteEstimado() < cota && !cotaInferiorCorte.compareAndSet(cota, mejor.getCosteEstimado()));
                    if (mejor.getCosteEstimado() < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.setFitness(mejor.getCosteEstimado());
                            estGlobal.actualizaProgreso();
                        }
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor);
                            System.out.format("** Nuevo (nueva solución) C: Anterior=%,d, Nuevo=%,d\n", cota, mejor.getCosteEstimado());
                        }
                    }
                }
            } else { // Es un nodo intermedio
                List<Nodo> lNF = actual.generaHijos(true).filter(n -> n.getCotaInferior() < cotaInferiorCorte.get()).collect(toList()); //PARALELO poner true
                OptionalInt menorCotaSuperior = lNF.stream().mapToInt(Nodo::getCotaSuperior).min();
                if (menorCotaSuperior.isPresent()) {
                    int cota;
                    do {
                        cota = cotaInferiorCorte.get();
                    } while (menorCotaSuperior.getAsInt() < cota && !cotaInferiorCorte.compareAndSet(cota, menorCotaSuperior.getAsInt()));
                    if (menorCotaSuperior.getAsInt() < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.setFitness(menorCotaSuperior.getAsInt());
                            estGlobal.actualizaProgreso();
                        }
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cota, menorCotaSuperior.getAsInt());
                        }
                    }
                    lNF.removeIf(n -> n.getCotaInferior() >= cotaInferiorCorte.get()); //Recalculamos lNF
                    // Limpiamos la LNV
                }
                if (ESTADISTICAS) {
                    estGlobal.addDescartados((tamanosNivel[actual.getIndiceDia() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getIndiceDia() + 1]);
                }
                final Nodo mejorAhora = mejor;
                Optional<Nodo> mejorHijo = lNF.parallelStream().sorted().map(n -> branchAndBound(n, mejorAhora)).filter(Optional<Nodo>::isPresent).map(Optional<Nodo>::get).min(Nodo::compareTo);
                if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) { // Tenemos un hijo que mejora
                    mejor = mejorHijo.get();
                }
            }
        } else if (ESTADISTICAS) {
            if (continuar) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.getIndiceDia()]);
            }
        }

        return Optional.ofNullable(mejor);
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
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
        private final int costeAcumulado;
        private final int cotaInferior;
        private final int cotaSuperior;
        private final int costeEstimado;
        //private final boolean valida;

        Nodo() {
            eleccion = Collections.emptyList();
            vecesConductor = new int[participantes.length];
            costeAcumulado = cotaInferior = 0;
            costeEstimado = cotaSuperior = Integer.MAX_VALUE;
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
            int maximo = 0, maxCS = 0, maxCI = 0;
            int minimo = Integer.MAX_VALUE, minCI = Integer.MAX_VALUE, minCS = Integer.MAX_VALUE;
            final boolean terminal = dias.length == indiceDia + 1;
            //int sum = 0;
            //nuevaAsignacion.getConductores().stream().forEachOrdered(ic -> ++vecesConductor[ic]);
            for (int i = 0; i < vecesConductor.length; i++) {
                if (participantesConCoche[i]) {
                    //sum = vecesConductor[i];
                    if (conductores[i]) {
                        ++vecesConductor[i];
                    }
                    int vecesConductorVirt = Math.round((float) (vecesConductor[i] * coefConduccion[i]));

                    if (vecesConductorVirt > maximo) {
                        maximo = vecesConductorVirt;
                    }

                    if (vecesConductorVirt < minimo) {
                        minimo = vecesConductorVirt;
                    }

                    if (!terminal) {
                        int vecesConductorVirtCS = Math.round((float) ((vecesConductor[i] + maxVecesCondDia[indiceDia + 1][i]) * coefConduccion[i]));
                        if (vecesConductorVirtCS > maxCS) {
                            maxCS = vecesConductorVirtCS;
                        }
                        if (vecesConductorVirtCS < minCS) {
                            minCS = vecesConductorVirtCS;
                        }

                        int vecesConductorVirtCI = Math.round((float) ((vecesConductor[i] + minVecesCondDia[indiceDia + 1][i]) * coefConduccion[i]));
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
                costeEstimado = cotaSuperior = cotaInferior = maximo * 1000 + (maximo - minimo) * 100 + costeAcumulado;
            } else {
                cotaInferior = maxCI * 1000 + (maxCI - minCS) * 100 + costeAcumulado + mejorCosteDia[indiceDia + 1];
                cotaSuperior = maxCS * 1000 + (maxCS - minCI) * 100 + costeAcumulado + peorCosteDia[indiceDia + 1] + 1; // Añadimos el 1 para evitar un bug que nos haría perder una solución
                costeEstimado = (int) (cotaInferior * pesoCotaInferior + cotaSuperior * (1 - pesoCotaInferior));
            }
            if (DEBUG && (cotaInferior < padre.cotaInferior || cotaSuperior > padre.cotaSuperior || cotaSuperior < cotaInferior)) {
                System.err.println("*****************\n**** ¡LIADA! --> Padre=" + padre + ", Hijo=" + this);
            }
        }

        int getCotaSuperior() {
            return cotaSuperior;
        }

        int getCotaInferior() {
            return cotaInferior;
        }

        int getCosteEstimado() {
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
            return Integer.compare(costeEstimado, o.costeEstimado);
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
            return String.format("Nodo{nivel=%d, estimado=%,d, inferior=%,d, superior=%,d, eleccion=%s}", indiceDia, getCosteEstimado(), getCotaInferior(), getCotaSuperior(), eleccion);
        }
    }

}
