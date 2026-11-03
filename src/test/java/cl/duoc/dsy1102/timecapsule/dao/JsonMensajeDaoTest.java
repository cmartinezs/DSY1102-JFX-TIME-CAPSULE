package cl.duoc.dsy1102.timecapsule.dao;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonMensajeDaoTest {
    @TempDir Path tempDir;

    @Test
    void guardaYLeeMensajes() throws Exception {
        Path archivo = tempDir.resolve("data/mensajes.json");
        MensajeDao dao = new JsonMensajeDao(archivo);

        assertTrue(dao.listar().isEmpty());
        dao.guardar(List.of(new MensajeFuturo("Vale", "Hola futuro", 5)));

        assertTrue(Files.exists(archivo));
        var recuperados = new JsonMensajeDao(archivo).listar();
        assertEquals(1, recuperados.size());
        assertEquals("Vale", recuperados.get(0).getAutor());
        assertEquals("Hola futuro", recuperados.get(0).getTexto());
        assertEquals(5, recuperados.get(0).getPlazoAnios());
    }

    @Test
    void archivoDañadoLanzaErrorYNoSeBorra() throws Exception {
        Path archivo = tempDir.resolve("mensajes.json");
        Files.writeString(archivo, "{mal-json");
        MensajeDao dao = new JsonMensajeDao(archivo);

        assertThrows(java.io.IOException.class, dao::listar);
        assertEquals("{mal-json", Files.readString(archivo));
    }
}
