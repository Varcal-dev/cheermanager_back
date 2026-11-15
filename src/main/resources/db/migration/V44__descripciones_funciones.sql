-- V44 · Descripciones de dos funciones del plan.
-- ROLES_PERSONALIZADOS y DASHBOARDS_AVANZADOS ya se hacen cumplir (FuncionesService.REGLAS): el texto decia
-- "(control pendiente)" y es lo que lee el cliente en la pantalla que le explica que su plan no la incluye.
UPDATE caracteristicas_saas
   SET descripcion = 'Crear, editar y eliminar roles del club y asignarles permisos, mas alla de los roles de la plantilla'
 WHERE codigo = 'ROLES_PERSONALIZADOS';

UPDATE caracteristicas_saas
   SET descripcion = 'Pestanas Financiero y Administracion del dashboard de Inicio: cartera, recaudo, tendencias'
 WHERE codigo = 'DASHBOARDS_AVANZADOS';
