package cl.duoc.dsy1102.timecapsule;

import cl.duoc.dsy1102.timecapsule.model.MensajeFuturo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class HelloController {
    @FXML private TableView<MensajeFuturo> tablaMensajes;
    @FXML private TableColumn<MensajeFuturo, String> colAutor;
    @FXML private TableColumn<MensajeFuturo, String> colTexto;
    @FXML private TableColumn<MensajeFuturo, String> colPlazo;

    private final ObservableList<MensajeFuturo> mensajes = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
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
}
