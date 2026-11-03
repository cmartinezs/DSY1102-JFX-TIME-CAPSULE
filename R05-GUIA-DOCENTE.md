# R05 · Introducir Service y reglas de negocio

**Punto de partida:** R04, dos vistas FXML y mensajes compartidos en memoria. **Resultado:** misma interfaz y navegación, pero validación y registro viven en `MensajeService`. No incluye DAO ni JSON.

## Paso a paso para IntelliJ (JDK 21)

1. Abrir el proyecto R04 y comprobar que se muestran dos mensajes. Hacer una captura **antes** del cambio: `FormularioController.guardarMensaje()` valida y agrega directamente.
2. Crear el paquete `cl.duoc.dsy1102.timecapsule.service` desde `src/main/java`.
3. Crear `MensajeService.java` con los métodos `registrar(String, String, Integer)` y `listar()`. Usar el código versionado de este commit, no fragmentos de otra rama.
4. Reemplazar `HelloApplication.java`: conservar carga de FXML, navegación y `Stage`; sustituir la lista JavaFX compartida por un `MensajeService` único. Cargar allí los dos mensajes de ejemplo al iniciar.
5. Reemplazar `HelloController.java`: conectar la tabla a `FXCollections.observableArrayList(app.getMensajeService().listar())`. Como el listado se vuelve a cargar al regresar desde el formulario, refleja los cambios. No incorporar JavaFX al servicio.
6. Reemplazar `FormularioController.java`: quitar la creación de `MensajeFuturo`, la validación y el `add`. Invocar `registrar` y mostrar `IllegalArgumentException` junto al formulario. El evento de navegación mantiene su comportamiento.
7. **No cambiar** `MensajeFuturo.java`, `hello-view.fxml`, `formulario-view.fxml` ni `pom.xml`.
8. Ejecutar `mvn test` (o desde Maven de IntelliJ) y después `mvn javafx:run`. Si hay un problema con `mvn` en Windows, usar el Maven Tool Window.
9. En el formulario, intentar autor vacío: debe mostrar el error y mantener la vista. Registrar `Ana`, `Hola dentro de cinco años`, `5`: volver al listado y ver tres filas. Cerrar y abrir: deben volver solo las dos filas iniciales, porque R05 **no persiste**.

## Capturas para las slides

- R05-01: `FormularioController` de R04, antes de refactorizar.
- R05-02: paquete `service` y `MensajeService` con `registrar/listar`.
- R05-03: `HelloApplication` con servicio compartido.
- R05-04: `HelloController` adaptando `List` a `ObservableList`.
- R05-05: `FormularioController` delegando y mostrando un mensaje de validación.
- R05-06: error por autor vacío en la UI.
- R05-07: listado con tercera fila luego del registro.
- R05-08: prueba JUnit o salida de Maven de tests.

## Explicación didáctica

El modelo sigue representando un mensaje; el Controller coordina eventos y feedback visual; el Service contiene reglas de registro y almacenamiento *en memoria*. `HelloApplication` crea el servicio y conecta vistas. Las listas JavaFX se crean únicamente en la capa de presentación. No se introduce un contrato DAO vacío por anticipación: en R06 aparece la necesidad de persistencia.

## Criterios de aceptación

- Misma experiencia de navegación R04.
- Datos inválidos no ingresan a la lista.
- Service testeable sin inicializar JavaFX.
- Registro aparece al volver al listado.
- Reiniciar pierde los datos nuevos, intencionalmente hasta R06.

**Estado:** propuesta implementada en rama de trabajo; pendiente validación de ejecución y capturas docentes antes de publicarla como demostración validada.
