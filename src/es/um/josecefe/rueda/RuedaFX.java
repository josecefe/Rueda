/*
 * Copyright (C) 2016 josecefe
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
package es.um.josecefe.rueda;

import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.persistencia.PersistenciaXML;
import es.um.josecefe.rueda.vista.PrincipalController;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 *
 * @author josec
 */
public class RuedaFX extends Application {

    private static final String COPYRIGHT = "(C)2016 José Ceferino Ortega Carretero";
    private static final String TITLE = "Optimizador de la Rueda";

    private Stage primaryStage;
    private final DatosRueda datosRueda;
    private BorderPane Principal;

    public RuedaFX() {
        this.datosRueda = new DatosRueda();
    }

    public DatosRueda getDatosRueda() {
        return datosRueda;
    }

    public Window getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage pStage) {
        try {
            primaryStage = pStage;
            primaryStage.setTitle(TITLE + " - " + COPYRIGHT);
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("res/rueda.png")));

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("vista/Principal.fxml"));
            Principal = (BorderPane) loader.load();

            // Give the controller access to the main app.
            PrincipalController controller = loader.getController();
            controller.setMainApp(this);

            // Show the scene containing the root layout.
            Scene scene = new Scene(Principal);
            primaryStage.setScene(scene);

            primaryStage.show();

            // Try to load last opened person file.
            File file = getLastFilePath();
            if (file != null) {
                cargaHorarios(file);
            }
        } catch (IOException ex) {
            Logger.getLogger(RuedaFX.class.getName()).log(Level.SEVERE, null, ex);
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
     * Loads horario data from the specified file. The current horario data will
     * be replaced.
     *
     * @param file
     */
    public void cargaHorarios(File file) {
        try {
            PersistenciaXML.cargaDatosRueda(file, datosRueda);
            // Save the file path to the registry.
            //datosRueda.reemplazar(hs);
            setLastFilePath(file);

        } catch (Exception e) { // catches ANY exception
            e.printStackTrace();
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
     * @param file
     */
    public void guardaHorarios(File file) {
        try {
            PersistenciaXML.guardaDatosRueda(file, datosRueda);
            // Save the file path to the registry.
            setLastFilePath(file);
        } catch (Exception e) { // catches ANY exception
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se ha podido guarda los datos al fichero:\n" + file.getPath());
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
    }
    
    /**
     * Saves the current horario data to the specified file.
     *
     * @param file
     * @return indica si se completo con exito la operación
     */
    public boolean exportaAsignacion(File file) {
        try {
            PersistenciaXML.exportaAsignacion(file, datosRueda);
            return true;
            //Duda: ¿Guardar dónde se hizo la última exportación o no?
        } catch (Exception e) { // catches ANY exception
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se ha podido exportar la asignación al fichero:\n" + file.getPath());
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
        return false;
    }

    /**
     * Muestra un dialogo con el acerca de...
     */
    public void acercaDe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(TITLE);
        alert.setHeaderText("Acerca de");
        alert.setContentText("Autor: José Ceferino Ortega Carretero");
        alert.showAndWait();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


}
