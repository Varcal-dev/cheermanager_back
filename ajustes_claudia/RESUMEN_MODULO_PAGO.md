# Módulo de Pago — archivos nuevos

## Por qué estos archivos y no otros

`Pago` ya existía como modelo (`Models/Financiero/Pago.java`), pero no tenía
repository, service ni controller. `Factura` y `PlanPago` ya tenían las tres
capas — usé exactamente ese mismo patrón y estilo de código para que
`PagoController` se sienta como un controller más del proyecto, no como un
añadido externo.

También descubrí en el camino que `TipoPago` y `MetodoPago` (los catálogos que
usa `Pago`) tampoco tenían repository — los agregué porque `PagoService` los
necesita para validar las relaciones.

## Archivos nuevos (ninguno reemplaza algo existente)

| Archivo | Ruta destino |
|---|---|
| `PagoRepository.java` | `src/main/java/com/varcal/cheermanager/repository/Financiero/` |
| `TipoPagoRepository.java` | `src/main/java/com/varcal/cheermanager/repository/Financiero/` |
| `MetodoPagoRepository.java` | `src/main/java/com/varcal/cheermanager/repository/Financiero/` |
| `PagoDTO.java` | `src/main/java/com/varcal/cheermanager/DTO/Financiero/` |
| `PagoResponseDTO.java` | `src/main/java/com/varcal/cheermanager/DTO/Financiero/` |
| `PagoService.java` | `src/main/java/com/varcal/cheermanager/Service/Financiero/` |
| `PagoController.java` | `src/main/java/com/varcal/cheermanager/Controller/Financiero/` |

Todos son archivos **nuevos**. No tocan ni `Factura`, ni `PlanPago`, ni nada
que ya tuvieras funcionando.

## Endpoints que quedan disponibles

```
POST   /api/pagos                          → registrar un pago
GET    /api/pagos                          → listar todos los pagos
GET    /api/pagos/{id}                     → ver un pago puntual
GET    /api/pagos/factura/{facturaId}      → historial de pagos de una factura
GET    /api/pagos/persona/{personaId}      → historial financiero de una persona
GET    /api/pagos/factura/{facturaId}/saldo → cuánto falta por pagar de esa factura
PATCH  /api/pagos/{id}/estado              → cambiar Pendiente → Pagado (o a Vencido)
DELETE /api/pagos/{id}                     → eliminar un pago (ej. error de digitación)
```

### Ejemplo: registrar un pago

```json
POST /api/pagos
{
  "facturaId": 4,
  "tipoPagoId": 1,
  "metodoPagoId": 2,
  "fecha": "2026-06-26",
  "estado": "Pagado",
  "monto": 80000
}
```

Respuesta (`PagoResponseDTO`):
```json
{
  "id": 12,
  "facturaId": 4,
  "numeroFactura": "FAC-2026-004",
  "totalFactura": 80000,
  "tipoPagoId": 1,
  "tipoPago": "Mensualidad",
  "metodoPagoId": 2,
  "metodoPago": "Transferencia",
  "fecha": "2026-06-26",
  "estado": "Pagado",
  "monto": 80000,
  "saldoPendienteFactura": 0
}
```

## Reglas de negocio que sí agregué (no estaban en ningún lado, las definí yo)

1. **No se puede sobrepagar una factura.** Si la suma de pagos en estado
   `Pagado` de una factura más el nuevo pago supera el `total` de la factura,
   el sistema rechaza la operación con un mensaje que indica cuánto saldo
   queda disponible. Esto aplica tanto al crear un pago directamente como
   `Pagado`, como al hacer `PATCH .../estado` para pasar un pago de
   `Pendiente` a `Pagado`.
2. **El saldo de una factura se calcula, no se guarda.** `Factura` no tiene
   columna de estado de pago — por diseño tuyo, el estado vive en `Pago`. El
   saldo pendiente (`saldoPendienteFactura` en la respuesta, o el endpoint
   `/saldo`) se calcula en tiempo real sumando los pagos `Pagado` de esa
   factura y restando al total. Si necesitas que esto sea más rápido en el
   futuro (muchas facturas), se puede agregar una columna `saldo` cacheada,
   pero hoy con `SUM()` está bien para el volumen que maneja un club.
3. **`@Transactional`** en `registrarPago` y `actualizarEstado`, siguiendo el
   mismo criterio que usamos en `PersonaService`: la validación de saldo y el
   guardado deben verse como una sola operación.

## Lo que NO hice (decisiones que te dejo a ti)

- **No conecté esto automáticamente con `Inscripcion`.** Sigue siendo un paso
  manual: inscribes al deportista → generas la factura → registras el/los
  pagos. Conectarlo (que al inscribir se genere la factura sola) es el
  siguiente paso natural si quieres, pero lo dejé fuera de este cambio para
  no mezclar dos cosas en un mismo parche.
- **No agregué `@RequiresPermission` al `PagoController`.** Noté que
  `FacturaController`, `PlanPagoController` y `ConvenioController` tampoco lo
  tienen (solo los controllers de `Persona/` lo usan) — mantuve la misma
  consistencia que ya existe en el módulo Financiero. Si quieres que el
  módulo financiero completo empiece a usar permisos granulares, es una
  conversación aparte que toca varios controllers a la vez, no solo este.
- **No creé endpoints para los catálogos `TipoPago`/`MetodoPago`** (crear,
  listar tipos de pago o métodos de pago desde la API). Si tu plan es que el
  admin pueda agregar nuevos métodos de pago desde la app (ej. agregar
  "Nequi"), dímelo y agrego esos dos controllers — son triviales.

## Próximo paso sugerido

Con esto cerrado, lo lógico que sigue (según lo que hablamos) es conectar
`Inscripcion` → `Factura` automáticamente, y después construir
`Horario_Asistencia`. Dime cuál prefieres y seguimos.
