/*
 * Copyright (C) 2016 josec
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

import es.um.josecefe.rueda.modelo.Asignacion;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Participante;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

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
            encoder.writeObject((Integer)datosRueda.getCosteAsignacion());
        } catch (Exception ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void cargaDatosRueda(File xmlfile, DatosRueda datosRueda) {
        try (XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(
                        new FileInputStream(xmlfile)))) {
            decoder.setExceptionListener(e -> e.printStackTrace());
            datosRueda.setDias((List<Dia>) decoder.readObject());
            datosRueda.setLugares((List<Lugar>) decoder.readObject());
            datosRueda.setParticipantes((List<Participante>) decoder.readObject());
            datosRueda.setHorarios((List<Horario>) decoder.readObject());
            datosRueda.setAsignacion((List<Asignacion>) decoder.readObject());
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
