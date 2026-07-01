# Cerrando los módulos que ya tenían algo de código

Cuatro frentes: `HistorialMedico` (completar), `LogAcceso` (construir desde
cero, modelo incluido), `HistorialNivelDeportista` (construir) e
`HistorialPlanesDeportista` (construir + conectar automáticamente).

---

## 1. HistorialMedico — solo le faltaba la mitad del Controller

Ya tenías `Model + Repository + Service` completos. El `Controller` solo
tenía `PUT` (actualizar) y `DELETE` (eliminar) — le agregué lo que faltaba.

| Archivo | Tipo de cambio | Reemplaza a |
|---|---|---|
| `HistorialMedicoController.java` | Completado | `Controller/Persona/HistorialMedicoController.java` |

**Endpoints nuevos en este controller:**
```
POST   /api/historial-medico                          -> crear
GET    /api/historial-medico/persona/{personaId}       -> listar (paginado)
GET    /api/historial-medico/{id}                      -> ver uno
GET    /api/historial-medico/persona/{personaId}/alerta -> tiene condicion activa?
```

Los dos que ya existían (`PUT`, `DELETE`) no los toqué.

El endpoint `/alerta` usa un método que **ya tenía tu `Service`** y que no
estaba expuesto: `tieneCondicionActiva()`, que revisa si hay un registro con
gravedad en los últimos 30 días. Útil para que un entrenador vea una bandera
antes de una sesión o competencia.

---

## 2. LogAcceso — el modelo estaba prácticamente vacío

Tu `Models/Funcionalidad/LogAcceso.java` solo tenía un campo `id` (con un
comentario tuyo `// ← ¿está esto?`, señal de que quedó a medio escribir). No
había Service, Controller, ni ningún lugar del código que escribiera ahí.
Tuve que diseñar el modelo completo, no solo exponerlo.

| Archivo | Tipo de cambio | Reemplaza a |
|---|---|---|
| `LogAcceso.java` | Reescrito con campos reales | `Models/Funcionalidad/LogAcceso.java` |
| `LogAccesoRepository.java` | Reescrito con queries | `repository/Funcionalidad/LogAccesoRepository.java` |
| `LogAccesoDTO.java` | Nuevo | `DTO/Funcionalidad/LogAccesoDTO.java` |
| `LogAccesoService.java` | Nuevo | `Service/Funcionalidad/LogAccesoService.java` |
| `LogAccesoController.java` | Nuevo | `Controller/Funcionalidad/LogAccesoController.java` |
| `AuthController.java` | Modificado: ahora registra logs | `Controller/Auth/AuthController.java` |

**Campos que le di al modelo:** `usuario` (opcional, puede no existir si el
email no está registrado), `emailIntento`, `fecha`, `accion`
(`LOGIN_EXITOSO` / `LOGIN_FALLIDO` / `LOGOUT` / `CUENTA_BLOQUEADA`),
`ipOrigen`, `detalle`.

**Dónde se registra:** modifiqué `AuthController` (no `AuthService`) porque
la IP del request solo está disponible a nivel de controller. Ahora:
- `POST /api/login` registra el intento, exitoso o fallido (incluyendo
  cuando la cuenta se bloquea).
- `POST /api/logout` registra la salida.

**Endpoints de consulta:**
```
GET /api/logs-acceso/usuario/{usuarioId}              -> historial de accesos de un usuario (paginado)
GET /api/logs-acceso/por-accion?accion=LOGIN_FALLIDO   -> todos los intentos fallidos, por ejemplo
GET /api/logs-acceso/rango?desde=...&hasta=...         -> por rango de fechas (paginado)
```

Acción pendiente de tu lado: usé el permiso `"ver_logs_acceso"` en el
`@RequiresPermission` de estos endpoints, y **no existe todavía en tu tabla
`permisos`**. Sin agregarlo (y asignarlo al rol Admin, o a quien corresponda),
estos endpoints van a devolver 403 para todo el mundo. Es una fila más en tu
tabla de permisos, igual que las que ya tienes.

---

## 3. HistorialNivelDeportista — construido, expuesto vía API manual

Tenías `Model + Repository` (vacío, sin queries). Construí `Service` y
`Controller` nuevos.

| Archivo | Tipo de cambio | Reemplaza a |
|---|---|---|
| `HistorialNivelDeportistaDTO.java` | Nuevo | `DTO/Historiales/` |
| `HistorialNivelDeportistaRepository.java` | Reescrito con 1 query | `repository/Historiales/` |
| `HistorialNivelDeportistaService.java` | Nuevo | `Service/Historiales/` |
| `HistorialNivelDeportistaController.java` | Nuevo | `Controller/Historiales/` |

```
POST /api/historial-nivel                  -> registrar un cambio de nivel
GET  /api/historial-nivel/deportista/{id}  -> ver el historial de un deportista
```

Importante: esto quedó **sin conectar automáticamente**. A diferencia de
`HistorialPlanesDeportista` (ver abajo), este historial no se llena solo
cuando cambias el `nivelActualId` de un deportista desde
`PersonaService.modificarDeportista`. Tendrías que llamarlo manualmente vía
este endpoint cada vez que subas a alguien de nivel. La razón por la que no
lo conecté automático: ya hemos tocado `PersonaService` varias veces en
sesiones distintas y prefería no mezclar un quinto cambio ahí sin que lo
veas primero. Si quieres que se conecte solo (como hice con el de planes),
dímelo y lo hago en un paso separado.

---

## 4. HistorialPlanesDeportista — construido Y conectado automáticamente

Mismo punto de partida que el anterior (`Model + Repository` vacío), pero
**a este sí lo conecté** a `InscripcionService.actualizarInscripcion`, donde
ya detectábamos `cambioPlan` desde el trabajo de los "3 cabos sueltos".

| Archivo | Tipo de cambio | Reemplaza a |
|---|---|---|
| `HistorialPlanesDeportistaDTO.java` | Nuevo | `DTO/Historiales/` |
| `HistorialPlanesDeportistaRepository.java` | Reescrito con 2 queries | `repository/Historiales/` |
| `HistorialPlanesDeportistaService.java` | Nuevo | `Service/Historiales/` |
| `HistorialPlanesDeportistaController.java` | Nuevo | `Controller/Historiales/` |
| `InscripcionService.java` | Modificado: llama al service nuevo | `Service/Org_dep/InscripcionService.java` (reemplaza la versión de "los 3 cabos") |

**Cómo funciona el cierre/apertura automático:** cada deportista tiene a lo
sumo un registro de plan "vigente" (`fechaFin = null`). Cuando
`actualizarInscripcion` detecta que cambiaste el `planPago`:
1. Busca el plan vigente anterior (si existe) y le pone `fechaFin = hoy`.
2. Crea un registro nuevo con el plan nuevo, `fechaInicio = hoy`,
   `fechaFin = null`.

Esto pasa **en la misma transacción** que ya teníamos (recalcular la
factura). Así que ahora cambiar el plan de una inscripción: recalcula la
factura (si no tiene pagos confirmados) y deja constancia en el historial de
planes — todo en un solo paso, sin que tengas que llamar nada manualmente.

```
POST /api/historial-planes                  -> registro manual (para casos no ligados a InscripcionService)
GET  /api/historial-planes/deportista/{id}  -> ver el historial de planes de un deportista
```

---

## Permisos que usé en `@RequiresPermission`

Reutilicé los que ya existían en tu tabla (`ver_historial_medico`,
`crear_historial_medico`, `modificar_historial_medico`,
`eliminar_historial_medico`, `ver_deportistas`, `modificar_deportista`,
`ver_inscripcion`, `modificar_inscripcion`). El único permiso **nuevo** que
necesitas crear es `ver_logs_acceso`, mencionado arriba.

## Qué validar de tu lado

1. Crear el permiso `ver_logs_acceso` y asignarlo al rol que corresponda.
2. Decidir si quieres que `HistorialNivelDeportista` también se conecte
   automático (hoy es manual).
3. Probar el flujo de cambio de plan end-to-end: crear inscripción, cambiar
   plan, confirmar que aparece en `GET /api/historial-planes/deportista/{id}`
   con la fecha de cierre del plan anterior correcta.
