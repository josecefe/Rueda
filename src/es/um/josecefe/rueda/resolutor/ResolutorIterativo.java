/*
 * Copyright (C) 2016 Jose
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
import es.um.josecefe.rueda.modelo.Participante;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author Jose
 */
public class ResolutorIterativo extends ResolutorAcotado {

    private final ResolutorAcotado resolutor;
    private Map<Dia, ? extends AsignacionDia> solucion;

    public ResolutorIterativo() {
        resolutor = Runtime.getRuntime().availableProcessors() > 4 ? new ResolutorV8() : new ResolutorV7();
    }

    public ResolutorIterativo(ResolutorAcotado resolutor) {
        this.resolutor = resolutor;
    }

    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, int cotaCorteTope, int cotaCorteBase) {
        solucion = null;
        int corte = cotaCorteBase ; // Punto de partida
        for (; (solucion==null || solucion.isEmpty()) && corte < cotaCorteTope; corte += Pesos.PESO_MAXIMO_VECES_CONDUCTOR) { // Valor inicial
            solucion = resolutor.resolver(horarios, corte);
        }
        if ((solucion==null || solucion.isEmpty())) {
            solucion = resolutor.resolver(horarios, cotaCorteTope);
        }
        return solucion;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, int cotaCorteTope) {
        long numDias = horarios.stream().map(Horario::getDia).distinct().count();
        long numParticipantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().count();
        Participante[] conductores = horarios.stream().filter(Horario::isCoche).map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);
        double tamMedioCoche = Stream.of(conductores).mapToInt(Participante::getPlazasCoche).average().orElse(1);
        int cotaCorteBase = (int) (((numParticipantes / tamMedioCoche) * numDias / conductores.length) +  1) * Pesos.PESO_MAXIMO_VECES_CONDUCTOR - 1;
        return resolver(horarios, cotaCorteTope, cotaCorteBase);
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        return resolver(horarios, (int) (horarios.stream().map(Horario::getDia).distinct().count() + 1) * Pesos.PESO_MAXIMO_VECES_CONDUCTOR - 1);
    }

    @Override
    public Estadisticas getEstadisticas() {
        return resolutor.getEstadisticas();
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucion;
    }

    @Override
    public void setEstrategia(Estrategia estrategia) {
        resolutor.setEstrategia(estrategia);
    }

    @Override
    public void parar() {
        super.parar();
        resolutor.parar();
    }
}
