package com.NexGen.nutriiftm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o AuthInterceptor.
 *
 * O painel administrativo exige conta com papel de administrador. O
 * A edição do perfil e as ações sociais exigem sessão; perfis públicos
 * podem ser visualizados sem login. Os demais recursos seguem públicos
 * para produtores/cooperativas usarem sem conta.
 *
 * IMPORTANTE: sempre que você criar uma página nova que deva exigir
 * login, adicione o padrão dela em addPathPatterns; por padrão, tudo que
 * não está listado aqui fica público.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/admin",
                        "/admin/**",
                        "/perfil",
                        "/perfil/salvar",
                        "/perfil/foto",
                        "/perfil/seguir/**",
                        "/perfil/deixar-de-seguir/**"
                );
    }
}