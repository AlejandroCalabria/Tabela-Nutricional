package com.NexGen.nutriiftm.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.NexGen.nutriiftm.model.Produto;
import com.NexGen.nutriiftm.service.FabricanteService;
import com.NexGen.nutriiftm.service.ProdutoService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * CORREÇÃO BUG #11:
 *   O binding automático de `name="fabricante"` com valor Long não funciona
 *   para Produto.fabricante (objeto). O controller agora recebe fabricanteId
 *   explicitamente e resolve o Fabricante via FabricanteService.
 *
 *   Formulários alterarProduto.html e inserirProduto.html devem usar:
 *     <select name="fabricanteId"> (não "fabricante")
 */
@Controller
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;
    private final FabricanteService fabricanteService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        return "produtos";
    }

    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("produto", new Produto());
        model.addAttribute("fabricantes", fabricanteService.listarTodos());
        return "inserirProduto";
    }

    @GetMapping("/alterar/{id}")
    public String formAlterar(@PathVariable Long id, Model model) {
        model.addAttribute("produto", produtoService.buscarPorId(id));
        model.addAttribute("fabricantes", fabricanteService.listarTodos());
        return "alterarProduto";
    }

    /**
     * Recebe fabricanteId como @RequestParam separado para binding correto.
     * O objeto Produto é recebido sem o fabricante (Spring não resolve Long → Fabricante).
     */
    @PostMapping("/salvar")
    public String salvar(
            Produto produto,
            @RequestParam(required = false) Long fabricanteId
    ) {
        if (fabricanteId != null && fabricanteId > 0) {
            produto.setFabricante(fabricanteService.buscarPorId(fabricanteId));
        }
        produtoService.salvar(produto);
        return "redirect:/produtos";
    }

    @GetMapping("/remover/{id}")
    public String formRemover(@PathVariable Long id, Model model) {
        model.addAttribute("produto", produtoService.buscarPorId(id));
        return "removerProduto";
    }

    @PostMapping("/remover/{id}")
    public String remover(@PathVariable Long id) {
        produtoService.deletar(id);
        return "redirect:/produtos";
    }
}