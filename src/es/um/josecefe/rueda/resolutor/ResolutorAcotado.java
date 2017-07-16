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
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;

import java.util.Map;
import java.util.Set;

/**
 * @author josecefe
 */
public abstract class ResolutorAcotado extends Resolutor {
    /**
     * Resuelve el problema de optimización de la rueda dado un horario de entrada sabiendo
     * que la solución dada tiene que tener un fitness menor o igual que el dado, descartando
     * las que lo tengan mayor.
     *
     * @param horarios     Entradas del horario
     * @param cotaInfCorte Valor de acotación del fitness de la solución (descartar soluciones con fitness mayor)
     * @return Resultado de la optimización
     */
    public abstract Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, int cotaInfCorte);

}
