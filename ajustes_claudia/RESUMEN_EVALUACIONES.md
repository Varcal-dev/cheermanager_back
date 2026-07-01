# Módulo Evaluaciones — construido completo

Punto de partida: solo existían los 5 `Models` (`TipoCategoria`,
`CriterioEvaluacion`, `Evaluacion`, `EstadoObjetivo`, `Objetivo`), cero
Repository, Service o Controller. Construí las 3 capas para los 5.

## Cómo está armado el dominio

```
TipoCategoria (catalogo: ej. "Gimnasia", "Elevaciones", "Tosses")
   |
   v
CriterioEvaluacion (nombre + categoria) -- ej. "Flexibilidad", dentro de Gimnasia
   |
   v
Evaluacion = deportista + criterio + fecha + puntaje

EstadoObjetivo (catalogo: ej. "En progreso", "Cumplido")
   |
   v
Objetivo = deportista + nombre + descripcion + fechaCreacion + estado
```

Son dos sub-dominios independientes que comparten el mismo `Deportista`:
**Evaluación** mide desempeño puntual en un criterio técnico, **Objetivo** es
una meta de desarrollo más abierta (ej. "Dominar el back walkover") con
seguimiento de estado a lo largo del tiempo.

Esto conecta directo con tu roadmap de FEDECOLCHEER que ya tienes documentado
(niveles 1-7, categorías Gimnasia/Elevaciones/Tosses, clasificación
Apropiada/Avanzada/Élite) — `TipoCategoria` y `CriterioEvaluacion` son
exactamente el lugar para cargar esos criterios técnicos por nivel.

## Archivos nuevos

| Archivo | Carpeta destino |
|---|---|
| `TipoCategoriaRepository.java`, `EstadoObjetivoRepository.java`, `CriterioEvaluacionRepository.java`, `EvaluacionRepository.java`, `ObjetivoRepository.java` | `repository/Evaluaciones/` |
| `CriterioEvaluacionDTO.java`, `CriterioEvaluacionResponseDTO.java` | `DTO/Evaluaciones/` |
| `EvaluacionDTO.java`, `EvaluacionResponseDTO.java`, `RegistroEvaluacionMasivoDTO.java` | `DTO/Evaluaciones/` |
| `ObjetivoDTO.java`, `ObjetivoResponseDTO.java` | `DTO/Evaluaciones/` |
| `CriterioEvaluacionService.java`, `EvaluacionService.java`, `ObjetivoService.java` | `Service/Evaluaciones/` |
| `CriterioEvaluacionController.java`, `EvaluacionController.java`, `ObjetivoController.java` | `Controller/Evaluaciones/` |

## Endpoints

### Criterios de evaluación (catálogo)
```
POST   /api/criterios-evaluacion                  -> crear (nombre + categoria)
GET    /api/criterios-evaluacion                  -> listar todos
GET    /api/criterios-evaluacion/categoria/{id}   -> los de una categoria
PUT    /api/criterios-evaluacion/{id}             -> actualizar
DELETE /api/criterios-evaluacion/{id}             -> eliminar
```

### Evaluaciones (registro y consulta)
```
POST /api/evaluaciones                                              -> registrar un criterio puntual
POST /api/evaluaciones/masivo                                       -> evaluar varios criterios de una sesion
GET  /api/evaluaciones/deportista/{id}                              -> historial completo
GET  /api/evaluaciones/deportista/{id}/rango?desde=...&hasta=...    -> historial en un rango
GET  /api/evaluaciones/deportista/{id}/criterio/{cId}/progresion    -> evolucion en un criterio (para graficar)
GET  /api/evaluaciones/deportista/{id}/categoria/{catId}/actual     -> ultima foto tecnica de una categoria
GET  /api/evaluaciones/deportista/{id}/criterio/{cId}/promedio      -> promedio historico en un criterio
DELETE /api/evaluaciones/{id}                                       -> eliminar
```

### Objetivos
```
POST   /api/objetivos                              -> crear
GET    /api/objetivos/deportista/{id}              -> historial de objetivos de un deportista
GET    /api/objetivos/deportista/{id}/estado/{eId} -> filtrados por estado
GET    /api/objetivos/estado/{estadoId}            -> panel: todos los objetivos en un estado (todos los deportistas)
PUT    /api/objetivos/{id}                         -> actualizar (nombre/descripcion/estado)
PATCH  /api/objetivos/{id}/estado                  -> atajo para solo cambiar el estado
DELETE /api/objetivos/{id}                         -> eliminar
```

## Cómo se usa en la práctica

**Evaluación de una sesión** (ej. el entrenador evalúa a un deportista en 3
criterios de Gimnasia el mismo día):
```json
POST /api/evaluaciones/masivo
{
  "deportistaId": 12,
  "fecha": "2026-06-28",
  "registros": [
    { "criterioId": 1, "puntajeObtenido": 8 },
    { "criterioId": 2, "puntajeObtenido": 7 },
    { "criterioId": 3, "puntajeObtenido": 9 }
  ]
}
```

**Ver evolución de un deportista en "Flexibilidad" a lo largo del tiempo**:
```
GET /api/evaluaciones/deportista/12/criterio/1/progresion
```
Devuelve la lista ordenada por fecha ascendente, lista para graficar.

## Decisiones que tomé (para que las valides)

1. **Sí usé `@RequiresPermission`** en los tres controllers, a diferencia de
   `Horario_Asistencia` (donde seguí la convención de su carpeta hermana sin
   permisos). Acá decidí protegerlo desde el inicio porque es información de
   desempeño/desarrollo de deportistas, muchos de ellos menores de edad — me
   pareció más prudente que dejarlo abierto por defecto.

2. **Permisos nuevos que necesitas crear** (ninguno existía en tu tabla):
   - `ver_evaluaciones`, `crear_evaluacion`, `eliminar_evaluacion`
   - `modificar_criterios_evaluacion`
   - `ver_objetivos`, `crear_objetivo`, `modificar_objetivo`, `eliminar_objetivo`

   Sin crearlos y asignarlos a los roles correspondientes (Entrenador,
   Admin), todos los endpoints de este módulo van a devolver 403.

3. **El puntaje no tiene escala fija.** El modelo `Evaluacion.puntajeObtenido`
   es un `Integer` libre — solo valido que no sea negativo. Si tu escala real
   es 0-10 o 0-100, dime y agrego ese límite superior en `validarPuntaje()`.

4. **El registro masivo es "un deportista, varios criterios"** (la sesión
   típica de evaluación individual), no "un criterio, todo un grupo" como
   hicimos con asistencia. Si también necesitas ese segundo caso (ej.
   evaluar "Flexibilidad" a todo un grupo el mismo día), es un endpoint
   adicional fácil de agregar con el mismo patrón.

5. **No conecté `Objetivo` con `Evaluacion`.** Son dos tablas independientes
   hoy, no hay forma de decir "este objetivo se cumple cuando el promedio de
   tal criterio supere X". Si quieres esa conexión, la conversamos en una
   sesión aparte porque implica diseño nuevo, no solo exponer lo que ya está.

## Qué validar de tu lado

1. Crear los 8 permisos nuevos y asignarlos a los roles correspondientes.
2. Confirmar si necesitas límite superior de puntaje.
3. Decidir si quieres el registro masivo "por grupo" además del "por deportista".
4. Probar el flujo: crear categoria, crear criterios, registrar evaluacion
   masiva, consultar progresion.
