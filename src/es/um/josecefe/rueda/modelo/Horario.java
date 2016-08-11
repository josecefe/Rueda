/**
 *
 */
package es.um.josecefe.rueda.modelo;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * SQL: CREATE TABLE "horario" ( "participante" INTEGER NOT NULL REFERENCES
 * participante(id) ON DELETE CASCADE, "dia" INTEGER NOT NULL REFERENCES dia(id)
 * ON DELETE CASCADE, "entrada" INTEGER NOT NULL, "salida" INTEGER NOT NULL,
 * "coche" INTEGER NOT NULL DEFAULT (1), PRIMARY KEY (participante,dia) );
 *
 * @author josec
 *
 */
@XmlRootElement(name = "horario")
public class Horario implements Comparable<Horario> {

    private Participante participante;
    private Dia dia;
    private int entrada;
    private int salida;
    private boolean coche;

    /** 
     * Constructor por defecto para cumplir el estandar de JavaBean. No debería
     * usarse directametne, solo debe usarse para la persistencia.
     */
    public Horario() {
    }

    /**
     * Crea una porción de horario que corresponde un participante y un día
     * dados
     *
     * @param participante Participante al que pertenece esta parte del horario
     * @param dia Día al que se refiere esta parte
     * @param entrada Hora de entrada del participante ese dia
     * @param salida Hora de salida del participante ese dia
     * @param coche Indica si ese día podría compartir su coche
     */
    public Horario(Participante participante, Dia dia, int entrada, int salida, boolean coche) {
        this.participante = participante;
        this.dia = dia;
        this.entrada = entrada;
        this.salida = salida;
        this.coche = coche;
    }

    /**
     * @return el participante
     */
    public Participante getParticipante() {
        return participante;
    }

    /**
     * @return el dia
     */
    public Dia getDia() {
        return dia;
    }

    /**
     * @return la hora de entrada
     */
    public int getEntrada() {
        return entrada;
    }

    /**
     * @return la hora de salida
     */
    public int getSalida() {
        return salida;
    }

    /**
     * @return the coche
     */
    public boolean isCoche() {
        return coche;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Horario [participante=" + participante + ", dia=" + dia + ", entrada=" + entrada + ", salida=" + salida
                + ", coche=" + coche + "]";
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dia == null) ? 0 : dia.hashCode());
        result = prime * result + ((participante == null) ? 0 : participante.hashCode());
        return result;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Horario other = (Horario) obj;
        if (dia == null) {
            if (other.dia != null) {
                return false;
            }
        } else if (!dia.equals(other.dia)) {
            return false;
        }
        if (participante == null) {
            if (other.participante != null) {
                return false;
            }
        } else if (!participante.equals(other.participante)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
	 * @see java.lang.comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Horario o) {
        int res = dia.compareTo(o.getDia());
        if (res == 0) {
            res = participante.compareTo(o.getParticipante());
        }

        return res;
    }

    /**
     * Permite fijar el participante de esta entrada en el horario
     * Nota: Este método se incluye para la persistencia
     * @param participante Participante al que pertenece esta entrada
     */
    public void setParticipante(Participante participante) {
        this.participante = participante;
    }

    /**
     * Permite fija el dia de esta entrada de horario. Perticipante + Dia no se pueden repetir
     * Nota: Este método se incluye para la persistencia
     * @param dia El día al que pertenece esta entrada
     */
    public void setDia(Dia dia) {
        this.dia = dia;
    }

    /**
     * Permite fijar la hora de entrada (entendida como un entero, usualmente empezando por 1 para primera hora, 2 para la segunda, etc.)
     * Nota: Este método se incluye para la persistencia
     * @param entrada Hora de entrada (como un entero referido a 1º hora = 1, 2º hora = 2, etc)
     */
    public void setEntrada(int entrada) {
        this.entrada = entrada;
    }

    /**
     * Permite fijar la hora de salida (entendida como un entero, usualmente empezando por 1 para primera hora, 2 para la segunda, etc.)
     * Nota: Este método se incluye para la persistencia
     * @param salida Hora de salida (como un entero referido a 1º hora = 1, 2º hora = 2, etc)
     */
    public void setSalida(int salida) {
        this.salida = salida;
    }

    /**
     * Indica si el participante dispone de coche para ese día
     * @param coche Indica si puede disponer de coche o no ese día
     */
    public void setCoche(boolean coche) {
        this.coche = coche;
    }

}
