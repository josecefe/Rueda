/**
 *
 */
package es.um.josecefe.rueda.persistencia;

import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.Lugar;
import es.um.josecefe.rueda.modelo.Horario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author josec
 *
 */
public class PersistenciaSQL {

    private static final int VERSION = 5;
    private static final String FECHA_VERSION = "2016-07-14";

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
                    System.out.println("Leido " + l);
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
                    System.out.println("Leido " + p);
                    if (rsp.getInt("lugar") > 0) {
                        p.getPuntosEncuentro().add(lugares.get(rsp.getInt("lugar")));
                        System.out.println("--> " + lugares.get(rsp.getInt("lugar")));
                    }

                }
            }

            // Leemos los dias
            try (ResultSet rsd = statement.executeQuery("select * from dia")) {
                while (rsd.next()) {
                    Dia d = new Dia(rsd.getInt("id"), rsd.getString("descripcion"));
                    System.out.println("Leido " + d);
                    dias.put(d.getId(), d);
                }
            }

            // Leemos los horarios
            try (ResultSet rsh = statement.executeQuery("select * from horario")) {
                while (rsh.next()) {
                    // read the result set
                    Horario horario = new Horario(participantes.get(rsh.getInt("participante")), dias.get(rsh.getInt("dia")),
                            rsh.getInt("entrada"), rsh.getInt("salida"), rsh.getBoolean("coche"));
                    System.out.println("Leido " + horario);
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
        System.out.println("Vamos a crear una BD nueva para la rueda: " + db);
        Connection connection = null;
        try {
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            // Tabla lugar:
            statement.execute("CREATE TABLE lugar (\n"
                    + "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                    + "    nombre TEXT NOT NULL,\n"
                    + "    latitud REAL,\n"
                    + "    longitud REAL,\n"
                    + "    direccion TEXT,\n"
                    + "    poblacion TEXT,\n"
                    + "    cp TEXT\n"
                    + ")");
            statement.execute("CREATE TABLE participante (\n"
                    + "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                    + "    nombre TEXT NOT NULL,\n"
                    + "    plazasCoche INTEGER NOT NULL, \n"
                    + "    residencia INTEGER REFERENCES lugar(id)\n"
                    + ")");
            statement.execute("CREATE TABLE dia (\n"
                    + "    id INTEGER PRIMARY KEY NOT NULL,\n"
                    + "    descripcion TEXT\n"
                    + ")");
            statement.execute("CREATE TABLE punto_encuentro (\n"
                    + "    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,\n"
                    + "    lugar INTEGER NOT NULL  REFERENCES lugar(id)  ON DELETE CASCADE,\n"
                    + "    orden INTEGER NOT NULL,\n"
                    + "    PRIMARY KEY (participante, lugar, orden)\n"
                    + ")");
            statement.execute("CREATE TABLE horario (\n"
                    + "    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,\n"
                    + "    dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE,\n"
                    + "    entrada INTEGER NOT NULL,\n"
                    + "    salida INTEGER NOT NULL,\n"
                    + "    coche INTEGER NOT NULL DEFAULT (1),\n"
                    + "    PRIMARY KEY (participante,dia)\n"
                    + ")");
            statement.execute("CREATE TABLE asignacion ( \n"
                    + " dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE, \n"
                    + " participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE, \n"
                    + " punto_encuentro_ida INTEGER, punto_encuentro_vuelta INTEGER, conduce INTEGER, \n"
                    + " PRIMARY KEY (dia, participante), \n"
                    + " FOREIGN KEY (participante, punto_encuentro_ida) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL,\n"
                    + " FOREIGN KEY (participante, punto_encuentro_vuelta) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL\n"
                    + ")");
            statement.execute("CREATE TABLE version (version INTEGER PRIMARY KEY, fecha DATE)");
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
        System.out.println("Vamos a guardar en la bbdd estos valores: " + resultadoRueda);
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
            /*
            resultadoRueda.entrySet().stream().forEach((da) -> {
                da.getValue().getPeIda().keySet().stream().map((p) -> {
                    valores.append('(');
                    valores.append(da.getKey().getId());
                    valores.append(",");
                    valores.append(p.getId());
                    return p;
                }).map((p) -> {
                    valores.append(",");
                    valores.append(da.getValue().getPeIda().get(p).getId());
                    return p;
                }).map((p) -> {
                    valores.append(",");
                    valores.append(da.getValue().getPeVuelta().get(p).getId());
                    return p;
                }).map((p) -> {
                    valores.append(",");
                    valores.append(da.getValue().getConductores().contains(p) ? "1" : "0");
                    return p;
                }).forEach((_item) -> {
                    valores.append("),");
                });
            });
             */
            valores.deleteCharAt(valores.length() - 1); //Quitamos la última coma;
            System.out.println(valores.toString());
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
