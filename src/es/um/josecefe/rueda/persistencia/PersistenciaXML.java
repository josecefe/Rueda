/*
 * Copyright (C) 2016 José Ceferino Ortega
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.persistencia;

import static es.um.josecefe.rueda.Version.COPYRIGHT;
import static es.um.josecefe.rueda.Version.TITLE;
import static es.um.josecefe.rueda.Version.VERSION;
import es.um.josecefe.rueda.modelo.Asignacion;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Participante;
import htmlflow.HtmlView;
import htmlflow.elements.HtmlTable;
import htmlflow.elements.HtmlTr;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import javafx.util.Pair;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 *
 * @author josec
 */
public class PersistenciaXML {

    public static void guardaDatosRueda(File xmlfile, DatosRueda datosRueda) {
        try (XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(
                        new FileOutputStream(xmlfile)))) {

            encoder.setExceptionListener(e -> e.printStackTrace());
            encoder.setPersistenceDelegate(Pair.class, new PairPersistenceDelegate());
            // Poco a poco
            encoder.writeObject(new ArrayList<>(datosRueda.getDias()));
            encoder.writeObject(new ArrayList<>(datosRueda.getLugares()));
            encoder.writeObject(new ArrayList<>(datosRueda.getParticipantes()));
            encoder.writeObject(new ArrayList<>(datosRueda.getHorarios()));
            encoder.writeObject(new ArrayList<>(datosRueda.getAsignacion()));
            encoder.writeObject((Integer) datosRueda.getCosteAsignacion());
        } catch (Exception ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void cargaDatosRueda(File xmlfile, DatosRueda datosRueda) {
        try (XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(
                        new FileInputStream(xmlfile)))) {
            decoder.setExceptionListener(e -> Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, e));
            List<?> ld = (List<?>) decoder.readObject();
            datosRueda.setDias(ld.stream().map(e -> (Dia) e).collect(toList()));
            List<?> ll = (List<?>) decoder.readObject();
            datosRueda.setLugares(ll.stream().map(e -> (Lugar) e).collect(toList()));
            List<?> lp = (List<?>) decoder.readObject();
            datosRueda.setParticipantes(lp.stream().map(e -> (Participante) e).collect(toList()));
            List<?> lh = (List<?>) decoder.readObject();
            datosRueda.setHorarios(lh.stream().map(e -> (Horario) e).collect(toList()));
            List<?> la = (List<?>) decoder.readObject();
            datosRueda.setAsignacion(la.stream().map(e -> (Asignacion) e).collect(toList()));
            datosRueda.setCosteAsignacion((Integer) decoder.readObject());
        } catch (Exception ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void guardaAsignacionRueda(String xmlfile, Map<Dia, ? extends AsignacionDia> solucionFinal) {
        try (XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(
                        new FileOutputStream(xmlfile)))) {
            encoder.writeObject(solucionFinal);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class ParticipanteIdaConduceLugar {

        Participante participante;
        boolean ida;
        boolean conduce;
        Lugar lugar;
    }

    public static void exportaAsignacion(File htmlfile, DatosRueda datosRueda) {

        try (PrintStream out = new PrintStream(htmlfile)) {
            boolean conLugar = datosRueda.getAsignacion().stream().flatMap(a -> a.getPeIda().stream()).map(Pair::getValue).distinct().count() > 1;

            // Todas las horas con actividad:
            Set<Integer> horasActivas = datosRueda.getHorarios().stream().map(Horario::getEntrada).collect(Collectors.toSet());
            horasActivas.addAll(datosRueda.getHorarios().stream().map(Horario::getSalida).collect(Collectors.toSet()));

            // Vamos a guardar todo en una tabla "virtual"
            Map<Dia, Map<Integer, List<ParticipanteIdaConduceLugar>>> datosTabla = new HashMap<>(datosRueda.getDias().size());
            for (Asignacion a : datosRueda.getAsignacion()) {
                Dia d = a.getDia();
                datosRueda.getHorarios().stream().filter(h -> h.getDia() == d).forEach(h -> {
                    Map<Integer, List<ParticipanteIdaConduceLugar>> horasDia = datosTabla.get(d);
                    if (horasDia == null) {
                        horasDia = new HashMap<>(horasActivas.size());
                        datosTabla.put(d, horasDia);
                    }
                    List<ParticipanteIdaConduceLugar> celdaIda = horasDia.get(h.getEntrada());
                    if (celdaIda == null) {
                        celdaIda = new ArrayList<>();
                        horasDia.put(h.getEntrada(), celdaIda);
                    }

                    List<ParticipanteIdaConduceLugar> celdaVuelta = horasDia.get(h.getSalida());
                    if (celdaVuelta == null) {
                        celdaVuelta = new ArrayList<>();
                        horasDia.put(h.getSalida(), celdaVuelta);
                    }

                    // Datos de la IDA y la VUELTA
                    ParticipanteIdaConduceLugar pIda = new ParticipanteIdaConduceLugar();
                    ParticipanteIdaConduceLugar pVuelta = new ParticipanteIdaConduceLugar();
                    pIda.ida = true;
                    pVuelta.ida = false;
                    pIda.participante = pVuelta.participante = h.getParticipante();
                    pIda.conduce = pVuelta.conduce = a.getConductores().contains(pIda.participante);
                    Optional<Pair<Participante, Lugar>> r = a.getPeIda().stream().filter(p -> p.getKey() == pIda.participante).findFirst();
                    if (r.isPresent()) {
                        pIda.lugar = r.get().getValue();
                        celdaIda.add(pIda);
                    }
                    Optional<Pair<Participante, Lugar>> s = a.getPeVuelta().stream().filter(p -> p.getKey() == pVuelta.participante).findFirst();
                    if (s.isPresent()) {
                        pVuelta.lugar = s.get().getValue();
                        celdaVuelta.add(pVuelta);
                    }
                });
            }
            //Ahora generamos la tabla en HTML
            HtmlView<?> htmlView = new HtmlView<>();
            htmlView.head()
                    .title(escapeHtml4("Asignación Rueda - " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .linkCss("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css");
            HtmlTable<?> table = htmlView
                    .body().classAttr("container")
                    .heading(3, escapeHtml4("Asignación Rueda - " + LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT))))
                    .div()
                    .table().classAttr("table table-bordered");
            HtmlTr<?> headerRow = table.tr();
            headerRow.th().text("Hora");
            datosRueda.getDias().stream().forEachOrdered(d -> headerRow.th().text(escapeHtml4(d.toString())));
            horasActivas.stream().sorted().forEachOrdered(hora -> {
                HtmlTr<?> tr = table.tr();
                tr.td().text(hora.toString()); //Hora
                datosRueda.getDias().stream().forEachOrdered(dia -> {
                    Map<Integer, List<ParticipanteIdaConduceLugar>> dd = datosTabla.get(dia);
                    List<ParticipanteIdaConduceLugar> t = dd != null ? dd.get(hora) : null;
                    String valor = "";
                    if (t != null) {
                        valor = t.stream()
                                .sorted((p1, p2) -> p1.conduce == p2.conduce ? 0 : p1.conduce ? -1 : 1) // Ponemos primero los conductores
                                .sorted((p1, p2) -> p1.lugar.compareTo(p2.lugar)) // Pero dentro de un lugar
                                .sorted((p1, p2) -> p1.ida == p2.ida ? 0 : p1.ida ? -1 : 1) // Teniendo en cuenta si es ida o vuelta
                                .map(p -> {
                                    StringBuilder res = new StringBuilder();
                                    if (p.ida) {
                                        res.append("<i>");
                                    }
                                    if (p.conduce) {
                                        res.append("<b>").append(escapeHtml4("*"));
                                    }
                                    res.append(escapeHtml4(p.participante.toString()));
                                    if (conLugar) {
                                        res.append(" [");
                                        res.append(escapeHtml4(p.lugar.toString()));
                                        res.append("]");
                                    }

                                    
                                    if (p.conduce) {
                                        res.append("</b>");
                                    }
                                    if (p.ida) {
                                        res.append("</i>");
                                    }
                                    return res.toString();
                                }).collect(Collectors.joining("<br>\n"));
                    }
                    tr.td().text(valor);
                });
            });

            // Leyenda
            if (conLugar) {
                htmlView.body().div().text("Leyenda: <i><b>*Conductor [Lugar de Ida]</b></i> | <i>Pasajero [Lugar de Ida]</i> | <b>*Conductor [Lugar de Vuelta]</b> | Pasajero [Lugar de Vuelta]").addAttr("style", "color:green;text-align:center");
            } else {
                htmlView.body().div().text("Leyenda: <i><b>*Conductor Ida</b></i> | <i>Pasajero Ida</i> | <b>*Conductor Vuelta</b> | Pasajero Vuelta").addAttr("style", "color:green;text-align:center");
            }
            
            // Coste
            htmlView.body().div().text(String.format("%s <b>%,d</b>", escapeHtml4("Coste total asignación: "), datosRueda.getCosteAsignacion())).addAttr("style", "color:royal-blue;text-align:right");
            
            // Cuadro de conductor/dias           
            htmlView.body().heading(4, escapeHtml4("Cuadro de conductor/días"));
            HtmlTable<?> tabla = htmlView.body().table().classAttr("table table-bordered");
            HtmlTr<?> cabecera = tabla.tr();
            cabecera.th().text(escapeHtml4("Conductor"));
            cabecera.th().text(escapeHtml4("Días"));
            cabecera.th().text(escapeHtml4("Total"));
            datosRueda.getParticipantes().stream().sorted().forEachOrdered(participante -> {
                List<Dia> dias=datosRueda.getAsignacion().stream().filter(a -> a.getConductores().contains(participante)).map(Asignacion::getDia).sorted().collect(toList());
                if (dias.size()>0) {
                    HtmlTr<?> tr = tabla.tr();
                    tr.td().text(escapeHtml4(participante.getNombre()));
                    tr.td().text(escapeHtml4(dias.toString()));
                    tr.td().text(String.valueOf(dias.size()));
                }
            });
            
            // Cuadro de dia/conductores        
            htmlView.body().heading(4, escapeHtml4("Cuadro de día/conductores"));
            HtmlTable<?> tablaDias = htmlView.body().table().classAttr("table table-bordered");
            HtmlTr<?> cabeceraDias = tablaDias.tr();
            cabeceraDias.th().text(escapeHtml4("Día"));
            cabeceraDias.th().text(escapeHtml4("Conductores"));
            cabeceraDias.th().text(escapeHtml4("Total"));
            datosRueda.getAsignacion().stream().sorted().forEachOrdered(a -> {
                    HtmlTr<?> tr = tablaDias.tr();
                    tr.td().text(escapeHtml4(a.getDia().getDescripcion()));
                    tr.td().text(escapeHtml4(a.getConductores().toString()));
                    tr.td().text(String.valueOf(a.getConductores().size()));
            });
            
            // Pie de pagina
            htmlView.body().hr().div().text("Generado con <b>"+TITLE+" "+VERSION+"<b> <i>"+COPYRIGHT+"</i>").addAttr("style", "color:royalblue;text-align:right");
            htmlView.setPrintStream(out);
            htmlView.write();
        } catch (Exception ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, "Problemas generando la exportación a HTML: ", ex);
        }
    }

    private static class PairPersistenceDelegate extends DefaultPersistenceDelegate {

        public PairPersistenceDelegate() {
            super(new String[]{"key", "value"});
        }

        @Override
        protected Expression instantiate(Object oldInstance, Encoder out) {
            Pair par = (Pair) oldInstance;
            Object[] constructorArgs = new Object[]{par.getKey(), par.getValue()};
            return new Expression(oldInstance, oldInstance.getClass(), "new", constructorArgs);
        }
    }

}
