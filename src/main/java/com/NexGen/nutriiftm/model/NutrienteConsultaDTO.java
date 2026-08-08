package com.NexGen.nutriiftm.model;

/**
 * Representa uma linha de nutriente na tela de Consulta de Ingrediente.
 *
 * DTO de saída (não persistido) — usado exclusivamente para exibir, lado a
 * lado, o valor do nutriente na quantidade padrão da TBCA (100g) e na
 * quantidade escolhida pelo usuário na consulta.
 *
 * {@code vdPadrao} / {@code vdSelecionado} ficam {@code null} quando o
 * nutriente não possui %VD estabelecido pela IN 75/2020 (ex.: Açúcares
 * Totais) — o front-end exibe "**" nesse caso, seguindo o mesmo padrão
 * já usado no rótulo ANVISA da calculadora.
 */
public record NutrienteConsultaDTO(
        String nome,
        String unidade,
        double valorPadrao,
        double valorSelecionado,
        Integer vdPadrao,
        Integer vdSelecionado
) {
}