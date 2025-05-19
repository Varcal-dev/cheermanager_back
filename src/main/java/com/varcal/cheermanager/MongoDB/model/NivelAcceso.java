package com.varcal.cheermanager.MongoDB.model;

public enum NivelAcceso {
    PUBLICO("público"),
    PRIVADO("privado"),
    RESTRINGIDO("restringido");
    
    private final String valor;
    
    NivelAcceso(String valor) {
        this.valor = valor;
    }
    
    public String getValor() {
        return valor;
    }
}