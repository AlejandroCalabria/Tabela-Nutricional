package com.NexGen.nutriiftm.model;

import java.util.List;

/**
 * Resultado completo da Consulta de Ingrediente: identificação do item da
 * TBCA + energia + tabela de macronutrientes/sódio (com %VD) + tabela de
 * informações complementares (micronutrientes/vitaminas, sem %VD), sempre
 * em duas colunas — quantidade padrão (100g, base da TBCA) e quantidade
 * selecionada pelo usuário.
 *
 * Funcionalidade exclusivamente de consulta: nada aqui é persistido.
 */
public record IngredienteConsultaDTO(
        String codigo,
        String descricao,
        String categoria,
        double quantidadePadraoG,
        double quantidadeSelecionadaG,

        int energiaPadraoKcal,
        int energiaPadraoKj,
        int energiaSelecionadaKcal,
        int energiaSelecionadaKj,
        Integer vdEnergiaPadrao,
        Integer vdEnergiaSelecionada,

        List<NutrienteConsultaDTO> macronutrientes,
        List<NutrienteConsultaDTO> complementares
) {
}