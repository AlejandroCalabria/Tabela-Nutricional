package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.domain.nutricional.ArredondamentoAnvisa;
import com.NexGen.nutriiftm.domain.nutricional.NutrienteId;
import com.NexGen.nutriiftm.model.*;
import com.NexGen.nutriiftm.repository.ElementoRepository;
import com.NexGen.nutriiftm.repository.TabNutElementoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service responsável pelo fluxo completo de cálculo e persistência de
 * tabelas nutricionais via Calculadora.
 *
 * Extraído de CalculadoraController para:
 *  - Garantir integridade transacional (R-04): Produto, TabelaNutricional e
 *    TabNutElementos são salvos em uma única transação. Falha parcial faz
 *    rollback de tudo.
 *  - Eliminar acesso direto a repositories no controller (P-08).
 *  - Separar responsabilidades: controller apenas lida com HTTP binding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculadoraService {

    private final TACOService              tacoService;
    private final MacronutrientesService   macroService;
    private final TabelaNutricionalService tabelaService;
    private final ProdutoService           produtoService;
    private final FabricanteService        fabricanteService;
    private final UnidadeMedidaService     unidadeService;
    private final ElementoRepository       elementoRepo;
    private final TabNutElementoRepository tneRepo;

    private static final int MAX_PORCOES = 9999;
    private static final int MIN_PORCOES = 1;

    /**
     * Executa o fluxo completo de salvar: resolve produto, calcula nutrientes
     * e persiste TabelaNutricional + TabNutElementos em uma única transação.
     *
     * @return TabelaNutricional salva (com ID gerado)
     * @throws IllegalArgumentException se ingredientes inválidos ou fabricante ausente para novo produto
     */
    @Transactional
    public TabelaNutricional salvar(
            Long    produtoId,
            String  novoNome,
            String  novoNomeFantasia,
            Long    novoFabricanteId,
            String  novoDataFabricacao,
            String  novoDataVencimento,
            double  novoPeso,
            String  novoRecomendacoes,
            Long    unidadeId,
            double  porcao,
            double  totalPorcoes,
            double  totalColheres,
            String  medidaCaseira,
            List<Map<String, Object>> ingredientes
    ) {
        int porcoesSanitizado = sanitizarPorcoes(totalPorcoes);

        String listaIngredientes = montarStringIngredientes(ingredientes);
        Produto produto = resolverProduto(
                produtoId, novoNome, novoNomeFantasia, novoFabricanteId,
                novoDataFabricacao, novoDataVencimento, novoPeso,
                novoRecomendacoes, listaIngredientes
        );

        Receita receita = montarReceita(ingredientes, porcao);
        // MacronutrientesService.calcular() lança IllegalArgumentException
        // se nenhum ingrediente for encontrado na TBCA — propaga para o controller
        ValoresNutricionais v = macroService.calcular(receita);

        TabelaNutricional tabela = new TabelaNutricional();
        tabela.setProduto(produto);
        tabela.setUnidadeMedida(unidadeService.buscarPorId(unidadeId));
        tabela.setTabPorcao(porcao);
        tabela.setTabTotalPorcao((double) porcoesSanitizado);
        tabela.setTabTotalColheres(totalColheres);
        if (medidaCaseira != null && !medidaCaseira.isBlank()) {
            tabela.setTabMedidaCaseira(medidaCaseira.trim());
        }
        // Valores calculados pelo backend — nunca vindos do rótulo editável
        tabela.setTabValorEnergeticoPorcao((double) v.getEnergiaPorcaoKcal());
        tabela.setTabValorEnergetico((double) v.getEnergia100kcal());
        tabela.setTabVD((double) v.getVDEnergia());
        tabela.setTabPorcaoPadrao(porcao);

        TabelaNutricional salva = tabelaService.salvar(tabela);
        salvarNutrientes(salva, v, porcao);

        return salva;
    }

    // ── Helpers — package-private para testabilidade ──────────────────────────

    /**
     * Monta uma Receita transiente a partir da lista de ingredientes.
     * TACOService.buscarPorNome() é chamado UMA ÚNICA VEZ por ingrediente (RI-04).
     */
    public Receita montarReceita(List<Map<String, Object>> ingredientes, double porcao) {
        Receita receita = new Receita();
        receita.setPorcaoG(porcao);
        List<IngredienteReceita> lista = new ArrayList<>();
        for (Map<String, Object> ing : ingredientes) {
            String nome = (String) ing.get("nome");
            double qtd  = ((Number) ing.get("quantidade")).doubleValue();
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

    /**
     * Persiste os TabNutElementos usando o enum NutrienteId como fonte de verdade
     * dos IDs, eliminando os "magic numbers" (S-03 / R-01).
     */
    void salvarNutrientes(TabelaNutricional tabela, ValoresNutricionais v, double porcao) {
        Map<Long, Double> nutrientesPorId = new LinkedHashMap<>();
        nutrientesPorId.put(NutrienteId.CARBOIDRATO.id(),     v.getCarboidratoPorcao());
        nutrientesPorId.put(NutrienteId.PROTEINA.id(),        v.getProteinaPorcao());
        nutrientesPorId.put(NutrienteId.GORDURAS_TOTAIS.id(), v.getLipideosPorcao());
        nutrientesPorId.put(NutrienteId.GORDURAS_SAT.id(),    v.getSaturadoPorcao());
        nutrientesPorId.put(NutrienteId.GORDURAS_TRANS.id(),  0.0);  // TBCA não fornece
        nutrientesPorId.put(NutrienteId.FIBRA.id(),           v.getFibraPorcao());
        nutrientesPorId.put(NutrienteId.SODIO.id(),           (double) v.getSodioPorcaoMg());

        Map<Long, Elemento> elementoPorId = new HashMap<>();
        elementoRepo.findAll().forEach(el -> elementoPorId.put(el.getEleCodigo(), el));

        List<TabNutElemento> tneList = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : nutrientesPorId.entrySet()) {
            Elemento elemento = elementoPorId.get(entry.getKey());
            if (elemento == null) {
                log.warn("Elemento com eleCodigo={} não encontrado no banco. " +
                         "Nutriente ignorado no rótulo.", entry.getKey());
                continue;
            }

            double valorPorcao = entry.getValue();

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

    private Produto resolverProduto(
            Long produtoId, String nome, String nomeFantasia,
            Long fabId, String dataFab, String dataVenc,
            double peso, String recomendacoes, String ingredientes
    ) {
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

    private int sanitizarPorcoes(double totalPorcoes) {
        return Math.min(MAX_PORCOES, Math.max(MIN_PORCOES, (int) Math.floor(totalPorcoes)));
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
}