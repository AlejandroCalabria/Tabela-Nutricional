package com.NexGen.nutriiftm.domain.nutricional;

/**
 * Enum com os IDs fixos dos nutrientes conforme nutricional.sql.
 *
 * Motivação (S-03 / R-01):
 *  Antes, CalculadoraController usava literais numéricos (1L, 4L, 5L...)
 *  diretamente no código. Qualquer reimportação do banco com IDs diferentes
 *  quebrava o mapeamento silenciosamente.
 *
 *  Com este enum:
 *  - Os IDs têm nomes semânticos
 *  - São rastreáveis via IDE (find usages)
 *  - Alterações têm um único ponto de mudança
 *  - O Javadoc documenta a relação com o banco
 *
 * ATENÇÃO: Os valores abaixo correspondem aos eleCodigoes definidos em
 * nutricional.sql. Alterá-los exige sincronizar o banco de dados.
 */
public enum NutrienteId {

    /** eleCodigo=1 — Carboidratos (g), VD base 300g */
    CARBOIDRATO(1L),

    /** eleCodigo=2 — Açúcares Totais (g), %VD sempre ** por norma ANVISA */
    ACUCARES_TOTAIS(2L),

    /** eleCodigo=4 — Proteínas (g), VD base 75g */
    PROTEINA(4L),

    /** eleCodigo=5 — Gorduras Totais (g), VD base 55g */
    GORDURAS_TOTAIS(5L),

    /** eleCodigo=6 — Gorduras Saturadas (g), VD base 22g */
    GORDURAS_SAT(6L),

    /**
     * eleCodigo=7 — Gorduras Trans (g), VD base 2g.
     * Valor declarado SEMPRE como 0 pois TBCA não fornece dados de trans.
     * %VD = "VD não estabelecido" por norma.
     */
    GORDURAS_TRANS(7L),

    /** eleCodigo=15 — Fibras Alimentares (g), VD base 25g */
    FIBRA(15L),

    /** eleCodigo=16 — Sódio (mg), VD base 2400mg */
    SODIO(16L);

    private final long eleCodigoValue;

    NutrienteId(long eleCodigoValue) {
        this.eleCodigoValue = eleCodigoValue;
    }

    /** Retorna o eleCodigo correspondente como Long (tipo usado pelo JPA). */
    public Long id() {
        return eleCodigoValue;
    }
}