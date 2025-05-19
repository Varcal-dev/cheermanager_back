package com.varcal.cheermanager.MongoDB.model;

public enum EstadoDocumento {
    ACTIVO("activo"),
    ARCHIVADO("archivado"),
    ELIMINADO("eliminado");
    
    private final String valor;
    
    EstadoDocumento(String valor) {
        this.valor = valor;
    }
    
    public String getValor() {
        return valor;
    }
    
    public static EstadoDocumento fromValor(String valor) {
        for (EstadoDocumento estado : EstadoDocumento.values()) {
            if (estado.getValor().equals(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado inválido: " + valor);
    }
}