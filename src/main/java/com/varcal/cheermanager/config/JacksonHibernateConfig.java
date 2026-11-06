package com.varcal.cheermanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

/**
 * Con spring.jpa.open-in-view=false, devolver una entidad con colecciones LAZY
 * sin inicializar rompía la serializacion JSON ("no Session"). Este modulo las
 * serializa como null (sin cargarlas), que es lo que ya esperan los DTO/front.
 * Spring Boot registra automaticamente cualquier bean Module en el ObjectMapper.
 */
@Configuration
public class JacksonHibernateConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module m = new Hibernate6Module();
        m.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        return m;
    }
}
