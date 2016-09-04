/**
 * (C) 2016 José Ceferino Ortega Carretero
 */
package es.um.josecefe.rueda;

import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.persistencia.PersistenciaXML;
import es.um.josecefe.rueda.resolutor.ResolutorJE;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.resolutor.ResolutorV8;
import es.um.josecefe.rueda.resolutor.Resolutor;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.persistencia.PersistenciaSQL;
import es.um.josecefe.rueda.resolutor.ResolutorGA;
import es.um.josecefe.rueda.resolutor.ResolutorV7;
import java.io.File;
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
//    private static final String RUEDA_BASE = "ruedamurcia";

    private static final String RUEDA_BASE = "ruedaalberca";
    private static final String RUEDABD = RUEDA_BASE + ".db";
    private static final String RUEDAXML_HORARIOS = RUEDA_BASE + "_horarios.xml";
    private static final String RUEDAXML_ASIGNACION = RUEDA_BASE + "_asignacion.xml";
    private static final boolean COMPARANDO = true;
    private static final boolean AMPLIADO = false;

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Rueda().pruebaResolutor();

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

    private void pruebaResolutor() {
        Set<Horario> horarios = PersistenciaSQL.cargaHorarios(RUEDABD);
        //DatosRueda datos = PersistenciaXML.cargaDatosRueda(new File(RUEDAXML_HORARIOS));
        //Set<Horario> horarios = datos.getHorarios();
        // Creando bd nueva y guardando
        DatosRueda datos = new DatosRueda();
        datos.poblarDesdeHorarios(horarios);
        //PersistenciaSQL.guardaDatosRueda(RUEDABD, datos);
        // Vamos a guardarlo en XML
        PersistenciaXML.guardaDatosRueda(new File(RUEDAXML_HORARIOS), datos);

        List<? extends Resolutor> resolutores = Arrays.asList(
                //                new ResolutorV1(),
                //                new ResolutorV2(),
                //                new ResolutorV3(),
                //                new ResolutorV4(),
                //                new ResolutorV5(),
                //                new ResolutorV6(),
                new ResolutorV7(),
                new ResolutorV8(),
                new ResolutorGA(),
                new ResolutorJE()
        );
        if (COMPARANDO) {
            resolutores.forEach(r -> {
                System.out.println("\n**********************************\n");
                System.out.format("Resolvemos el problema normal con %s:\n", r.getClass().getSimpleName());
                r.resolver(horarios);
                System.out.println("\n**********************************\n");
            });
            resolutores.forEach(r -> {
                System.out.format("Resolutor %s:\n->sol=%s\n->%s\n", r.getClass().getSimpleName(), r.getSolucionFinal(), r.getEstadisticas());
            });
        } else {
            Resolutor r = resolutores.get(resolutores.size() - 1);
            System.out.println("\n**********************************\n");
            System.out.format("Resolvemos el problema normal con %s:\n", r.getClass().getSimpleName());
            r.resolver(horarios);
            System.out.println("\n**********************************\n");
            System.out.format("Resolutor %s:\n=%s\n->%s\n", r.getClass().getSimpleName(), r.getSolucionFinal(), r.getEstadisticas());
            //PersistenciaSQL.guardaAsignacionRueda(RUEDABD, r.getSolucionFinal());
            PersistenciaXML.guardaAsignacionRueda(RUEDAXML_ASIGNACION, r.getSolucionFinal());
        }
        if (AMPLIADO) {
            Set<Horario> horariosAmpliado = duplicarHorario(horarios);
            Resolutor r = new ResolutorJE();
            ResolutorV8 r2 = new ResolutorV8();

            System.out.println("\n**********************************\n");
            System.out.format("Resolvemos el problema Ampliado con %s y %s:\n", r.getClass().getSimpleName(), r2.getClass().getSimpleName());
            System.out.format("Fase 1: Ampliado %s:\n", r.getClass().getSimpleName());
            System.out.println(r.resolver(horariosAmpliado));
            System.out.format("->Est: %s\n", r.getEstadisticas());
            if (r.getSolucionFinal() != null) {
                int C = r.getEstadisticas().getFitness() + 1; //Uno más para que encuentre la misma solución por lo menos...
                System.out.format("Fase 2: Ampliado %s tomando como entrada %,d como mejor coste máximo:\n", r2.getClass().getSimpleName(), C);
                System.out.println(r2.resolver(horariosAmpliado, C));
            } else {
                System.out.format("Fase 2: Ampliado %s sin tener información adicional:\n", r2.getClass().getSimpleName());
                System.out.println(r2.resolver(horariosAmpliado));
            }
            System.out.format("->Est: %s\n", r2.getEstadisticas());
        }
    }
}
