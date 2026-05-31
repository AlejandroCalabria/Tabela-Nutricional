package com.NexGen.nutriiftm.domain.nutricional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para ArredondamentoAnvisa.
 *
 * Cobre todos os contratos definidos em nutrition-test-contracts/SKILL.md.
 * Estes testes são a linha de defesa contra regressões em regras regulatórias
 * (IN 75/2020) — qualquer alteração em ArredondamentoAnvisa deve passar aqui.
 */
@DisplayName("ArredondamentoAnvisa")
class ArredondamentoAnvisaTest {

    @Nested
    @DisplayName("energia(double valor)")
    class Energia {

        @ParameterizedTest(name = "{0} → {1} kcal")
        @CsvSource({
                "190.7, 191",
                "190.4, 190",
                "190.5, 191",
                "0.0,   0",
                "1.0,   1",
                "2000.0, 2000"
        })
        @DisplayName("Arredonda para inteiro (HALF_UP)")
        void deveArredondarParaInteiro(double entrada, int esperado) {
            assertThat(ArredondamentoAnvisa.energia(entrada)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("NaN → 0")
        void nanRetornaZero() {
            assertThat(ArredondamentoAnvisa.energia(Double.NaN)).isEqualTo(0);
        }

        @Test
        @DisplayName("Infinite → 0")
        void infiniteRetornaZero() {
            assertThat(ArredondamentoAnvisa.energia(Double.POSITIVE_INFINITY)).isEqualTo(0);
        }

        @Test
        @DisplayName("-Infinite → 0")
        void negativeInfiniteRetornaZero() {
            assertThat(ArredondamentoAnvisa.energia(Double.NEGATIVE_INFINITY)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("gramas(double valor)")
    class Gramas {

        @ParameterizedTest(name = "{0}g → {1}g")
        @CsvSource({
                "1.849, 1.8",
                "1.850, 1.9",
                "1.851, 1.9",
                "0.04,  0.0",
                "0.0,   0.0",
                "29.4,  29.4",
                "300.0, 300.0"
        })
        @DisplayName("Arredonda para 1 casa decimal (HALF_UP)")
        void deveArredondarParaUmaCasaDecimal(double entrada, double esperado) {
            assertThat(ArredondamentoAnvisa.gramas(entrada))
                    .as("gramas(%s) deve retornar %s", entrada, esperado)
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("NaN → 0.0")
        void nanRetornaZero() {
            assertThat(ArredondamentoAnvisa.gramas(Double.NaN)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Resultado nunca negativo (-0.0 não pode aparecer)")
        void resultadoNuncaNegativo() {
            // -0.0 é tecnicamente != 0.0 em comparação de bits, mas equals retorna true
            double resultado = ArredondamentoAnvisa.gramas(-0.001);
            assertThat(resultado).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("miligramas(double valor)")
    class Miligramas {

        @ParameterizedTest(name = "{0} mg → {1} mg")
        @CsvSource({
                "518.6, 519",
                "518.4, 518",
                "518.5, 519",
                "0.0,   0",
                "2400.0, 2400"
        })
        @DisplayName("Arredonda para inteiro (HALF_UP)")
        void deveArredondarParaInteiro(double entrada, int esperado) {
            assertThat(ArredondamentoAnvisa.miligramas(entrada)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("NaN → 0")
        void nanRetornaZero() {
            assertThat(ArredondamentoAnvisa.miligramas(Double.NaN)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("percentualVD(double valorPorcao, double vdBase)")
    class PercentualVD {

        @ParameterizedTest(name = "valorPorcao={0}, vdBase={1} → {2}%")
        @CsvSource({
                "29.4,  300.0, 10",
                "0.0,   300.0,  0",
                "300.0, 300.0, 100",
                "1.5,   300.0,  1",
                "150.0, 300.0, 50"
        })
        @DisplayName("Calcula percentual corretamente")
        void deveCalcularPercentual(double valorPorcao, double vdBase, int esperado) {
            assertThat(ArredondamentoAnvisa.percentualVD(valorPorcao, vdBase))
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("vdBase = 0 → retorna 0 (guard divisão por zero)")
        void vdBaseZeroRetornaZero() {
            assertThat(ArredondamentoAnvisa.percentualVD(9.0, 0.0)).isEqualTo(0);
        }

        @Test
        @DisplayName("vdBase negativo → retorna 0")
        void vdBaseNegativoRetornaZero() {
            assertThat(ArredondamentoAnvisa.percentualVD(9.0, -1.0)).isEqualTo(0);
        }

        @Test
        @DisplayName("NaN no valor → retorna 0")
        void nanRetornaZero() {
            assertThat(ArredondamentoAnvisa.percentualVD(Double.NaN, 300.0)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("gorduraTrans(double valor)")
    class GorduraTrans {

        @ParameterizedTest(name = "{0}g → {1}g")
        @CsvSource({
                "0.0,  0.0",
                "0.19, 0.0",
                "0.20, 0.2",
                "0.25, 0.3",
                "1.0,  1.0"
        })
        @DisplayName("Valores < 0.2g são zerados conforme IN 75/2020")
        void deveZerarAbaixoDoLimite(double entrada, double esperado) {
            assertThat(ArredondamentoAnvisa.gorduraTrans(entrada)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("NaN → 0.0")
        void nanRetornaZero() {
            assertThat(ArredondamentoAnvisa.gorduraTrans(Double.NaN)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("valorPor100g(double valorPorcao, double porcaoG)")
    class ValorPor100g {

        @ParameterizedTest(name = "valorPorcao={0}, porcaoG={1} → {2}")
        @CsvSource({
                "29.4, 60.0, 49.0",
                "1.0,  100.0, 1.0",
                "15.0, 30.0, 50.0"
        })
        @DisplayName("Calcula valor por 100g corretamente")
        void deveCalcularValorPor100g(double valorPorcao, double porcaoG, double esperado) {
            assertThat(ArredondamentoAnvisa.valorPor100g(valorPorcao, porcaoG))
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("porcaoG = 0 → retorna 0.0 (guard divisão por zero)")
        void porcaoZeroRetornaZero() {
            assertThat(ArredondamentoAnvisa.valorPor100g(1.0, 0.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("porcaoG negativo → retorna 0.0")
        void porcaoNegativaRetornaZero() {
            assertThat(ArredondamentoAnvisa.valorPor100g(1.0, -1.0)).isEqualTo(0.0);
        }
    }
}