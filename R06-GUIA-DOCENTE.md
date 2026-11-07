# DSY1102 · Cápsula del Tiempo FX · R06 — Recuperación y protección de archivos

**Punto de partida:** R05 original validado visualmente: Service + DAO + JSON, con dos mensajes guardados.  
**Requerimiento nuevo:** al cerrar y abrir debemos recuperar lo escrito. Si el archivo se daña, no debemos borrar mensajes silenciosamente; necesitamos un respaldo y una recuperación explícita.

**Rama:** `feat/dsy1102-r06-json-recovery` (sale de `feat/dsy1102-r05-service`).
**Estado:** implementación en revisión; el docente aporta capturas reales antes de crear las slides.


## Corrección R06 · guardado con JSON modificado mientras la app sigue abierta

**Problema reproducido por el docente:** se estropea `mensajes.json` y la ventana continúa mostrando los mensajes anteriores; tras registrar otro, el JSON vuelve a aparecer válido. **Causa identificada en el código anterior:** el servicio mantenía los registros en memoria, y `JsonMensajeDao.guardar()` reescribía el archivo sin comprobar su contenido actual. No existe lectura automática del archivo `.bak`: observar las filas anteriores no prueba que se haya cargado el respaldo.

**Corrección aplicada a `JsonMensajeDao`:**

1. `listar()` conserva una instantánea de bytes de la última versión cargada correctamente.
2. `guardar()` vuelve a leer y validar el JSON actual antes de preparar el temporal. Si es inválido, preserva una copia `.corrupt-*.json` y **no guarda**.
3. Si el archivo cambió externamente, aunque el contenido nuevo sea JSON válido, el guardado se cancela para evitar sobrescribir datos que el servicio todavía no tiene en memoria.
4. Si todo está correcto, prepara el temporal, comprueba otra vez si hubo cambios, conserva `.bak` y reemplaza el JSON. Actualiza su instantánea solo tras completar el movimiento.
5. El formulario conserva la entrada y muestra una **alerta de error de persistencia**; el usuario no es enviado a la tabla como si se hubiera guardado.

**Nueva evidencia que debe capturarse antes de cerrar R06:** mantener la ventana abierta con registros válidos, modificar y guardar `data/mensajes.json` con `{json roto`, volver al formulario y pulsar Guardar; debe aparecer la alerta, permanecer corrupto el JSON y conservarse el `.bak` anterior. Además, detener JavaFX y arrancar de nuevo con el JSON corrupto para comprobar el bloqueo de carga y la copia de resguardo. Restaurar después desde una **copia válida**.

```bash
git switch feat/dsy1102-r06-json-recovery
git pull --ff-only origin feat/dsy1102-r06-json-recovery
bash ./mvnw -Dtest=R06RecuperacionTest test
bash ./mvnw javafx:run
```

**Nota de seguridad:** la comparación es una defensa didáctica contra cambios externos detectables, no una transacción multiusuario ni un bloqueo de archivos. Persisten pequeñas ventanas de carrera entre la comprobación y el movimiento; el objetivo R06 no incluye concurrencia.

## Guías complementarias para contenidos que todavía se están introduciendo

La implementación R06 **mantiene** `recuperados.stream().anyMatch(this::esInvalido)`. No es necesario eliminar Streams ni reescribir la solución usando `for`: ambos enfoques se explican en material de apoyo fuera del flujo principal de la presentación.

- **[Streams, lambdas y referencias a métodos · cápsula DSY1102](https://github.com/cmartinezs/DSY1102-DESARROLLO-ORIENTADO-OBJETOS-2026-2/blob/master/clases/unidad-02/capsula-tiempo-fx/docs/GUIA-STREAMS-LAMBDAS-R06.md)**: compara `for` con `anyMatch`, explica `Predicate`, cortocircuito y `this::esInvalido`.
- **[NIO.2: Path, Files, File y manejo de archivos · cápsula DSY1102](https://github.com/cmartinezs/DSY1102-DESARROLLO-ORIENTADO-OBJETOS-2026-2/blob/master/clases/unidad-02/capsula-tiempo-fx/docs/GUIA-NIO2-PATH-FILES-R06.md)**: ruta relativa/absoluta, `Files`, temporal, respaldo, opciones de copia y errores.

### Evidencia recibida y pendiente

Con capturas reales se observan: dos registros persistidos, interfaz recuperada, lectura y escritura del DAO, tercer registro, JSON actual con tres elementos y archivo `.bak` con dos. **No se ha observado** la alerta por JSON dañado ni la creación de la copia `.corrupt-*.json`; esos criterios permanecen pendientes.

**Diagnóstico no destructivo de la prueba pendiente:** modificar `mensajes.json` mientras JavaFX sigue abierto no ejecuta `listar()`: la lectura sucede al construir el servicio durante el **arranque de un proceso nuevo**. Termina la aplicación, verifica la ruta real de `Path.of("data","mensajes.json").toAbsolutePath()`, altera exclusivamente ese archivo luego de respaldarlo y arranca otra vez. Si no hay alerta, ejecutar el test de corrupción individual y aportar el error exacto; no asumir éxito.

### Diagnóstico actualizado de ruta efectiva (7 noviembre de 2026)

Si luego de un `git pull` el comportamiento visual no cambia, primero comprueba **de qué repositorio y rama** vinieron los cambios. Las slides HTML viven en el repositorio docente `DSY1102-DESARROLLO-ORIENTADO-OBJETOS-2026-2`, mientras la aplicación JavaFX vive en `DSY1102-JFX-TIME-CAPSULE`.

R06 incluye ahora un diagnóstico **en la aplicación**: `HelloApplication.start()` imprime en la consola el directorio de trabajo, la ruta absoluta de `data/mensajes.json` y si el archivo existe al iniciar. Cuando la carga falla, el diálogo incluye esa ruta y la consola registra la excepción. La lectura sigue ocurriendo solamente al iniciar, **no** al editar archivos mientras la aplicación está abierta.

```bash
git branch --show-current
git pull --ff-only origin feat/dsy1102-r06-json-recovery
git log -1 --oneline
bash ./mvnw clean test
bash ./mvnw javafx:run
```

Para probar el error, detener la ejecución, **respaldar el JSON correcto**, editar la ruta exacta impresa bajo `[R06] Archivo JSON efectivo:` con `{json roto`, y **arrancar un proceso nuevo** con `bash ./mvnw javafx:run`. Debe aparecer la alerta con la ruta y el diagnóstico. Si no aparece, capturar la consola `[R06]` para identificar un directorio diferente, archivo ausente o un proceso que no se reinició.

No alterar el archivo durante una ejecución y dar por comprobado el error: la aplicación aún no implementa vigilancia de archivos y no pretende hacerlo.

## 1. Nueva necesidad antes de mostrar código

Situación de R05: la tabla muestra dos mensajes y `data/mensajes.json` contiene esos dos objetos.

Preguntas para el aula:
- ¿Al cerrar la aplicación, la tabla debe iniciar vacía o con los dos mensajes?
- ¿Qué ocurre si un usuario edita accidentalmente el JSON?
- ¿Qué dato preferimos conservar si falla una escritura: el archivo anterior o uno parcialmente generado?

La intención es **no añadir nuevas pantallas** ni introducir JDBC. La interfaz sigue siendo JavaFX con FXML; cambian las garantías de persistencia.

## 2. Secuencia en IntelliJ, con código íntegro en GitHub

1. Confirmar que los dos mensajes de R05 aparecen en `data/mensajes.json`. Si ya hay datos valiosos, guardar una copia manual fuera de `data/` antes de experimentar.
2. Cambiar a la rama R06:
   ```bash
   git fetch origin
   git switch feat/dsy1102-r06-json-recovery
   ```
   Si hay cambios locales, confirmarlos o resguardarlos antes de cambiar.
3. Examinar `JsonMensajeDao.listar()`. Ahora comprueba que la raíz no sea `null` y que cada mensaje tenga autor, texto y plazo válidos. JSON malformado o estructura inválida genera un error claro.
4. Examinar `preservarArchivoDanado()`: deja el original intacto y crea una copia con nombre `mensajes.json.corrupt-*.json` para diagnóstico. **No** intenta recuperar automáticamente datos posiblemente incorrectos.
5. Examinar `guardar()`: serializa primero a un temporal `mensajes-*.tmp` dentro del mismo directorio; si ya existe un JSON, copia su versión previa a `mensajes.json.bak`; luego reemplaza el archivo mediante un movimiento atómico cuando el sistema lo admite.
6. Examinar `HelloApplication.start()`: si `listar()` falla, muestra la alerta y termina JavaFX. No abre una tabla vacía como si se hubieran perdido los mensajes.
7. Revisar `R06RecuperacionTest.java` en `src/test/.../dao/`: pruebas de reinicio simulado, creación de `.bak`, JSON malformado, raíz `null` y valores inválidos.
8. Ejecutar `mvn clean test` o `./mvnw clean test` (Windows: `mvnw.cmd clean test`), y abrir la aplicación con `mvn javafx:run` o Maven Tool Window.
9. Cerrar la aplicación completamente y volverla a abrir: las dos filas deben mantenerse. Esto prueba recuperación real, además de los tests.
10. Registrar un tercer mensaje (ejemplo: **Vale**, **El futuro también se construye hoy**, **10 años**). Verificar que `mensajes.json` incluye los tres objetos y `mensajes.json.bak` conserva **la versión anterior**, con dos. El respaldo no es un duplicado de la última escritura.
11. **Ensayo controlado de corrupción:** detener la aplicación, copiar manualmente el último `mensajes.json` a un lugar seguro con otro nombre, modificar temporalmente `mensajes.json` para que contenga `{json roto`, y arrancar. Se muestra una alerta, se crea `mensajes.json.corrupt-*.json` y el original permanece como fue modificado. **No realizar este ensayo sobre información que no haya sido respaldada.**
12. Restaurar el JSON correcto desde la copia manual, no desde `.corrupt-*.json`. Alternativamente, recuperar desde `.bak` enseña la diferencia: se recupera la **versión anterior**, no necesariamente todos los registros del último guardado. Reiniciar y comprobar.

## 3. Capturas a aportar para las slides

| Captura | Evidencia real esperada |
| --- | --- |
| R06-01 | `mensajes.json` de R05 con los dos mensajes antes del reinicio |
| R06-02 | Tabla con los dos registros luego de cerrar y abrir la aplicación |
| R06-03 | `JsonMensajeDao.listar()`: lectura y comprobaciones de estructura |
| R06-04 | `guardar()`: temporal, archivo `.bak` y movimiento |
| R06-05 | Explorador de proyecto: `mensajes.json` y `mensajes.json.bak` después del tercer registro |
| R06-06 | Contenido de `.bak` con la versión **anterior** (dos mensajes) |
| R06-07 | Archivo JSON modificado deliberadamente durante la prueba controlada |
| R06-08 | Alerta de error al iniciar con el JSON malformado |
| R06-09 | Copia `mensajes.json.corrupt-*.json` y archivo original conservado |
| R06-10 | Aplicación funcionando tras la restauración manual |
| R06-11 | Resultado visible de `mvn clean test` y pruebas ejecutadas |

No utilizar ilustraciones artificiales como reemplazo de estas evidencias. Mantener la narrativa **problema → hipótesis → cambio de código → ejecución → checkpoint → explicación**. Enlazar el código completo de cada archivo y congelar commit cuando se valide.

## 4. Archivos Java y contratos

- [DAO completo](src/main/java/cl/duoc/dsy1102/timecapsule/dao/JsonMensajeDao.java)
- [Contrato DAO](src/main/java/cl/duoc/dsy1102/timecapsule/dao/MensajeDao.java) — se conserva de R05.
- [Service](src/main/java/cl/duoc/dsy1102/timecapsule/service/MensajeService.java) — sin cambios en R06.
- [Inicio JavaFX](src/main/java/cl/duoc/dsy1102/timecapsule/HelloApplication.java) — agrega salida controlada después del error de arranque.
- [Nuevas pruebas R06](src/test/java/cl/duoc/dsy1102/timecapsule/dao/R06RecuperacionTest.java)
- [Pruebas anteriores R05](src/test/java/cl/duoc/dsy1102/timecapsule/dao/JsonMensajeDaoTest.java)

**Compatibilidad:** Java 21, JavaFX 21.0.6, Jackson 2.22.4 y JUnit 5.12.1; mantener el POM R05. No modificar FXML, formularios ni la forma de registrar.

## 5. Decisiones y límites explícitos

- `.bak` contiene el JSON válido **antes del último reemplazo**. No significa respaldo de la versión actual.
- Si existe `mensajes.json` pero es ilegible o inválido, la aplicación avisa y no lo reemplaza por `[]`.
- La copia `.corrupt-*.json` conserva la versión dañada para diagnóstico; no se restaura automáticamente.
- En sistemas de archivos que no soporten `ATOMIC_MOVE`, se usa un movimiento ordinario con `.bak` previo. Por tanto no se promete atomicidad universal ni resistencia garantizada ante un corte eléctrico.
- No se incorporan concurrencia, base de datos, interfaz de restauración, control de versiones de registros ni múltiples usuarios: quedan fuera del alcance académico de R06.
- Los archivos de ejecución siguen fuera de Git por `.gitignore`. Asegurar que `data/` pertenece al directorio de trabajo del proyecto.
- No confundir pruebas unitarias incluidas en Git con ejecución verificada. Validación local y capturas son requisitos de cierre.

**Criterio de cierre R06:** test Maven ejecutado, dos mensajes recuperados tras reinicio real, `.bak` observado y JSON malformado detectado con copia preservada; además, restauración manual comprobada y capturas recibidas.
