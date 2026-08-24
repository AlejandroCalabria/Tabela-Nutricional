package com.NexGen.nutriiftm.controller;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.NexGen.nutriiftm.model.Fabricante;
import com.NexGen.nutriiftm.service.CooperativaService;
import com.NexGen.nutriiftm.service.FabricanteService;

import lombok.RequiredArgsConstructor;
@Controller
@RequestMapping("/produtores")
@RequiredArgsConstructor
public class FabricanteController {

    private final FabricanteService fabricanteService;
    private final CooperativaService cooperativaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("fabricantes", fabricanteService.listarTodos());
        return "produtores";
    }

    @GetMapping("/inserir")
    public String formInserir(Model model) {
        model.addAttribute("fabricante", new Fabricante());
        model.addAttribute("cooperativas", cooperativaService.listarTodos());
        return "inserirProdutor";
    }

    @GetMapping("/alterar/{id}")
    public String formAlterar(@PathVariable Long id, Model model) {
        model.addAttribute("fabricante", fabricanteService.buscarPorId(id));
        model.addAttribute("cooperativas", cooperativaService.listarTodos());
        return "alterarProdutor";
    }

    @PostMapping("/salvar")
    public String salvar(Fabricante fabricante) {
        fabricanteService.salvar(fabricante);
        return "redirect:/produtores";
    }

    @GetMapping("/remover/{id}")
    public String formRemover(@PathVariable Long id, Model model) {
        model.addAttribute("fabricante", fabricanteService.buscarPorId(id));
        return "removerProdutor";
    }

    @PostMapping("/remover/{id}")
    public String remover(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fabricanteService.deletar(id);
        } catch (DataIntegrityViolationException e) {
            // O produtor/fabricante tem produtos cadastrados vinculados a ele —
            // o banco recusa a remoção por causa da FK. Em vez de estourar erro
            // 500, avisamos o usuário de forma clara.
            redirectAttributes.addFlashAttribute("erro",
                    "Não foi possível remover este produtor: ele possui produtos "
                    + "cadastrados vinculados a ele. Remova ou transfira esses produtos "
                    + "antes de excluir o produtor.");
        }
        return "redirect:/produtores";
    }
}