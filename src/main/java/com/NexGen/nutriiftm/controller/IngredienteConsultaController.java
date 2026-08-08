package com.NexGen.nutriiftm.controller;

import com.NexGen.nutriiftm.model.IngredienteConsultaDTO;
import com.NexGen.nutriiftm.model.ItemTACO;
import com.NexGen.nutriiftm.service.IngredienteConsultaService;
import com.NexGen.nutriiftm.service.TACOService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de Ingrediente — funcionalidade exclusivamente de visualização.
 *
 * Permite ao usuário procurar (com o mesmo autocomplete fuzzy usado na
 * Calculadora) um ingrediente da TBCA e ver sua tabela nutricional completa,
 * lado a lado, para a quantidade padrão da base (100g) e para a quantidade
 * que ele escolher consultar. Nada aqui é criado, alterado ou salvo no
 * banco — é somente leitura sobre os dados já carregados pelo TACOService.
 */
@Controller
@RequestMapping("/ingredientes")
@RequiredArgsConstructor
public class IngredienteConsultaController {

    private final TACOService tacoService;
    private final IngredienteConsultaService consultaService;

    // ── GET /ingredientes/consulta ────────────────────────────────────────────
    @GetMapping("/consulta")
    public String form() {
        return "consultaIngrediente";
    }

    /**
     * Lista leve dos itens da TBCA para alimentar o autocomplete fuzzy no
     * navegador — mesmo formato usado em /calculadora/taco-dados.
     */
    @GetMapping("/consulta/dados")
    @ResponseBody
    public List<Map<String, Object>> dados() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (ItemTACO item : tacoService.buscarTodos()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", item.getCodigo());
            m.put("descricao", item.getDescricao());
            m.put("categoria", item.getCategoria());
            m.put("energia", item.getEnergia());
            lista.add(m);
        }
        return lista;
    }

    /**
     * Tabela nutricional completa de um ingrediente específico, calculada
     * para a quantidade padrão (100g) e para a quantidade selecionada.
     *
     * @param codigo     código do item na TBCA (ItemTACO.codigo)
     * @param quantidade quantidade em gramas escolhida pelo usuário para
     *                   consulta; se ausente/≤0, assume 100g (igual ao padrão)
     */
    @GetMapping("/consulta/{codigo}")
    @ResponseBody
    public ResponseEntity<IngredienteConsultaDTO> valores(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "100") double quantidade
    ) {
        ItemTACO item = tacoService.buscarPorCodigo(codigo);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(consultaService.montar(item, quantidade));
    }
}