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
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Clase ResolutorCombinado. Es un resolutor que se basa en dos, uno primero
 * tipicamente de tipo no exhaustivo que da una solución buena pero normalmente
 * no optima y un segundo de tipo exhaustivo, que tomando como base la solución
 * del primero para realizar descartes en el arbol de soluciones, intenta dar una
 * solución óptima en un tiempo de ejecución mejor que si se ejecutarse a ciegas
 * 
 * @author josec
 */
public class ResolutorCombinado extends Resolutor {
    private Resolutor primero;
    private ResolutorAcotado segundo;
    private final EstadisticasCombinado estadisticas;
    private Map<Dia, ? extends AsignacionDia> solucion = Collections.emptyMap();

    /**
     * Crea un Resolutor combinando un ResolutorGA como resolutor no exhaustivo
     * y un ResolutorV8 como resolutor acotado que tomará como cota superior el
     * resultado del primero.
     */
    public ResolutorCombinado() {
        this.primero = new ResolutorGA();
        this.segundo = new ResolutorV8();
        estadisticas = new EstadisticasCombinado(primero.getEstadisticas(), segundo.getEstadisticas());
    }

    /**
     * Crea una nueva instancia de la clase, tomando como resolutores a los 
     * dos que se le pasan. El primero debe ser un Resolutor de rápida ejecución,
     * tipicamente no exhaustivo (algoritmo evolutivo o voraz) y un segundo
     * resolutor de tipo exhustivo que tomará la solución del primero como base
     * para realizar una busqueda guiada.
     * 
     * @param primero Resolutor no exhaustivo de rápida ejecución
     * @param segundo Resolutor actotado, generalmente exhaustivo
     */
    public ResolutorCombinado(Resolutor primero, ResolutorAcotado segundo) {
        this.primero = primero;
        this.segundo = segundo;
        estadisticas = new EstadisticasCombinado(primero.getEstadisticas(), segundo.getEstadisticas());
    }

    public Resolutor getPrimero() {
        return primero;
    }

    public void setPrimero(Resolutor primero) {
        this.primero = primero;
        estadisticas.setPrimera(primero.getEstadisticas());
    }

    public ResolutorAcotado getSegundo() {
        return segundo;
    }

    public void setSegundo(ResolutorAcotado segundo) {
        this.segundo = segundo;
        estadisticas.setSegunda(segundo.getEstadisticas());
    }
    
    /**
     * Resuelve el problema mediante la aplicación sucesiva del resolutor
     * primero y segundo, tomando el segundo como cota superior del fitness de
     * la solución el resultado del primero.
     * 
     * @param horarios Datos de entrada
     * @return null o Collections.emptyMap() si no hay solución, una solución válida en otro caso
     */
    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        estadisticas.inicia();
        solucion = primero.resolver(horarios);
        final Estadisticas estadisticas1 = primero.getEstadisticas();
        estadisticas.setFitness(estadisticas1.getFitness());
        estadisticas.actualizaProgreso();
        Map<Dia, ? extends AsignacionDia> solucion2 = segundo.resolver(horarios, estadisticas1.getFitness());
        Estadisticas estadisticas2 = segundo.getEstadisticas();
        if (solucion2!=null && !solucion2.isEmpty() && estadisticas2.getFitness() < estadisticas1.getFitness()) {
            solucion = solucion2;
            estadisticas.setFitness(estadisticas2.getFitness());
        }
        estadisticas.actualizaProgreso();
        return solucion;
    }

    @Override
    public void parar() {
        primero.parar();
        segundo.parar();
    }
    
    @Override
    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucion;
    }    
}
