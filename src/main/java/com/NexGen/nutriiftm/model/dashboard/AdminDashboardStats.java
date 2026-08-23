package com.NexGen.nutriiftm.model.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Estatísticas agregadas exibidas no painel administrativo
 * (/admin/dashboard) — cartões numéricos + dados para os gráficos de
 * barra. Montado por AdminDashboardService a partir dos dados já
 * cadastrados (produtos, fabricantes, cooperativas, tabelas nutricionais).
 */
@Getter
@Builder
public class AdminDashboardStats {

    private long totalProdutos;
    private long totalRotulos;
    private long totalFabricantes;
    private long totalCooperativas;

    /** Quantidade de produtos cadastrados por fabricante. */
    private List<ChartEntry> produtosPorFabricante;

    /** Quantidade de produtos cadastrados por cooperativa. */
    private List<ChartEntry> produtosPorCooperativa;

    /** Valor energético médio (kcal/porção) dos rótulos, por fabricante. */
    private List<ChartEntry> caloriasMediaPorFabricante;

    /** Distribuição dos rótulos por faixa de valor energético (kcal/porção). */
    private List<ChartEntry> distribuicaoCalorica;

    /** Quantidade de rótulos cadastrados por unidade de medida da porção. */
    private List<ChartEntry> rotulosPorUnidadeMedida;
}