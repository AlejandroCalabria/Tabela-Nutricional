package com.NexGen.nutriiftm.controller;

import com.NexGen.nutriiftm.domain.nutricional.ArredondamentoAnvisa;
import com.NexGen.nutriiftm.model.*;
import com.NexGen.nutriiftm.service.*;
import com.NexGen.nutriiftm.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Correções aplicadas:
 *  #2  — tneValorPadrao calculado via ArredondamentoAnvisa (único ponto, fallback correto).
 *  #3  — tneVD calculado via ArredondamentoAnvisa.percentualVD (sem -0.0 nem NaN).
 *  #7  — salvar() ignora campos editáveis do rótulo; usa valores calculados pelo backend.
 *  #10 — JSON parseado com Jackson em vez de regex caseiro.
 *  #13 — totalPorcoes limitado a MAX_PORCOES_EMBALAGEM.
 */
@Slf4j
@Controller
@RequestMapping("/calculadora")
@RequiredArgsConstructor
public class CalculadoraController {

    private final TACOService              tacoService;
    private final MacronutrientesService   macroService;
    private final TabelaNutricionalService tabelaService;
    private final ProdutoService           produtoService;
    private final FabricanteService        fabricanteService;
    private final UnidadeMedidaService     unidadeService;
    private final ElementoRepository       elementoRepo;
    private final TabNutElementoRepository tneRepo;
    private final ObjectMapper             objectMapper;  // injetado pelo Spring Boot

    private static final int MAX_PORCOES = 9999;

    // ── GET /calculadora ──────────────────────────────────────────────────────
    @GetMapping
    public String form(Model model) {
        model.addAttribute("produtos",    produtoService.listarTodos());
        model.addAttribute("fabricantes", fabricanteService.listarTodos());
        model.addAttribute("unidades",    unidadeService.listarTodos());
        return "calculadora";
    }

    // ── GET /calculadora/taco-dados ───────────────────────────────────────────
    @GetMapping("/taco-dados")
    @ResponseBody
    public List<Map<String, Object>> tacoDados() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (ItemTACO item : tacoService.buscarTodos()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo",    item.getCodigo());
            m.put("descricao", item.getDescricao());
            m.put("categoria", item.getCategoria());
            m.put("energia",   item.getEnergia());
            lista.add(m);
        }
        return lista;
    }

    // ── POST /calculadora/calcular ────────────────────────────────────────────
    @PostMapping("/calcular")
    public ResponseEntity<Map<String, Object>> calcular(
            @RequestBody List<Map<String, Object>> ingredientes,
            @RequestParam(defaultValue = "100") double porcao
    ) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum ingrediente informado."));
        }
        try {
            Receita receita = montarReceita(ingredientes, porcao);
            ValoresNutricionais v = macroService.calcular(receita);
            return ResponseEntity.ok(nutrientesParaMap(v, receita.getPesoTotalG()));
        } catch (IllegalArgumentException e) {
            log.warn("Cálculo rejeitado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── POST /calculadora/salvar ──────────────────────────────────────────────
    @PostMapping("/salvar")
    public String salvar(
            @RequestParam(required = false) Long    produtoId,
            @RequestParam(required = false) String  novoNome,
            @RequestParam(required = false) String  novoNomeFantasia,
            @RequestParam(required = false) Long    novoFabricanteId,
            @RequestParam(required = false) String  novoDataFabricacao,
            @RequestParam(required = false) String  novoDataVencimento,
            @RequestParam(required = false, defaultValue = "0") double novoPeso,
            @RequestParam(required = false) String  novoRecomendacoes,
            @RequestParam                   Long    unidadeId,
            @RequestParam                   double  porcao,
            @RequestParam                   double  totalPorcoes,
            @RequestParam(required = false, defaultValue = "0") double totalColheres,
            @RequestParam                   String  ingredientesJson,
            @RequestParam(required = false) String  medidaCaseira,
            Model model
    ) {
        // CORREÇÃO #10: Jackson em vez de regex caseiro
        List<Map<String, Object>> ingredientesLista = parsearIngredientesJson(ingredientesJson);
        if (ingredientesLista.isEmpty()) {
            model.addAttribute("erro", "Nenhum ingrediente válido informado.");
            return form(model);
        }

        // CORREÇÃO #13: limitar totalPorcoes
        int porcoesSanitizado = Math.min(MAX_PORCOES,
                Math.max(1, (int) Math.floor(totalPorcoes)));

        String listaIngredientes = montarStringIngredientes(ingredientesLista);
        Produto produto = resolverProduto(produtoId, novoNome, novoNomeFantasia,
                novoFabricanteId, novoDataFabricacao, novoDataVencimento,
                novoPeso, novoRecomendacoes, listaIngredientes);

        Receita receita = montarReceita(ingredientesLista, porcao);
        ValoresNutricionais v;
        try {
            v = macroService.calcular(receita);
        } catch (IllegalArgumentException e) {
            log.warn("Nenhum ingrediente TBCA encontrado ao salvar: {}", e.getMessage());
            model.addAttribute("erro", e.getMessage());
            return form(model);
        }

        TabelaNutricional tabela = new TabelaNutricional();
        tabela.setProduto(produto);
        tabela.setUnidadeMedida(unidadeService.buscarPorId(unidadeId));
        tabela.setTabPorcao(porcao);
        tabela.setTabTotalPorcao((double) porcoesSanitizado);
        tabela.setTabTotalColheres(totalColheres);
        if (medidaCaseira != null && !medidaCaseira.isBlank()) {
            tabela.setTabMedidaCaseira(medidaCaseira.trim());
        }
        // CORREÇÃO #7: valores calculados pelo backend, não capturados do rótulo editável
        tabela.setTabValorEnergeticoPorcao((double) v.getEnergiaPorcaoKcal());
        tabela.setTabValorEnergetico((double) v.getEnergia100kcal());
        tabela.setTabVD((double) v.getVDEnergia());
        tabela.setTabPorcaoPadrao(porcao);
        TabelaNutricional salva = tabelaService.salvar(tabela);

        salvarNutrientes(salva, v, porcao);

        return "redirect:/tabela";
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Produto resolverProduto(Long produtoId, String nome, String nomeFantasia,
            Long fabId, String dataFab, String dataVenc,
            double peso, String recomendacoes, String ingredientes) {
        if (produtoId != null && produtoId > 0) {
            return produtoService.buscarPorId(produtoId);
        }
        if (fabId == null || fabId <= 0) {
            throw new IllegalArgumentException("Fabricante obrigatório para novo produto.");
        }
        Produto p = new Produto();
        p.setProNome(nome);
        p.setProNomeFantasia(nomeFantasia);
        p.setFabricante(fabricanteService.buscarPorId(fabId));
        if (dataFab  != null && !dataFab.isBlank())  p.setProDataFabricacao(LocalDate.parse(dataFab));
        if (dataVenc != null && !dataVenc.isBlank())  p.setProDataVencimento(LocalDate.parse(dataVenc));
        p.setProPeso(peso);
        p.setProRecomendacoes(recomendacoes);
        p.setProIngredientes(ingredientes);
        return produtoService.salvar(p);
    }

    private void salvarNutrientes(TabelaNutricional tabela, ValoresNutricionais v, double porcao) {
        // IDs conforme nutricional.sql — nunca usar nomes textuais
        Map<Long, Double> nutrientesPorId = new LinkedHashMap<>();
        nutrientesPorId.put(1L,  v.getCarboidratoPorcao());
        nutrientesPorId.put(4L,  v.getProteinaPorcao());
        nutrientesPorId.put(5L,  v.getLipideosPorcao());
        nutrientesPorId.put(6L,  v.getSaturadoPorcao());
        nutrientesPorId.put(7L,  0.0);  // Gorduras Trans — sem dado TBCA
        nutrientesPorId.put(15L, v.getFibraPorcao());
        nutrientesPorId.put(16L, (double) v.getSodioPorcaoMg());

        Map<Long, Elemento> elementoPorId = new HashMap<>();
        elementoRepo.findAll().forEach(el -> elementoPorId.put(el.getEleCodigo(), el));

        List<TabNutElemento> tneList = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : nutrientesPorId.entrySet()) {
            Elemento elemento = elementoPorId.get(entry.getKey());
            if (elemento == null) continue;

            double valorPorcao = entry.getValue();

            // CORREÇÃO #2/#3: único lugar da verdade — sem duplicação de lógica
            TabNutElemento tne = new TabNutElemento();
            tne.setTabelaNutricional(tabela);
            tne.setElemento(elemento);
            tne.setTneValor(valorPorcao);
            tne.setTneValorPadrao(ArredondamentoAnvisa.valorPor100g(valorPorcao, porcao));
            tne.setTneVD(ArredondamentoAnvisa.percentualVD(valorPorcao, elemento.getEleValorRecomendado()));
            tneList.add(tne);
        }
        tneRepo.saveAll(tneList);
    }

    private Receita montarReceita(List<Map<String, Object>> ingredientes, double porcao) {
        Receita receita = new Receita();
        receita.setPorcaoG(porcao);
        List<IngredienteReceita> lista = new ArrayList<>();
        for (Map<String, Object> ing : ingredientes) {
            String nome = (String) ing.get("nome");
            double qtd  = ((Number) ing.get("quantidade")).doubleValue();
            // CORREÇÃO #6: busca por nome feita uma única vez aqui
            ItemTACO item = tacoService.buscarPorNome(nome);
            IngredienteReceita ir = new IngredienteReceita();
            ir.setNome(nome);
            ir.setQuantidadeG(qtd);
            ir.setItemTACO(item);
            lista.add(ir);
        }
        receita.setIngredientes(lista);
        return receita;
    }

    /** CORREÇÃO #10: Jackson em vez de regex */
    private List<Map<String, Object>> parsearIngredientesJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("JSON de ingredientes inválido: {}", json, e);
            return Collections.emptyList();
        }
    }

    private String montarStringIngredientes(List<Map<String, Object>> lista) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> ing : lista) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ing.get("nome"))
              .append(" (").append(ing.get("quantidade")).append("g)");
        }
        return sb.toString();
    }

    private Map<String, Object> nutrientesParaMap(ValoresNutricionais v, double pesoTotal) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pesoTotal",                 pesoTotal);
        // 100g
        m.put("energia100g",               v.getEnergia100kcal());
        m.put("energiaKj100g",             v.getEnergia100kj());
        m.put("carboidrato100g",           v.getCarboidrato100g());
        m.put("acucaresTotal100g",         v.getAcucaresTotal100g());
        m.put("acucaresAdicionados100g",   0.0);  // N.D.
        m.put("proteina100g",              v.getProteina100g());
        m.put("lipideos100g",              v.getLipideos100g());
        m.put("saturado100g",              v.getSaturado100g());
        m.put("fibra100g",                 v.getFibra100g());
        m.put("sodio100g",                 v.getSodio100mg());
        // Porção
        m.put("energiaPorcao",             v.getEnergiaPorcaoKcal());
        m.put("energiaKjPorcao",           v.getEnergiaPorcaoKj());
        m.put("carboidratoPorcao",         v.getCarboidratoPorcao());
        m.put("acucaresTotalPorcao",       v.getAcucaresTotalPorcao());
        m.put("acucaresAdicionadosPorcao", 0.0);  // N.D.
        m.put("proteinaPorcao",            v.getProteinaPorcao());
        m.put("lipideosPorcao",            v.getLipideosPorcao());
        m.put("saturadoPorcao",            v.getSaturadoPorcao());
        m.put("fibraPorcao",               v.getFibraPorcao());
        m.put("sodioPorcao",               v.getSodioPorcaoMg());
        // %VD
        m.put("vdEnergia",                 v.getVDEnergia());
        m.put("vdCarb",                    v.getVDCarboidrato());
        m.put("vdProt",                    v.getVDProteina());
        m.put("vdLip",                     v.getVDLipideos());
        m.put("vdSat",                     v.getVDSaturado());
        m.put("vdFibra",                   v.getVDFibra());
        m.put("vdSodio",                   v.getVDSodio());
        return m;
    }
}