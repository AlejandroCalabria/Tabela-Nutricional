package com.NexGen.nutriiftm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Bloqueia o acesso às páginas do sistema quando não existe uma sessão
 * válida (criada em AuthController após verificar o token do Firebase).
 *
 * Não confunda com autenticação em si — este interceptor NÃO valida
 * nada do Firebase, apenas confere se já existe uma sessão marcada como
 * logada. A validação do token acontece uma única vez, em
 * POST /auth/sessao.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        HttpSession session = request.getSession(false); // não cria sessão nova aqui
        boolean logado = session != null && session.getAttribute("uid") != null;

        if (logado) {
            return true;
        }

        response.sendRedirect("/login");
        return false;
    }
}