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
import es.um.josecefe.rueda.modelo.DatosRueda;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.resolutor.ResolutorV8;
import java.io.File;
import java.net.URISyntaxException;
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
import static java.util.stream.Collectors.toMap;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
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
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * FXML Controller class
 *
 * @author josec
 */
public class PrincipalController {

    private RuedaFX mainApp;

    @FXML
    private DatosRueda datosRueda;

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

    @FXML
    private ComboBox<Dia> cbDia;

    @FXML
    private ComboBox<Participante> cbParticipante;

    @FXML
    private TextField tfEntrada;

    @FXML
    private TextField tfSalida;

    @FXML
    private CheckBox cCoche;

    @FXML
    private TableView<Dia> tablaDias;

    @FXML
    private TableColumn<Dia, Integer> columnaIdDia;

    @FXML
    private TableColumn<Dia, String> columnaDescripcionDia;

    @FXML
    private TextField tfDescripcionDia;

    private Window stage;
    private boolean cerrandoAcercade;

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
    }

    public void setMainApp(RuedaFX mainApp) {
        this.mainApp = mainApp;
        this.stage = mainApp.getPrimaryStage();
        datosRueda = mainApp.getDatosRueda();
        tablaHorario.setItems(datosRueda.getHorarios());
        columnaDia.setCellFactory(ComboBoxTableCell.forTableColumn(datosRueda.getDias()));
        columnaParticipante.setCellFactory(ComboBoxTableCell.forTableColumn(datosRueda.getParticipantes()));
        tablaResultado.setItems(datosRueda.getAsignacion());
        // Combos
        cbDia.setItems(datosRueda.getDias());
        cbParticipante.setItems(datosRueda.getParticipantes());
        // Tabla de Dias
        tablaDias.setItems(datosRueda.getDias());
    }

    /**
     * Borra todos los datos y deja un horario vacío
     */
    @FXML
    private void handleNew() {
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
    private void handleOpen() {
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

    /**
     * Opens an about dialog.
     */
    @FXML
    private void handleAbout() {
        // Preparamos la ventana
        cerrandoAcercade = false;
        Stage acercadeStage = new Stage(StageStyle.TRANSPARENT);
        acercadeStage.initOwner(stage);
        acercadeStage.initModality(Modality.APPLICATION_MODAL);
        acercadeStage.setTitle("Acerca de Optimización de Rueda");
        //acercadeStage.initStyle(StageStyle.TRANSPARENT);

        Pane acercadeRoot = new Pane();

        try {
            BackgroundImage fondo = new BackgroundImage(
                    new Image(mainApp.getClass().getResourceAsStream("res/rueda.png")), BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(100, 100, true, true, true, false));
            //BackgroundFill bf = new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY);
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
//            final double anchoAS = acercadeRoot.getWidth() / (audioSpectrumNumBands * 4);
//            final int altoAS = (int) (acercadeRoot.getHeight() - (textoSolidosBounds.getHeight() + textoAutorBounds.getHeight() + 160));
//            final double yAS = textoSolidosBounds.getHeight() + 40;
//            final String rellenoMusica = "linear-gradient(from 0px " + yAS + "px to 0px " + (yAS + altoAS) + "px, purple 0%, red  30% , orange 60%, yellow 75%,  green 90%, cyan 100%)";
//            final Paint relleno = Paint.valueOf(rellenoMusica);
//            final Rectangle rectAudioSpectrum[] = new Rectangle[audioSpectrumNumBands];
//            for (int i = 0; i < audioSpectrumNumBands; i++) {
//                Rectangle r = new Rectangle();
//                r.setX(anchoAS * audioSpectrumNumBands + anchoAS * i * 2);
//                r.setWidth(anchoAS);
//                r.setY(0);
//                r.setHeight(0); //Al principio no se veran
//                r.setArcWidth(10);
//                r.setArcHeight(10);
//                r.setFill(relleno);
//                rectAudioSpectrum[i] = r;
//                acercadeRoot.getChildren().add(r);
//            }
//
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
//                            for (int i = 0; i < audioSpectrumNumBands; i++) {
//                                final double newHeight = ((60 + magnitudes[i]) / 30.0) * altoAS;
//                                final double newY = yAS + altoAS - newHeight;
//                                rectAudioSpectrum[i].setY(newY);
//                                rectAudioSpectrum[i].setHeight(newHeight);
//                            }
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
    private void handleExit() {
        System.exit(0);
    }

    @FXML
    private void handleCalculaAsignacion() {
        ResolutorService resolutorService = new ResolutorService();
        resolutorService.setResolutor(new ResolutorV8(new HashSet<>(datosRueda.getHorarios())));
        resolutorService.setOnSucceeded((WorkerStateEvent e) -> {
            datosRueda.setCosteAsignacion(resolutorService.getResolutor().getEstadisticas().get().getFitness());
            barraEstado.textProperty().unbind();
            barraEstado.setText("Calculo de asignación completado con un coste final de " + datosRueda.getCosteAsignacion());
            indicadorProgreso.progressProperty().unbind();
            indicadorProgreso.setProgress(0);
            datosRueda.setSolucion(resolutorService.getValue());
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        barraEstado.textProperty().bind(resolutorService.messageProperty());
        indicadorProgreso.setProgress(0);
        indicadorProgreso.progressProperty().bind(resolutorService.progressProperty());
        datosRueda.asignacionProperty().clear(); // Borramos todo antes de empezar
        stage.getScene().setCursor(Cursor.WAIT);
        resolutorService.start();
    }

    /**
     * Extiende el horario actual duplicando los días y sus entradas
     * correspondientes
     */
    @FXML
    private void handleExtiendeHorario() {
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
        if (descripcion.isEmpty() || datosRueda.getDias().stream().map(Dia::getDescripcion).anyMatch(d -> d.equalsIgnoreCase(descripcion))) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("nombre o descripción inválido");
            alert.setHeaderText("El nombre o descripción del día no es válido o ya está usado");
            alert.setContentText("Por favor, introduzca un nombre o descripción de día que no este en uso");
            alert.showAndWait();
            return;
        }
        datosRueda.getDias().add(new Dia(datosRueda.getDias().stream().mapToInt(Dia::getId).max().orElse(0) + 1, tfDescripcionDia.getText()));
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
            alert.setHeaderText("No ha seleccionado ninguna entrada en la tabla de horarios");
            alert.setContentText("Por favor, seleccione una entrada del horario para eliminarla");
            alert.showAndWait();
        }
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
                    updateProgress(0, 1);
                    updateMessage("Iniciando la resolución...");
                    resolutor.get().progresoProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                        updateProgress(newValue.doubleValue(), 1.0);
                        updateMessage("Calculando asignación: " + resolutor.get().getEstadisticas().get().toString());
                    });
                    return resolutor.get().resolver();
                }
            };
        }

    }
}
