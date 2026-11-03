package cl.duoc.dsy1102.timecapsule.service;

import cl.duoc.dsy1102.timecapsule.dao.MensajeDao;
import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MensajeServiceTest {

    private static class DaoMemoria implements MensajeDao {
        List<MensajeFuturo> guardados = new ArrayList<>();

        @Override
        public List<MensajeFuturo> listar() {
            return List.copyOf(guardados);
        }

        @Override
        public void guardar(List<MensajeFuturo> mensajes) {
            guardados = new ArrayList<>(mensajes);
        }
    }

    @Test
    void registrarMensajeValidoNormalizaYPersiste() throws Exception {
        DaoMemoria dao = new DaoMemoria();
        MensajeService service = new MensajeService(dao);

        var mensaje = service.registrar("  Vale  ", "  Hola futuro  ", 5);

        assertEquals("Vale", mensaje.getAutor());
        assertEquals("Hola futuro", mensaje.getTexto());
        assertEquals(5, mensaje.getPlazoAnios());
        assertEquals(1, service.listar().size());
        assertEquals(1, dao.guardados.size());
    }

    @Test
    void rechazaDatosInvalidosSinPersistir() throws Exception {
        DaoMemoria dao = new DaoMemoria();
        MensajeService service = new MensajeService(dao);

        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("  ", "Mensaje", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "  ", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "Mensaje", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("Carlos", "Mensaje", 0));

        assertTrue(service.listar().isEmpty());
        assertTrue(dao.guardados.isEmpty());
    }

    @Test
    void listadoEsUnaCopiaNoModificable() throws Exception {
        MensajeService service = new MensajeService(new DaoMemoria());
        service.registrar("Vale", "Mensaje", 1);

        assertThrows(UnsupportedOperationException.class, () -> service.listar().clear());
        assertEquals(1, service.listar().size());
    }

    @Test
    void fallaDeEscrituraNoAgregaMensajeEnMemoria() throws Exception {
        MensajeDao dao = new MensajeDao() {
            public List<MensajeFuturo> listar() { return List.of(); }
            public void guardar(List<MensajeFuturo> mensajes) throws IOException {
                throw new IOException("Disco no disponible");
            }
        };
        MensajeService service = new MensajeService(dao);

        assertThrows(IOException.class,
                () -> service.registrar("Carlos", "Hola", 5));
        assertTrue(service.listar().isEmpty());
    }
}
