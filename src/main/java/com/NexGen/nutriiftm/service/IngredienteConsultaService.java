package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.domain.nutricional.ArredondamentoAnvisa;
import com.NexGen.nutriiftm.domain.nutricional.NutricionalConstants;
import com.NexGen.nutriiftm.model.IngredienteConsultaDTO;
import com.NexGen.nutriiftm.model.ItemTACO;
import com.NexGen.nutriiftm.model.NutrienteConsultaDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Monta a visualização de "Consulta de Ingrediente": os valores nutricionais
 * de um item da TBCA (por definição, sempre por 100g) recalculados também
 * para a quantidade que o usuário escolheu consultar.
 *
 * Funcionalidade exclusivamente de consulta — nenhum dado é persistido.
 *
 * Convenções seguidas (mesmas do restante do sistema):
 *  - Arredondamento de energia/macronutrientes/sódio/%VD delegado 100% para
 *    {@link ArredondamentoAnvisa} (single source of truth, ver P.17.1/17.2
 *    da arquitetura do projeto).
 *  - VDs de referência vêm de {@link NutricionalConstants} (P.17.2).
 *  - Açúcares Totais não tem %VD estabelecido pela IN 75/2020 → vd = null
 *    (o front-end exibe "**"), mesmo tratamento dado ao rótulo da
 *    calculadora.
 *  - Micronutrientes/vitaminas da TBCA não possuem VD de referência
 *    cadastrado no sistema (não existe constante correspondente em
 *    NutricionalConstants) — por isso aparecem apenas como valor, na aba
 *    "Informações complementares", sem coluna de %VD.
 */
@Service
public class IngredienteConsultaService {

    /** A TBCA declara todos os valores nutricionais por 100g do alimento. */
    public static final double QUANTIDADE_PADRAO_G = 100.0;

    public IngredienteConsultaDTO montar(ItemTACO item, double quantidadeSelecionadaG) {
        double qtd = quantidadeSelecionadaG > 0 ? quantidadeSelecionadaG : QUANTIDADE_PADRAO_G;
        double fator = qtd / QUANTIDADE_PADRAO_G;

        int energiaPadraoKcal = ArredondamentoAnvisa.energia(item.getEnergia());
        int energiaPadraoKj = ArredondamentoAnvisa.energia(item.getEnergiaKj());
        int energiaSelKcal = ArredondamentoAnvisa.energia(item.getEnergia() * fator);
        int energiaSelKj = ArredondamentoAnvisa.energia(item.getEnergiaKj() * fator);

        Integer vdEnergiaPadrao = ArredondamentoAnvisa.percentualVD(item.getEnergia(), NutricionalConstants.VD_ENERGIA_KCAL);
        Integer vdEnergiaSel = ArredondamentoAnvisa.percentualVD(item.getEnergia() * fator, NutricionalConstants.VD_ENERGIA_KCAL);

        List<NutrienteConsultaDTO> macros = List.of(
                comVD("Carboidratos", "g", item.getCarboidrato(), fator, NutricionalConstants.VD_CARBOIDRATO_G),
                semVD("Açúcares Totais", "g", item.getAcucarTotal(), fator),
                comVD("Proteínas", "g", item.getProteina(), fator, NutricionalConstants.VD_PROTEINA_G),
                comVD("Gorduras Totais", "g", item.getLipideos(), fator, NutricionalConstants.VD_LIPIDEOS_G),
                comVD("Gorduras Saturadas", "g", item.getAcidoGraxoSaturado(), fator, NutricionalConstants.VD_SATURADO_G),
                comVD("Fibra Alimentar", "g", item.getFibra(), fator, NutricionalConstants.VD_FIBRA_G),
                comVDmg("Sódio", "mg", item.getSodio(), fator, NutricionalConstants.VD_SODIO_MG)
        );

        List<NutrienteConsultaDTO> complementares = List.of(
                micro("Gorduras Monoinsaturadas", "g", item.getAcidoGraxoMonoinsaturado(), fator),
                micro("Gorduras Poliinsaturadas", "g", item.getAcidoGraxoPoliinsaturado(), fator),
                micro("Colesterol", "mg", item.getColesterol(), fator),
                micro("Cálcio", "mg", item.getCalcio(), fator),
                micro("Ferro", "mg", item.getFerro(), fator),
                micro("Magnésio", "mg", item.getMagnesio(), fator),
                micro("Manganês", "mg", item.getManganes(), fator),
                micro("Fósforo", "mg", item.getFosforo(), fator),
                micro("Potássio", "mg", item.getPotassio(), fator),
                micro("Cobre", "mg", item.getCobre(), fator),
                micro("Zinco", "mg", item.getZinco(), fator),
                micro("Retinol (Vit. A)", "mcg", item.getRetinol(), fator),
                micro("Vitamina B1", "mg", item.getVitamB1(), fator),
                micro("Vitamina B2", "mg", item.getVitamB2(), fator),
                micro("Vitamina B6", "mg", item.getVitamB6(), fator),
                micro("Vitamina B12", "mcg", item.getVitamB12(), fator),
                micro("Vitamina C", "mg", item.getVitamC(), fator),
                micro("Vitamina D", "mcg", item.getVitamD(), fator),
                micro("Vitamina E", "mg", item.getVitamE(), fator),
                micro("Cinzas", "g", item.getCinzas(), fator),
                micro("Umidade", "%", item.getUmidade(), fator)
        );

        return new IngredienteConsultaDTO(
                item.getCodigo(),
                item.getDescricao(),
                item.getCategoria(),
                QUANTIDADE_PADRAO_G,
                qtd,
                energiaPadraoKcal,
                energiaPadraoKj,
                energiaSelKcal,
                energiaSelKj,
                vdEnergiaPadrao,
                vdEnergiaSel,
                macros,
                complementares
        );
    }

    // ── Macronutrientes em gramas, com %VD ──────────────────────────────────
    private NutrienteConsultaDTO comVD(String nome, String unidade, double valor100g, double fator, double vdBase) {
        double padrao = ArredondamentoAnvisa.gramas(valor100g);
        double selecionado = ArredondamentoAnvisa.gramas(valor100g * fator);
        int vdPadrao = ArredondamentoAnvisa.percentualVD(valor100g, vdBase);
        int vdSel = ArredondamentoAnvisa.percentualVD(valor100g * fator, vdBase);
        return new NutrienteConsultaDTO(nome, unidade, padrao, selecionado, vdPadrao, vdSel);
    }

    // ── Sódio (mg), com %VD ──────────────────────────────────────────────────
    private NutrienteConsultaDTO comVDmg(String nome, String unidade, double valor100g, double fator, double vdBase) {
        int padrao = ArredondamentoAnvisa.miligramas(valor100g);
        int selecionado = ArredondamentoAnvisa.miligramas(valor100g * fator);
        int vdPadrao = ArredondamentoAnvisa.percentualVD(valor100g, vdBase);
        int vdSel = ArredondamentoAnvisa.percentualVD(valor100g * fator, vdBase);
        return new NutrienteConsultaDTO(nome, unidade, padrao, selecionado, vdPadrao, vdSel);
    }

    // ── Nutriente em gramas sem %VD estabelecido (ex.: Açúcares Totais) ────
    private NutrienteConsultaDTO semVD(String nome, String unidade, double valor100g, double fator) {
        double padrao = ArredondamentoAnvisa.gramas(valor100g);
        double selecionado = ArredondamentoAnvisa.gramas(valor100g * fator);
        return new NutrienteConsultaDTO(nome, unidade, padrao, selecionado, null, null);
    }

    /**
     * Informação complementar (micronutriente/vitamina): a IN 75/2020 não
     * define %VD para esses itens no escopo deste sistema, então nunca tem
     * coluna de %VD. Arredondado com 2 casas decimais — 1 casa decimal
     * zeraria valores tipicamente pequenos de micronutrientes/vitaminas.
     */
    private NutrienteConsultaDTO micro(String nome, String unidade, double valor100g, double fator) {
        double padrao = arredondar2casas(valor100g);
        double selecionado = arredondar2casas(valor100g * fator);
        return new NutrienteConsultaDTO(nome, unidade, padrao, selecionado, null, null);
    }

    private static double arredondar2casas(double valor) {
        if (!Double.isFinite(valor)) return 0.0;
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}