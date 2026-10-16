package com.varcal.cheermanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Usa el perfil "test" (H2 en memoria, sin secretos reales): no requiere MySQL ni variables de entorno.
@SpringBootTest
@ActiveProfiles("test")
class CheermanagerApplicationTests {

	@Test
	void contextLoads() {
	}
}
