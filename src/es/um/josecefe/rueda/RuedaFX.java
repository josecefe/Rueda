/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda;

import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.persistencia.PersistenciaXML;
import es.um.josecefe.rueda.vista.PrincipalController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static es.um.josecefe.rueda.Version.*;

/**
 * @author josec
 */
public class RuedaFX extends Application {
    private final DatosRueda datosRueda;
    private Stage primaryStage;

    public RuedaFX() {
        this.datosRueda = new DatosRueda();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public Window getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage pStage) {
        try {
            primaryStage = pStage;
            primaryStage.setTitle(TITLE + " " + VERSION + " - " + COPYRIGHT);
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("res/rueda.png")));

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("vista/Principal.fxml"));
            BorderPane principal = loader.load();

            // Give the controller access to the main app.
            PrincipalController controller = loader.getController();
            controller.setDatosRueda(datosRueda);
            controller.setMainApp(this);

            // Show the scene containing the root layout.
            Scene scene = new Scene(principal);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);

            primaryStage.show();

            // Try to load last opened person file.
            File file = getLastFilePath();
            if (file != null) {
                cargaHorarios(file);
            }
        } catch (IOException ex) {
            Logger.getLogger(RuedaFX.class.getName()).log(Level.SEVERE, "Fallo inicializando la ventana principal", ex);
        }
    }

    /**
     * Returns the file preference, i.e. the file that was last opened. The
     * preference is read from the OS specific registry. If no such preference
     * can be found, null is returned.
     *
     * @return Last file save
     */
    public File getLastFilePath() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String lastSave = prefs.get("lastSave", null);
        if (lastSave != null) {
            return new File(lastSave);
        } else {
            return null;
        }
    }

    /**
     * Sets the file path of the currently loaded file. The path is persisted in
     * the OS specific registry.
     *
     * @param file the file or null to remove the path
     */
    public void setLastFilePath(File file) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        if (file != null) {
            prefs.put("lastSave", file.getPath());

            // Update the stage title.
            primaryStage.setTitle(TITLE + " [" + file.getName() + "] - " + COPYRIGHT);
        } else {
            prefs.remove("filePath");

            // Update the stage title.
            primaryStage.setTitle(TITLE + " - " + COPYRIGHT);
        }
    }

    /**
     * Loads horario data from the specified file. The current datosRueda data will
     * be replaced.
     *
     * @param file Fichero del que cargar los datos
     */
    public void cargaHorarios(File file) {
        try {
            PersistenciaXML.cargaDatosRueda(file, datosRueda);
            // Save the file path to the registry.
            //datosRueda.reemplazar(hs);
            setLastFilePath(file);

        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "No se ha podido cargar los datos del fichero:\n" + file.getPath(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se ha podido cargar los datos del fichero:\n" + file.getPath());
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
    }

    /**
     * Saves the current horario data to the specified file.
     *
     * @param file Fichero en el que guardar los datosRueda
     */
    public void guardaHorarios(File file) {
        try {
            PersistenciaXML.guardaDatosRueda(file, datosRueda);
            // Save the file path to the registry.
            setLastFilePath(file);
        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "No se ha podido guardar los datos en el fichero:\n" + file.getPath(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se ha podido guardar los datos en el fichero:\n" + file.getPath());
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
    }

    /**
     * Saves the current horario data to the specified file.
     *
     * @param file Fichero en el que guardar el HTML generado
     * @return indica si se completo con exito la operación
     */
    public boolean exportaAsignacion(File file) {
        try {
            PersistenciaXML.exportaAsignacion(file, datosRueda);
            return true;
            //Duda: ¿Guardar dónde se hizo la última exportación o no?
        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "No se ha podido exportar la asignación en el fichero:\n" + file.getPath(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se ha podido exportar la asignación en el fichero:\n" + file.getPath());
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
        return false;
    }
}
