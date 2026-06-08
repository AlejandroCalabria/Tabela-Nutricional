package com.NexGen.nutriiftm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.NexGen.nutriiftm.model.Elemento;
import com.NexGen.nutriiftm.service.ElementoService;

@Controller
@RequestMapping("/nutrientes")
@RequiredArgsConstructor
public class ElementoController {

    private final ElementoService elementoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("elementos", elementoService.listarTodos());
        return "nutrientes";
    }

    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("elemento", new Elemento());
        return "inserirNutriente";
    }

    @GetMapping("/alterar/{id}")
    public String formAlterar(@PathVariable Long id, Model model) {
        model.addAttribute("elemento", elementoService.buscarPorId(id));
        return "alterarNutriente";
    }

    @PostMapping("/salvar")
    public String salvar(Elemento elemento, Model model) {

        boolean ordemExiste;

        // ALTERAÇÃO
        if (elemento.getEleCodigo() != null) {

            ordemExiste = elementoService
                    .existeOrdemEmOutroElemento(
                            elemento.getEleOrdem(),
                            elemento.getEleCodigo());

        } else {

            // INSERÇÃO
            ordemExiste = elementoService
                    .existeOrdem(elemento.getEleOrdem());
        }

        if (ordemExiste) {

            model.addAttribute(
                    "erro",
                    "Já existe um nutriente com essa ordem.");

            model.addAttribute("elemento", elemento);

            if (elemento.getEleCodigo() != null) {
                return "alterarNutriente";
            }

            return "inserirNutriente";
        }

        elementoService.salvar(elemento);

        return "redirect:/nutrientes";
    }

    @GetMapping("/remover/{id}")
    public String formRemover(@PathVariable Long id, Model model) {
        model.addAttribute("elemento", elementoService.buscarPorId(id));
        return "removerNutriente";
    }

    @PostMapping("/remover/{id}")
    public String remover(@PathVariable Long id) {
        elementoService.deletar(id);
        return "redirect:/nutrientes";
    }
}