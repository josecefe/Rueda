/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda;

import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.persistencia.PersistenciaXML;
import es.um.josecefe.rueda.resolutor.*;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Clase principal de la aplicación Rueda
 *
 * @author josecefe@um.es
 */
public class Rueda {
//    private static final String RUEDA_BASE = "ruedamurcia";

    private static final String RUEDA_BASE = "rueda";
    private static final String RUEDABD = RUEDA_BASE + ".db";
    private static final String RUEDAXML_HORARIOS = RUEDA_BASE + "_alberca201617.xml";
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
        //Set<Horario> horarios = PersistenciaSQL.cargaHorarios(RUEDABD);
        DatosRueda datos = new DatosRueda();
        PersistenciaXML.cargaDatosRueda(new File(RUEDAXML_HORARIOS), datos);
        Set<Horario> horarios = new HashSet<>(datos.getHorarios());
        // Creando bd nueva y guardando
        //DatosRueda datos = new DatosRueda();
        //datos.poblarDesdeHorarios(horarios);
        //PersistenciaSQL.guardaDatosRueda(RUEDABD, datos);
        // Vamos a guardarlo en XML
        //PersistenciaXML.guardaDatosRueda(new File(RUEDAXML_HORARIOS), datos);

        List<? extends Resolutor> resolutores = Arrays.asList(
                new ResolutorV8(),
                new ResolutorV7(),
                new ResolutorExhaustivo()
        );
        if (COMPARANDO) {
            resolutores.forEach(r -> {
                System.out.println("\n**********************************\n");
                System.out.format("Resolvemos el problema normal con %s:\n", r.getClass().getSimpleName());
                r.resolver(horarios);
                System.out.println("\n**********************************\n");
            });
            resolutores.forEach(r -> System.out.format("Resolutor %s:\n->sol=%s\n->%s\n", r.getClass().getSimpleName(), r.getSolucionFinal(), r.getEstadisticas()));
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
            ResolutorV8 r = new ResolutorV8();

            System.out.println("\n**********************************\n");
            System.out.format("Resolvemos el problema Ampliado con %s:\n", r.getClass().getSimpleName());
            System.out.format("Fase 1: Ampliado %s:\n", r.getClass().getSimpleName());
            System.out.println(r.resolver(horariosAmpliado));
            System.out.format("->Est: %s\n", r.getEstadisticas());
        }
    }
}
