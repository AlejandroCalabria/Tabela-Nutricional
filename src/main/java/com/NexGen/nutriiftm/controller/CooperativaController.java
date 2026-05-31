package com.NexGen.nutriiftm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.NexGen.nutriiftm.model.Cooperativa;
import com.NexGen.nutriiftm.service.CooperativaService;

import lombok.RequiredArgsConstructor;

/**
 * CORREÇÃO BUG #3:
 *   inserirCooperativa.html postava para /inserirCooperativa — rota inexistente.
 *   Adicionados:
 *     GET  /cooperativas/inserir  → formulário de inserção
 *     POST /cooperativas/salvar   → salva (insert ou update)
 *   A rota /cooperativas/deletar/{id} mantida mas renomeada internamente para
 *   seguir convenção REST (GET para ação destrutiva é aceitável aqui por simplicidade).
 */
@Controller
@RequestMapping("/cooperativas")
@RequiredArgsConstructor
public class CooperativaController {

    private final CooperativaService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cooperativas", service.listarTodos());
        return "cooperativas";
    }

    /** Formulário de inserção de nova cooperativa. */
    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("cooperativa", new Cooperativa());
        return "inserirCooperativa";
    }

    @GetMapping("/alterar/{id}")
    public String formAlterar(@PathVariable Long id, Model model) {
        model.addAttribute("cooperativa", service.buscarPorId(id));
        return "alterarCooperativa";
    }

    /** Salva insert ou update — o JPA distingue pelo ID presente ou não. */
    @PostMapping("/salvar")
    public String salvar(Cooperativa cooperativa) {
        service.salvar(cooperativa);
        return "redirect:/cooperativas";
    }

    @GetMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id) {
        service.deletar(id);
        return "redirect:/cooperativas";
    }
}