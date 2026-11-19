package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;

/**
 * Protege la vigencia del token (FRT-01): un JWT robado no debe servir horas. El front lo renueva mientras el
 * usuario trabaja (ver lib/api.ts), así que 60 min no afecta a quien está activo.
 */
class JwtUtilExpiracionTest {

    private static final long SESENTA_MIN_MS = 60L * 60 * 1000;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdC1zZWNyZXRvLXBhcmEtcHJ1ZWJhcy1kZS1jaGVlcm1hbmFnZXItbWluaW1vLTMyLWJ5dGVzLW9rLW9r");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", SESENTA_MIN_MS);
        jwtUtil.init();
    }

    @Test
    void elTokenExpiraSegunLaVigenciaConfigurada() {
        String token = jwtUtil.generateToken(1L, "a@b.co", "ADMIN", List.of("ver_deportistas"), 7);
        Claims c = jwtUtil.getClaims(token);
        long segundos = (c.getExpiration().getTime() - c.getIssuedAt().getTime()) / 1000;
        assertEquals(3600, segundos);
    }

    @Test
    void laVigenciaPorDefectoDeLaAplicacionEsDe60Minutos() throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("src/main/resources/application.properties")) {
            p.load(in);
        }
        assertEquals("${JWT_EXPIRATION_MS:3600000}", p.getProperty("jwt.expiration-ms"),
                "El default de jwt.expiration-ms debe ser 60 min (3600000 ms)");
    }
}
