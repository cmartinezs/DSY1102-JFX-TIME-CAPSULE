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
import java.util.Arrays;
import java.util.List;

public class JsonMensajeDao implements MensajeDao {

    private static final TypeReference<List<MensajeFuturo>> TIPO_MENSAJES =
            new TypeReference<>() {};

    private final Path archivo;
    private final ObjectMapper mapper = new ObjectMapper();

    // R06: referencia de la última versión válida que el DAO cargó o guardó.
    private byte[] versionConocida;
    private boolean versionRegistrada;

    public JsonMensajeDao(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public List<MensajeFuturo> listar() throws IOException {
        // Diagnóstico docente: identifica el archivo que realmente lee Jackson.
        System.out.println("[R06][DAO] Archivo leído: "
                + archivo.toAbsolutePath().normalize());
        byte[] contenido = leerContenidoActual();
        if (contenido == null) {
            System.out.println("[R06][DAO] Archivo ausente; primera ejecución sin datos.");
            versionConocida = null;
            versionRegistrada = true;
            return List.of();
        }

        List<MensajeFuturo> recuperados = leerYValidar(contenido);
        System.out.println("[R06][DAO] JSON válido; mensajes recuperados: "
                + recuperados.size());
        versionConocida = contenido;
        versionRegistrada = true;
        return recuperados;
    }

    private byte[] leerContenidoActual() throws IOException {
        if (Files.notExists(archivo)) {
            return null;
        }
        return Files.readAllBytes(archivo);
    }

    private List<MensajeFuturo> leerYValidar(byte[] contenido) throws IOException {
        try {
            List<MensajeFuturo> recuperados = mapper.readValue(
                    contenido, TIPO_MENSAJES
            );
            if (recuperados == null || recuperados.stream().anyMatch(this::esInvalido)) {
                throw preservarArchivoDanado(new IOException("Estructura JSON inválida."));
            }
            return recuperados;
        } catch (JsonProcessingException error) {
            System.err.println("[R06][DAO] Jackson rechazó el archivo: "
                    + archivo.toAbsolutePath().normalize());
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

        // Nunca sobrescribir ni respaldar como «bueno» un JSON que está dañado.
        byte[] contenidoActual = leerContenidoActual();
        if (contenidoActual != null) {
            leerYValidar(contenidoActual);
        }

        // También protegemos cambios externos válidos: el Service puede tener
        // una versión anterior en memoria. Debe reiniciarse para recuperar la nueva.
        if (versionRegistrada && !Arrays.equals(versionConocida, contenidoActual)) {
            throw new IOException("El archivo JSON cambió mientras la aplicación estaba "
                    + "abierta. No se guardó el mensaje para evitar perder datos. "
                    + "Cierra y vuelve a abrir la cápsula.");
        }

        // También permite usar el DAO directamente sin llamar antes a listar().
        if (!versionRegistrada) {
            versionConocida = contenidoActual;
            versionRegistrada = true;
        }

        // El temporal está en la misma carpeta para permitir movimiento atómico.
        Path temporal = Files.createTempFile(directorio, "mensajes-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), mensajes);
            byte[] nuevaVersion = Files.readAllBytes(temporal);

            // Segunda comprobación por si el archivo cambió durante la escritura.
            if (!Arrays.equals(versionConocida, leerContenidoActual())) {
                throw new IOException("El archivo JSON fue modificado mientras se "
                        + "preparaba el guardado. Operación cancelada.");
            }

            // El respaldo solo se actualiza si el JSON vigente es válido y conocido.
            if (contenidoActual != null) {
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

            // Esta es ahora la versión que conocemos: futuros guardados la comprobarán.
            versionConocida = nuevaVersion;
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
