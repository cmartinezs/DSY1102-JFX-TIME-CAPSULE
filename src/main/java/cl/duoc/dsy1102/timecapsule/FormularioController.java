package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class FormularioController {

    private HelloApplication app;

    @FXML private TextField campoAutor;
    @FXML private TextField campoTexto;
    @FXML private ComboBox<Integer> campoPlazo;
    @FXML private Label estadoRegistro;

    public void configurar(HelloApplication app) {
        this.app = app;
    }

    @FXML
    private void initialize() {
        campoPlazo.getItems().setAll(1, 5, 10);
        campoPlazo.setValue(5);
    }

    @FXML
    private void guardarMensaje() throws IOException {

        String autor = campoAutor.getText().trim();
        String texto = campoTexto.getText().trim();

        if (autor.isBlank() || texto.isBlank()) {
            estadoRegistro.setText(
                    "Debes completar el autor y el mensaje."
            );
            return;
        }

        MensajeFuturo nuevo = new MensajeFuturo(
                autor,
                texto,
                campoPlazo.getValue()
        );

        // Agregamos a la lista compartida
        app.getMensajes().add(nuevo);

        // Volvemos al listado
        app.mostrarListado();
    }

    @FXML
    private void volver() throws IOException {
        app.mostrarListado();
    }
}
