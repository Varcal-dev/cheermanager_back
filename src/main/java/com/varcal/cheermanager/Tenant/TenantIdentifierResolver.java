package com.varcal.cheermanager.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/** Le dice a Hibernate cual es el club de la sesion que se abre (ver TenantContext). */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Integer> {

    @Override
    public Integer resolveCurrentTenantIdentifier() {
        return TenantContext.get();
    }

    /**
     * true: si el club cambia mientras hay una sesion abierta, Hibernate lanza excepcion en vez de
     * mezclar datos de dos clubes en una misma sesion. Es mas seguro que silenciar el error.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}