package com.NexGen.nutriiftm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o AuthInterceptor.
 *
 * Modelo de acesso do sistema: só o Painel Administrativo (/admin/**)
 * exige login. Todo o restante — cadastro/edição/remoção de produtos,
 * cooperativas, fabricantes, nutrientes, tabelas, calculadora, etc. —
 * é público, pensado para produtores/cooperativas usarem sem precisar
 * de conta.
 *
 * IMPORTANTE: sempre que você criar uma página nova que deva exigir
 * login, coloque-a sob "/admin/**" (ou adicione o padrão dela aqui em
 * addPathPatterns) — por padrão, tudo que não está listado aqui fica
 * público.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/admin/**");
    }
}