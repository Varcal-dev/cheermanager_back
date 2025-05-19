package com.varcal.cheermanager.MongoDB.Repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.varcal.cheermanager.MongoDB.model.Documento;

import java.util.List;

public interface DocumentoRepositoryMongo extends MongoRepository<Documento, String> {
    
    List<Documento> findByEntidadOrigenAndEntidadId(String entidadOrigen, Integer entidadId);
    
    List<Documento> findByCargadoPor(Integer usuarioId);
    
    List<Documento> findByEstado(Integer estado);
    
    @Query("{'$text': {'$search': ?0}}")
    List<Documento> buscarPorTexto(String texto);
}