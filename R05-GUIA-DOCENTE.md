# R05 · Service + DAO + persistencia JSON (alcance original)

**Punto de partida:** R04, con dos FXML, TableView, formulario, validación y navegación. **Objetivo:** separar responsabilidades y reemplazar la colección temporal por un archivo JSON mediante Jackson. R06 desarrollará pruebas sistemáticas de recuperación, archivos malformados y respaldo.

## Arquitectura

```text
FXML → Controller → MensajeService → MensajeDao (interfaz)
                                      └─ JsonMensajeDao (Jackson)
                                                   └─ data/mensajes.json
```

`HelloApplication` construye una sola instancia de `MensajeService` inyectando `JsonMensajeDao`. `HelloController` convierte la lista resultante en `ObservableList`, de uso exclusivamente visual. `FormularioController` invoca el servicio y muestra errores. El modelo `MensajeFuturo` conserva sus getters, agrega constructor sin argumentos y setters necesarios para la deserialización. No cambiar los FXML respecto a R04.

## Secuencia de implementación/capturas

1. **R05-01: estado previo.** Capturar `FormularioController` de R04: reglas de registro en Controller y lista en `HelloApplication`. En la rama R05 parcial esto ya está refactorizado.
2. **R05-02: Service.** Crear paquete `service`, implementar `registrar` y `listar` independientes de JavaFX. Verificar que un mensaje inválido produce `IllegalArgumentException`.
3. **R05-03: contrato DAO.** Crear paquete `dao` y la interfaz `MensajeDao` con `listar()` y `guardar(...)` que declaran `IOException`.
4. **R05-04: Jackson.** Agregar `jackson-databind:2.18.3` al `pom.xml`; actualizar `module-info.java` con `requires com.fasterxml.jackson.databind` y `opens ...model` para Jackson. El proyecto usa Java 21.
5. **R05-05: modelo.** Añadir constructor vacío y setters a `MensajeFuturo` para que Jackson pueda reconstruir objetos. Capturar el antes/después del modelo.
6. **R05-06: JsonMensajeDao.** Crear la implementación con `ObjectMapper`, `Path.of("data", "mensajes.json")`, `readValue` y `writeValue`. Archivo ausente → lista vacía; archivo existente malformado → `IOException`, no vaciar ni sobrescribir.
7. **R05-07: Service + DAO.** Inyectar `MensajeDao` en el constructor de `MensajeService`; cargar al iniciar y guardar al registrar, agregando a memoria sólo si la escritura termina correctamente.
8. **R05-08: aplicación y controllers.** Inicializar el servicio en `HelloApplication`; quitar los datos de prueba de R04, de modo que el primer arranque sin JSON muestre lista vacía. Mantener navegación. `FormularioController` debe manejar `IllegalArgumentException` y `IOException`.
9. **R05-09: ejecución.** Ejecutar `mvn clean test` y `mvn javafx:run`. Registrar `Ana`, texto `Hola dentro de cinco años`, plazo `5`. Capturar ventana con una fila y archivo `data/mensajes.json` legible.
10. **R05-10: reinicio breve.** Cerrar y abrir, comprobar que se recupera Ana. R06 ampliará el test de recuperación y el análisis de fallos.

## Archivos de R05

- `pom.xml` — Jackson 2.18.3 y Surefire 3.2.5.
- `src/main/java/module-info.java` — módulos y `opens` para deserialización.
- `model/MensajeFuturo.java` — constructor vacío y setters.
- `dao/MensajeDao.java` — contrato de persistencia.
- `dao/JsonMensajeDao.java` — lectura y escritura JSON.
- `service/MensajeService.java` — reglas, carga y guardado.
- `HelloApplication.java` — composición y manejo del error de arranque.
- `FormularioController.java` — error de entrada y error de persistencia.
- `HelloController.java` — sigue adaptando a ObservableList (de la primera parte de R05).
- Tests `MensajeServiceTest` y `JsonMensajeDaoTest`.

## Comprobaciones y límites

- Validación falla antes de guardar y no modifica datos.
- El servicio sólo actualiza su lista después de guardar.
- Al primer arranque, la tabla está vacía; no se reinsertan ejemplos en cada inicio.
- El archivo `data/mensajes.json` queda fuera de Git por `.gitignore`; su ubicación depende del directorio de trabajo.
- La escritura de R05 es directa, **no** es todavía atómica ni dispone de archivo de respaldo; R06 podrá abordar esos riesgos.
- Una lectura corrupta impide iniciar la interfaz y se informa mediante una alerta, sin sobrescribir el archivo.
- El repositorio contiene pruebas unitarias y de DAO; su ejecución real debe verificarse en el entorno docente antes de declarar el incremento validado.

**Estado:** código propuesto en `feat/dsy1102-r05-service`. Pendiente ejecución local, capturas y aceptación docente. Las slides publicadas R00–R04 permanecen intactas.
