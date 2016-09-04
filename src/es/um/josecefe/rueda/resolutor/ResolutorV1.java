/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.AsignacionDiaV1;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * @author josec
 *
 */
/**
 * ResolutorV1
 */
public class ResolutorV1 extends Resolutor {

    private Set<Horario> horarios;
    private Dia[] dias;
    //Participante[] participantes;
    private Map<Dia, List<Set<Participante>>> solucionesCandidatas;
    private Map<Dia, AsignacionDiaV1> solucionFinal;
    private EstadisticasV1 estadisticas = new EstadisticasV1();

    @Override
    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    //Function<Nodo, Double> funcionCoste;
    final private class Nodo implements Comparable<Nodo> {

        private final List<Set<Participante>> eleccion;
        private final int indiceDia;
        private final int indiceCandidatoDia;
        private double costeEstimado;
        private double cotaInferior;
        private double cotaSuperior;

        Nodo() {
            this.eleccion = Collections.emptyList();
            cotaInferior = 0;
            costeEstimado = cotaSuperior = Double.POSITIVE_INFINITY;
            indiceCandidatoDia = -1;
            indiceDia = -1;
        }

        Nodo(List<Set<Participante>> eleccion, int indiceDia, int indiceCandidatoDia) {
            this.eleccion = eleccion;
            this.indiceDia = indiceDia;
            this.indiceCandidatoDia = indiceCandidatoDia;
            calculaCostes();
        }

        /**
         * Esta función calcula las cotas superior e inferior y el coste
         * estimado del nodo
         */
        private void calculaCostes() {
            Map<Participante, Long> vecesCoche = eleccion.stream().flatMap(Set::stream).collect(Collectors.groupingBy(Function.identity(), counting()));
            //return vecesCoche.values().stream().mapToDouble(Long::doubleValue).max().orElse(Double.POSITIVE_INFINITY);
            LongSummaryStatistics estadisticas = vecesCoche.values().stream().mapToLong(Long::longValue).summaryStatistics();

            cotaInferior = estadisticas.getMax();
            cotaSuperior = 2 * (estadisticas.getMax() + (dias.length - 1 - indiceDia)) - estadisticas.getMin();
            //cotaSuperior = cotaInferior + (dias.length - indiceDia - 1);
            costeEstimado = 2 * (estadisticas.getMax() + (dias.length - 1 - indiceDia) / 2.0) - estadisticas.getMin();
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

        List<Set<Participante>> getEleccion() {
            return eleccion;
        }

        List<Nodo> generaHijos() {
            if (esTerminal()) { //Es un nodo terminal
                return Collections.emptyList();
            }
            int id = indiceDia + 1;
            List<Set<Participante>> candList = solucionesCandidatas.get(dias[id]);
            ArrayList<Nodo> hijos = new ArrayList<>(candList.size());

            int icd = 0;
            for (Set<Participante> solDia : candList) {
                List<Set<Participante>> nuevaEleccion = new ArrayList<>(eleccion.size() + 1);
                nuevaEleccion.addAll(eleccion);
                nuevaEleccion.add(solDia);
                hijos.add(new Nodo(nuevaEleccion, id, icd++));
            }
            return hijos;
        }

        boolean esTerminal() {
            return dias.length == indiceDia + 1;
        }

        @Override
        public int compareTo(Nodo o) {
            return Double.compare(costeEstimado, o.costeEstimado);
            //return Double.compare(o.costeEstimado, costeEstimado);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.eleccion);
            hash = 23 * hash + this.indiceDia;
            hash = 23 * hash + this.indiceCandidatoDia;
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
            if (this.indiceCandidatoDia != other.indiceCandidatoDia) {
                return false;
            }
            return Objects.equals(this.eleccion, other.eleccion);
        }

        @Override
        public String toString() {
            return "Nodo{" + "eleccion=" + eleccion + ", indiceDia=" + indiceDia + ", indiceCandidatoDia=" + indiceCandidatoDia + ", costeEstimado=" + costeEstimado + ", cotaInferior=" + cotaInferior + ", cotaSuperior=" + cotaSuperior + '}';
        }

    }

    public ResolutorV1() {
    }

    private void inicializa() {
        continuar = true;
        dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        solucionesCandidatas = new HashMap<>(dias.length);
        solucionFinal = null;

        // Vamos a trabajar día a día
        for (Dia d : dias) {
            ArrayList<Set<Participante>> solucionesDia = new ArrayList<>();
            Set<Horario> horariosDia = horarios.stream().filter(h -> h.getDia() == d).collect(toSet());
            Map<Participante, Horario> participanteHorario = horariosDia.stream()
                    .collect(toMap(Horario::getParticipante, Function.identity()));
            // Para cada hora de entrada, obtenemos los conductores disponibles
            Map<Integer, Set<Participante>> entradaConductor = horariosDia.stream().filter(Horario::isCoche)
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, toSet())));
            // Para comprobar, vemos los participantes, sus entradas y salidas
            Map<Integer, Long> entradaParticipante = horariosDia.stream()
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, counting())));
            Map<Integer, Long> salidaParticipante = horariosDia.stream()
                    .collect(groupingBy(Horario::getSalida, mapping(Horario::getParticipante, counting())));
            // Generamos todas las posibilidades y a ver cuales sirven...
            List<Iterable<Set<Participante>>> conductoresDia = entradaConductor.keySet().stream()
                    .map(key -> new SubSets<>(entradaConductor.get(key), 1, entradaConductor.get(key).size())).collect(toList());
            Combinador<Set<Participante>> combinarConductoresDia = new Combinador<>(conductoresDia);
            for (List<Set<Participante>> lp : combinarConductoresDia) {
                boolean validada = true;
                Map<Integer, Integer> plazasEntrada = lp.stream().flatMap(e -> e.stream())
                        .map(e -> participanteHorario.get(e)).collect(
                        groupingBy(Horario::getEntrada, summingInt(h -> h.getParticipante().getPlazasCoche())));
                Map<Integer, Integer> plazasSalida = lp.stream().flatMap(e -> e.stream())
                        .map(e -> participanteHorario.get(e))
                        .collect(groupingBy(Horario::getSalida, summingInt(h -> h.getParticipante().getPlazasCoche())));
                for (Entry<Integer, Long> e : entradaParticipante.entrySet()) {
                    if (plazasEntrada.getOrDefault(e.getKey(), 0) < e.getValue().intValue()) {
                        validada = false;
                        break;
                    }
                }
                for (Entry<Integer, Long> e : salidaParticipante.entrySet()) {
                    if (plazasSalida.getOrDefault(e.getKey(), 0) < e.getValue().intValue()) {
                        validada = false;
                        break;
                    }
                }
                if (validada) { // Tenemos una solución prometedora
                    // Eliminamos exceso de info
                    solucionesDia.add(lp.stream().flatMap(e -> e.stream()).collect(toSet()));
                }
            }
            if (solucionesDia.size() > 0) {
                solucionesCandidatas.put(d, solucionesDia);
            } else {
                // ¡¡¡Sin solución!!!
                solucionesCandidatas = Collections.emptyMap();
                break;
            }
        }

        System.out.println("Potencialmente vamos a comprobar " + solucionesCandidatas.keySet().stream().map(k -> String.valueOf(solucionesCandidatas.get(k).size())).collect(Collectors.joining("*")) + "="
                + solucionesCandidatas.keySet().stream().mapToDouble(k -> (double) solucionesCandidatas.get(k).size()).reduce(1.0, (a, b) -> a * b));
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        this.horarios = horarios;
        inicializa();
        // Preparamos el algoritmo
        Nodo actual = new Nodo();
        Nodo mejor = actual;
        double C = actual.getCotaSuperior();
        PriorityQueue<Nodo> listaNodosVivos = new PriorityQueue<>();
        listaNodosVivos.offer(actual);

        // Bucle principal
        long expandidos = 0L, generados = 0L;
        long ti = System.currentTimeMillis();

        do {
            actual = listaNodosVivos.poll();
            if (++expandidos % 50000L == 0L) {
                System.out.println("Total expandidos hasta ahora = " + expandidos + ", tamaño LNV = " + listaNodosVivos.size() + ", tiempo transcurrido (s) = " + (System.currentTimeMillis() - ti) / 1000.0 + ", velocidad (expandidos/s) = " + (expandidos * 1000.0 / (System.currentTimeMillis() - ti)));
                System.out.println("Trabajando con " + actual);
            }
            if (actual.getCotaInferior() < C) { //Estrategia de poda: si la cotaInferior >= C no seguimos
                for (Nodo n : actual.generaHijos()) {
                    if (++generados % 5000000L == 0L) {
                        System.out.println("Total generados hasta ahora: " + generados + ", tiempo transcurrido (s) = " + (System.currentTimeMillis() - ti) / 1000.0 + ", velocidad (generados/s) = " + (generados * 1000.0 / (System.currentTimeMillis() - ti)));
                    }

                    //System.out.println("--> Hijo: "+n);
                    if (n.esTerminal()) { //Se trata de una solución completa, hay que valorarla
                        if (n.getCosteEstimado() < mejor.getCosteEstimado()) { //Mejoramos
                            mejor = n;
                            System.out.println("Mejoramos: " + mejor);
                            System.out.println("Por nueva solución, nuevo corte: Anterior=" + C + ", Nuevo=" + mejor.getCosteEstimado());
                            C = mejor.getCosteEstimado();
                        }
                    } else //Es un nodo intermedio
                     if (n.getCotaInferior() < C) {
                            //System.out.println("<-- Entro a LNV");
                            listaNodosVivos.offer(n);
                            if (n.getCotaSuperior() < C) {
                                System.out.println("Nuevo corte: Anterior=" + C + ", Nuevo=" + n.getCotaSuperior());
                                C = n.getCotaSuperior(); //Actualizamos C si es necesario
                            }
                        }
                }
            }
        } while (!listaNodosVivos.isEmpty());

        //Estadisticas finales
        estadisticas.expandidos = expandidos;
        estadisticas.generados = generados;
        estadisticas.setFitness((int) (C * 1000)).actualizaProgreso();
        System.out.println("Estadísticas finales");
        System.out.println("====================");
        System.out.println("Tiempo transcurrido (s) = " + (System.currentTimeMillis() - ti) / 1000.0);
        System.out.println("Total expandidos: " + expandidos + ", velocidad (expandidos/s) = " + (expandidos * 1000.0 / (System.currentTimeMillis() - ti)));
        System.out.println("Total generados: " + generados + ", velocidad (generados/s) = " + (generados * 1000.0 / (System.currentTimeMillis() - ti)));

        // Construimos la solución final
        if (mejor.getCosteEstimado() != Double.POSITIVE_INFINITY) {
            Iterator<Set<Participante>> i = mejor.getEleccion().iterator();
            solucionFinal = Stream.of(dias).collect(toMap(Function.identity(),
                    d -> new AsignacionDiaV1(i.next(), null, null, 0)));
        }

        return solucionFinal;
    }

    /**
     * Resuelve el problema de optimización por el metodo de fuerza bruta,
     * probando todas las combinaciones posibles.
     *
     * @return la solución, si existe. <code>null</code> si no hay solución
     * posible.
     */
    public Map<Dia, ? extends AsignacionDia> resolverCombinador() {
        // Todas las soluciones posibles por día, para luego combinar...
        List<Set<Participante>> mejorSolucion = null; //La no solución tiene el mayor coste posible
        double costeMejor = Double.POSITIVE_INFINITY;

        inicializa();
        // Combinación final: todas las combinaciones posibles de todos los días entre si
        // Vamos a usar fuerza bruta de momento, valorando cada solución y guardando solo la mejor hasta el momento

        List<Iterable<Set<Participante>>> listaCandidatosDia = solucionesCandidatas.keySet().stream().sorted().map(d -> solucionesCandidatas.get(d)).collect(toList());

        Combinador<Set<Participante>> combinarDias = new Combinador<>(listaCandidatosDia);
        long procesadas = 0L;
        long ti = System.currentTimeMillis();
        for (List<Set<Participante>> solucionActual : combinarDias) {
            double costeActual = costeSolucion(solucionActual);
            if (costeActual < costeMejor) {
                mejorSolucion = solucionActual;
                costeMejor = costeActual;
                System.out.println("Mejoramos solución = " + mejorSolucion + ", coste = " + costeMejor);
            }
            if (++procesadas % 10000000L == 0L) {
                System.out.println("Total procesadas hasta ahora: " + procesadas + ", tiempo transcurrido (ms) = " + (System.currentTimeMillis() - ti) + ", velocidad (procesadas/s) = " + (procesadas * 1000L / (System.currentTimeMillis() - ti)));
            }
        }
        if (mejorSolucion != null) {
            Iterator<Set<Participante>> i = mejorSolucion.iterator();
            solucionFinal = Stream.of(dias).collect(toMap(Function.identity(),
                    d -> new AsignacionDiaV1(i.next(), null, null, 0)));
        }

        return solucionFinal;
    }

    /**
     * Función de coste (a minimizar) para evaluar cada posible solución
     *
     * @param solucionActual
     * @return
     */
    private double costeSolucion(List<Set<Participante>> solucionActual) {
        Map<Participante, Long> vecesCoche = solucionActual.stream().flatMap(Set::stream).collect(Collectors.groupingBy(Function.identity(), counting()));
        //return vecesCoche.values().stream().mapToDouble(Long::doubleValue).max().orElse(Double.POSITIVE_INFINITY);
        LongSummaryStatistics est = vecesCoche.values().stream().mapToLong(Long::longValue).summaryStatistics();

        return 2 * est.getMax() - est.getMin();
        //return solucionActual.stream().flatMap(Set::stream).count();
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucionFinal;
    }
}
