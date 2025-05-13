package com.varcal.cheermanager.Service.Auth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;

@Service
public class PermisoService {

    @Autowired
    private PermisoRepository permisoRepository;

    public Object listar() {
        return permisoRepository.findAll();
    }

    public Optional<Permiso> obtenerPorId(Integer id) {
        return permisoRepository.findById(id);
    }

    public Permiso guardar(Permiso permiso) {
        return permisoRepository.save(permiso);
    }

    public void eliminar(Integer id) {
        permisoRepository.deleteById(id);
    }

}
