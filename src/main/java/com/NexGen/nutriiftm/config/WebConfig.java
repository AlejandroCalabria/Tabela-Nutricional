package com.NexGen.nutriiftm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o AuthInterceptor e define quais rotas ficam de fora da
 * exigência de login.
 *
 * IMPORTANTE: sempre que você criar uma página nova que deva ser pública
 * (ex: "esqueci minha senha", uma landing page, etc.), adicione o padrão
 * dela aqui em excludePathPatterns — senão ela fica bloqueada por padrão.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/cadastro",
                        "/recuperar-senha",
                        "/auth/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",

                        // Visualizar rótulo é uma funcionalidade pública — não exige
                        // login. Pensado para links diretos/QR code em embalagens de
                        // produtos, para que o consumidor final consiga ver a
                        // informação nutricional sem precisar de conta no sistema.
                        // Continua existindo apenas para rótulos já cadastrados por
                        // um administrador; cadastro/edição/remoção seguem protegidos.
                        "/tabela/visualizar/**",
                        "/tabela/imprimir/**",
                        "/tabela/gerar-pdf/**"
                );
    }
}