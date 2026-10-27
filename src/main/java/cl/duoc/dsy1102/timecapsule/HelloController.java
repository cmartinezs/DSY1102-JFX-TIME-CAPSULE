package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;


public class HelloController {
    @FXML private TableView<MensajeFuturo> tablaMensajes;
    @FXML private TableColumn<MensajeFuturo, String> colAutor;
    @FXML private TableColumn<MensajeFuturo, String> colTexto;
    @FXML private TableColumn<MensajeFuturo, String> colPlazo;

    @FXML private TextField campoAutor;
    @FXML private TextField campoTexto;
    @FXML private ComboBox<Integer> campoPlazo;
    @FXML private Label estadoRegistro;


    private final ObservableList<MensajeFuturo> mensajes = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        campoPlazo.getItems().setAll(1, 5, 10);
        campoPlazo.setValue(5);

        // 1. Relacionamos las columnas con el modelo
        colAutor.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(
                        celda.getValue().getAutor()
                )
        );

        colTexto.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(
                        celda.getValue().getTexto()
                )
        );

        colPlazo.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(
                        celda.getValue().getPlazoAnios() + " años"
                )
        );

        // 2. Conectamos la lista con la tabla
        tablaMensajes.setItems(mensajes);

        // 3. Agregamos datos de demostración
        mensajes.add(new MensajeFuturo(
                "Valentina",
                "Nunca dejes de perseguir tus sueños.",
                5
        ));

        mensajes.add(new MensajeFuturo(
                "Carlos",
                "Recuerda de dónde comenzaste.",
                10
        ));
    }


    @FXML
    private void agregarMensaje() {

        String autor = campoAutor.getText().trim();
        String texto = campoTexto.getText().trim();

        // Requerimiento: no aceptar datos vacíos
        if (autor.isBlank() || texto.isBlank()) {
            estadoRegistro.setText(
                    "Debes completar el autor y el mensaje."
            );
            return;
        }

        // Crear nuestro objeto de dominio
        MensajeFuturo nuevo = new MensajeFuturo(
                autor,
                texto,
                campoPlazo.getValue()
        );

        // La tabla observa automáticamente esta lista
        mensajes.add(nuevo);

        // Limpiar formulario y confirmar
        campoAutor.clear();
        campoTexto.clear();
        campoPlazo.setValue(5);

        estadoRegistro.setText(
                "¡Mensaje agregado correctamente!"
        );

        tablaMensajes.scrollTo(nuevo);
    }

}
