package com.varcal.cheermanager.Service.Persona;

import java.util.List;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;

@Service
public class GeneroService {
    private final GeneroRepository generoRepository;

    public GeneroService(GeneroRepository generoRepository) {
        this.generoRepository = generoRepository;
    }

    public List<Genero> listar() {
        return generoRepository.findAll();
    }
}
