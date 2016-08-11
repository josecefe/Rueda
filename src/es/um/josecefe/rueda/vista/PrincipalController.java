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
package es.um.josecefe.rueda.vista;

import es.um.josecefe.rueda.RuedaFX;
import es.um.josecefe.rueda.modelo.Asignacion;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.resolutor.ResolutorV8;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;

/**
 * FXML Controller class
 *
 * @author josec
 */
public class PrincipalController {

    private RuedaFX mainApp;

    @FXML
    private TableView<Horario> tablaHorario;

    @FXML
    private TableColumn<Horario, Dia> columnaDia;

    @FXML
    private TableColumn<Horario, Participante> columnaParticipante;

    @FXML
    private TableColumn<Horario, Integer> columnaEntrada;

    @FXML
    private TableColumn<Horario, Integer> columnaSalida;

    @FXML
    private TableColumn<Horario, Boolean> columnaCoche;

    @FXML
    private TableView<Asignacion> tablaResultado;
    
    @FXML
    private TableColumn<Asignacion, Dia> columnaDiaAsignacion;

    @FXML
    private TableColumn<Asignacion, Set<Participante>> columnaConductores;
    
    @FXML
    private TableColumn<Asignacion, Map<Participante, Lugar>> columnaPeIda;
    
    @FXML
    private TableColumn<Asignacion, Map<Participante, Lugar>> columnaPeVuelta;
    
    @FXML
    private TableColumn<Asignacion, Integer> columnaCoste;

    @FXML
    private ProgressBar indicadorProgreso;

    @FXML
    private Label barraEstado;

    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        // Tabla de horarios
        columnaDia.setCellValueFactory(new PropertyValueFactory<>("dia"));
        columnaParticipante.setCellValueFactory(new PropertyValueFactory<>("participante"));
        columnaEntrada.setCellValueFactory(new PropertyValueFactory<>("entrada"));
        columnaEntrada.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        columnaSalida.setCellValueFactory(new PropertyValueFactory<>("salida"));
        columnaSalida.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        columnaCoche.setCellValueFactory(new PropertyValueFactory<>("coche"));
        columnaCoche.setCellFactory(CheckBoxTableCell.forTableColumn(columnaCoche));
        
        // Tabla de asignaciones
        columnaDiaAsignacion.setCellValueFactory(new PropertyValueFactory<>("dia"));
        columnaConductores.setCellValueFactory(new PropertyValueFactory<>("conductores"));
        columnaPeIda.setCellValueFactory(new PropertyValueFactory<>("peIda"));
        columnaPeVuelta.setCellValueFactory(new PropertyValueFactory<>("peVuelta"));
        columnaCoste.setCellValueFactory(new PropertyValueFactory<>("coste"));
    }

    public void setMainApp(RuedaFX mainApp) {
        this.mainApp = mainApp;
        tablaHorario.setItems(mainApp.getDatosRueda().getHorarios());
        columnaDia.setCellFactory(ComboBoxTableCell.forTableColumn(mainApp.getDatosRueda().getDias()));
        columnaParticipante.setCellFactory(ComboBoxTableCell.forTableColumn(mainApp.getDatosRueda().getParticipantes()));
        tablaResultado.setItems(mainApp.getDatosRueda().getAsignacion());
    }

    /**
     * Creates an empty address book.
     */
    @FXML
    private void handleNew() {
        //mainApp.getPersonData().clear();
        mainApp.setLastFilePath(null);
    }

    /**
     * Opens a FileChooser to let the user select an address book to load.
     */
    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos XML (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());

        if (file != null) {
            mainApp.cargaHorarios(file);
        }
    }

    /**
     * Saves the file to the person file that is currently open. If there is no
     * open file, the "save as" dialog is shown.
     */
    @FXML
    private void handleSave() {
        File personFile = mainApp.getLastFilePath();
        if (personFile != null) {
            mainApp.guardaHorarios(personFile);
        } else {
            handleSaveAs();
        }
    }

    /**
     * Opens a FileChooser to let the user select a file to save to.
     */
    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos XML (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.getPath().endsWith(".xml")) {
                file = new File(file.getPath() + ".xml");
            }
            mainApp.guardaHorarios(file);
        }
    }

    /**
     * Opens an about dialog.
     */
    @FXML
    private void handleAbout() {
        mainApp.acercaDe();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        System.exit(0);
    }

    @FXML
    private void handleCalculaAsignacion() {
        ResolutorService resolutorService = new ResolutorService();
        resolutorService.setResolutor(new ResolutorV8(new HashSet<>(mainApp.getDatosRueda().getHorarios())));
        resolutorService.setOnSucceeded((WorkerStateEvent e) -> {
            barraEstado.setText("Calculo de asignación completado con un coste final de "+resolutorService.getResolutor().getEstadisticas().get().getFitness());
            indicadorProgreso.progressProperty().unbind();
            indicadorProgreso.setProgress(1);
            mainApp.getDatosRueda().setSolucion(resolutorService.getValue());
        });
        barraEstado.setText("Calculando asignación...");
        indicadorProgreso.setProgress(-1);
        indicadorProgreso.progressProperty().bind(resolutorService.progressProperty());
        resolutorService.start();
    }

    private static class ResolutorService extends Service<Map<Dia, ? extends AsignacionDia>> {

        private final ObjectProperty<ResolutorV8> resolutor = new SimpleObjectProperty<>();

        public ResolutorV8 getResolutor() {
            return resolutor.get();
        }

        public void setResolutor(ResolutorV8 value) {
            resolutor.set(value);
        }

        public ObjectProperty resolutorProperty() {
            return resolutor;
        }

        @Override
        protected Task<Map<Dia, ? extends AsignacionDia>> createTask() {
            return new Task<Map<Dia, ? extends AsignacionDia>>() {
                @Override
                protected Map<Dia, ? extends AsignacionDia> call() throws Exception {
                    return resolutor.get().resolver();
                }
            };
        }

    }
}
