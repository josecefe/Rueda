/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.vista

import es.um.josecefe.rueda.COPYRIGHT
import es.um.josecefe.rueda.RuedaApp
import es.um.josecefe.rueda.TITLE
import es.um.josecefe.rueda.VERSION
import es.um.josecefe.rueda.modelo.*
import es.um.josecefe.rueda.resolutor.Resolutor
import es.um.josecefe.rueda.resolutor.Resolutor.*
import javafx.animation.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SetProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleSetProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.effect.*
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.paint.Color
import javafx.scene.text.*
import javafx.stage.*
import javafx.util.Duration
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter
import org.apache.commons.lang3.SystemUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.DateFormatSymbols
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.system.exitProcess

/**
 * FXML Controller class
 *
 * @author josec
 */
class PrincipalController {
    @FXML
    internal lateinit var tablaHorario: TableView<Horario>
    @FXML
    internal lateinit var columnaDia: TableColumn<Horario, Dia>
    @FXML
    internal lateinit var columnaParticipante: TableColumn<Horario, Participante>
    @FXML
    internal lateinit var columnaEntrada: TableColumn<Horario, Int>
    @FXML
    internal lateinit var columnaSalida: TableColumn<Horario, Int>
    @FXML
    internal lateinit var columnaCoche: TableColumn<Horario, Boolean>
    @FXML
    internal lateinit var lCoste: Label
    @FXML
    internal lateinit var tablaResultado: TableView<Asignacion>
    @FXML
    internal lateinit var lEtiquetaCoste: Label
    @FXML
    internal lateinit var columnaDiaAsignacion: TableColumn<Asignacion, Dia>
    @FXML
    internal lateinit var columnaConductores: TableColumn<Asignacion, Set<Participante>>
    @FXML
    internal lateinit var columnaPeIda: TableColumn<Asignacion, Map<Participante, Lugar>>
    @FXML
    internal lateinit var columnaPeVuelta: TableColumn<Asignacion, Map<Participante, Lugar>>
    @FXML
    internal lateinit var columnaCoste: TableColumn<Asignacion, Int>
    @FXML
    internal lateinit var tablaResultadoLugares: TableView<AsignacionParticipante>
    @FXML
    internal lateinit var columnaParticipanteLugares: TableColumn<AsignacionParticipante, Participante>
    @FXML
    internal lateinit var columnaLugaresIda: TableColumn<AsignacionParticipante, Lugar>
    @FXML
    internal lateinit var columnaLugaresVuelta: TableColumn<AsignacionParticipante, Lugar>
    @FXML
    internal lateinit var columnaLugaresConductor: TableColumn<AsignacionParticipante, Boolean>
    @FXML
    internal lateinit var indicadorProgreso: ProgressBar
    @FXML
    internal lateinit var barraEstado: Label
    @FXML
    internal lateinit var bCalcular: Button
    @FXML
    internal lateinit var bCancelarCalculo: Button
    @FXML
    internal lateinit var bExportar: Button
    @FXML
    internal lateinit var cbDia: ComboBox<Dia>
    @FXML
    internal lateinit var cbParticipante: ComboBox<Participante>
    @FXML
    internal lateinit var tfEntrada: TextField
    @FXML
    internal lateinit var tfSalida: TextField
    @FXML
    internal lateinit var cCoche: CheckBox
    @FXML
    internal lateinit var tablaDias: TableView<Dia>
    @FXML
    internal lateinit var columnaDescripcionDia: TableColumn<Dia, String>
    @FXML
    internal lateinit var tfDescripcionDia: TextField
    @FXML
    internal lateinit var tablaLugares: TableView<Lugar>
    @FXML
    internal lateinit var columnaNombreLugar: TableColumn<Lugar, String>
    @FXML
    internal lateinit var tfNombreLugar: TextField
    @FXML
    internal lateinit var tablaParticipantes: TableView<Participante>
    @FXML
    internal lateinit var columnaNombreParticipante: TableColumn<Participante, String>
    @FXML
    internal lateinit var columnaPlazasCoche: TableColumn<Participante, Int>
    @FXML
    internal lateinit var columnaLugaresParticipante: TableColumn<Participante, List<Lugar>>
    @FXML
    internal lateinit var tfNombreParticipante: TextField
    @FXML
    internal lateinit var sPlazas: Spinner<Int>
    @FXML
    internal lateinit var cbLugares: ComboBox<Lugar>
    @FXML
    internal lateinit var lvLugaresEncuentro: ListView<Lugar>
    @FXML
    internal lateinit var cbAlgoritmo: ChoiceBox<Resolutor>
    @FXML
    internal lateinit var cbEstrategia: ChoiceBox<Estrategia>
    @FXML
    private lateinit var mCalcular: MenuItem
    @FXML
    private lateinit var mCancelarCalculo: MenuItem
    @FXML
    private lateinit var mExportar: MenuItem

    private lateinit var mainApp: RuedaApp
    private lateinit var stage: Window

    private var datosRuedaFX: DatosRuedaFX = DatosRuedaFX()
    private var cerrandoAcercade: Boolean = false
    private var resolutorService: ResolutorService? = null

    /**
     * Initializes the controller class.
     */
    @FXML
    fun initialize() {
        // Tabla de horarios
        columnaDia.cellValueFactory = PropertyValueFactory("dia")
        columnaParticipante.cellValueFactory = PropertyValueFactory("participante")
        columnaEntrada.cellValueFactory = PropertyValueFactory("entrada")
        columnaEntrada.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        columnaSalida.cellValueFactory = PropertyValueFactory("salida")
        columnaSalida.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        columnaCoche.cellValueFactory = PropertyValueFactory("coche")
        columnaCoche.cellFactory = CheckBoxTableCell.forTableColumn(columnaCoche)

        // Tabla de asignaciones
        columnaDiaAsignacion.cellValueFactory = PropertyValueFactory("dia")
        columnaConductores.cellValueFactory = PropertyValueFactory("conductores")
        columnaPeIda.cellValueFactory = PropertyValueFactory("peIda")
        columnaPeVuelta.cellValueFactory = PropertyValueFactory("peVuelta")
        columnaCoste.cellValueFactory = PropertyValueFactory("coste")

        tablaResultado.selectionModel.selectedItemProperty().addListener { _: ObservableValue<out Asignacion?>, _: Asignacion?, n: Asignacion? ->
            if (n != null) {
                val mapa = HashMap<Participante, AsignacionParticipante>(datosRuedaFX.participantes.size)
                for (p in n.conductores) {
                    val a = AsignacionParticipante()
                    a.participante = p.toString()
                    a.isConductor = true
                    mapa[p] = a
                }
                for (e in n.peIda) {
                    val a = mapa.getOrDefault(e.participante, AsignacionParticipante())
                    a.participante = e.participante.toString()
                    a.ida = e.lugar.toString()
                    mapa.putIfAbsent(e.participante, a)
                }
                for (e in n.peVuelta) {
                    val a = mapa.getOrDefault(e.participante, AsignacionParticipante())
                    a.participante = e.participante.toString()
                    a.vuelta = e.lugar.toString()
                    mapa.putIfAbsent(e.participante, a)
                }
                tablaResultadoLugares.setItems(FXCollections.observableArrayList(mapa.values))
            } else {
                tablaResultadoLugares.setItems(null)
            }
        }

        //Tabla Resultado Lugares
        columnaParticipanteLugares.cellValueFactory = PropertyValueFactory("participante")
        columnaLugaresIda.cellValueFactory = PropertyValueFactory("ida")
        columnaLugaresVuelta.cellValueFactory = PropertyValueFactory("vuelta")
        columnaLugaresConductor.cellValueFactory = PropertyValueFactory("conductor")
        columnaLugaresConductor.cellFactory = CheckBoxTableCell.forTableColumn(columnaLugaresConductor)

        // Tabla de dias
        columnaDescripcionDia.cellValueFactory = PropertyValueFactory("descripcion")
        columnaDescripcionDia.cellFactory = TextFieldTableCell.forTableColumn()
        columnaDescripcionDia.setOnEditCommit { v: TableColumn.CellEditEvent<Dia, String> ->
            val descripcion = v.newValue
            if (descripcion.isEmpty() || datosRuedaFX.dias.map { it.descripcion }.any {
                it.equals(descripcion, ignoreCase = true)
            }) {
                val alert = Alert(AlertType.WARNING)
                alert.initOwner(stage)
                alert.title = "nombre o descripción inválido"
                alert.headerText = "El nombre o descripción del día no es válido o ya está usado"
                alert.contentText = "Por favor, introduzca un nombre o descripción de día que no este en uso"
                alert.showAndWait()
                tablaDias.refresh()
            } else {
                v.rowValue.descripcion = descripcion
            }
        }

        // Tabla de lugares
        columnaNombreLugar.cellValueFactory = PropertyValueFactory("nombre")
        columnaNombreLugar.cellFactory = TextFieldTableCell.forTableColumn()
        columnaNombreLugar.setOnEditCommit { v: TableColumn.CellEditEvent<Lugar, String> ->
            val nombre = v.newValue
            if (nombre.isEmpty() || datosRuedaFX.lugares.map { it.nombre }.any { it.equals(nombre, ignoreCase = true) }) {
                val alert = Alert(AlertType.WARNING)
                alert.initOwner(stage)
                alert.title = "Nombre de Lugar inválido"
                alert.headerText = "El nombre del Lugar de encuentro no es válido o ya está en uso"
                alert.contentText = "Por favor, introduzca un nombre de Lugar de encuentro que no esté en uso"
                alert.showAndWait()
                tablaLugares.refresh()
            } else {
                v.rowValue.nombre = nombre
            }
        }

        //Tabla Participantes
        columnaNombreParticipante.cellValueFactory = PropertyValueFactory("nombre")
        columnaNombreParticipante.cellFactory = TextFieldTableCell.forTableColumn()
        columnaNombreParticipante.setOnEditCommit { v: TableColumn.CellEditEvent<Participante, String> ->
            val nombre = v.newValue
            if (nombre.isEmpty() || datosRuedaFX.participantes.map { it.nombre }.any {
                it.equals(nombre, ignoreCase = true)
            }) {
                val alert = Alert(AlertType.WARNING)
                alert.initOwner(stage)
                alert.title = "Nombre inválido"
                alert.headerText = "El nombre del Participante no es válido o ya está en uso"
                alert.contentText = "Por favor, introduzca un nombre de Participante que no esté en uso"
                alert.showAndWait()
                tablaLugares.refresh()
            } else {
                v.rowValue.nombre = nombre
            }
        }
        columnaPlazasCoche.cellValueFactory = PropertyValueFactory("plazasCoche")
        columnaPlazasCoche.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        columnaPlazasCoche.setOnEditCommit { v: TableColumn.CellEditEvent<Participante, Int> ->
            val plazasCoche = v.newValue
            if (plazasCoche < 0 || plazasCoche > 9) {
                val alert = Alert(AlertType.WARNING)
                alert.initOwner(stage)
                alert.title = "Nº de plazas incorrecto"
                alert.headerText = "El nº de plazas debe estar entre 0 y 9"
                alert.contentText = "Cada participante tiene asociado un nº de plazas disponibles en su vehículo que comparte con el resto de participantes. Si un participante tiene 0 plazas se entiende que nunca va a disponer de vehículo propio y por tanto nunca será conductor (sólo será pasajero)."
                alert.showAndWait()
                tablaParticipantes.refresh()
            } else {
                v.rowValue.plazasCoche = plazasCoche
            }
        }
        columnaLugaresParticipante.cellValueFactory = PropertyValueFactory("puntosEncuentro")
        tablaParticipantes.selectionModel.selectedItemProperty().addListener { _: ObservableValue<out Participante>, _: Participante?, n: Participante? ->
            if (n != null) lvLugaresEncuentro.items = n.puntosEncuentroProperty().get()
        }

        // Spinner del nº de plazas
        sPlazas.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 5)

        // Coste de la solución
        lEtiquetaCoste.visibleProperty().bind(lCoste.visibleProperty())

        // Algoritmos para la optimización
        for (resolutor in RESOLUTORES) {
            try {
                val resolutorCls = Class.forName(resolutor)
                val resolutorIns = resolutorCls.getDeclaredConstructor().newInstance() as Resolutor
                cbAlgoritmo.items.add(resolutorIns)
            } catch (e: Exception) {
                Logger.getLogger(javaClass.name).log(Level.SEVERE, "Imposible instanciar el resolutor $resolutor", e)
            }

        }
        cbAlgoritmo.converter = object : StringConverter<Resolutor>() {
            override fun toString(r: Resolutor): String {
                return r.javaClass.simpleName
            }

            override fun fromString(string: String): Resolutor {
                throw UnsupportedOperationException("Not supported yet.")
            }
        }
        //cbAlgoritmo.selectionModel.select(if (Runtime.getRuntime().availableProcessors() >= 4) 1 else 0)
        cbAlgoritmo.selectionModel.select(0)

        cbEstrategia.items.addAll(*Estrategia.entries.toTypedArray())
        cbEstrategia.converter = object : StringConverter<Estrategia>() {
            override fun toString(e: Estrategia): String {
                return e.toString()
            }

            override fun fromString(string: String): Estrategia {
                return Estrategia.valueOf(string)
            }
        }

        cbEstrategia.selectionModel.select(0)
    }

    fun setMainApp(mainApp: RuedaApp) {
        this.mainApp = mainApp
        this.stage = mainApp.getPrimaryStage()
        tablaHorario.items = datosRuedaFX.horariosProperty
        columnaDia.cellFactory = ComboBoxTableCell.forTableColumn(datosRuedaFX.diasProperty)
        columnaParticipante.cellFactory = ComboBoxTableCell.forTableColumn(datosRuedaFX.participantesProperty)
        tablaResultado.items = datosRuedaFX.asignacionProperty
        // Combos
        cbDia.items = datosRuedaFX.diasProperty
        cbParticipante.items = datosRuedaFX.participantesProperty
        cbLugares.items = datosRuedaFX.lugaresProperty
        // Tabla de Dias
        tablaDias.items = datosRuedaFX.diasProperty
        // Tabla de Lugares
        tablaLugares.items = datosRuedaFX.lugaresProperty
        //Tabla de Participantes
        tablaParticipantes.items = datosRuedaFX.participantesProperty
        // Etiqueta de coste
        lCoste.textProperty().bind(datosRuedaFX.costeAsignacionProperty.asString("%,d"))
        lCoste.visibleProperty().bind(datosRuedaFX.costeAsignacionProperty.greaterThan(0))
        mExportar.disableProperty().bind(datosRuedaFX.costeAsignacionProperty.isEqualTo(0))
        bExportar.disableProperty().bind(datosRuedaFX.costeAsignacionProperty.isEqualTo(0))
    }

    /**
     * Borra todos los datos y deja un horario vacío
     */
    @FXML
    fun handleNew() {
        val alert = Alert(AlertType.CONFIRMATION)
        alert.initOwner(stage)
        alert.title = "Nuevo horario"
        alert.headerText = "Eliminar todos los datos actuales y empezar un nuevo horario"
        alert.contentText = "Atención: usando esta función perderá la configuración del horario actual que no haya guardado. " + "¿Desea continuar?"
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            mainApp.lastFilePath = null
            datosRuedaFX.reemplazar(DatosRueda())
        }
    }

    /**
     * Opens a FileChooser to let the user select an address book to load.
     */
    @FXML
    fun handleOpen() {
        val fileChooser = FileChooser()
        if (mainApp.lastFilePath != null && mainApp.lastFilePath!!.parentFile.isDirectory) {
            fileChooser.initialDirectory = mainApp.lastFilePath!!.parentFile
        }
        // Set extension filter
        val extFilter = FileChooser.ExtensionFilter(
                "Archivos de horarios (*.drj;*.json;*.xml)", "*.drj", "*.json", "*.xml")
        fileChooser.extensionFilters.add(extFilter)

        // Show save file dialog
        val file = fileChooser.showOpenDialog(stage)

        if (file != null) {
            datosRuedaFX.reemplazar(mainApp.cargaHorarios(file))
        }
    }

    /**
     * Saves the file to the person file that is currently open. If there is no
     * open file, the "save as" dialog is shown.
     */
    @FXML
    fun handleSave() {
        val personFile = mainApp.lastFilePath
        if (personFile != null) {
            mainApp.guardaHorarios(personFile, datosRuedaFX.toDatosRueda())
        } else {
            handleSaveAs()
        }
    }

    /**
     * Opens a FileChooser to let the user select a file to save to.
     */
    @FXML
    fun handleSaveAs() {
        val fileChooser = FileChooser()

        // Set extension filter
        if (mainApp.lastFilePath != null && mainApp.lastFilePath!!.parentFile.isDirectory) {
            fileChooser.initialDirectory = mainApp.lastFilePath!!.parentFile
        }
        val extFilter = FileChooser.ExtensionFilter(
                "Archivos de horario (*.drj)", "*.drj")
        fileChooser.extensionFilters.add(extFilter)

        // Show save file dialog
        var file: File? = fileChooser.showSaveDialog(stage)

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.path.endsWith(".drj")) {
                file = File(file.path + ".drj")
            }
            mainApp.guardaHorarios(file, datosRuedaFX.toDatosRueda())
        }
    }

    @FXML
    fun handleExportar() {
        val fileChooser = FileChooser()

        // Set extension filter
        if (mainApp.lastFilePath != null && mainApp.lastFilePath!!.parentFile.isDirectory) {
            fileChooser.initialDirectory = mainApp.lastFilePath!!.parentFile
            fileChooser.initialFileName = mainApp.lastFilePath!!.name.replace(".json", ".html")
        }
        val extFilter = FileChooser.ExtensionFilter(
                "Archivos HTML (*.html)", "*.html")
        fileChooser.extensionFilters.add(extFilter)

        // Show save file dialog
        fileChooser.title = "Exportar resultado de la asignación"
        var file: File? = fileChooser.showSaveDialog(stage)

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.path.endsWith(".html")) {
                file = File(file.path + ".html")
            }
            if (mainApp.exportaAsignacion(file, datosRuedaFX.toDatosRueda())) {
                barraEstado.text = "Exportación completada con éxito"
                val comando = (if (SystemUtils.IS_OS_WINDOWS) "explorer" else "xdg-open")
                try {
                    ProcessBuilder(comando, file.path).inheritIO().start()
                } catch (e: Exception) {
                    System.err.println("Fallo al intentar abrir el archivo: ${file.path}")
                    e.printStackTrace(System.err)
                }

            } else {
                barraEstado.text = "Exportación cancelada"
            }
        }
    }

    /**
     * Opens an about dialog.
     */
    @FXML
    fun handleAbout() {
        // Preparamos la ventana
        cerrandoAcercade = false
        val acercadeStage = Stage(StageStyle.TRANSPARENT)
        acercadeStage.initOwner(stage)
        acercadeStage.initModality(Modality.APPLICATION_MODAL)
        acercadeStage.title = "Acerca de Optimización de Rueda"

        val acercadeRoot = Pane()

        try {
            val fondo = BackgroundImage(
                    Image(mainApp.javaClass.getResourceAsStream("res/fondo_acercade.png")),
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT
                    //new BackgroundSize(100, 100, true, true, true, false)
            )
            acercadeRoot.background = Background(fondo)
        } catch (th: Throwable) {
            Logger.getLogger(javaClass.name).log(Level.SEVERE, null, th)
        }

        val fade = FadeTransition(Duration.seconds(3.0), acercadeRoot)
        fade.fromValue = 0.0
        fade.toValue = 1.0

        val light = Light.Distant()
        light.azimuth = 45.0
        light.elevation = 30.0
        val lighting = Lighting()
        lighting.light = light

        val ds = DropShadow()
        ds.color = Color.rgb(128, 128, 160, 0.3)
        ds.offsetX = 5.0
        ds.offsetY = 5.0
        ds.radius = 5.0
        ds.spread = 0.2

        val blend = Blend()
        blend.mode = BlendMode.MULTIPLY
        blend.bottomInput = ds
        blend.topInput = lighting

        val textoSolidos = Text(TITLE)
        textoSolidos.font = Font.font("", FontWeight.BOLD, 76.0)
        textoSolidos.fill = Color.RED
        textoSolidos.effect = blend
        textoSolidos.isCache = true

        val textoAutor = Text(COPYRIGHT)
        textoAutor.font = Font.font("", FontWeight.BOLD, FontPosture.ITALIC, 55.0)
        textoAutor.fill = Color.AQUAMARINE
        textoAutor.effect = blend
        textoAutor.isCache = true

        var creditosString = String.format("%s %s - %s\n=====================================\n\n", TITLE, VERSION,
                COPYRIGHT)
        try {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            BufferedReader(InputStreamReader(mainApp.javaClass.getResourceAsStream("res/creditos.txt"),
                    StandardCharsets.UTF_8)).use { recursoCreditos ->
                creditosString += recursoCreditos.lines().collect(Collectors.joining("\n"))
            }
        } catch (ex: Exception) {
            Logger.getLogger(PrincipalController::class.java.name).log(Level.SEVERE, null, ex)
        }

        creditosString = creditosString
                .replaceFirst("%java-version%".toRegex(), System.getProperty("java.version"))
                .replaceFirst("%java-vendor%".toRegex(), System.getProperty("java.vendor"))
                .replaceFirst("%os-name%".toRegex(), System.getProperty("os.name"))
                .replaceFirst("%os-version%".toRegex(), System.getProperty("os.version"))
        val textoCreditos = Text(creditosString)
        textoCreditos.textAlignment = TextAlignment.CENTER
        textoCreditos.font = Font.font("", FontWeight.BOLD, 22.0)
        textoCreditos.fill = Color.ROYALBLUE
        textoCreditos.effect = blend
        textoCreditos.isCache = true

        acercadeRoot.children.addAll(textoCreditos, textoAutor, textoSolidos)
        val scene = Scene(acercadeRoot, 1280.0, 720.0)
        scene.fill = null
        acercadeStage.scene = scene

        textoSolidos.textOrigin = VPos.TOP
        val textoSolidosBounds = textoSolidos.boundsInLocal
        textoSolidos.x = (acercadeRoot.width - textoSolidosBounds.width) / 2
        textoSolidos.y = -textoSolidosBounds.height
        val entradaTextoSolidos = TranslateTransition(Duration.millis(2000.0), textoSolidos)
        entradaTextoSolidos.fromY = 0.0
        entradaTextoSolidos.toY = textoSolidosBounds.height + 20

        textoAutor.textOrigin = VPos.BOTTOM
        val textoAutorBounds = textoAutor.boundsInLocal
        textoAutor.x = (acercadeRoot.width - textoAutorBounds.width) / 2
        textoAutor.y = 0.0
        val entradaTextoAutor = TranslateTransition(Duration.millis(2000.0), textoAutor)
        entradaTextoAutor.fromY = acercadeRoot.height + textoAutorBounds.height
        entradaTextoAutor.toY = acercadeRoot.height - 20

        textoCreditos.textOrigin = VPos.TOP
        textoCreditos.wrappingWidth = max(textoAutorBounds.width, textoSolidosBounds.width)
        val textoCreditosBounds = textoCreditos.boundsInLocal
        textoCreditos.x = (acercadeRoot.width - textoCreditosBounds.width) / 2
        textoCreditos.y = acercadeRoot.height

        val scrollTextoCreditos = TranslateTransition(Duration.millis(30000.0), textoCreditos)
        //scrollTextoCreditos.setFromY(acercadeRoot.getHeight());
        scrollTextoCreditos.toY = -max(
                textoCreditosBounds.height + (acercadeRoot.height - textoCreditosBounds.height) / 2,
                textoCreditosBounds.height)
        scrollTextoCreditos.delay = Duration.seconds(3.0)

        var fadeMusica: Animation
        try {
            // La musica
            @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            val audioMedia = Media(mainApp.javaClass.getResource("res/musica_acercade.mp3").toURI().toString())

            val audioSpectrumNumBands = 2
            val audioMediaPlayer = MediaPlayer(audioMedia)
            audioMediaPlayer.volume = 0.0 //Al principio no se oirá
            audioMediaPlayer.audioSpectrumNumBands = audioSpectrumNumBands
            audioMediaPlayer.cycleCount = Timeline.INDEFINITE

            audioMediaPlayer.isAutoPlay = true

            fadeMusica = Timeline(
                    KeyFrame(Duration.ZERO, KeyValue(audioMediaPlayer.volumeProperty(), 0.0)),
                    KeyFrame(Duration.seconds(3.0), KeyValue(audioMediaPlayer.volumeProperty(), 1.0)))
            fadeMusica.setOnFinished {
                audioMediaPlayer.setAudioSpectrumListener { _: Double, _: Double, magnitudes: FloatArray, _: FloatArray ->
                    if (!cerrandoAcercade) {
                        textoSolidos.translateY = textoSolidosBounds.height + (60 + magnitudes[0])
                        textoAutor.translateY = acercadeRoot.height - (60 + magnitudes[1])
                    }
                }
            }
        } catch (ex: Exception) {
            Logger.getLogger(javaClass.name).log(Level.SEVERE, null, ex)
            fadeMusica = PauseTransition(Duration.seconds(3.0))
        }

        val transiciones = SequentialTransition(
                ParallelTransition(fade),
                ParallelTransition(fadeMusica, entradaTextoSolidos, entradaTextoAutor)
        )

        acercadeRoot.addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (!cerrandoAcercade) {
                cerrandoAcercade = true
                scrollTextoCreditos.stop()
                transiciones.rate = -2.0
                transiciones.play()
                transiciones.setOnFinished { acercadeStage.close() }
            }
        }

        transiciones.play()
        scrollTextoCreditos.play()

        acercadeStage.showAndWait()
    }

    /**
     * Closes the application.
     */
    @FXML
    fun handleExit() {
        exitProcess(0)
    }

    @FXML
    fun handleCalculaAsignacion() {
        if (!datosRuedaFX.validar()) {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Estado de los datos incoherente"
            alert.headerText = "Ha fallado la validación de los datos"
            alert.contentText = datosRuedaFX.mensajeValidacion
            alert.showAndWait()
            return
        }
        val service = ResolutorService()
        resolutorService = service
        val r: Resolutor = cbAlgoritmo.value
        r.estrategia = cbEstrategia.value
        with(service) {
            setResolutor(cbAlgoritmo.value)
            setHorarios(HashSet(datosRuedaFX.horarios))
        }
        service.setOnSucceeded {
            barraEstado.textProperty().unbind()
            if (service.value == null || service.value.isEmpty()) {
                barraEstado.text = "Optimización finalizada, NO HAY SOLUCIÓN"
                datosRuedaFX.setSolucion(emptyMap(), 0)
            } else {
                barraEstado.text = "Optimización finalizada, calculo de asignación realizado en " + service.getResolutor().estadisticas.tiempoString
                datosRuedaFX.setSolucion(service.value,
                        service.getResolutor().estadisticas.fitness)
            }
            indicadorProgreso.progressProperty().unbind()
            indicadorProgreso.progress = 0.0

            stage.scene.cursor = Cursor.DEFAULT
            bCalcular.isDisable = false
            mCalcular.isDisable = false
            bCancelarCalculo.isDisable = true
            mCancelarCalculo.isDisable = true
        }
        service.setOnFailed {
            barraEstado.textProperty().unbind()
            if (service.exception != null) {

                val alert = Alert(AlertType.ERROR)
                alert.initOwner(stage)
                alert.title = "Algo ha fallado"
                alert.headerText = "Fallo la optimización"
                alert.contentText = "El cálculo no ha ido bien. Revise los datos de entrada y\nsi el problema persiste, revise la instalación de la aplicación"

                val ex = service.exception

                Logger.getLogger(javaClass.name).log(Level.INFO, "Problema intentando calcular una asignación: ",
                        ex)

                // Create expandable Exception.
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                ex.printStackTrace(pw)
                val exceptionText = sw.toString()

                val label = Label("La traza de la excepción fue:")

                val textArea = TextArea(exceptionText)
                textArea.isEditable = false
                textArea.isWrapText = true

                textArea.maxWidth = java.lang.Double.MAX_VALUE
                textArea.maxHeight = java.lang.Double.MAX_VALUE
                GridPane.setVgrow(textArea, Priority.ALWAYS)
                GridPane.setHgrow(textArea, Priority.ALWAYS)

                val expContent = GridPane()
                expContent.maxWidth = java.lang.Double.MAX_VALUE
                expContent.add(label, 0, 0)
                expContent.add(textArea, 0, 1)

                // Set expandable Exception into the dialog pane.
                alert.dialogPane.expandableContent = expContent

                alert.showAndWait()

            }
            barraEstado.text = "Optimización finalizada con fracaso, cálculo de asignación NO realizado. Compruebe los datos de entrada."
            indicadorProgreso.progressProperty().unbind()
            indicadorProgreso.progress = 0.0

            stage.scene.cursor = Cursor.DEFAULT
            bCalcular.isDisable = false
            mCalcular.isDisable = false
            bCancelarCalculo.isDisable = true
            mCancelarCalculo.isDisable = true
        }

        barraEstado.textProperty().bind(service.messageProperty())
        indicadorProgreso.progress = -1.0
        indicadorProgreso.progressProperty().bind(service.progressProperty().subtract(0.01))
        datosRuedaFX.asignacionProperty.clear() // Borramos antes de empezar
        stage.scene.cursor = Cursor.WAIT
        service.start()
        bCancelarCalculo.isDisable = false
        mCancelarCalculo.isDisable = false

        bCalcular.isDisable = true
        mCalcular.isDisable = true
    }

    @FXML
    fun handleCancelaCalculo() {
        bCancelarCalculo.isDisable = true
        if (resolutorService != null) {
            resolutorService!!.getResolutor().parar()
        }
    }

    /**
     * Extiende el horario actual duplicando los días y sus entradas
     * correspondientes
     */
    @FXML
    fun handleExtiendeHorario() {
        val alert = Alert(AlertType.CONFIRMATION)
        alert.initOwner(stage)
        alert.title = "Extender horario"
        alert.headerText = "Duplica el nº de días y extiende el horario"
        alert.contentText = "Atención: usando esta función perderá la configuración del horario actual, creandose uno nuevo consistente en duplicar el nº de días repitiendo el patrón actual. Es útil para intentar mejorar la asignación en una extensión de tiempo mayor. ¿Desea continuar?"
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val nHorarios = HashSet(datosRuedaFX.horarios)
            val diasOriginal = datosRuedaFX.horarios.map { it.dia }.distinct()
            val maxNOrden = diasOriginal.maxOfOrNull { it.orden } ?: 1
            val dias = datosRuedaFX.horarios.sorted().map { it.dia }.distinct().associateWith { Dia(descripcion = it.descripcion + "Ex", orden = maxNOrden + it.orden) }
            nHorarios.addAll(datosRuedaFX.horarios.map { Horario(it.participante, dias[it.dia], it.entrada, it.salida, it.coche) })
            datosRuedaFX.poblarDesdeHorarios(nHorarios)
            mainApp.lastFilePath = null // Eliminamos la referencia al último fichero guardado para evitar lios...
        }
    }

    @FXML
    fun handleAdd() {
        var mensaje: String? = null
        try {
            val d = cbDia.value
            val p = cbParticipante.value
            if (d == null || p == null) {
                mensaje = "Debe seleccionar un día y un participante"
            } else {
                val entrada = Integer.decode(tfEntrada.text)
                val salida = Integer.decode(tfSalida.text)
                if (entrada >= salida) {
                    mensaje = "La hora de entrada debe ser anterior a la de salida"
                } else {
                    val h = Horario(p, d, entrada, salida, cCoche.isSelected)
                    if (datosRuedaFX.horarios.contains(h)) {
                        mensaje = "Entrada duplicada: ya existen una entrada para el día y el participante indicados. Si lo desea puede modificarla a través de la tabla."
                    } else {
                        datosRuedaFX.horarios.add(h)
                    }
                }
            }
        } catch (_: NumberFormatException) {
            mensaje = "Entrada y salida deben ser números enteros que indiquen, en forma de cardinal, la hora de entrada/salida, ej. primera hora = 1"
        } catch (e: Exception) {
            mensaje = "Imposible añadir la entrada: " + e.localizedMessage
        }

        if (mensaje != null) {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Entrada no añadida al horario"
            alert.headerText = "No se ha podido añadir la entrada al horario"
            alert.contentText = mensaje
            alert.showAndWait()
        }
    }

    @FXML
    fun handleDelete() {
        val selectedIndex = tablaHorario.selectionModel.selectedIndex
        if (selectedIndex >= 0) {
            tablaHorario.items.removeAt(selectedIndex)
        } else {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nada seleccionado"
            alert.headerText = "No ha seleccionado ninguna entrada en la tabla de horarios"
            alert.contentText = "Por favor, seleccione una entrada del horario para eliminarla"
            alert.showAndWait()
        }
    }

    @FXML
    fun handleAddDia() {
        val descripcion = tfDescripcionDia.text
        if (descripcion.isEmpty() || compruebaDia(descripcion)) {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nombre o descripción inválido"
            alert.headerText = "El nombre o descripción del día no es válido o ya está usado"
            alert.contentText = "Por favor, introduzca un nombre o descripción de día que no este en uso"
            alert.showAndWait()
            return
        }
        datosRuedaFX.dias.add(Dia(descripcion, (datosRuedaFX.dias.maxOfOrNull { it.orden } ?: 0) + 1))
        tfDescripcionDia.clear()
    }

    private fun compruebaDia(nombreDia: String): Boolean = datosRuedaFX.dias.any {
        it.descripcion.equals(nombreDia, ignoreCase = true)
    }

    @FXML
    fun handleAddDiasSemana() {
        val dias = DateFormatSymbols.getInstance().weekdays
        for (i in intArrayOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY)) {
            var diaProp = dias[i]
            var intento = 1
            while (compruebaDia(diaProp)) {
                diaProp = dias[i] + ++intento
            }
            datosRuedaFX.dias.add(Dia(diaProp, (datosRuedaFX.dias.maxOfOrNull { it.orden } ?: 0) + 1))
        }
    }

    @FXML
    fun handleDeleteDia() {
        val selectedIndex = tablaDias.selectionModel.selectedIndex

        if (selectedIndex >= 0) {
            val ds = tablaDias.items[selectedIndex]
            if (datosRuedaFX.horarios.map { it.dia }.any { it == ds }) {
                val alert = Alert(AlertType.CONFIRMATION)
                alert.initOwner(stage)
                alert.title = "Dia en uso"
                alert.headerText = "El día seleccionado está siendo usado"
                alert.contentText = "El día seleccionado está en uso, si continua se eliminarán todas las entradas del horario que lo usen. ¿Desea continuar?"
                val result = alert.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    val aBorrar = datosRuedaFX.horarios.filter { h -> h.dia == ds }
                    datosRuedaFX.horarios.removeAll(aBorrar)
                } else {
                    return
                }
            }
            tablaDias.items.removeAt(selectedIndex)
        } else {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nada seleccionado"
            alert.headerText = "No ha seleccionado Día de la tabla"
            alert.contentText = "Por favor, seleccione un Día para eliminarlo"
            alert.showAndWait()
        }
    }

    @FXML
    fun handleAddLugar() {
        val nombre = tfNombreLugar.text
        if (nombre.isEmpty() || datosRuedaFX.lugares.map { it.nombre }.any { it.equals(nombre, ignoreCase = true) }) {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nombre de Lugar inválido"
            alert.headerText = "El nombre del Lugar no es válido o ya está usado"
            alert.contentText = "Por favor, introduzca un nombre de Lugar que no este en uso"
            alert.showAndWait()
            return
        }
        datosRuedaFX.lugares.add(Lugar(tfNombreLugar.text))
        tfNombreLugar.clear()
    }

    @FXML
    fun handleDeleteLugar() {
        val selectedIndex = tablaLugares.selectionModel.selectedIndex

        if (selectedIndex >= 0) {
            val ds = tablaLugares.items[selectedIndex]
            if (datosRuedaFX.participantes.map { it.puntosEncuentro }.flatten().any { it == ds }) {
                val alert = Alert(AlertType.CONFIRMATION)
                alert.initOwner(stage)
                alert.title = "Lugar en uso"
                alert.headerText = "El Lugar seleccionado está en uso"
                alert.contentText = "El Lugar seleccionado está en uso como punto de encuentro de algún participante. Si continua se eliminarán todos los puntos de encuentro que lo usen. ¿Desea continuar?"
                val result = alert.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    datosRuedaFX.participantes.forEach { p: Participante -> p.puntosEncuentro.remove(ds) }
                } else {
                    return
                }
            }
            tablaLugares.items.removeAt(selectedIndex)
        } else {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nada seleccionado"
            alert.headerText = "No ha seleccionado ninguno Lugar"
            alert.contentText = "Por favor, seleccione un Lugar para eliminarlo"
            alert.showAndWait()
        }
    }

    @FXML
    fun handleAddParticipante() {
        val nombre = tfNombreParticipante.text
        if (nombre.isEmpty() || datosRuedaFX.participantes.map { it.nombre }.any {
            it.equals(nombre, ignoreCase = true)
        }) {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nombre de Participante inválido"
            alert.headerText = "El nombre del Participante no es válido o ya está en suo"
            alert.contentText = "Por favor, introduzca un nombre de Participante que no esté en uso"
            alert.showAndWait()
            return
        }
        datosRuedaFX.participantes.add(Participante(nombre, sPlazas.value, emptyList()))
        tfNombreParticipante.clear()
    }

    @FXML
    fun handleDeleteParticipante() {
        val selectedIndex = tablaParticipantes.selectionModel.selectedIndex

        if (selectedIndex >= 0) {
            val ps = tablaParticipantes.items[selectedIndex]
            if (datosRuedaFX.horarios.map { it.participante }.any { it == ps }) {
                val alert = Alert(AlertType.CONFIRMATION)
                alert.initOwner(stage)
                alert.title = "Participante con horario asignado"
                alert.headerText = "El Participante seleccionado tiene entradas en el horario"
                alert.contentText = "El Participante seleccionado tiene datos en el horario. Si continua se eliminarán todas las entradas del horario que lo referencien. ¿Desea continuar?"
                val result = alert.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    val aBorrar = datosRuedaFX.horarios.filter { h -> h.participante == ps }
                    datosRuedaFX.horarios.removeAll(aBorrar)
                } else {
                    return
                }
            }
            tablaParticipantes.items.removeAt(selectedIndex)
        } else {
            val alert = Alert(AlertType.WARNING)
            alert.initOwner(stage)
            alert.title = "Nada seleccionado"
            alert.headerText = "No ha seleccionado ninguno Participante"
            alert.contentText = "Por favor, seleccione un Participante para eliminarlo"
            alert.showAndWait()
        }
    }

    @FXML
    fun handleAddLugarEncuentro() {
        if (!lvLugaresEncuentro.items.contains(cbLugares.value)) {
            lvLugaresEncuentro.items.add(cbLugares.value)
        }
    }

    @FXML
    fun handleDeleteLugarEncuentro() {
        lvLugaresEncuentro.items.remove(lvLugaresEncuentro.selectionModel.selectedItem)
    }

    @FXML
    fun handleUpLugarEncuentro() {
        val pos = lvLugaresEncuentro.selectionModel.selectedIndex
        if (pos > 0) {
            val l = lvLugaresEncuentro.selectionModel.selectedItem
            lvLugaresEncuentro.items.remove(l)
            lvLugaresEncuentro.items.add(pos - 1, l)
            lvLugaresEncuentro.selectionModel.select(l)
        }
    }

    @FXML
    fun handleDownLugarEncuentro() {
        val pos = lvLugaresEncuentro.selectionModel.selectedIndex
        if (pos < lvLugaresEncuentro.items.size - 1) {
            val l = lvLugaresEncuentro.selectionModel.selectedItem
            lvLugaresEncuentro.items.remove(l)
            lvLugaresEncuentro.items.add(pos + 1, l)
            lvLugaresEncuentro.selectionModel.select(l)
        }
    }

    fun setDatosRueda(datosRueda: DatosRueda) {
        this.datosRuedaFX.reemplazar(datosRueda)
    }

    private class ResolutorService : Service<Map<Dia, AsignacionDia>>() {

        private val resolutor = SimpleObjectProperty<Resolutor>()
        private val horarios = SimpleSetProperty<Horario>()

        fun getResolutor(): Resolutor {
            return resolutor.get()
        }

        fun setResolutor(value: Resolutor) {
            resolutor.set(value)
        }

        @Suppress("unused")
        fun resolutorProperty(): ObjectProperty<Resolutor> = resolutor

        fun getHorarios(): Set<Horario> {
            return horarios.get()
        }

        fun setHorarios(value: Set<Horario>) {
            horarios.set(FXCollections.observableSet(value))
        }

        @Suppress("unused")
        fun horariosProperty(): SetProperty<Horario> = horarios

        override fun createTask(): Task<Map<Dia, AsignacionDia>> {
            return object : Task<Map<Dia, AsignacionDia>>() {
                @Throws(Exception::class)
                override fun call(): Map<Dia, AsignacionDia> {
                    updateProgress(0, 1)
                    updateMessage("Iniciando la resolución...")
                    getResolutor().estadisticas.progresoProperty().addListener { _: ObservableValue<out Number>, _: Number, newValue: Number ->
                        updateProgress(newValue.toDouble(), 1.0)
                        updateMessage(getResolutor().estadisticas.toString())
                    }

                    return getResolutor().resolver(getHorarios())
                }
            }
        }

    }

    companion object {

        private val RESOLUTORES = arrayOf("es.um.josecefe.rueda.resolutor.ResolutorBalanceado",
                "es.um.josecefe.rueda.resolutor.ResolutorExhaustivo",
                "es.um.josecefe.rueda.resolutor.ResolutorSimple",
                "es.um.josecefe.rueda.resolutor.ResolutorV7",
                "es.um.josecefe.rueda.resolutor.ResolutorV8",
                "es.um.josecefe.rueda.resolutor.ResolutorGA",
                "es.um.josecefe.rueda.resolutor.ResolutorCombinado",
                "es.um.josecefe.rueda.resolutor.ResolutorIterativo")
    }
}
