package com.varcal.cheermanager.Controller.Clubes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.OnboardingService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Progreso de puesta en marcha del club del usuario autenticado. */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("ver_onboarding")
    public OnboardingService.Estado estado() {
        return service.estado();
    }
}