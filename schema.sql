CREATE TABLE lugar (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nombre TEXT NOT NULL,
    latitud REAL,
    longitud REAL,
    direccion TEXT,
    poblacion TEXT,
    cp TEXT
);
CREATE TABLE participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nombre TEXT NOT NULL,
    plazasCoche INTEGER NOT NULL, 
    residencia INTEGER REFERENCES lugar(id)
);
CREATE TABLE dia (
    id INTEGER PRIMARY KEY NOT NULL,
    descripcion TEXT
);
CREATE TABLE punto_encuentro (
    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,
    lugar INTEGER NOT NULL  REFERENCES lugar(id)  ON DELETE CASCADE,
    orden INTEGER NOT NULL,
    PRIMARY KEY (participante, lugar, orden)
);
CREATE TABLE horario (
    participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE,
    dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE,
    entrada INTEGER NOT NULL,
    salida INTEGER NOT NULL,
    coche INTEGER NOT NULL DEFAULT (1),
    PRIMARY KEY (participante,dia)
);
CREATE TABLE asignacion ( 
 dia INTEGER NOT NULL REFERENCES dia(id) ON DELETE CASCADE, 
 participante INTEGER NOT NULL REFERENCES participante(id) ON DELETE CASCADE, 
 punto_encuentro_ida INTEGER, punto_encuentro_vuelta INTEGER, conduce INTEGER, 
 PRIMARY KEY (dia, participante), 
 FOREIGN KEY (participante, punto_encuentro_ida) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL,
 FOREIGN KEY (participante, punto_encuentro_vuelta) REFERENCES punto_encuentro(participante, lugar) ON DELETE SET NULL
);
