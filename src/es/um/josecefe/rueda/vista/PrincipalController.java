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
package es.um.josecefe.rueda.vista;

import es.um.josecefe.rueda.RuedaFX;
import es.um.josecefe.rueda.modelo.Asignacion;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.resolutor.Resolutor;
import java.io.File;
import java.net.URISyntaxException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.apache.commons.lang3.SystemUtils;
import static java.util.stream.Collectors.toMap;

/**
 * FXML Controller class
 *
 * @author josec
 */
public class PrincipalController {
    private static final String[] RESOLUTORES = new String[]{
        "es.um.josecefe.rueda.resolutor.ResolutorV7",
        "es.um.josecefe.rueda.resolutor.ResolutorV8",
        "es.um.josecefe.rueda.resolutor.ResolutorGA",
        "es.um.josecefe.rueda.resolutor.ResolutorJE",
        "es.um.josecefe.rueda.resolutor.ResolutorCombinado"};

    private RuedaFX mainApp;

    DatosRueda datosRueda;

    @FXML
    private MenuItem mCalcular;

    @FXML
    private MenuItem mCancelarCalculo;

    @FXML
    private MenuItem mExportar;

    @FXML
    TableView<Horario> tablaHorario;

    @FXML
    TableColumn<Horario, Dia> columnaDia;

    @FXML
    TableColumn<Horario, Participante> columnaParticipante;

    @FXML
    TableColumn<Horario, Integer> columnaEntrada;

    @FXML
    TableColumn<Horario, Integer> columnaSalida;

    @FXML
    TableColumn<Horario, Boolean> columnaCoche;

    @FXML
    Label lCoste;

    @FXML
    Label lEtiquetaCoste;

    @FXML
    TableView<Asignacion> tablaResultado;

    @FXML
    TableColumn<Asignacion, Dia> columnaDiaAsignacion;

    @FXML
    TableColumn<Asignacion, Set<Participante>> columnaConductores;

    @FXML
    TableColumn<Asignacion, Map<Participante, Lugar>> columnaPeIda;

    @FXML
    TableColumn<Asignacion, Map<Participante, Lugar>> columnaPeVuelta;

    @FXML
    TableColumn<Asignacion, Integer> columnaCoste;

    @FXML
    TableView<AsignacionParticipante> tablaResultadoLugares;

    @FXML
    TableColumn<AsignacionParticipante, Participante> columnaParticipanteLugares;

    @FXML
    TableColumn<AsignacionParticipante, Lugar> columnaLugaresIda;

    @FXML
    TableColumn<AsignacionParticipante, Lugar> columnaLugaresVuelta;

    @FXML
    TableColumn<AsignacionParticipante, Boolean> columnaLugaresConductor;

    @FXML
    ProgressBar indicadorProgreso;

    @FXML
    Label barraEstado;

    @FXML
    Button bCalcular;

    @FXML
    Button bCancelarCalculo;

    @FXML
    Button bExportar;

    @FXML
    ComboBox<Dia> cbDia;

    @FXML
    ComboBox<Participante> cbParticipante;

    @FXML
    TextField tfEntrada;

    @FXML
    TextField tfSalida;

    @FXML
    CheckBox cCoche;

    @FXML
    TableView<Dia> tablaDias;

    @FXML
    TableColumn<Dia, Integer> columnaIdDia;

    @FXML
    TableColumn<Dia, String> columnaDescripcionDia;

    @FXML
    TextField tfDescripcionDia;

    @FXML
    TableView<Lugar> tablaLugares;

    @FXML
    TableColumn<Lugar, Integer> columnaIdLugar;

    @FXML
    TableColumn<Lugar, String> columnaNombreLugar;

    @FXML
    TextField tfNombreLugar;

    @FXML
    TableView<Participante> tablaParticipantes;

    @FXML
    TableColumn<Participante, Integer> columnaIdParticipante;

    @FXML
    TableColumn<Participante, String> columnaNombreParticipante;

    @FXML
    TableColumn<Participante, Integer> columnaPlazasCoche;

    @FXML
    TableColumn<Participante, List<Lugar>> columnaLugaresParticipante;

    @FXML
    TextField tfNombreParticipante;

    @FXML
    Spinner<Integer> sPlazas;

    @FXML
    ComboBox<Lugar> cbLugares;

    @FXML
    ListView<Lugar> lvLugaresEncuentro;

    @FXML
    ChoiceBox<Resolutor> cbAlgoritmo;

    private Window stage;
    private boolean cerrandoAcercade;
    private ResolutorService resolutorService;

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

        tablaResultado.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Asignacion> ob, Asignacion o, Asignacion n) -> {
            if (n != null) {
                Map<Participante, AsignacionParticipante> mapa = new HashMap<>(datosRueda.getParticipantes().size());
                for (Participante p : n.getConductores()) {
                    AsignacionParticipante a = new AsignacionParticipante();
                    a.setParticipante(p.toString());
                    a.setConductor(true);
                    mapa.put(p, a);
                }
                for (Pair<Participante, Lugar> e : n.getPeIda()) {
                    AsignacionParticipante a = mapa.getOrDefault(e.getKey(), new AsignacionParticipante());
                    a.setParticipante(e.getKey().toString());
                    a.setIda(e.getValue().toString());
                    mapa.putIfAbsent(e.getKey(), a);
                }
                for (Pair<Participante, Lugar> e : n.getPeVuelta()) {
                    AsignacionParticipante a = mapa.getOrDefault(e.getKey(), new AsignacionParticipante());
                    a.setParticipante(e.getKey().toString());
                    a.setVuelta(e.getValue().toString());
                    mapa.putIfAbsent(e.getKey(), a);
                }
                tablaResultadoLugares.setItems(FXCollections.observableArrayList(mapa.values()));
            } else {
                tablaResultadoLugares.setItems(null);
            }
        });

        //Tabla Resultado Lugares
        columnaParticipanteLugares.setCellValueFactory(new PropertyValueFactory<>("participante"));
        columnaLugaresIda.setCellValueFactory(new PropertyValueFactory<>("ida"));
        columnaLugaresVuelta.setCellValueFactory(new PropertyValueFactory<>("vuelta"));
        columnaLugaresConductor.setCellValueFactory(new PropertyValueFactory<>("conductor"));
        columnaLugaresConductor.setCellFactory(CheckBoxTableCell.forTableColumn(columnaLugaresConductor));

        // Tabla de dias
        columnaIdDia.setCellValueFactory(new PropertyValueFactory<>("id"));
        columnaDescripcionDia.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        columnaDescripcionDia.setCellFactory(TextFieldTableCell.forTableColumn());
        columnaDescripcionDia.setOnEditCommit((TableColumn.CellEditEvent<Dia, String> v) -> {
            final String descripcion = v.getNewValue();
            if (descripcion.isEmpty() || datosRueda.getDias().stream().map(Dia::getDescripcion).anyMatch(d -> d.equalsIgnoreCase(descripcion))) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("nombre o descripción inválido");
                alert.setHeaderText("El nombre o descripción del día no es válido o ya está usado");
                alert.setContentText("Por favor, introduzca un nombre o descripción de día que no este en uso");
                alert.showAndWait();
                tablaDias.refresh();
            } else {
                v.getRowValue().setDescripcion(descripcion);
            }
        });

        // Tabla de lugares
        columnaIdLugar.setCellValueFactory(new PropertyValueFactory<>("id"));
        columnaNombreLugar.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        columnaNombreLugar.setCellFactory(TextFieldTableCell.forTableColumn());
        columnaNombreLugar.setOnEditCommit((TableColumn.CellEditEvent<Lugar, String> v) -> {
            final String nombre = v.getNewValue();
            if (nombre.isEmpty() || datosRueda.getLugares().stream().map(Lugar::getNombre).anyMatch(d -> d.equalsIgnoreCase(nombre))) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Nombre de Lugar inválido");
                alert.setHeaderText("El nombre del Lugar de encuentro no es válido o ya está en uso");
                alert.setContentText("Por favor, introduzca un nombre de Lugar de encuentro que no esté en uso");
                alert.showAndWait();
                tablaLugares.refresh();
            } else {
                v.getRowValue().setNombre(nombre);
            }
        });

        //Tabla Participantes
        columnaIdParticipante.setCellValueFactory(new PropertyValueFactory<>("id"));
        columnaNombreParticipante.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        columnaNombreParticipante.setCellFactory(TextFieldTableCell.forTableColumn());
        columnaNombreParticipante.setOnEditCommit((TableColumn.CellEditEvent<Participante, String> v) -> {
            final String nombre = v.getNewValue();
            if (nombre.isEmpty() || datosRueda.getParticipantes().stream().map(Participante::getNombre).anyMatch(d -> d.equalsIgnoreCase(nombre))) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Nombre inválido");
                alert.setHeaderText("El nombre del Participante no es válido o ya está en uso");
                alert.setContentText("Por favor, introduzca un nombre de Participante que no esté en uso");
                alert.showAndWait();
                tablaLugares.refresh();
            } else {
                v.getRowValue().setNombre(nombre);
            }
        });
        columnaPlazasCoche.setCellValueFactory(new PropertyValueFactory<>("plazasCoche"));
        columnaPlazasCoche.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        columnaPlazasCoche.setOnEditCommit((TableColumn.CellEditEvent<Participante, Integer> v) -> {
            final int plazasCoche = v.getNewValue();
            if (plazasCoche < 0 || plazasCoche > 9) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Nº de plazas incorrecto");
                alert.setHeaderText("El nº de plazas debe estar entre 0 y 9");
                alert.setContentText("Cada participante tiene asociado un nº de plazas disponibles en su vehículo que "
                        + "comparte con el resto de participantes. Si un participante tiene 0 plazas se entiende que nunca va "
                        + "a disponer de vehículo propio y por tanto nunca será conductor (sólo será pasajero).");
                alert.showAndWait();
                tablaParticipantes.refresh();
            } else {
                v.getRowValue().setPlazasCoche(plazasCoche);
            }
        });
        columnaLugaresParticipante.setCellValueFactory(new PropertyValueFactory<>("puntosEncuentro"));
        tablaParticipantes.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Participante> ob, Participante o, Participante n) -> {
            if (n != null) {
                lvLugaresEncuentro.setItems(n.puntosEncuentroProperty().get());
            }
        });

        // Spinner del nº de plazas
        sPlazas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 5));

        // Coste de la solución
        lEtiquetaCoste.visibleProperty().bind(lCoste.visibleProperty());

        // Algoritmos para la optimización
        for (String resolutor : RESOLUTORES) {
            try {
                Class<?> resolutorCls = Class.forName(resolutor);
                Resolutor resolutorIns = (Resolutor) resolutorCls.newInstance();
                cbAlgoritmo.getItems().add(resolutorIns);
            } catch(Exception e) {
                System.err.printf("Imposible instanciar el resolutor %s:\n", resolutor);
                e.printStackTrace();
            }
        }
        cbAlgoritmo.setConverter(new StringConverter<Resolutor>() {
            @Override
            public String toString(Resolutor r) {
                return r.getClass().getSimpleName();
            }

            @Override
            public Resolutor fromString(String string) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        cbAlgoritmo.getSelectionModel().select(1);
    }

    public void setMainApp(RuedaFX mainApp) {
        this.mainApp = mainApp;
        this.stage = mainApp.getPrimaryStage();
        datosRueda = mainApp.getDatosRueda();
        tablaHorario.setItems(datosRueda.horariosProperty());
        columnaDia.setCellFactory(ComboBoxTableCell.forTableColumn(datosRueda.diasProperty()));
        columnaParticipante.setCellFactory(ComboBoxTableCell.forTableColumn(datosRueda.participantesProperty()));
        tablaResultado.setItems(datosRueda.asignacionProperty());
        // Combos
        cbDia.setItems(datosRueda.diasProperty());
        cbParticipante.setItems(datosRueda.participantesProperty());
        cbLugares.setItems(datosRueda.lugaresProperty());
        // Tabla de Dias
        tablaDias.setItems(datosRueda.diasProperty());
        // Tabla de Lugares
        tablaLugares.setItems(datosRueda.lugaresProperty());
        //Tabla de Participantes
        tablaParticipantes.setItems(datosRueda.participantesProperty());
        // Etiqueta de coste
        lCoste.textProperty().bind(datosRueda.costeAsignacionProperty().asString("%,d"));
        lCoste.visibleProperty().bind(datosRueda.costeAsignacionProperty().greaterThan(0));
        mExportar.disableProperty().bind(datosRueda.costeAsignacionProperty().isEqualTo(0));
        bExportar.disableProperty().bind(datosRueda.costeAsignacionProperty().isEqualTo(0));
    }

    /**
     * Borra todos los datos y deja un horario vacío
     */
    @FXML
    void handleNew() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Nuevo horario");
        alert.setHeaderText("Eliminar todos los datos actuales y empezar un nuevo horario");
        alert.setContentText("Atención: usando esta función perderá la configuración del horario actual que no haya guardado. "
                + "¿Desea continuar?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            mainApp.setLastFilePath(null);
            datosRueda.reemplazar(new DatosRueda());
        }
    }

    /**
     * Opens a FileChooser to let the user select an address book to load.
     */
    @FXML
    void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        if (mainApp.getLastFilePath() != null) {
            fileChooser.setInitialDirectory(mainApp.getLastFilePath().getParentFile());
        }
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos XML (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            mainApp.cargaHorarios(file);
        }
    }

    /**
     * Saves the file to the person file that is currently open. If there is no
     * open file, the "save as" dialog is shown.
     */
    @FXML
    void handleSave() {
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
    void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter
        if (mainApp.getLastFilePath() != null) {
            fileChooser.setInitialDirectory(mainApp.getLastFilePath().getParentFile());
        }
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos XML (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.getPath().endsWith(".xml")) {
                file = new File(file.getPath() + ".xml");
            }
            mainApp.guardaHorarios(file);
        }
    }

    @FXML
    void handleExportar() {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter
        if (mainApp.getLastFilePath() != null) {
            fileChooser.setInitialDirectory(mainApp.getLastFilePath().getParentFile());
        }
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos HTML (*.html)", "*.html");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        fileChooser.setTitle("Exportar resultado de la asignación");
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.getPath().endsWith(".html")) {
                file = new File(file.getPath() + ".html");
            }
            if (mainApp.exportaAsignacion(file)) {
                barraEstado.setText("Exportación completada con éxito");
                String comando = (SystemUtils.IS_OS_WINDOWS ? "explorer" : "xdg-open") + " \"" + file.getPath() + "\"";
                try {
                    Runtime.getRuntime().exec(comando);
                } catch (Exception e) {
                    System.err.println("Fallo al intentar ejecutar el comando " + comando);
                    e.printStackTrace(System.err);
                }
            } else {
                barraEstado.setText("Exportación cancelada");
            }
        }
    }

    /**
     * Opens an about dialog.
     */
    @FXML
    void handleAbout() {
        // Preparamos la ventana
        cerrandoAcercade = false;
        Stage acercadeStage = new Stage(StageStyle.TRANSPARENT);
        acercadeStage.initOwner(stage);
        acercadeStage.initModality(Modality.APPLICATION_MODAL);
        acercadeStage.setTitle("Acerca de Optimización de Rueda");

        Pane acercadeRoot = new Pane();

        try {
            BackgroundImage fondo = new BackgroundImage(
                    new Image(mainApp.getClass().getResourceAsStream("res/fondo_acercade.png")), BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT
            //new BackgroundSize(100, 100, true, true, true, false)
            );
            acercadeRoot.setBackground(new Background(fondo));
        } catch (Throwable th) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, th);
        }

        FadeTransition fade = new FadeTransition(Duration.seconds(3), acercadeRoot);
        fade.setFromValue(0);
        fade.setToValue(1);

        Light.Distant light = new Light.Distant();
        light.setAzimuth(45.0);
        light.setElevation(30.0);
        Lighting lighting = new Lighting();
        lighting.setLight(light);

        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(128, 128, 160, 0.3));
        ds.setOffsetX(5);
        ds.setOffsetY(5);
        ds.setRadius(5);
        ds.setSpread(0.2);

        Blend blend = new Blend();
        blend.setMode(BlendMode.MULTIPLY);
        blend.setBottomInput(ds);
        blend.setTopInput(lighting);

        Text textoSolidos = new Text("Optimizador de Rueda");
        textoSolidos.setFont(Font.font("", FontWeight.BOLD, 76));
        textoSolidos.setFill(Color.RED);
        textoSolidos.setEffect(blend);
        textoSolidos.setCache(true);

        Text textoAutor = new Text("(c)2016 José Ceferino Ortega");
        textoAutor.setFont(Font.font("", FontWeight.BOLD, FontPosture.ITALIC, 55));
        textoAutor.setFill(Color.AQUAMARINE);
        textoAutor.setEffect(blend);
        textoAutor.setCache(true);

        acercadeRoot.getChildren().addAll(textoAutor, textoSolidos);
        Scene scene = new Scene(acercadeRoot, 1280, 720);
        scene.setFill(null);
        acercadeStage.setScene(scene);

        textoSolidos.setTextOrigin(VPos.TOP);
        Bounds textoSolidosBounds = textoSolidos.getBoundsInLocal();
        textoSolidos.setX((acercadeRoot.getWidth() - textoSolidosBounds.getWidth()) / 2);
        textoSolidos.setY(-textoSolidosBounds.getHeight());
        TranslateTransition entradaTextoSolidos = new TranslateTransition(Duration.millis(2000), textoSolidos);
        entradaTextoSolidos.setFromY(0);
        entradaTextoSolidos.setToY(textoSolidosBounds.getHeight() + 20);

        textoAutor.setTextOrigin(VPos.BOTTOM);
        Bounds textoAutorBounds = textoAutor.getBoundsInLocal();
        textoAutor.setX((acercadeRoot.getWidth() - textoAutorBounds.getWidth()) / 2);
        textoAutor.setY(0);
        TranslateTransition entradaTextoAutor = new TranslateTransition(Duration.millis(2000), textoAutor);
        entradaTextoAutor.setFromY(acercadeRoot.getHeight() + textoAutorBounds.getHeight());
        entradaTextoAutor.setToY(acercadeRoot.getHeight() - 20);

        Animation fadeMusica;
        try {
            // La musica
            Media audioMedia = new Media(mainApp.getClass().getResource("res/musica_acercade.mp3").toURI().toString());

            final int audioSpectrumNumBands = 2;
            MediaPlayer audioMediaPlayer = new MediaPlayer(audioMedia);
            audioMediaPlayer.setVolume(0); //Al principio no se oirá
            //audioMediaPlayer.setAudioSpectrumInterval(0.02);
            audioMediaPlayer.setAudioSpectrumNumBands(audioSpectrumNumBands);
            audioMediaPlayer.setCycleCount(Timeline.INDEFINITE);

            audioMediaPlayer.setAutoPlay(true);

            fadeMusica = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(audioMediaPlayer.volumeProperty(), 0.0)),
                    new KeyFrame(Duration.seconds(3), new KeyValue(audioMediaPlayer.volumeProperty(), 1.0)));
            fadeMusica.setOnFinished(of -> {
                audioMediaPlayer.setAudioSpectrumListener(
                        (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
                            if (!cerrandoAcercade) {
                                textoSolidos.setTranslateY(textoSolidosBounds.getHeight() + (60 + magnitudes[0]));
                                textoAutor.setTranslateY((acercadeRoot.getHeight() - (60 + magnitudes[1])));
                            }
                        });
            });
        } catch (URISyntaxException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            fadeMusica = new PauseTransition(Duration.seconds(3));
        }

        SequentialTransition transiciones = new SequentialTransition(
                new ParallelTransition(fade),
                new ParallelTransition(fadeMusica, entradaTextoSolidos, entradaTextoAutor)
        );

        acercadeRoot.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent emc) -> {
            if (!cerrandoAcercade) {
                cerrandoAcercade = true;
                transiciones.setRate(-2);
                transiciones.play();
                transiciones.setOnFinished(ef -> acercadeStage.close());
            }
        });

        transiciones.play();

        acercadeStage.showAndWait();
    }

    /**
     * Closes the application.
     */
    @FXML
    void handleExit() {
        System.exit(0);
    }

    @FXML
    void handleCalculaAsignacion() {
        if (!datosRueda.validar()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Estado de los datos incoherente");
            alert.setHeaderText("Ha fallado la validación de los datos");
            alert.setContentText(datosRueda.getMensajeValidacion());
            alert.showAndWait();
            return;
        }
        resolutorService = new ResolutorService();
        resolutorService.setResolutor(cbAlgoritmo.getValue());
        resolutorService.setHorarios(new HashSet<>(datosRueda.getHorarios()));
        resolutorService.setOnSucceeded((WorkerStateEvent e) -> {
            barraEstado.textProperty().unbind();
            if (resolutorService.getValue() == null || resolutorService.getValue().isEmpty()) {
                barraEstado.setText("Optimización finalizada, NO HAY SOLUCIÓN");
                datosRueda.setSolucion(null, 0);
            } else {
                barraEstado.setText("Optimización finalizada, calculo de asignación realizado en "+resolutorService.getResolutor().getEstadisticas().getTiempoString());
                datosRueda.setSolucion(resolutorService.getValue(), resolutorService.getResolutor().getEstadisticas().getFitness());
            }
            indicadorProgreso.progressProperty().unbind();
            indicadorProgreso.setProgress(0);
            
            stage.getScene().setCursor(Cursor.DEFAULT);
            bCalcular.setDisable(false);
            mCalcular.setDisable(false);
            bCancelarCalculo.setDisable(true);
            mCancelarCalculo.setDisable(true);
        });
        resolutorService.setOnFailed((WorkerStateEvent e) -> {
            barraEstado.textProperty().unbind();
            barraEstado.setText("Optimización finalizada con fracaso, cálculo de asignación NO realizado. Compruebe los datos de entrada.");
            indicadorProgreso.progressProperty().unbind();
            indicadorProgreso.setProgress(0);

            stage.getScene().setCursor(Cursor.DEFAULT);
            bCalcular.setDisable(false);
            mCalcular.setDisable(false);
            bCancelarCalculo.setDisable(true);
            mCancelarCalculo.setDisable(true);
        });
        barraEstado.textProperty().bind(resolutorService.messageProperty());
        indicadorProgreso.setProgress(-1);
        indicadorProgreso.progressProperty().bind(resolutorService.progressProperty().subtract(0.01));
        datosRueda.asignacionProperty().clear(); // Borramos todo antes de empezar
        stage.getScene().setCursor(Cursor.WAIT);
        resolutorService.start();
        bCancelarCalculo.setDisable(false);
        mCancelarCalculo.setDisable(false);

        bCalcular.setDisable(true);
        mCalcular.setDisable(true);
    }

    @FXML
    void handleCancelaCalculo() {
        bCancelarCalculo.setDisable(true);
        if (resolutorService != null) {
            resolutorService.getResolutor().parar();
        }
    }

    /**
     * Extiende el horario actual duplicando los días y sus entradas
     * correspondientes
     */
    @FXML
    void handleExtiendeHorario() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Extender horario");
        alert.setHeaderText("Duplica el nº de días y extiende el horario");
        alert.setContentText("Atención: usando esta función perderá la configuración del horario actual, "
                + "creandose uno nuevo consistente en duplicar el nº de días repitiendo "
                + "el patrón actual. Es útil para intentar mejorar la asignación en una "
                + "extensión de tiempo mayor. ¿Desea continuar?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Set<Horario> nHorarios = new HashSet<>(datosRueda.getHorarios());
            IntSummaryStatistics estadisticas = datosRueda.getHorarios().stream().mapToInt(h -> h.getDia().getId()).summaryStatistics();
            int desplazamiento = estadisticas.getMax() - estadisticas.getMin() + 1;
            Map<Dia, Dia> dias = datosRueda.getHorarios().stream().map(Horario::getDia).sorted().distinct().collect(toMap(Function.identity(), d -> new Dia(d.getId() + desplazamiento, d.getDescripcion() + "Ex")));
            nHorarios.addAll(datosRueda.getHorarios().stream().map(h -> new Horario(h.getParticipante(), dias.get(h.getDia()), h.getEntrada(), h.getSalida(), h.isCoche())).collect(toList()));
            datosRueda.poblarDesdeHorarios(nHorarios);
            mainApp.setLastFilePath(null); // Eliminamos la referencia al último fichero guardado para evitar lios...
        }
    }

    @FXML
    void handleAdd() {
        String mensaje = null;
        try {
            Dia d = cbDia.getValue();
            Participante p = cbParticipante.getValue();
            if (d == null || p == null) {
                mensaje = "Debe seleccionar un día y un participante";
            } else {
                int entrada = Integer.decode(tfEntrada.getText());
                int salida = Integer.decode(tfSalida.getText());
                if (entrada >= salida) {
                    mensaje = "La hora de entrada debe ser anterior a la de salida";
                } else {
                    Horario h = new Horario(p, d, entrada, salida, cCoche.isSelected());
                    if (datosRueda.getHorarios().contains(h)) {
                        mensaje = "Entrada duplicada: ya existen una entrada para el día y el participante indicados. Si lo desea puede modificarla a través de la tabla.";
                    } else {
                        datosRueda.getHorarios().add(h);
                    }
                }
            }
        } catch (NumberFormatException ne) {
            mensaje = "Entrada y salida deben ser números enteros que indiquen, en forma de cardinal, la hora de entrada/salida, ej. primera hora = 1";
        } catch (Exception e) {
            mensaje = "Imposible añadir la entrada: " + e.getLocalizedMessage();
        }
        if (mensaje != null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Entrada no añadida al horario");
            alert.setHeaderText("No se ha podido añadir la entrada al horario");
            alert.setContentText(mensaje);
            alert.showAndWait();
        }
    }

    @FXML
    void handleDelete() {
        int selectedIndex = tablaHorario.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            tablaHorario.getItems().remove(selectedIndex);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nada seleccionado");
            alert.setHeaderText("No ha seleccionado ninguna entrada en la tabla de horarios");
            alert.setContentText("Por favor, seleccione una entrada del horario para eliminarla");
            alert.showAndWait();
        }
    }

    @FXML
    void handleAddDia() {
        final String descripcion = tfDescripcionDia.getText();
        if (descripcion.isEmpty() || compruebaDia(descripcion)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nombre o descripción inválido");
            alert.setHeaderText("El nombre o descripción del día no es válido o ya está usado");
            alert.setContentText("Por favor, introduzca un nombre o descripción de día que no este en uso");
            alert.showAndWait();
            return;
        }
        datosRueda.getDias().add(new Dia(datosRueda.getDias().stream().mapToInt(Dia::getId).max().orElse(0) + 1, descripcion));
        tfDescripcionDia.clear();
    }

    private boolean compruebaDia(final String nombreDia) {
        return datosRueda.getDias().stream().map(Dia::getDescripcion).anyMatch(d -> d.equalsIgnoreCase(nombreDia));
    }

    @FXML
    void handleAddDiasSemana() {
        String[] dias = DateFormatSymbols.getInstance().getWeekdays();
        for (int i : new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY}) {
            String diaProp = dias[i];
            int intento = 1;
            while (compruebaDia(diaProp)) {
                diaProp = dias[i] + (++intento);
            }
            datosRueda.getDias().add(new Dia(datosRueda.getDias().stream().mapToInt(Dia::getId).max().orElse(0) + 1, diaProp));
        }
    }

    @FXML
    void handleDeleteDia() {
        int selectedIndex = tablaDias.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            final Dia ds = tablaDias.getItems().get(selectedIndex);
            if (datosRueda.getHorarios().stream().map(Horario::getDia).anyMatch(d -> d.equals(ds))) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Dia en uso");
                alert.setHeaderText("El día seleccionado está siendo usado");
                alert.setContentText("El día seleccionado está en uso, si continua se eliminarán todas las entradas del horario que lo usen. ¿Desea continuar?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    final List<Horario> aBorrar = datosRueda.getHorarios().stream().filter(h -> h.getDia().equals(ds)).collect(toList());
                    datosRueda.getHorarios().removeAll(aBorrar);
                } else {
                    return;
                }
            }
            tablaDias.getItems().remove(selectedIndex);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nada seleccionado");
            alert.setHeaderText("No ha seleccionado Día de la tabla");
            alert.setContentText("Por favor, seleccione un Día para eliminarlo");
            alert.showAndWait();
        }
    }

    @FXML
    void handleAddLugar() {
        final String nombre = tfNombreLugar.getText();
        if (nombre.isEmpty() || datosRueda.getLugares().stream().map(Lugar::getNombre).anyMatch(d -> d.equalsIgnoreCase(nombre))) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nombre de Lugar inválido");
            alert.setHeaderText("El nombre del Lugar no es válido o ya está usado");
            alert.setContentText("Por favor, introduzca un nombre de Lugar que no este en uso");
            alert.showAndWait();
            return;
        }
        datosRueda.getLugares().add(new Lugar(datosRueda.getLugares().stream().mapToInt(Lugar::getId).max().orElse(0) + 1, tfNombreLugar.getText(), null, null, null, null, null));
        tfNombreLugar.clear();
    }

    @FXML
    void handleDeleteLugar() {
        int selectedIndex = tablaLugares.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            final Lugar ds = tablaLugares.getItems().get(selectedIndex);
            if (datosRueda.getParticipantes().stream().map(Participante::getPuntosEncuentro).flatMap(List::stream).anyMatch(d -> d.equals(ds))) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Lugar en uso");
                alert.setHeaderText("El Lugar seleccionado está en uso");
                alert.setContentText("El Lugar seleccionado está en uso como punto de encuentro de algún participante. Si continua se eliminarán todos los puntos de encuentro que lo usen. ¿Desea continuar?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    datosRueda.getParticipantes().stream().forEach((Participante p) -> {
                        p.getPuntosEncuentro().remove(ds);
                    });
                } else {
                    return;
                }
            }
            tablaLugares.getItems().remove(selectedIndex);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nada seleccionado");
            alert.setHeaderText("No ha seleccionado ninguno Lugar");
            alert.setContentText("Por favor, seleccione un Lugar para eliminarlo");
            alert.showAndWait();
        }
    }

    @FXML
    void handleAddParticipante() {
        final String nombre = tfNombreParticipante.getText();
        if (nombre.isEmpty() || datosRueda.getParticipantes().stream().map(Participante::getNombre).anyMatch(d -> d.equalsIgnoreCase(nombre))) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nombre de Participante inválido");
            alert.setHeaderText("El nombre del Participante no es válido o ya está en suo");
            alert.setContentText("Por favor, introduzca un nombre de Participante que no esté en uso");
            alert.showAndWait();
            return;
        }
        datosRueda.getParticipantes().add(new Participante(datosRueda.getParticipantes().stream().mapToInt(Participante::getId).max().orElse(0) + 1, nombre, sPlazas.getValue(), null, Collections.emptyList()));
        tfNombreParticipante.clear();
    }

    @FXML
    void handleDeleteParticipante() {
        int selectedIndex = tablaParticipantes.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            final Participante ps = tablaParticipantes.getItems().get(selectedIndex);
            if (datosRueda.getHorarios().stream().map(Horario::getParticipante).anyMatch(d -> d.equals(ps))) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Participante con horario asignado");
                alert.setHeaderText("El Participante seleccionado tiene entradas en el horario");
                alert.setContentText("El Participante seleccionado tiene datos en el horario. Si continua se eliminarán todas las entradas del horario que lo referencien. ¿Desea continuar?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    final List<Horario> aBorrar = datosRueda.getHorarios().stream().filter(h -> h.getParticipante().equals(ps)).collect(toList());
                    datosRueda.getHorarios().removeAll(aBorrar);
                } else {
                    return;
                }
            }
            tablaParticipantes.getItems().remove(selectedIndex);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nada seleccionado");
            alert.setHeaderText("No ha seleccionado ninguno Participante");
            alert.setContentText("Por favor, seleccione un Participante para eliminarlo");
            alert.showAndWait();
        }
    }

    @FXML
    void handleAddLugarEncuentro() {
        if (!lvLugaresEncuentro.getItems().contains(cbLugares.getValue())) {
            lvLugaresEncuentro.getItems().add(cbLugares.getValue());
        }
    }

    @FXML
    void handleDeleteLugarEncuentro() {
        lvLugaresEncuentro.getItems().remove(lvLugaresEncuentro.getSelectionModel().getSelectedItem());
    }

    @FXML
    void handleUpLugarEncuentro() {
        int pos = lvLugaresEncuentro.getSelectionModel().getSelectedIndex();
        if (pos > 0) {
            Lugar l = lvLugaresEncuentro.getSelectionModel().getSelectedItem();
            lvLugaresEncuentro.getItems().remove(l);
            lvLugaresEncuentro.getItems().add(pos - 1, l);
            lvLugaresEncuentro.getSelectionModel().select(l);
        }
    }

    @FXML
    void handleDownLugarEncuentro() {
        int pos = lvLugaresEncuentro.getSelectionModel().getSelectedIndex();
        if (pos < lvLugaresEncuentro.getItems().size() - 1) {
            Lugar l = lvLugaresEncuentro.getSelectionModel().getSelectedItem();
            lvLugaresEncuentro.getItems().remove(l);
            lvLugaresEncuentro.getItems().add(pos + 1, l);
            lvLugaresEncuentro.getSelectionModel().select(l);
        }
    }

    private static class ResolutorService extends Service<Map<Dia, ? extends AsignacionDia>> {

        private final ObjectProperty<Resolutor> resolutor = new SimpleObjectProperty<>();
        private final SetProperty<Horario> horarios = new SimpleSetProperty<Horario>();

        public ResolutorService() {
        }

        public ResolutorService(Resolutor r, Set<Horario> h) {
            resolutor.set(r);
            horarios.clear();
            horarios.addAll(h);
        }

        public Resolutor getResolutor() {
            return resolutor.get();
        }

        public void setResolutor(Resolutor value) {
            resolutor.set(value);
        }

        public ObjectProperty<Resolutor> resolutorProperty() {
            return resolutor;
        }

        public Set<Horario> getHorarios() {
            return horarios.get();
        }

        public void setHorarios(Set<Horario> value) {
            horarios.set(FXCollections.observableSet(value));
        }

        public SetProperty<Horario> horariosProperty() {
            return horarios;
        }

        @Override
        protected Task<Map<Dia, ? extends AsignacionDia>> createTask() {
            return new Task<Map<Dia, ? extends AsignacionDia>>() {
                @Override
                protected Map<Dia, ? extends AsignacionDia> call() throws Exception {
                    updateProgress(0, 1);
                    updateMessage("Iniciando la resolución...");
                    getResolutor().getEstadisticas().progresoProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                        updateProgress(newValue.doubleValue(), 1.0);
                        updateMessage("Calculando asignación: " + getResolutor().getEstadisticas().toString());
                    });
                    return getResolutor().resolver(getHorarios());
                }
            };
        }

    }
}
