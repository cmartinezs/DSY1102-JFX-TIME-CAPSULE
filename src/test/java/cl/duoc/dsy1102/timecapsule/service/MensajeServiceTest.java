package cl.duoc.dsy1102.timecapsule.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MensajeServiceTest {

    @Test
    void registrarMensajeValidoNormalizaDatos() {
        MensajeService service = new MensajeService();

        var mensaje = service.registrar("  Vale  ", "  Hola futuro  ", 5);

        assertEquals("Vale", mensaje.getAutor());
        assertEquals("Hola futuro", mensaje.getTexto());
        assertEquals(5, mensaje.getPlazoAnios());
        assertEquals(1, service.listar().size());
    }

    @Test
    void rechazaDatosInvalidosSinAgregarMensajes() {
        MensajeService service = new MensajeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("  ", "Mensaje", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "  ", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "Mensaje", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "Mensaje", 0));

        assertTrue(service.listar().isEmpty());
    }

    @Test
    void listadoEsUnaCopiaNoModificable() {
        MensajeService service = new MensajeService();
        service.registrar("Vale", "Mensaje", 1);

        var listado = service.listar();

        assertThrows(UnsupportedOperationException.class, listado::clear);
        assertEquals(1, service.listar().size());
    }
}
