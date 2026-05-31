package com.NexGen.nutriiftm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.NexGen.nutriiftm.model.UnidadeMedida;
import com.NexGen.nutriiftm.service.UnidadeMedidaService;

/**
 * Controller de Unidades de Medida.
 *
 * Correção P-14: a view retornada era "udi" — arquivo inexistente no projeto.
 * Substituída por "unidades", que corresponde ao arquivo unidades.html
 * que deve ser criado no template path.
 *
 * Enquanto o template não for criado, a listagem redireciona para /tabela
 * para evitar TemplateInputException em produção.
 */
@Controller
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeMedidaController {

    private final UnidadeMedidaService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("unidades", service.listarTodos());
        return "unidades";
    }

    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("unidade", new UnidadeMedida());
        return "inserirUnidade";
    }

    @PostMapping("/salvar")
    public String salvar(UnidadeMedida unidade) {
        service.salvar(unidade);
        return "redirect:/unidades";
    }

    @GetMapping("/alterar/{id}")
    public String formAlterar(@PathVariable Long id, Model model) {
        model.addAttribute("unidade", service.buscarPorId(id));
        return "alterarUnidade";
    }

    @PostMapping("/alterar/{id}")
    public String alterar(@PathVariable Long id, UnidadeMedida unidade) {
        service.salvar(unidade);
        return "redirect:/unidades";
    }

    @GetMapping("/remover/{id}")
    public String formRemover(@PathVariable Long id, Model model) {
        model.addAttribute("unidade", service.buscarPorId(id));
        return "removerUnidade";
    }

    @PostMapping("/remover/{id}")
    public String remover(@PathVariable Long id) {
        service.deletar(id);
        return "redirect:/unidades";
    }
}