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

import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.DatosRueda;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author josec
 */
public class PersistenciaXML {

    public static void guardaDatosRueda(File xmlfile, DatosRueda datosRueda) {
        try (XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(
                        new FileOutputStream(xmlfile)))) {
            encoder.writeObject(datosRueda);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static DatosRueda cargaDatosRueda(File xmlfile) {
        DatosRueda datosRueda = null;
        try (XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(
                        new FileInputStream(xmlfile)))) {
            datosRueda = (DatosRueda) decoder.readObject();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PersistenciaXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return datosRueda;
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

}
