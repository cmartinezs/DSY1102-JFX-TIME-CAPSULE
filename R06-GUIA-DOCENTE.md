# DSY1102 · Cápsula del Tiempo FX · R06 — Recuperación y protección de archivos

**Punto de partida:** R05 original validado visualmente: Service + DAO + JSON, con dos mensajes guardados.  
**Requerimiento nuevo:** al cerrar y abrir debemos recuperar lo escrito. Si el archivo se daña, no debemos borrar mensajes silenciosamente; necesitamos un respaldo y una recuperación explícita.

**Rama:** `feat/dsy1102-r06-json-recovery` (sale de `feat/dsy1102-r05-service`).
**Estado:** implementación en revisión; el docente aporta capturas reales antes de crear las slides.

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
