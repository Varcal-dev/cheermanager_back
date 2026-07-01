# Módulo Horario_Asistencia — construido completo

Punto de partida: solo existían los 4 `Models` (`Horario`,
`HorarioEntrenamiento`, `Asistencia`, `EstadoAsistencia`) — cero Repository,
Service o Controller. Construí las 3 capas para los 4, más 2 piezas que
descubrí que faltaban en el camino (`DiaSemanaRepository` y un método nuevo
en `DeportistaPerteneceGrupoRepository`).

## Cómo está armado el dominio (para que el mapa te quede claro)

```
Horario  (dia + hora inicio + hora fin)
   |
   | (via HorarioEntrenamiento, tabla puente)
   v
GrupoEntrenamiento
   |
   | (via DeportistaPerteneceGrupo, ya existia)
   v
Deportista

Asistencia = deportista + horario + fecha + estado
```

Detalle importante de diseño que noté y respeté: `Asistencia` apunta a
`Horario`, no directamente a `HorarioEntrenamiento` ni a `GrupoEntrenamiento`.
Esto significa que, en teoría, un mismo bloque de horario podría estar
asignado a más de un grupo (ej. dos niveles que entrenan a la misma hora en
gimnasios distintos). Para que "asistencias de este grupo en esta fecha" no
se confunda con las de otro grupo que comparta el mismo horario, la consulta
(`AsistenciaRepository.findByGrupoIdAndFecha`) cruza explícitamente por
`HorarioEntrenamiento.grupoEntrenamiento.id`, no solo por `horario.id`.

## Archivos nuevos

| Archivo | Carpeta destino |
|---|---|
| `EstadoAsistenciaRepository.java` | `repository/Horario_Asistencia/` |
| `HorarioRepository.java` | `repository/Horario_Asistencia/` |
| `HorarioEntrenamientoRepository.java` | `repository/Horario_Asistencia/` |
| `AsistenciaRepository.java` | `repository/Horario_Asistencia/` |
| `DiaSemanaRepository.java` | `repository/Org_dep/` (no existía, lo necesitaba `HorarioService`) |
| `DeportistaPerteneceGrupoRepository.java` | `repository/Org_dep/` (reemplaza al existente, le agregué 1 método) |
| `HorarioDTO.java`, `HorarioResponseDTO.java`, `AsignarHorarioGrupoDTO.java` | `DTO/Horario_Asistencia/` |
| `AsistenciaDTO.java`, `RegistroAsistenciaMasivoDTO.java`, `AsistenciaResponseDTO.java` | `DTO/Horario_Asistencia/` |
| `HorarioService.java`, `AsistenciaService.java` | `Service/Horario_Asistencia/` |
| `HorarioController.java`, `AsistenciaController.java` | `Controller/Horario_Asistencia/` |

## Endpoints

### Horarios (definir bloques y asignarlos a grupos)
```
POST   /api/horarios                         -> crear un bloque de horario (dia + horas)
GET    /api/horarios                         -> listar todos (con sus grupos asignados)
GET    /api/horarios/{id}                    -> ver uno
GET    /api/horarios/grupo/{grupoId}         -> horarios de un grupo especifico
PUT    /api/horarios/{id}                    -> actualizar
DELETE /api/horarios/{id}                    -> eliminar (limpia sus asignaciones a grupos primero)
POST   /api/horarios/asignar-grupo           -> vincular un horario existente a un grupo existente
DELETE /api/horarios/asignacion/{id}         -> desvincular un horario de un grupo
```

### Asistencias (el uso del día a día)
```
POST /api/asistencias                                            -> registrar un deportista
POST /api/asistencias/masivo                                     -> pasar lista a todo un grupo de una vez
GET  /api/asistencias/grupo/{grupoId}/deportistas-activos        -> quien debe aparecer en la lista
GET  /api/asistencias/grupo/{grupoId}?fecha=...                  -> ver la lista ya tomada
GET  /api/asistencias/deportista/{id}                            -> historial completo de un deportista
GET  /api/asistencias/deportista/{id}/rango?desde=...&hasta=...  -> historial en un rango
GET  /api/asistencias/deportista/{id}/porcentaje?desde=...&hasta=... -> % de asistencia
DELETE /api/asistencias/{id}                                     -> eliminar un registro
```

## Cómo se usa en la práctica (flujo completo)

1. **Una sola vez, al configurar el grupo**: crear el `Horario` (ej. un
   registro para "Martes 4-6pm" y otro para "Jueves 4-6pm" — recuerda que un
   `Horario` es un solo día) y asignarlos al grupo con
   `POST /api/horarios/asignar-grupo`.

2. **Cada día de entrenamiento**: el entrenador abre la app, llama a
   `GET /api/asistencias/grupo/{grupoId}/deportistas-activos` para ver la
   lista de quién debería estar, marca el estado de cada uno, y manda todo
   junto con `POST /api/asistencias/masivo`:

```json
POST /api/asistencias/masivo
{
  "horarioId": 5,
  "fecha": "2026-06-27",
  "registros": [
    { "deportistaId": 12, "estadoAsistenciaId": 1 },
    { "deportistaId": 13, "estadoAsistenciaId": 2 },
    { "deportistaId": 14, "estadoAsistenciaId": 1 }
  ]
}
```

3. Si algún deportista del lote falla (ej. un ID que ya no existe), la
   respuesta viene con status `207` y un detalle de cuáles sí se guardaron y
   cuáles no — no se pierde el resto del lote por un solo error.

## Decisiones que tomé (para que las valides)

1. **No usé `@RequiresPermission` en estos dos controllers.** Igual que con
   `PagoController` en su momento, seguí la convención real de la carpeta
   hermana más cercana: `GrupoEntrenamientoController` (en `Org_dep`) tampoco
   lo usa. Si decides que todo el módulo de horarios/asistencia debería
   tener permisos granulares, es una conversación aparte — hoy cualquier
   usuario autenticado puede llamar estos endpoints.

2. **`EstadoAsistencia` es un catálogo de texto libre** (`unique`, sin enum
   fijo en Java), igual que ya hacías con `EstadoPersona`. El porcentaje de
   asistencia (`calcularPorcentajeAsistencia`) asume que uno de los valores
   en tu catálogo se llama exactamente `"Presente"`. Si lo nombras distinto
   (ej. "Asistió"), avísame y ajusto el método.

3. **No valido cruces de horario entre grupos distintos.** Un mismo
   `Horario` puede asignarse a más de un grupo sin que el sistema se queje.
   Si en tu operación real eso nunca debería pasar (ej. nunca dos grupos
   entrenan a la misma hora porque solo hay un gimnasio), puedo agregar esa
   validación.

4. **El registro masivo actualiza en vez de duplicar** si ya existía un
   registro para ese deportista+horario+fecha — así que es seguro volver a
   enviar el mismo lote si el entrenador necesita corregir algo, sin generar
   filas duplicadas.

## Qué validar de tu lado

1. Confirmar que tu catálogo `estados_asistencia` tenga un valor llamado
   exactamente `"Presente"` (por el cálculo de porcentaje).
2. Decidir si quieres permisos granulares en este módulo.
3. Probar el flujo completo: crear horario, asignar a grupo, ver
   deportistas activos, registrar asistencia masiva, consultar porcentaje.
