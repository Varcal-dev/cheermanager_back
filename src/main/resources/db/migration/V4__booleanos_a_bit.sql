-- Flyway V4: los booleanos que V1 creo como TINYINT pasan a BIT(1), que es lo que Hibernate
-- (ddl-auto=validate) espera para los campos Boolean de las entidades.
-- No se edita V1 porque Flyway verifica su checksum.
ALTER TABLE `eventos`             MODIFY `tiene_resultados` BIT(1) NOT NULL DEFAULT b'0';
ALTER TABLE `planes_pago`         MODIFY `activo`           BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE `niveles_competencia` MODIFY `activo`           BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE `notificaciones`      MODIFY `vista`            BIT(1) NOT NULL DEFAULT b'0';
ALTER TABLE `descuentos`          MODIFY `activo`           BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE `usuarios`            MODIFY `activo`           BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE `usuarios`            MODIFY `bloqueado`        BIT(1) NOT NULL DEFAULT b'0';
