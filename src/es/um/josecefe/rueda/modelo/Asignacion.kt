package es.um.josecefe.rueda.modelo

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javafx.beans.property.*
import javafx.collections.FXCollections

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@id")
class Asignacion(dia: Dia, conductores: List<Participante>, peIda: List<ParticipanteLugar>,
                 peVuelta: List<ParticipanteLugar>, coste: Int) : Comparable<Asignacion> {

    // Clase interna para reemplazar Pair
    data class ParticipanteLugar(
        val participante: Participante,
        val lugar: Lugar
    )

    private val diaProp: SimpleObjectProperty<Dia> = SimpleObjectProperty()
    private val conductoresProp: SimpleListProperty<Participante> = SimpleListProperty(FXCollections.observableArrayList())
    private val peidaProp: SimpleListProperty<ParticipanteLugar> = SimpleListProperty(FXCollections.observableArrayList())
    private val pevueltaProp: SimpleListProperty<ParticipanteLugar> = SimpleListProperty(FXCollections.observableArrayList())
    private val costeProp = SimpleIntegerProperty()

    init {
        diaProp.set(dia)
        conductoresProp.addAll(conductores)
        peidaProp.addAll(peIda)
        pevueltaProp.addAll(peVuelta)
        costeProp.set(coste)
    }

    constructor(dia: Dia, asignacionDia: AsignacionDia) : this(dia, asignacionDia.conductores.toList(),
            asignacionDia.peIda.entries.map { ParticipanteLugar(it.key, it.value) },
            asignacionDia.peVuelta.entries.map { ParticipanteLugar(it.key, it.value) }, asignacionDia.coste)

    var dia: Dia
        get() = diaProp.get()
        set(value) = diaProp.set(value)

    fun diaProperty(): ObjectProperty<Dia> = diaProp

    var conductores: List<Participante>
        get() = conductoresProp.get()
        set(value) {
            conductoresProp.clear()
            conductoresProp.addAll(value)
        }

    fun participantesProperty(): ListProperty<Participante> = conductoresProp

    var peIda: List<ParticipanteLugar>
        get() = peidaProp.get()
        set(value) {
            peidaProp.clear()
            peidaProp.addAll(value)
        }

    fun peIdaProperty(): ListProperty<ParticipanteLugar> = peidaProp

    var peVuelta: List<ParticipanteLugar>
        get() = pevueltaProp.get()
        set(value) {
            pevueltaProp.clear()
            pevueltaProp.addAll(value)
        }

    fun peVueltaProperty(): ListProperty<ParticipanteLugar> = pevueltaProp

    var coste: Int
        get() = costeProp.get()
        set(value) = costeProp.set(value)

    fun costeProperty(): IntegerProperty = costeProp

    override fun compareTo(other: Asignacion): Int {
        return dia.compareTo(other.dia)
    }
}