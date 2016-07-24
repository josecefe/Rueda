/**
 * (C) 2016 José Ceferino Ortega Carretero
 */
package es.um.josecefe.rueda;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Clase principal de la aplicación Rueda
 *
 * @author josecefe@um.es
 *
 */
public class Rueda {

    private static final String RUEDABD = "ruedaalberca.db";
    private static final boolean COMPARANDO = false;
    private static final boolean AMPLIADO = true;

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Rueda().pruebaResolutorSQL();

        //pruebaCombinatoria();
    }

    private Set<Horario> duplicarHorario(Set<Horario> horarios) {
        Set<Horario> nHorarios = new HashSet<>(horarios);
        IntSummaryStatistics estadisticas = horarios.stream().mapToInt(h -> h.getDia().getId()).summaryStatistics();
        int desplazamiento = estadisticas.getMax() - estadisticas.getMin() + 1;
        Map<Dia, Dia> dias = horarios.stream().map(Horario::getDia).sorted().distinct().collect(toMap(Function.identity(), d -> new Dia(d.getId() + desplazamiento, d.getDescripcion() + "Ex")));
        nHorarios.addAll(horarios.stream().map(h -> new Horario(h.getParticipante(), dias.get(h.getDia()), h.getEntrada(), h.getSalida(), h.isCoche())).collect(toList()));

        return nHorarios;
    }

    private void pruebaResolutorSQL() {
        //PersistenciaSQL.creaBD("prueba.db");
        Set<Horario> horarios = PersistenciaSQL.cargaHorarios(RUEDABD);
        List<? extends Resolutor> resolutores = Arrays.asList(
                new ResolutorV1(horarios),
                new ResolutorV2(horarios),
                new ResolutorV3(horarios),
                new ResolutorV4(horarios),
                new ResolutorV5(horarios),
                new ResolutorV6(horarios),
                new ResolutorV7(horarios),
                new ResolutorV8(horarios),
                new ResolutorGA(horarios));
        if (COMPARANDO) {
            resolutores.forEach(r -> {
                System.out.println("\n**********************************\n");
                System.out.format("Resolvemos el problema normal con %s:\n", r.getClass().getName());
                r.resolver();
                System.out.println("\n**********************************\n");
            });
            resolutores.forEach(r -> {
                System.out.format("Resolutor %s:\n->sol=%s\n->%s\n", r.getClass().getName(), r.getSolucionFinal(), r.getEstadisticas());
            });
        } else {
            Resolutor r = resolutores.get(resolutores.size() - 1);
            System.out.println("\n**********************************\n");
            System.out.format("Resolvemos el problema normal con %s:\n", r.getClass().getName());
            r.resolver();
            System.out.println("\n**********************************\n");
            System.out.format("Resolutor %s:\n=%s\n->%s\n", r.getClass().getName(), r.getSolucionFinal(), r.getEstadisticas());
            PersistenciaSQL.guardaAsignacionRueda(RUEDABD, r.getSolucionFinal());
        }
        if (AMPLIADO) {
            Set<Horario> horariosAmpliado = duplicarHorario(horarios);
            Resolutor r = new ResolutorGA(horariosAmpliado);
            ResolutorV8 r2 = new ResolutorV8(horariosAmpliado);
            
            System.out.println("\n**********************************\n");
            System.out.format("Resolvemos el problema Ampliado con %s y %s:\n", r.getClass().getName(), r2.getClass().getName());
            System.out.format("Fase 1: Ampliado %s:\n", r.getClass().getName());
            System.out.println(r.resolver());
            System.out.format("->Est: %s\n", r.getEstadisticas());
            if (r.getEstadisticas().isPresent()) {
                int C = r.getEstadisticas().get().getFitness() + 1; //Uno más para que encuentre la misma solución por lo menos...
                System.out.format("Fase 2: Ampliado %s tomando como entrada %,d como mejor coste máximo:\n", r2.getClass().getName(), C);
                System.out.println(r2.resolver(C, 0.5));
            } else {
                System.out.format("Fase 2: Ampliado %s sin tener información adicional:\n", r2.getClass().getName());
                System.out.println(r2.resolver());
            }
            System.out.format("->Est: %s\n", r2.getEstadisticas());
        }
    }
}
