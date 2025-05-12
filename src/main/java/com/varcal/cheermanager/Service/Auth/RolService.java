package com.varcal.cheermanager.Service.Auth;

import java.util.List;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.repository.Auth.RolRepository;

@Service
public class RolService {
    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<Rol> listar() {
        return rolRepository.findAll();
    }
}