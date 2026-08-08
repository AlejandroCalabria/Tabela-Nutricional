package com.NexGen.nutriiftm.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Ponte entre o Firebase Authentication (que roda no navegador) e a sessão
 * do Spring Boot (que protege as páginas Thymeleaf do sistema).
 *
 * Fluxo:
 *  1. O navegador loga no Firebase (email/senha ou Google) e recebe um
 *     ID token (JWT).
 *  2. O navegador manda esse token pra cá, em POST /auth/sessao.
 *  3. Aqui a gente verifica o token com o Firebase Admin SDK — só cria
 *     sessão se o token for legítimo e não estiver expirado.
 *  4. Guardamos o uid/email na HttpSession. As demais páginas do sistema
 *     passam a confiar nessa sessão (via um interceptor/filtro),
 *     sem precisar validar token do Firebase de novo a cada clique.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/sessao")
    public ResponseEntity<Void> criarSessao(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.substring("Bearer ".length());

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

            HttpSession session = request.getSession(true); // cria se não existir
            session.setAttribute("uid", decoded.getUid());
            session.setAttribute("email", decoded.getEmail());
            session.setAttribute("nome", decoded.getName());

            log.info("Sessão criada para usuário {}", decoded.getEmail());
            return ResponseEntity.ok().build();

        } catch (FirebaseAuthException e) {
            log.warn("Token do Firebase inválido/expirado: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}