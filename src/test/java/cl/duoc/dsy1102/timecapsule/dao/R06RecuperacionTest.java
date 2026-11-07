package cl.duoc.dsy1102.timecapsule.dao;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import cl.duoc.dsy1102.timecapsule.service.MensajeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class R06RecuperacionTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void reiniciarRecuperaMensajesSinDuplicarlos() throws Exception {
        Path archivo = carpetaTemporal.resolve("data/mensajes.json");

        MensajeService primeraEjecucion = new MensajeService(new JsonMensajeDao(archivo));
        primeraEjecucion.registrar("Carlos", "Mensaje de prueba", 5);
        primeraEjecucion.registrar("Carlos", "Otro Mensaje", 5);

        // Simulamos un segundo arranque creando un servicio y DAO nuevos.
        MensajeService segundaEjecucion = new MensajeService(new JsonMensajeDao(archivo));

        assertEquals(2, segundaEjecucion.listar().size());
        assertEquals("Mensaje de prueba", segundaEjecucion.listar().get(0).getTexto());
        assertEquals("Otro Mensaje", segundaEjecucion.listar().get(1).getTexto());
    }

    @Test
    void segundoGuardadoConservaVersionAnteriorEnBak() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        JsonMensajeDao dao = new JsonMensajeDao(archivo);

        dao.guardar(List.of(new MensajeFuturo("Vale", "Primero", 1)));
        dao.guardar(List.of(
                new MensajeFuturo("Vale", "Primero", 1),
                new MensajeFuturo("Carlos", "Segundo", 5)
        ));

        Path respaldo = carpetaTemporal.resolve("mensajes.json.bak");
        assertTrue(Files.exists(respaldo));
        assertEquals(1, new JsonMensajeDao(respaldo).listar().size());
        assertEquals(2, new JsonMensajeDao(archivo).listar().size());
    }

    @Test
    void jsonCorruptoConservaOriginalYCopiaDeResguardo() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Files.writeString(archivo, "{contenido-danado");

        IOException error = assertThrows(IOException.class,
                () -> new JsonMensajeDao(archivo).listar());

        assertTrue(error.getMessage().contains("está dañado"));
        assertEquals("{contenido-danado", Files.readString(archivo));

        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            List<Path> copias = archivos
                    .filter(p -> p.getFileName().toString().startsWith("mensajes.json.corrupt-"))
                    .toList();

            assertEquals(1, copias.size());
            assertEquals("{contenido-danado", Files.readString(copias.get(0)));
        }
    }

    @Test
    void raizJsonNullSeRechazaYSeConserva() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Files.writeString(archivo, "null");

        assertThrows(IOException.class, () -> new JsonMensajeDao(archivo).listar());
        assertEquals("null", Files.readString(archivo));
    }

    @Test
    void estructuraInvalidaNoSeAceptaComoMensaje() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Files.writeString(archivo, "[{\"autor\":\"\",\"texto\":\"Hola\",\"plazoAnios\":5}]");

        assertThrows(IOException.class, () -> new JsonMensajeDao(archivo).listar());
        assertTrue(Files.exists(archivo));
    }

    @Test
    void faltaComaEntreObjetosDetectaCorrupcionSinBorrarOriginal() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        // Mismo tipo de error que se puede producir al editar el JSON a mano:
        // dos objetos consecutivos sin coma separadora.
        String contenido = """
                [
                  {"autor":"Carlos","texto":"Primero","plazoAnios":5}
                  {"autor":"Carlos","texto":"Segundo","plazoAnios":5}
                ]
                """;
        Files.writeString(archivo, contenido);

        IOException error = assertThrows(IOException.class,
                () -> new JsonMensajeDao(archivo).listar());

        assertTrue(error.getMessage().contains("está dañado"));
        assertEquals(contenido, Files.readString(archivo));

        try (Stream<Path> entradas = Files.list(carpetaTemporal)) {
            assertEquals(1, entradas.filter(p ->
                    p.getFileName().toString().startsWith("mensajes.json.corrupt-")
            ).count());
        }
    }

    @Test
    void guardarMientrasJavaFxSigueAbiertoNoReparaJsonCorrupto() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        MensajeService service = new MensajeService(new JsonMensajeDao(archivo));
        service.registrar("Carlos", "Primero", 5);
        service.registrar("Carlos", "Segundo", 5);

        Path backup = carpetaTemporal.resolve("mensajes.json.bak");
        String backupAnterior = Files.readString(backup);

        // Simula que el JSON se edita cuando el Service ya tiene datos en memoria.
        Files.writeString(archivo, "{json roto");

        IOException error = assertThrows(IOException.class,
                () -> service.registrar("Carlos", "Tercero", 5));

        assertTrue(error.getMessage().contains("está dañado"));
        assertEquals("{json roto", Files.readString(archivo));
        assertEquals(backupAnterior, Files.readString(backup));
        assertEquals(2, service.listar().size(), "No agrega el tercero en memoria");

        try (Stream<Path> entradas = Files.list(carpetaTemporal)) {
            assertEquals(1, entradas.filter(p ->
                    p.getFileName().toString().startsWith("mensajes.json.corrupt-")
            ).count(), "Debe conservar una copia del JSON dañado");
        }
    }

    @Test
    void guardarNoSobrescribeCambioExternoAunqueJsonSeaValido() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        MensajeService service = new MensajeService(new JsonMensajeDao(archivo));
        service.registrar("Carlos", "Original", 5);

        // Otro editor cambia el archivo a JSON sintácticamente válido.
        Files.writeString(archivo, "[]");

        IOException error = assertThrows(IOException.class,
                () -> service.registrar("Carlos", "No debe guardarse", 5));

        assertTrue(error.getMessage().contains("cambió mientras"));
        assertEquals("[]", Files.readString(archivo));
        assertEquals(1, service.listar().size());
    }

}
