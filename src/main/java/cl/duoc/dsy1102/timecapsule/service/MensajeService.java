package cl.duoc.dsy1102.timecapsule.service;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;

import java.util.ArrayList;
import java.util.List;

public class MensajeService {

    private final List<MensajeFuturo> mensajes = new ArrayList<>();

    public MensajeFuturo registrar(String autor, String texto, Integer plazoAnios) {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el autor.");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Debes escribir un mensaje.");
        }
        if (plazoAnios == null || plazoAnios <= 0) {
            throw new IllegalArgumentException("Selecciona un plazo válido.");
        }

        MensajeFuturo mensaje = new MensajeFuturo(
                autor.trim(),
                texto.trim(),
                plazoAnios
        );
        mensajes.add(mensaje);
        return mensaje;
    }

    public List<MensajeFuturo> listar() {
        return List.copyOf(mensajes);
    }
}
