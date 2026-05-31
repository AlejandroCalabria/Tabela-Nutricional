package com.NexGen.nutriiftm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.NexGen.nutriiftm.model.Receita;
import com.NexGen.nutriiftm.model.ValoresNutricionais;
import com.NexGen.nutriiftm.service.ReceitaService;
import com.NexGen.nutriiftm.service.TACOService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/receita")
@RequiredArgsConstructor
public class ReceitaController {

    private final ReceitaService receitaService;
    private final TACOService tacoService;

    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("receita", new Receita());
        model.addAttribute("itensTACO", tacoService.buscarTodos());
        return "inserirReceita";
    }

    @PostMapping("/salvar")
    public String salvar(Receita receita, Model model) {
        Receita salva = receitaService.salvarComCalculo(receita);
        return "redirect:/receita/resultado/" + salva.getId();
    }

    @GetMapping("/resultado/{id}")
    public String resultado(@PathVariable Long id, Model model) {
        Receita receita = receitaService.buscarPorId(id);
        ValoresNutricionais valores = receitaService.calcularPreview(receita);
        model.addAttribute("receita", receita);
        model.addAttribute("valores", valores);
        return "salvarReceita";
    }
}