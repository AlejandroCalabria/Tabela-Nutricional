package com.NexGen.nutriiftm.controller;

import com.NexGen.nutriiftm.config.FirebaseWebConfig.FirebaseWeb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final FirebaseWeb firebaseWeb;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("firebaseWeb", firebaseWeb);
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("firebaseWeb", firebaseWeb);
        return "cadastro";
    }

    @GetMapping("/recuperar-senha")
    public String recuperarSenha(Model model) {
        model.addAttribute("firebaseWeb", firebaseWeb);
        return "recuperarsenha";
    }
}