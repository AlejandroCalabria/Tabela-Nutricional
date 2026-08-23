package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.Produto;
import com.NexGen.nutriiftm.model.TabelaNutricional;
import com.NexGen.nutriiftm.model.dashboard.AdminDashboardStats;
import com.NexGen.nutriiftm.model.dashboard.ChartEntry;
import com.NexGen.nutriiftm.repository.CooperativaRepository;
import com.NexGen.nutriiftm.repository.FabricanteRepository;
import com.NexGen.nutriiftm.repository.ProdutoRepository;
import com.NexGen.nutriiftm.repository.TabelaNutricionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Monta as estatísticas do painel administrativo (/admin/dashboard).
 *
 * Não existem colunas de data de criação em `produto`/`tabelanutricional`
 * no schema atual, então as séries são agregadas sobre dados que já
 * existem (fabricante, cooperativa, unidade de medida, valor energético)
 * em vez de uma linha do tempo. Os conjuntos de dados de um sistema para
 * pequenos produtores tendem a ser pequenos, então a agregação é feita
 * em memória com streams — sem necessidade de queries nativas/agregações
 * no banco.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final ProdutoRepository produtoRepository;
    private final FabricanteRepository fabricanteRepository;
    private final CooperativaRepository cooperativaRepository;
    private final TabelaNutricionalRepository tabelaNutricionalRepository;

    public AdminDashboardStats montarEstatisticas() {
        List<Produto> produtos = produtoRepository.findAll();
        List<TabelaNutricional> tabelas = tabelaNutricionalRepository.findAllComElementos();

        return AdminDashboardStats.builder()
                .totalProdutos(produtos.size())
                .totalRotulos(tabelas.size())
                .totalFabricantes(fabricanteRepository.count())
                .totalCooperativas(cooperativaRepository.count())
                .produtosPorFabricante(produtosPorFabricante(produtos))
                .produtosPorCooperativa(produtosPorCooperativa(produtos))
                .caloriasMediaPorFabricante(caloriasMediaPorFabricante(tabelas))
                .distribuicaoCalorica(distribuicaoCalorica(tabelas))
                .rotulosPorUnidadeMedida(rotulosPorUnidadeMedida(tabelas))
                .build();
    }

    private List<ChartEntry> produtosPorFabricante(List<Produto> produtos) {
        return produtos.stream()
                .filter(p -> p.getFabricante() != null)
                .collect(Collectors.groupingBy(p -> p.getFabricante().getFabNome(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChartEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartEntry::getValor).reversed())
                .collect(Collectors.toList());
    }

    private List<ChartEntry> produtosPorCooperativa(List<Produto> produtos) {
        return produtos.stream()
                .filter(p -> p.getFabricante() != null && p.getFabricante().getCooperativa() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getFabricante().getCooperativa().getCooNome(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChartEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartEntry::getValor).reversed())
                .collect(Collectors.toList());
    }

    private List<ChartEntry> caloriasMediaPorFabricante(List<TabelaNutricional> tabelas) {
        return tabelas.stream()
                .filter(t -> t.getProduto() != null
                        && t.getProduto().getFabricante() != null
                        && t.getTabValorEnergeticoPorcao() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getProduto().getFabricante().getFabNome(),
                        Collectors.averagingDouble(TabelaNutricional::getTabValorEnergeticoPorcao)))
                .entrySet().stream()
                .map(e -> new ChartEntry(e.getKey(), Math.round(e.getValue() * 10.0) / 10.0))
                .sorted(Comparator.comparingDouble(ChartEntry::getValor).reversed())
                .collect(Collectors.toList());
    }

    /** Agrupa os rótulos em faixas de valor energético por porção (kcal). */
    private List<ChartEntry> distribuicaoCalorica(List<TabelaNutricional> tabelas) {
        String[] faixas = {"0–100", "101–200", "201–300", "301–400", "401–500", "500+"};
        long[] contagem = new long[faixas.length];

        for (TabelaNutricional t : tabelas) {
            Double energia = t.getTabValorEnergeticoPorcao();
            if (energia == null) {
                continue;
            }
            int idx;
            if (energia <= 100) idx = 0;
            else if (energia <= 200) idx = 1;
            else if (energia <= 300) idx = 2;
            else if (energia <= 400) idx = 3;
            else if (energia <= 500) idx = 4;
            else idx = 5;
            contagem[idx]++;
        }

        List<ChartEntry> resultado = new ArrayList<>();
        for (int i = 0; i < faixas.length; i++) {
            resultado.add(new ChartEntry(faixas[i] + " kcal", contagem[i]));
        }
        return resultado;
    }

    private List<ChartEntry> rotulosPorUnidadeMedida(List<TabelaNutricional> tabelas) {
        return tabelas.stream()
                .filter(t -> t.getUnidadeMedida() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getUnidadeMedida().getUndNome(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChartEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartEntry::getValor).reversed())
                .collect(Collectors.toList());
    }
}