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

    @Test
    void jsonCorruptoConBakValidoNoCargaRespaldoAutomaticamente() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Path respaldo = carpetaTemporal.resolve("mensajes.json.bak");

        // Simula la situación descrita: JSON dañado y .bak legible.
        Files.writeString(archivo, "{json roto");
        new JsonMensajeDao(respaldo).guardar(
                List.of(new MensajeFuturo("Carlos", "Desde respaldo", 5))
        );

        IOException error = assertThrows(IOException.class,
                () -> new MensajeService(new JsonMensajeDao(archivo)));

        assertTrue(error.getMessage().contains("está dañado"));
        assertEquals("{json roto", Files.readString(archivo));
        assertEquals(1, new JsonMensajeDao(respaldo).listar().size());

        // El código NO selecciona el .bak como fallback.
        try (Stream<Path> entradas = Files.list(carpetaTemporal)) {
            assertEquals(1, entradas.filter(p ->
                    p.getFileName().toString().startsWith("mensajes.json.corrupt-")
            ).count());
        }
    }

    @Test
    void jsonConListaVaciaDevuelveListaVaciaAunqueHayaRespaldo() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Path respaldo = carpetaTemporal.resolve("mensajes.json.bak");
        Files.writeString(archivo, "[]");
        new JsonMensajeDao(respaldo).guardar(
                List.of(new MensajeFuturo("Carlos", "Solo en respaldo", 5))
        );

        List<MensajeFuturo> recuperados = new JsonMensajeDao(archivo).listar();

        assertTrue(recuperados.isEmpty());
        assertEquals(1, new JsonMensajeDao(respaldo).listar().size());
    }


    @Test
    void propiedadesDuplicadasEnMismoObjetoSonRechazadas() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");

        // Reproduce el debugger: el segundo objeto repite autor, texto y plazoAnios;
        // Jackson no debe quedarse silenciosamente con "3er registro".
        String contenido = """
                [
                  {
                    "autor": "Carlos",
                    "texto": "Mensaje de prueba",
                    "plazoAnios": 5
                  },
                  {
                    "autor": "Carlos",
                    "texto": "Otro Mensaje",
                    "plazoAnios": 5,
                    "autor": "Carlos",
                    "texto": "3er registro",
                    "plazoAnios": 5
                  }
                ]
                """;
        Files.writeString(archivo, contenido);

        IOException error = assertThrows(IOException.class,
                () -> new JsonMensajeDao(archivo).listar());

        assertTrue(error.getMessage().contains("está dañado"));
        assertEquals(contenido, Files.readString(archivo),
                "El original debe conservarse sin modificaciones");

        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            List<Path> copias = archivos
                    .filter(p -> p.getFileName().toString()
                            .startsWith("mensajes.json.corrupt-"))
                    .toList();
            assertEquals(1, copias.size());
            assertEquals(contenido, Files.readString(copias.get(0)),
                    "La copia debe conservar exactamente el JSON con duplicados");
        }
    }

    @Test
    void propiedadesIgualesEnObjetosDiferentesSonValidas() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        Files.writeString(archivo, """
                [
                  {"autor":"Carlos","texto":"Primero","plazoAnios":5},
                  {"autor":"Carlos","texto":"Segundo","plazoAnios":5}
                ]
                """);

        List<MensajeFuturo> mensajes = new JsonMensajeDao(archivo).listar();

        assertEquals(2, mensajes.size(),
                "El control de duplicados es por objeto, no por archivo");
        assertEquals("Primero", mensajes.get(0).getTexto());
        assertEquals("Segundo", mensajes.get(1).getTexto());
    }

    @Test
    void duplicadosDuranteLaEjecucionBloqueanGuardadoYProtegenBak() throws Exception {
        Path archivo = carpetaTemporal.resolve("mensajes.json");
        MensajeService servicio = new MensajeService(new JsonMensajeDao(archivo));
        servicio.registrar("Carlos", "Primero", 5);
        servicio.registrar("Carlos", "Segundo", 5);

        Path respaldo = carpetaTemporal.resolve("mensajes.json.bak");
        String respaldoAntes = Files.readString(respaldo);
        String duplicado = """
                [
                  {"autor":"Carlos","texto":"Primero","plazoAnios":5,
                   "texto":"Tercero"}
                ]
                """;
        Files.writeString(archivo, duplicado);

        assertThrows(IOException.class,
                () -> servicio.registrar("Carlos", "No debe registrarse", 5));

        assertEquals(2, servicio.listar().size());
        assertEquals(duplicado, Files.readString(archivo));
        assertEquals(respaldoAntes, Files.readString(respaldo),
                "El respaldo anterior no debe sobrescribirse con JSON inválido");
    }

}
