package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.dao.JsonMensajeDao;
import cl.duoc.dsy1102.timecapsule.service.MensajeService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;

public class HelloApplication extends Application {

    private Stage stage;
    private MensajeService mensajeService;

    public MensajeService getMensajeService() {
        return mensajeService;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Cápsula del Tiempo FX");

        try {
            mensajeService = new MensajeService(
                    new JsonMensajeDao(Path.of("data", "mensajes.json"))
            );
            mostrarListado();
            stage.show();
        } catch (IOException error) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("No fue posible abrir la cápsula");
            alerta.setHeaderText("Error al recuperar los mensajes");
            alerta.setContentText(error.getMessage());
            alerta.showAndWait();
            // No arrancar con lista vacía ni sobrescribir un JSON corrupto.
        }
    }

    public void mostrarListado() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();
        HelloController controller = loader.getController();
        controller.configurar(this);
        stage.setScene(new Scene(root, 860, 480));
    }

    public void mostrarFormulario() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("formulario-view.fxml"));
        Parent root = loader.load();
        FormularioController controller = loader.getController();
        controller.configurar(this);
        stage.setScene(new Scene(root, 860, 480));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
