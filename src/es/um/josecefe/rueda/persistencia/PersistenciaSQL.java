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
package es.um.josecefe.rueda.persistencia;

import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Horario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author josec
 *
 */
public class PersistenciaSQL {

    private static final boolean DEBUG = true;

    // Database version and date
    private static final int VERSION = 5;
    private static final String FECHA_VERSION = "2016-07-14";

    public static void guardaHorarios(String db, Set<Horario> horarios) {
        Dia[] dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        Participante[] participantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);
        Lugar[] lugares = Stream.of(participantes).map(Participante::getPuntosEncuentro).flatMap(List::stream).distinct().toArray(Lugar[]::new);

        Connection connection = null;
        try {
            creaBD(db); // Creamos la base de datos si no existe

            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            // Borramos todos los datos
            statement.executeUpdate("delete from asignacion");
            statement.executeUpdate("delete from horario");
            statement.executeUpdate("delete from punto_encuentro");
            statement.executeUpdate("delete from dia");
            statement.executeUpdate("delete from participante");
            statement.executeUpdate("delete from lugar");

            // Primero guardamos los lugares
            PreparedStatement insLugar = connection.prepareStatement("insert into lugar(nombre,latitud,longitud,direccion,poblacion,cp) values (?, ?, ?, ?, ?,?)", Statement.RETURN_GENERATED_KEYS);

            for (Lugar l : lugares) {
                //insLugar.setInt("id", l.getId());
                insLugar.setString(1, l.getNombre());
                insLugar.setDouble(2, l.getLatitud());
                insLugar.setDouble(3, l.getLongitud());
                insLugar.setString(4, l.getDireccion());
                insLugar.setString(5, l.getPoblacion());
                insLugar.setString(6, l.getCp());
                if (insLugar.executeUpdate()==1) {
                    ResultSet rsl = insLugar.getGeneratedKeys();
                    l.setId(rsl.getInt(1));
                    rsl.close();
                } else {
                    System.err.println("Fallo la inserción del lugar "+l);
                }
            }
            insLugar.close();

            // Luego los participantes con sus puntos de encuentro
            PreparedStatement insPart = connection.prepareStatement("insert into participante(nombre,plazasCoche,residencia) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement insPuntos = connection.prepareStatement("insert into punto_encuentro(participante,lugar,orden) values (?, ?, ?)", Statement.NO_GENERATED_KEYS);

            for (Participante p : participantes) {
                insPart.setString(1, p.getNombre());
                insPart.setDouble(2, p.getPlazasCoche());
                if (p.getResidencia() != null) {
                    insPart.setInt(3, p.getResidencia().getId());
                } else {
                    insPart.setNull(3, java.sql.Types.INTEGER);
                }
                if (insPart.executeUpdate()==1) {
                    ResultSet rsl = insPart.getGeneratedKeys();
                    p.setId(rsl.getInt(1));
                    rsl.close();
                } else {
                    System.err.println("Falla la insercion del participante " + p);
                }
                for (int i = 0; i < p.getPuntosEncuentro().size(); ++i) {
                    insPuntos.setInt(1, p.getId());
                    insPuntos.setInt(2, p.getPuntosEncuentro().get(i).getId());
                    insPuntos.setInt(3, i+1); //El primero será 1 no 0
                    if (insPuntos.executeUpdate() != 1) {
                        System.err.println("Falla la insercion del punto de encuentro " + p.getPuntosEncuentro().get(i));
                    }
                }
            }
            insPuntos.close();
            insPart.close();

            // Luego los dias
            PreparedStatement insDia = connection.prepareStatement("insert into dia(descripcion) values (?)", Statement.RETURN_GENERATED_KEYS);
            for (Dia d : dias) {
                insDia.setString(1, d.getDescripcion());
                if (insDia.executeUpdate() == 1) {
                    ResultSet rsl = insDia.getGeneratedKeys();
                    d.setId(rsl.getInt(1));
                    rsl.close();
                } else {
                    System.err.println("Falla la insercion del dia " + d);
                }
            }
            insDia.close();

            // Y por ultimo los horarios
            PreparedStatement insHorario = connection.prepareStatement("insert into horario(participante,dia,entrada,salida,coche) values (?, ?, ?, ?, ?)", Statement.NO_GENERATED_KEYS);

            for (Horario h : horarios) {
                insHorario.setInt(1, h.getParticipante().getId());
                insHorario.setInt(2, h.getDia().getId());
                insHorario.setInt(3, h.getEntrada());
                insHorario.setInt(4, h.getSalida());
                insHorario.setBoolean(5, h.isCoche());
                if (insHorario.executeUpdate() != 1) {
                    System.err.println("Falla la insercion del horario " + h);
                }
            }
            insHorario.close();
            connection.commit();

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Problema con los drivers, compruebe que el archivo .jar del sqlite-jdbc esta en su sitio");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
                e.printStackTrace();
            }
        }

    }

    public static Set<Horario> cargaHorarios(String db) {
        Set<Horario> horarios = new HashSet<>();
        Map<Integer, Participante> participantes = new HashMap<>();
        Map<Integer, Dia> dias = new HashMap<>();
        Map<Integer, Lugar> lugares = new HashMap<>();

        Connection connection = null;
        try {
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            // Leemos los lugares
            try (ResultSet rsl = statement.executeQuery("select * from lugar")) {
                while (rsl.next()) {
                    // read the result set
                    Lugar l = new Lugar(rsl.getInt("id"), rsl.getString("nombre"), rsl.getDouble("latitud"), rsl.getDouble("longitud"),
                            rsl.getString("direccion"), rsl.getString("poblacion"), rsl.getString("cp"));
                    if (DEBUG) {
                        System.out.println("Leido " + l);
                    }
                    lugares.put(l.getId(), l);
                }
            }

            // Leemos los participantes
            try (ResultSet rsp = statement.executeQuery("select * from participante left join punto_encuentro on id = participante order by orden")) {
                while (rsp.next()) {
                    final String nombre = rsp.getString("nombre");
                    final int plazasCoche = rsp.getInt("plazasCoche");
                    final Lugar residencia = lugares.get(rsp.getInt("residencia"));
                    Participante p = participantes.computeIfAbsent(rsp.getInt("id"), id
                            -> new Participante(id, nombre, plazasCoche, residencia, new ArrayList<>()));
                    if (DEBUG) {
                        System.out.println("Leido " + p);
                    }
                    if (rsp.getInt("lugar") > 0) {
                        p.getPuntosEncuentro().add(lugares.get(rsp.getInt("lugar")));
                        if (DEBUG) {
                            System.out.println("--> " + lugares.get(rsp.getInt("lugar")));
                        }
                    }

                }
            }

            // Leemos los dias
            try (ResultSet rsd = statement.executeQuery("select * from dia")) {
                while (rsd.next()) {
                    Dia d = new Dia(rsd.getInt("id"), rsd.getString("descripcion"));
                    if (DEBUG) {
                        System.out.println("Leido " + d);
                    }
                    dias.put(d.getId(), d);
                }
            }

            // Leemos los horarios
            try (ResultSet rsh = statement.executeQuery("select * from horario")) {
                while (rsh.next()) {
                    // read the result set
                    Horario horario = new Horario(participantes.get(rsh.getInt("participante")), dias.get(rsh.getInt("dia")),
                            rsh.getInt("entrada"), rsh.getInt("salida"), rsh.getBoolean("coche"));
                    if (DEBUG) {
                        System.out.println("Leido " + horario);
                    }
                    horarios.add(horario);
                }
            }

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Problema con los drivers, compruebe que el archivo .jar del sqlite-jdbc esta en su sitio");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
        return horarios;
    }

    public static void creaBD(String db) {
        if (DEBUG) {
            System.out.println("Vamos a crear una BD nueva para la rueda: " + db);
        }
        Connection connection = null;
        try {
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            // Tabla lugar:
            statement.execute("CREATE TABLE IF NOT EXISTS lugar (\n"
                    + "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                    + "    nombre TEXT NOT NULL,\n"
                    + "    latitud REAL,\n"
                    + "    longitud REAL,\n"
                    + "    direccion TEXT,\n"
                    + "    poblacion TEXT,\n"
                    + "    cp TEXT\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS participante (\n"
                    + "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                    + "    nombre TEXT NOT NULL,\n"
                    + "    plazasCoche INTEGER NOT NULL, \n"
                    + "    residencia INTEGER REFERENCES lugar(id)\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS dia (\n"
                    + "    id INTEGER PRIMARY KEY NOT NULL,\n"
                    + "    descripcion TEXT\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS punto_encuentro (\n"
                    + "    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,\n"
                    + "    lugar INTEGER NOT NULL  REFERENCES lugar(id)  ON DELETE CASCADE,\n"
                    + "    orden INTEGER NOT NULL,\n"
                    + "    PRIMARY KEY (participante, lugar, orden)\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS horario (\n"
                    + "    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,\n"
                    + "    dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE,\n"
                    + "    entrada INTEGER NOT NULL,\n"
                    + "    salida INTEGER NOT NULL,\n"
                    + "    coche INTEGER NOT NULL DEFAULT (1),\n"
                    + "    PRIMARY KEY (participante,dia)\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS asignacion ( \n"
                    + " dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE, \n"
                    + " participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE, \n"
                    + " punto_encuentro_ida INTEGER, punto_encuentro_vuelta INTEGER, conduce INTEGER, \n"
                    + " PRIMARY KEY (dia, participante), \n"
                    + " FOREIGN KEY (participante, punto_encuentro_ida) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL,\n"
                    + " FOREIGN KEY (participante, punto_encuentro_vuelta) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL\n"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS version (version INTEGER PRIMARY KEY, fecha DATE)");
            statement.executeUpdate("DELETE FROM version");
            statement.executeUpdate("INSERT INTO version(version,fecha) VALUES (" + VERSION + ",'" + FECHA_VERSION + "')");
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Problema con los drivers, compruebe que el archivo .jar del sqlite-jdbc esta en su sitio");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    public static void guardaAsignacionRueda(String db, Map<Dia, ? extends AsignacionDia> resultadoRueda) {
        if (DEBUG) {
            System.out.println("Vamos a guardar en la bbdd estos valores: " + resultadoRueda);
        }
        Connection connection = null;
        try {
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            statement.executeUpdate("delete from asignacion");
            StringBuilder valores = new StringBuilder("insert into asignacion(dia, participante, punto_encuentro_ida, punto_encuentro_vuelta, conduce) values ");
            for (Map.Entry<Dia, ? extends AsignacionDia> da : resultadoRueda.entrySet()) {
                for (Participante p : da.getValue().getPeIda().keySet()) {
                    valores.append('(');
                    valores.append(da.getKey().getId());
                    valores.append(",");
                    valores.append(p.getId());
                    valores.append(",");
                    valores.append(da.getValue().getPeIda().get(p).getId());
                    valores.append(",");
                    valores.append(da.getValue().getPeVuelta().get(p).getId());
                    valores.append(",");
                    valores.append(da.getValue().getConductores().contains(p) ? "1" : "0");
                    valores.append("),");
                }
            }
            valores.deleteCharAt(valores.length() - 1); //Quitamos la última coma;
            if (DEBUG) {
                System.out.println(valores.toString());
            }
            statement.executeUpdate(valores.toString());
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Problema con los drivers, compruebe que el archivo .jar del sqlite-jdbc esta en su sitio");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
}
