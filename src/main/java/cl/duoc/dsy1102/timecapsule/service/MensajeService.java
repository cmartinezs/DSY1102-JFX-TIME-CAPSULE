package cl.duoc.dsy1102.timecapsule.service;

import cl.duoc.dsy1102.timecapsule.dao.MensajeDao;
import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MensajeService {

    private final MensajeDao dao;
    private final List<MensajeFuturo> mensajes;

    public MensajeService(MensajeDao dao) throws IOException {
        this.dao = dao;
        this.mensajes = new ArrayList<>(dao.listar());
    }

    public MensajeFuturo registrar(String autor, String texto, Integer plazoAnios)
            throws IOException {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el autor.");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Debes escribir un mensaje.");
        }
        if (plazoAnios == null || plazoAnios <= 0) {
            throw new IllegalArgumentException("Selecciona un plazo válido.");
        }

        MensajeFuturo nuevo = new MensajeFuturo(
                autor.trim(), texto.trim(), plazoAnios
        );
        List<MensajeFuturo> actualizados = new ArrayList<>(mensajes);
        actualizados.add(nuevo);

        // Solo actualizamos la memoria si la escritura terminó bien.
        dao.guardar(actualizados);
        mensajes.add(nuevo);
        return nuevo;
    }

    public List<MensajeFuturo> listar() {
        return List.copyOf(mensajes);
    }
}
