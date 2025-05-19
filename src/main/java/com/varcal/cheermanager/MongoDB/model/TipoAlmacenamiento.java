package com.varcal.cheermanager.MongoDB.model;


public enum TipoAlmacenamiento {
    EMBEDDED("embedded"),
    GRIDFS("gridfs"),
    S3("s3"),
    LOCAL("local");
    
    private final String valor;
    
    TipoAlmacenamiento(String valor) {
        this.valor = valor;
    }
    
    public String getValor() {
        return valor;
    }
}
