package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

public class HelloController {
    private HelloApplication app;
    @FXML private TableView<MensajeFuturo> tablaMensajes;
    @FXML private TableColumn<MensajeFuturo, String> colAutor;
    @FXML private TableColumn<MensajeFuturo, String> colTexto;
    @FXML private TableColumn<MensajeFuturo, String> colPlazo;

    @FXML
    private void initialize() {
        colAutor.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().getAutor())
        );

        colTexto.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().getTexto())
        );

        colPlazo.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().getPlazoAnios() + " años")
        );
    }

    public void configurar(HelloApplication app) {
        this.app = app;
        // La misma lista que conserva HelloApplication
        tablaMensajes.setItems(app.getMensajes());
    }

    @FXML
    private void abrirFormulario() throws IOException {
        app.mostrarFormulario();
    }
}
