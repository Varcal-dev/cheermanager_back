# Los 3 cabos sueltos — resueltos

Un solo archivo cambia: `InscripcionService.java` (reemplaza la versión que
te di en el paso anterior). `Factura.java`, `FacturaRepository.java` e
`InscripcionController.java` siguen siendo los mismos que ya tienes — no
hubo que tocarlos de nuevo.

| Archivo | Reemplaza a |
|---|---|
| `InscripcionService.java` | `Service/Org_dep/InscripcionService.java` |

---

## Cabo 1 — Descuento de convenio aplicado automáticamente

Ahora, al generar la factura (ya sea al crear la inscripción o al recalcularla
en el cabo 2), el sistema revisa si el deportista tiene `convenioId` y, si
tiene uno **vigente**, combina su descuento con el del plan.

**"Vigente" significa que se cumplen las 3 condiciones a la vez:**
- La fecha de inscripción cae dentro del rango `fechaInicio`/`fechaFin` del `Convenio`.
- El `Descuento` asociado al convenio tiene `activo = true`.
- La fecha de inscripción cae dentro del rango `fechaInicio`/`fechaFin` del `Descuento`.

Si cualquiera de las tres falla, simplemente no se aplica el descuento de
convenio (no es un error, la factura se genera igual solo con el descuento
del plan).

**Cómo se combinan los dos descuentos:** no se suman los porcentajes, se
aplican en cadena. Ejemplo con un plan de $100.000, descuento de plan 10% y
descuento de convenio 20%:

```
Valor base:                         $100.000
Después del descuento de plan:       $90.000   (100.000 x 0.90)
Después del descuento de convenio:   $72.000   (90.000 x 0.80)
```

No es $100.000 x (1 - 0.30) = $70.000. La diferencia es chica en este
ejemplo, pero es la forma correcta de combinar descuentos independientes —
evita que dos descuentos grandes (ej. 40% + 40%) terminen "regalando" el
100% o más, cosa que sí pasaría si simplemente se sumaran los porcentajes.

La `Factura` generada guarda el `Descuento` del convenio en su campo
`descuento` (el mismo campo que ya existía) y la `descripcion` queda con el
detalle de qué se aplicó, por ejemplo:

```
"Inscripción - Plan: Mensualidad Nivel 2 (descuento de plan 10%) + convenio 'Colegio San Juan' (20%)"
```

---

## Cabo 2 — Cambiar el plan de una inscripción recalcula su factura

`actualizarInscripcion` ahora detecta si el `planPago` cambió respecto al que
tenía la inscripción. Si cambió, busca la factura asociada y recalcula su
`total`, `descuento` y `descripcion` con el plan nuevo (aplicando de nuevo la
lógica del Cabo 1, por si el convenio cambió de vigencia entre tanto).

**Excepción importante que agregué:** si la factura **ya tiene algún pago
registrado en estado `Pagado`**, el cambio de plan se **rechaza** con un
error claro:

```
"No se puede cambiar el plan de pago: la factura FAC-2026-0001 ya tiene
pagos registrados. Anula o ajusta los pagos antes de cambiar el plan."
```

La razón: si alguien ya pagó $80.000 de una factura y el nuevo plan vale
$60.000, recalcular en silencio dejaría un saldo negativo sin sentido (o un
sobrepago no reconocido). Ese caso necesita una decisión humana (¿se hace
nota crédito? ¿se reembolsa? ¿se deja como abono a la siguiente
mensualidad?), no una regla automática.

---

## Cabo 3 — Qué pasa con la factura al eliminar una inscripción

`eliminarInscripcion` ahora revisa la factura asociada antes de borrar la
inscripción:

- **Si la factura no tiene ningún pago registrado** (ni siquiera uno
  `Pendiente`): se elimina junto con la inscripción. No hay nada que perder.
- **Si la factura ya tiene al menos un pago** (de cualquier estado): la
  factura **no se borra** — se desvincula de la inscripción (su campo
  `inscripcion` pasa a `null`) y se conserva junto con sus pagos. Así no se
  pierde historial financiero real solo porque alguien borró un registro de
  inscripción.

En ambos casos la inscripción sí se elimina al final.

---

## Qué validar de tu lado

1. **El orden de combinación de descuentos** (primero plan, después
   convenio) — si tu negocio espera el orden contrario, o un tope máximo
   combinado (ej. "nunca más de 30% de descuento total"), dime y lo ajusto.
2. **El bloqueo por pagos confirmados en el Cabo 2** — si prefieres que en
   vez de rechazar el cambio, se permita y simplemente quede un saldo
   pendiente o sobrepago visible (en lugar de impedir la operación), es una
   decisión de negocio que puedo cambiar fácilmente.
3. Sigue sin existir un endpoint para "transferir" o reasignar una factura
   huérfana (la que quedó con `inscripcion = null` tras el Cabo 3) — hoy
   solo queda visible vía `GET /api/facturas` o `GET /api/facturas/{id}`,
   sin un filtro específico de "facturas sin inscripción". Si lo necesitas
   para algún reporte, lo agregamos.
