# Inscripción → Factura automática

## Qué cambia

Antes: inscribir a un deportista y facturarle eran dos pasos manuales y
desconectados (`POST /api/inscripciones` y `POST /api/facturas` por separado,
sin relación entre ellos).

Ahora: al hacer `POST /api/inscripciones`, el sistema genera automáticamente
la `Factura` correspondiente, usando el valor del `PlanPago` elegido
(aplicando su `descuentoPorcentaje` si lo tiene). Ya puedes seguir el flujo
completo: inscripción → factura (automática) → pago (con el módulo que
construimos antes).

## Archivos

| Archivo | Tipo de cambio | Reemplaza a |
|---|---|---|
| `Factura.java` | Modificado: campo nuevo `inscripcion` (opcional) | `Models/Financiero/Factura.java` |
| `FacturaRepository.java` | Modificado: 2 métodos nuevos | `repository/Financiero/FacturaRepository.java` |
| `InscripcionService.java` | Modificado: genera factura al crear inscripción | `Service/Org_dep/InscripcionService.java` |
| `InscripcionController.java` | Modificado: 1 endpoint nuevo | `Controller/Org_dep/InscripcionController.java` |

**Importante**: el campo `inscripcion` en `Factura` es opcional
(`nullable = true`). Las facturas que ya tienes hoy, y las que se sigan
creando manualmente desde `POST /api/facturas`, simplemente tendrán ese campo
en `null` — no se rompe nada existente.

## Flujo nuevo, de punta a punta

```
POST /api/inscripciones
{
  "deportistaId": 12,
  "planPagoId": 3,
  "fechaInscripcion": "2026-06-26",
  "fechaVencimiento": "2026-07-26",
  "estado": "Activa"
}
```

Esto, en una sola transacción:
1. Crea la `Inscripcion`.
2. Busca el `PlanPago` (ej. "Mensualidad Nivel 2", valor $80.000, sin descuento).
3. Genera la `Factura`: número `FAC-2026-0001` (correlativo, único, no colisiona
   con facturas creadas manualmente), persona = la del deportista, total = el
   valor del plan ya con su descuento aplicado, y queda enlazada a la inscripción.

Para ver esa factura después:
```
GET /api/inscripciones/12/factura
```

Y para registrar el pago de esa factura, usas el módulo que ya tienes:
```
POST /api/pagos
{
  "facturaId": <el id que te devolvió la factura>,
  "tipoPagoId": 1,
  "metodoPagoId": 2,
  "fecha": "2026-06-26",
  "estado": "Pagado",
  "monto": 80000
}
```

## Decisiones que tomé (para que las valides)

1. **El total de la factura sale del `PlanPago`, no de `Descuento`.**
   `PlanPago` ya tenía su propio `valorMensual` y `descuentoPorcentaje` —
   los usé directamente. El campo `Descuento` de `Factura` (que está ligado a
   `Convenio`) lo dejé en `null` en las facturas automáticas. Si un deportista
   tiene convenio y debe llevar ese descuento también, hoy seguiría siendo un
   ajuste manual sobre la factura ya creada. Si quieres que el convenio se
   aplique también automáticamente (usando el `convenioId` que ya tiene
   `Deportista`), es un cambio pequeño adicional — dime si lo quieres.
2. **El número de factura es correlativo por año**: `FAC-2026-0001`,
   `FAC-2026-0002`, etc. Si dos inscripciones se crean en el mismo instante
   exacto, el método reintenta hasta encontrar un número libre (usa
   `findByNumeroFactura` para chequear antes de guardar).
3. **La fecha de emisión de la factura = fecha de inscripción.** Pareció lo
   más lógico (se factura el día que se inscribe), pero si tu flujo real es
   distinto (ej. facturar el primer día del mes siguiente), lo ajustamos.
4. **Si `crearInscripcion` falla generando la factura, la inscripción tampoco
   se guarda** (gracias a `@Transactional`) — evita inscritos fantasma sin
   nada que cobrarles.

## Lo que NO toqué

- `actualizarInscripcion` (`PUT /api/inscripciones/{id}`) sigue igual — **no**
  regenera ni ajusta la factura si cambias el plan de pago de una inscripción
  existente. Si cambias el plan después de creada, la factura original queda
  con el valor viejo. Esto puede ser un caso que quieras cubrir después.
- `eliminarInscripcion` tampoco toca la factura asociada — si borras una
  inscripción, la factura generada se queda huérfana (con `inscripcion_id`
  apuntando a un registro que ya no existe, salvo que tu BD tenga
  `ON DELETE SET NULL`/`CASCADE` configurado a nivel de columna, que con
  `ddl-auto=update` típicamente Hibernate no fuerza). Si quieres, en el
  próximo ajuste agrego que al eliminar una inscripción se anule (o elimine)
  su factura asociada también.
