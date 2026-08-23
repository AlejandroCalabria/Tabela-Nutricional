package com.NexGen.nutriiftm.model.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Um ponto de dado para os gráficos de barra do painel administrativo
 * (/admin/dashboard). Não é entidade JPA — apenas um DTO serializado
 * como JSON (via Jackson) para ser consumido pelo Chart.js no navegador.
 */
@Getter
@AllArgsConstructor
public class ChartEntry {
    private final String label;
    private final double valor;
}