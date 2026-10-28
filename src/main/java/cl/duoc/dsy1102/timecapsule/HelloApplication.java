package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    private Stage stage;

    // Una lista compartida por ambas pantallas
    private final ObservableList<MensajeFuturo> mensajes =
            FXCollections.observableArrayList(
                    new MensajeFuturo(
                            "Valentina",
                            "Nunca dejes de perseguir tus sueños.",
                            5
                    ),
                    new MensajeFuturo(
                            "Carlos",
                            "Recuerda de dónde comenzaste.",
                            10
                    )
            );

    public ObservableList<MensajeFuturo> getMensajes() {
        return mensajes;
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        stage.setTitle("Cápsula del Tiempo FX");

        mostrarListado();
        stage.show();
    }

    public void mostrarListado() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("hello-view.fxml")
        );

        Parent root = loader.load();

        HelloController controller = loader.getController();
        controller.configurar(this);

        stage.setScene(new Scene(root, 860, 480));
    }

    public void mostrarFormulario() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("formulario-view.fxml")
        );

        Parent root = loader.load();

        FormularioController controller = loader.getController();
        controller.configurar(this);

        stage.setScene(new Scene(root, 860, 480));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
