/**
 *
 */
package es.um.josecefe.rueda;

import java.util.Objects;

/**
 * SQL: CREATE TABLE asignacion ( "dia" INTEGER NOT NULL REFERENCES dia(id) ON
 * DELETE CASCADE, "participante" INTEGER NOT NULL REFERENCES participante(id)
 * ON DELETE CASCADE, "punto_encuentro_ida" INTEGER, "punto_encuentro_vuelta" INTEGER, "conduce" INTEGER, PRIMARY KEY
 * (dia, participante), FOREIGN KEY (participante, punto_encuentro_ida) 
 * REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL ,
 * FOREIGN KEY (participante, punto_encuentro_vuelta) REFERENCES
 * punto_encuentro(participante, lugar) ON DELETE SET NULL )
 *
 * @author josec
 *
 */
public class Asignacion {

    private final Dia dia;
    private final Participante participante;
    private final Lugar puntoEncuentroIda;
    private final Lugar puntoEncuentroVuelta;
    private final boolean conduce;

    /**
     * Crea una asignación del resultado final de la optimización de la rueda
     * 
     * @param dia
     * @param participante
     * @param puntoEncuentroIda
     * @param puntoEncuentroVuelta
     * @param conduce
     */
    public Asignacion(Dia dia, Participante participante, Lugar puntoEncuentroIda, Lugar puntoEncuentroVuelta, boolean conduce) {
        this.dia = dia;
        this.participante = participante;
        this.puntoEncuentroIda = puntoEncuentroIda;
        this.puntoEncuentroVuelta = puntoEncuentroVuelta;
        this.conduce = conduce;
    }

    /**
     * @return the dia
     */
    public Dia getDia() {
        return dia;
    }

    /**
     * @return the participante
     */
    public Participante getParticipante() {
        return participante;
    }

    /**
     * @return the puntoEncuentroIda
     */
    public Lugar getPuntoEncuentroIda() {
        return puntoEncuentroIda;
    }
    
    /**
     * @return the puntoEncuentroVuelta
     */
    public Lugar getPuntoEncuentroVuelta() {
        return puntoEncuentroVuelta;
    }

    public boolean isConduce() {
        return conduce;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.dia);
        hash = 31 * hash + Objects.hashCode(this.participante);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Asignacion other = (Asignacion) obj;
        if (!Objects.equals(this.dia, other.dia)) {
            return false;
        }
        return Objects.equals(this.participante, other.participante);
    }

    @Override
    public String toString() {
        return "Asignacion{" + "dia=" + dia + ", participante=" + participante + ", puntoEncuentro=" + puntoEncuentroIda + ", conduce=" + conduce + '}';
    }

}
