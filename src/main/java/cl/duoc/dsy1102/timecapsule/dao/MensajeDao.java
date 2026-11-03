package cl.duoc.dsy1102.timecapsule.dao;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import java.io.IOException;
import java.util.List;

public interface MensajeDao {
    List<MensajeFuturo> listar() throws IOException;
    void guardar(List<MensajeFuturo> mensajes) throws IOException;
}
