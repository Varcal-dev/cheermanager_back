# CheerManager — Diseño de KPIs y Métricas por Dashboard

Estructurado sobre los 9 permisos de dashboard que ya existen en el seed
(`ver_dash_*`), que hasta ahora no tienen ningún controller/service detrás.
Cada sección indica: qué mostrar, de qué tabla/entidad sale, y qué tan listo
está hoy el dato (🟢 disponible ya / 🟡 disponible parcial / 🔴 requiere
construir el módulo primero).

---

## 1. `ver_dash_inicio` — Overview general (landing al entrar)

Vista resumen de una sola pantalla, pensada para cualquier rol con acceso
básico. Un número por tarjeta, sin desglose.

| KPI | Fuente | Estado |
|---|---|---|
| Deportistas activos | `Deportista` + `Persona.estado` | 🟢 |
| Grupos de entrenamiento activos | `GrupoEntrenamiento` | 🟢 |
| Próximo evento (nombre + fecha + días restantes) | `Evento.findByFechaGreaterThanEqual` | 🟢 |
| Cartera pendiente total (COP) | `Factura.saldoPendienteFactura` sumado | 🟢 |
| % asistencia últimos 7 días (todos los grupos) | `Asistencia` | 🟢 |
| Alertas activas (médicas + facturas vencidas) | `HistorialMedico.tieneCondicionActiva()` + `Factura` vencida | 🟡 (falta exponerlo agregado) |

---

## 2. `ver_dash_personas` — Deportistas y entrenadores

| KPI | Fuente | Estado |
|---|---|---|
| Total deportistas por género / edad / nivel | `Deportista`, `Persona` | 🟢 |
| Distribución por división FEDECOLCHEER (Tiny/Mini/Youth/Junior/Senior/Open) | Requiere el campo de división por año de nacimiento del PDF 2026 que aún no está cargado en `ReglaCategoria`/`CategoriaNivel` | 🔴 |
| Altas y bajas del mes/trimestre | `Persona.estado` + fecha de cambio (no hay historial de estado hoy) | 🔴 (falta `HistorialEstadoPersona` o similar) |
| Tiempo promedio de permanencia por nivel | `HistorialNivelDeportista` | 🔴 (módulo existe pero no está conectado — cambios de nivel no generan registro automático) |
| Entrenadores activos y grupos a cargo c/u | `Entrenador`, `GrupoEntrenamiento` | 🟢 |
| Condiciones médicas activas (conteo, no detalle clínico en el dashboard) | `HistorialMedico` | 🟢 |
| % de deportistas con documentos completos (waiver, registro médico) | `Documento` | 🟡 (según qué tan completo esté ese módulo) |

**Nota de privacidad:** el dashboard debe mostrar *conteos* de condiciones
médicas, nunca el detalle — el detalle sigue restringido a
`ver_historial_medico`.

---

## 3. `ver_dash_organizacion` — Grupos, niveles, divisiones

| KPI | Fuente | Estado |
|---|---|---|
| Deportistas por grupo (ocupación vs. rango 8-30 de la tabla FEDECOLCHEER) | `GrupoEntrenamiento.deportistas` | 🟢 |
| Grupos por debajo del mínimo de atletas (riesgo de no calificar en competencia) | Cruce `GrupoEntrenamiento` + tabla de cantidades por nivel | 🟡 (la tabla de cantidades ya existe para la rúbrica, falta el cruce) |
| Distribución de grupos por nivel/modalidad (Novice/Prep/Elite/International) | `GrupoEntrenamiento.categoriaNivel` | 🟢 |
| Grupos mixtos vs. femeninos, y cumplimiento de la regla COED (nivel 3-4) | `GrupoEntrenamiento` + conteo de hombres | 🟡 (dato existe, falta el cálculo de regla) |

---

## 4. `ver_dash_horarios` — Asistencia y operación diaria

| KPI | Fuente | Estado |
|---|---|---|
| % asistencia por grupo (semana/mes) | `Asistencia`, `Horario` | 🟢 |
| Deportistas con asistencia < 70% (alerta de deserción temprana) | `Asistencia` agregada por deportista | 🟢 |
| Horas de entrenamiento programadas vs. efectivas | `Horario` + `Asistencia` | 🟢 |
| Entrenador con más carga horaria semanal | `Horario.entrenador` | 🟢 |

Esta es de las más fáciles de construir ya — todo el dato está completo y
con las 4 capas armadas.

---

## 5. `ver_dash_evaluacion` — Rendimiento técnico

Es el dashboard más rico dado el motor de `EvaluacionRutina` que ya está
construido.

| KPI | Fuente | Estado |
|---|---|---|
| Puntaje total más reciente por grupo | `EvaluacionRutina.puntajeTotal` | 🟢 |
| **Progresión de puntaje evento-a-evento** (línea de tiempo) | `EvaluacionRutina.eventoOficial` + `ResultadoCompetencia` (ya conectados) | 🟢 recién habilitado |
| Desglose por sub-criterio (dificultad vs. ejecución vs. showmanship) | `RegistroSubCriterio` | 🟢 |
| Comparativo entre grupos del mismo nivel | `EvaluacionRutina` filtrado por `NivelCompetencia` | 🟢 |
| Objetivos cumplidos vs. pendientes por deportista/grupo | `Objetivo`, `Evaluacion` (módulo de evaluación individual, distinto al de rutina) | 🟢 |
| Brecha entre puntaje interno (simulacro) y puntaje real de competencia | `EvaluacionRutina.evento` (texto libre = simulacro) vs. `eventoOficial` (FK) | 🟢 — es justo el uso que se pensó para separar ambos campos |

---

## 6. `ver_dash_eventos` — Competencias

| KPI | Fuente | Estado |
|---|---|---|
| Próximos eventos + grupos inscritos por evento | `Evento`, `GrupoEvento` | 🟢 |
| Historial de resultados (posición, puntaje) por evento | `ResultadoCompetencia` | 🟢 |
| Podios totales de la temporada (conteo de top-3) | `ResultadoCompetencia.posicion <= 3` | 🟢 |
| Premios ganados (tipo/cantidad) | `PremioEvento` + `ResultadoCompetencia.premio` | 🟢 |
| Costo de participación por evento (si se liga a `Financiero`) | No existe hoy relación Evento↔gasto | 🔴 |

---

## 7. `ver_dash_finanzas` — Financiero

| KPI | Fuente | Estado |
|---|---|---|
| Recaudo del mes vs. proyectado | `Pago`, `PlanPago` | 🟢 |
| Cartera vencida (monto y % sobre cartera total) | `Factura.saldoPendienteFactura` + `fechaVencimiento` | 🟢 |
| Tasa de morosidad por plan de pago | `Factura` agrupado por `PlanPago` | 🟢 |
| Ingreso por convenios/descuentos aplicados (impacto real en recaudo) | `Convenio`, `Descuento` | 🟢 |
| Ingreso por venta de inventario (tienda) | `Venta` | 🔴 (módulo Inventario sin construir) |
| Proyección de flujo de caja próximos 30/60/90 días | `Factura.fechaVencimiento` futura | 🟢 |

---

## 8. `ver_dash_tienda` — Inventario y ventas

**Bloqueado por completo**: el módulo `Inventario` solo tiene Models
(`Producto`, `CategoriaProducto`, `MovimientoInventario`, `Venta`,
`DetalleVenta`) — cero Repository/Service/Controller. Este dashboard no
puede construirse hasta que se levante el módulo, igual que se hizo recién
con `Eventos`.

KPIs previstos una vez exista el módulo:
- Ventas del mes (monto y unidades)
- Productos con stock bajo (`MovimientoInventario` acumulado vs. mínimo)
- Producto más vendido / margen por categoría
- Rotación de inventario

**Recomendación de secuencia**: dado que `Eventos` ya quedó cerrado, el
candidato natural para el próximo módulo completo es `Inventario` — no solo
por este dashboard, sino porque hoy es el único módulo (junto a Eventos) sin
ninguna capa construida.

---

## 9. `ver_dash_administracion` — Vista para Admin/Dirección técnica

Agregador de alto nivel, cruza varios dominios — es el único dashboard que
necesita datos de todos los demás módulos.

| KPI | Fuente | Estado |
|---|---|---|
| Salud financiera (recaudo vs. cartera vencida) | Finanzas | 🟢 |
| Salud operativa (asistencia promedio general) | Horarios | 🟢 |
| Salud competitiva (progresión de puntaje promedio del club) | Evaluación | 🟢 |
| Uso del sistema (usuarios activos, logs de acceso) | `LogAcceso` | 🟢 |
| Auditoría reciente (últimos cambios sensibles: pagos, historial médico) | `Auditoria` (si tiene las 4 capas — verificar, quedó pendiente de confirmar en el análisis anterior) | 🟡 |

---

## Arquitectura propuesta para implementar esto

En vez de que cada `Service` existente calcule sus propios KPIs (lo que
duplicaría lógica de agregación por todos lados), conviene un módulo nuevo
y transversal:

```
Models/Dashboard/          (no necesita entidades propias, solo lee)
DTO/Dashboard/
    ResumenInicioDTO.java
    ResumenPersonasDTO.java
    ResumenOrganizacionDTO.java
    ResumenHorariosDTO.java
    ResumenEvaluacionDTO.java
    ResumenEventosDTO.java
    ResumenFinanzasDTO.java
    ResumenTiendaDTO.java        (cuando exista Inventario)
    ResumenAdministracionDTO.java
Service/Dashboard/
    DashboardService.java   (o uno por área, si crecen mucho)
Controller/Dashboard/
    DashboardController.java
    GET /api/dashboard/inicio
    GET /api/dashboard/personas
    GET /api/dashboard/organizacion
    GET /api/dashboard/horarios
    GET /api/dashboard/evaluacion
    GET /api/dashboard/eventos
    GET /api/dashboard/finanzas
    GET /api/dashboard/tienda
    GET /api/dashboard/administracion
```

Cada método del `DashboardService` inyecta los repositories que ya existen
(no crea entidades nuevas, no duplica lógica de negocio) y arma queries de
agregación (`@Query` con `COUNT`, `SUM`, `AVG`, o cálculo en Java sobre listas
pequeñas — a esta escala de club no hace falta optimizar con vistas
materializadas todavía).

**Sugerencia de orden de construcción**, de más a menos inmediato:

1. `ver_dash_horarios` y `ver_dash_evaluacion` — 100% de datos ya disponibles, cero bloqueos.
2. `ver_dash_finanzas` y `ver_dash_eventos` — mismo caso, datos completos.
3. `ver_dash_inicio` y `ver_dash_personas` — casi completos, con 1-2 huecos menores (historial de altas/bajas, cruce de división FEDECOLCHEER).
4. `ver_dash_organizacion` — requiere el cruce con las tablas de cantidad por nivel (ya existen, falta la consulta).
5. `ver_dash_administracion` — al final, porque agrega todo lo anterior.
6. `ver_dash_tienda` — depende de construir Inventario completo primero.

¿Empezamos por `ver_dash_horarios` y `ver_dash_evaluacion` (los más listos), o prefieres que levantemos primero el módulo `Inventario` para desbloquear `ver_dash_tienda` y `ver_dash_finanzas` completo?
