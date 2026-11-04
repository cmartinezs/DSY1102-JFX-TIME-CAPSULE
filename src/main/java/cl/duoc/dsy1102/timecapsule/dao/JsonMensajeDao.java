package cl.duoc.dsy1102.timecapsule.dao;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class JsonMensajeDao implements MensajeDao {

    private static final TypeReference<List<MensajeFuturo>> TIPO_MENSAJES =
            new TypeReference<>() {};

    private final Path archivo;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonMensajeDao(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public List<MensajeFuturo> listar() throws IOException {
        if (Files.notExists(archivo)) {
            return List.of();
        }

        try {
            List<MensajeFuturo> recuperados = mapper.readValue(
                    archivo.toFile(), TIPO_MENSAJES
            );

            if (recuperados == null || recuperados.stream().anyMatch(this::esInvalido)) {
                throw preservarArchivoDanado(new IOException("Estructura JSON inválida."));
            }
            return recuperados;
        } catch (JsonProcessingException error) {
            throw preservarArchivoDanado(error);
        }
    }

    private boolean esInvalido(MensajeFuturo mensaje) {
        return mensaje == null
                || mensaje.getAutor() == null || mensaje.getAutor().isBlank()
                || mensaje.getTexto() == null || mensaje.getTexto().isBlank()
                || mensaje.getPlazoAnios() <= 0;
    }

    private IOException preservarArchivoDanado(IOException causa) {
        Path copia = null;
        try {
            Path directorio = archivo.toAbsolutePath().getParent();
            copia = Files.createTempFile(
                    directorio, archivo.getFileName() + ".corrupt-", ".json"
            );
            Files.copy(archivo, copia, StandardCopyOption.REPLACE_EXISTING);
            return new IOException(
                    "El JSON está dañado. Se conservó una copia en "
                            + copia.toAbsolutePath()
                            + ". Revisa el respaldo .bak antes de continuar. "
                            + "El archivo original no se modificó.",
                    causa
            );
        } catch (IOException errorCopia) {
            if (copia != null) {
                try {
                    Files.deleteIfExists(copia);
                } catch (IOException errorLimpieza) {
                    errorCopia.addSuppressed(errorLimpieza);
                }
            }
            IOException error = new IOException(
                    "El JSON está dañado y no fue posible crear una copia de resguardo. "
                            + "No se modificó el archivo original.",
                    causa
            );
            error.addSuppressed(errorCopia);
            return error;
        }
    }

    @Override
    public void guardar(List<MensajeFuturo> mensajes) throws IOException {
        Path destino = archivo.toAbsolutePath();
        Path directorio = destino.getParent();
        Files.createDirectories(directorio);

        // El temporal está en la misma carpeta para permitir movimiento atómico.
        Path temporal = Files.createTempFile(directorio, "mensajes-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), mensajes);

            // Antes de sustituir el JSON, conservar la versión anterior.
            if (Files.exists(destino)) {
                Path respaldo = destino.resolveSibling(destino.getFileName() + ".bak");
                Files.copy(destino, respaldo, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(temporal, destino,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                // La alternativa no es atómica; el archivo .bak protege la versión anterior.
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
