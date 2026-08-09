package com.NexGen.nutriiftm.config;

import com.NexGen.nutriiftm.model.Usuario;
import com.NexGen.nutriiftm.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Deixa o atributo "usuarioLogado" disponível em TODO template Thymeleaf,
 * sem cada controller precisar adicioná-lo manualmente no Model.
 *
 * Vale para páginas públicas também (index, visualizar rótulo, etc) — é
 * assim que o nav sabe se deve mostrar "Entrar" ou o avatar da pessoa.
 * Se não tiver sessão, ou a sessão não tiver "uid", retorna null (o
 * Thymeleaf trata isso normalmente com th:if="${usuarioLogado != null}").
 */
@ControllerAdvice
@RequiredArgsConstructor
public class UsuarioLogadoAdvice {

    private final UsuarioRepository usuarioRepository;

    @ModelAttribute("usuarioLogado")
    public Usuario usuarioLogado(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        String uid = (String) session.getAttribute("uid");
        if (uid == null) {
            return null;
        }
        return usuarioRepository.findByFirebaseUid(uid).orElse(null);
    }
}