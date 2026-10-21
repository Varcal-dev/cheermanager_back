package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.Service.Auth.PasswordResetNotifier;
import com.varcal.cheermanager.Service.Auth.UsuarioService;
import com.varcal.cheermanager.repository.Auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UsuarioService usuarioService;
    @Mock JwtUtil jwtUtil;
    @Mock PasswordResetNotifier notifier;
    @InjectMocks AuthService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxLoginAttempts", 3);
        ReflectionTestUtils.setField(service, "lockDurationMinutes", 15);
        ReflectionTestUtils.setField(service, "resetPasswordTokenExpirationMinutes", 15);
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("a@b.com");
        usuario.setUsername("a");
        usuario.setPasswordHash(BCrypt.hashpw("Correcta123", BCrypt.gensalt(4)));
    }

    @Test
    void emailInexistenteYClaveIncorrectaDanElMismoError() {
        when(userRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));
        assertEquals("invalid_credentials", service.authenticate("no@existe.com", "x").getErrorCode());
        assertEquals("invalid_credentials", service.authenticate("a@b.com", "mala").getErrorCode());
    }

    @Test
    void bloqueaTrasLosIntentosMaximosYNoRevelaEstadoConClaveMala() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 3; i++) service.authenticate("a@b.com", "mala");
        assertTrue(usuario.getBloqueado());
        assertNotNull(usuario.getBloqueadoHasta());
        // clave mala + cuenta bloqueada => sigue siendo invalid_credentials (sin enumeracion)
        assertEquals("invalid_credentials", service.authenticate("a@b.com", "mala").getErrorCode());
        // clave correcta + bloqueada => account_locked
        assertEquals("account_locked", service.authenticate("a@b.com", "Correcta123").getErrorCode());
    }

    @Test
    void elBloqueoExpiraSolo() {
        usuario.setBloqueado(true);
        usuario.setIntentosFallidos(3);
        usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));
        when(usuarioService.obtenerPermisos(1)).thenReturn(Set.of("ver_deportista"));
        when(jwtUtil.generateToken(any(), any(), any(), any(), any())).thenReturn("tok");
        AuthService.LoginResult r = service.authenticate("a@b.com", "Correcta123");
        assertTrue(r.isSuccess());
        assertFalse(usuario.getBloqueado());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void forgotPasswordNoDevuelveTokenYGuardaSoloElHash() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));
        service.forgotPassword("a@b.com");
        org.mockito.ArgumentCaptor<String> tok = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notifier).enviar(eq("a@b.com"), eq("a"), tok.capture());
        assertNotEquals(tok.getValue(), usuario.getTokenResetPassword()); // se guarda el hash, no el token
        assertEquals(64, usuario.getTokenResetPassword().length());      // SHA-256 hex
    }

    @Test
    void forgotPasswordConEmailInexistenteNoHaceNadaNiFalla() {
        when(userRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.forgotPassword("no@existe.com"));
        verifyNoInteractions(notifier);
    }

    @Test
    void resetRechazaClaveDebilYTokenInvalido() {
        assertEquals("weak_password", service.resetPassword("t", "123").getErrorCode());
        when(userRepository.findByTokenResetPassword(any())).thenReturn(Optional.empty());
        assertEquals("token_invalid", service.resetPassword("t", "ClaveNueva123").getErrorCode());
    }
}