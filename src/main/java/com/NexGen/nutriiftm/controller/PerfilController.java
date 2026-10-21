package com.NexGen.nutriiftm.controller;
import com.NexGen.nutriiftm.config.FirebaseWebConfig.FirebaseWeb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PerfilController{
    @GetMapping ("/perfil")
    public String perfil(){
        return "perfil";
    }
}
