package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.IngredienteReceita;
import com.NexGen.nutriiftm.model.ItemTACO;
import com.NexGen.nutriiftm.model.Receita;
import com.NexGen.nutriiftm.model.ValoresNutricionais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para MacronutrientesService.
 *
 * Cobre os contratos definidos em nutrition-test-contracts/SKILL.md.
 * MacronutrientesService é o componente crítico de cálculo — qualquer
 * regressão aqui impacta diretamente a conformidade regulatória.
 *
 * Não usa Mockito/Spring — service é instanciado diretamente (POJO puro).
 */
@DisplayName("MacronutrientesService")
class MacronutrientesServiceTest {

    private MacronutrientesService service;

    @BeforeEach
    void setUp() {
        service = new MacronutrientesService();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemTACO itemTaco(double energia, double proteina, double carboidrato,
                               double lipideos, double saturado, double fibra, double sodio) {
        ItemTACO item = new ItemTACO();
        item.setId(1);
        item.setCodigo("1");
        item.setDescricao("Item Teste");
        item.setCategoria("Teste");
        item.setEnergia(energia);
        item.setProteina(proteina);
        item.setCarboidrato(carboidrato);
        item.setLipideos(lipideos);
        item.setAcidoGraxoSaturado(saturado);
        item.setFibra(fibra);
        item.setSodio(sodio);
        return item;
    }

    private Receita receitaCom(double qtdG, ItemTACO item, double porcaoG) {
        IngredienteReceita ir = new IngredienteReceita();
        ir.setNome("Teste");
        ir.setQuantidadeG(qtdG);
        ir.setItemTACO(item);

        Receita receita = new Receita();
        receita.setPorcaoG(porcaoG);
        receita.setIngredientes(new ArrayList<>(List.of(ir)));
        return receita;
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cálculo de Energia")
    class CalculoEnergia {

        @Test
        @DisplayName("Energia calculada via Atwater: proteína×4 + carb×4 + lip×9")
        void deveCalcularEnergiaViaAtwater() {
            // 100g de frango: proteína=31.5, carb=0.0, lip=3.6
            // Energia = (31.5×4) + (0.0×4) + (3.6×9) = 126 + 0 + 32.4 = 158.4 kcal
            ItemTACO frango = itemTaco(158.4, 31.5, 0.0, 3.6, 1.0, 0.0, 90.0);
            Receita receita = receitaCom(100.0, frango, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getEnergiaPorcaoKcal())
                    .as("Energia por porção calculada via Atwater")
                    .isEqualTo(158);
        }

        @Test
        @DisplayName("Fibra NÃO entra no cálculo de energia (IN 75/2020)")
        void fibraNaoDeveEntrarNaEnergia() {
            // 100g só de fibra — energia deve ser 0
            ItemTACO soFibra = itemTaco(0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0);
            Receita receita = receitaCom(100.0, soFibra, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getEnergiaPorcaoKcal())
                    .as("Energia deve ser 0 quando só há fibra")
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("kJ = round(kcal × 4.184)")
        void deveCalcularKjCorretamente() {
            ItemTACO item = itemTaco(190.0, 0.0, 47.5, 0.0, 0.0, 0.0, 0.0);
            Receita receita = receitaCom(100.0, item, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            // 190 kcal × 4.184 = 795.0 kJ
            assertThat(v.getEnergiaPorcaoKj())
                    .as("kJ deve ser round(190 × 4.184)")
                    .isEqualTo((int) Math.round(190.0 * 4.184));
        }
    }

    @Nested
    @DisplayName("Cálculo por Porção")
    class CalculoPorcao {

        @Test
        @DisplayName("Valores por porção proporcionais ao peso total")
        void valoresPorPorcaoSaoProporcionais() {
            // 200g de frango, porção de 100g → metade dos nutrientes totais
            ItemTACO frango = itemTaco(158.0, 31.5, 0.0, 3.6, 1.0, 0.0, 90.0);
            Receita receita = receitaCom(200.0, frango, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            // 200g total, porção 100g = 50% do total
            // Proteína total = 31.5 × 2 = 63g, por porção = 31.5g
            assertThat(v.getProteinaPorcao())
                    .as("Proteína por porção deve ser metade do total")
                    .isEqualTo(31.5);
        }

        @Test
        @DisplayName("Sódio retornado como inteiro (mg)")
        void sodioDeveSerInteiro() {
            ItemTACO item = itemTaco(100.0, 10.0, 10.0, 5.0, 2.0, 1.0, 518.6);
            Receita receita = receitaCom(100.0, item, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getSodioPorcaoMg())
                    .as("Sódio deve ser inteiro arredondado de 518.6 para 519")
                    .isEqualTo(519);
        }
    }

    @Nested
    @DisplayName("Cálculo de %VD")
    class CalculoVD {

        @Test
        @DisplayName("%VD carboidrato calculado corretamente (VD base 300g)")
        void vdCarboidratoCorreto() {
            // 29.4g de carb por porção → %VD = round(29.4/300×100) = round(9.8) = 10%
            ItemTACO item = itemTaco(100.0, 0.0, 29.4, 0.0, 0.0, 0.0, 0.0);
            Receita receita = receitaCom(100.0, item, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getVDCarboidrato())
                    .as("%VD de carboidrato deve ser 10%")
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("%VD energia calculado (VD base 2000 kcal)")
        void vdEnergiaCorreto() {
            // 200 kcal por porção → %VD = round(200/2000×100) = 10%
            ItemTACO item = itemTaco(0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0);
            Receita receita = receitaCom(100.0, item, 100.0);

            ValoresNutricionais v = service.calcular(receita);

            // Energia = 50.0 × 4.0 = 200 kcal → %VD = 10%
            assertThat(v.getVDEnergia())
                    .as("%VD energia deve ser 10%")
                    .isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Casos de Falha")
    class CasosFalha {

        @Test
        @DisplayName("Lança IllegalArgumentException quando todos os itemTACO são null")
        void deveLancarExcecaoSemIngredientesTBCA() {
            IngredienteReceita ir = new IngredienteReceita();
            ir.setNome("Ingrediente sem TBCA");
            ir.setQuantidadeG(100.0);
            ir.setItemTACO(null);

            Receita receita = new Receita();
            receita.setPorcaoG(100.0);
            receita.setIngredientes(List.of(ir));

            assertThatThrownBy(() -> service.calcular(receita))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TBCA");
        }

        @Test
        @DisplayName("Ingredientes com itemTACO null são ignorados silenciosamente")
        void ingredientesSemTBCAIgnorados() {
            ItemTACO item = itemTaco(100.0, 10.0, 20.0, 5.0, 1.0, 2.0, 100.0);

            IngredienteReceita irValido = new IngredienteReceita();
            irValido.setNome("Válido");
            irValido.setQuantidadeG(100.0);
            irValido.setItemTACO(item);

            IngredienteReceita irSemTBCA = new IngredienteReceita();
            irSemTBCA.setNome("Sem TBCA");
            irSemTBCA.setQuantidadeG(50.0);
            irSemTBCA.setItemTACO(null);

            Receita receita = new Receita();
            receita.setPorcaoG(100.0);
            receita.setIngredientes(List.of(irValido, irSemTBCA));

            // Não deve lançar exceção — apenas ignora o ingrediente sem TBCA
            assertThatNoException().isThrownBy(() -> service.calcular(receita));
        }
    }

    @Nested
    @DisplayName("Invariantes do ValoresNutricionais")
    class InvariantesValores {

        @Test
        @DisplayName("Nenhum campo contém NaN ou Infinite")
        void nenhumCampoNanOuInfinite() {
            ItemTACO item = itemTaco(158.0, 31.5, 0.5, 3.6, 1.0, 0.5, 90.0);
            Receita receita = receitaCom(100.0, item, 30.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getCarboidratoPorcao()).isFinite();
            assertThat(v.getProteinaPorcao()).isFinite();
            assertThat(v.getLipideosPorcao()).isFinite();
            assertThat(v.getSaturadoPorcao()).isFinite();
            assertThat(v.getFibraPorcao()).isFinite();
            assertThat((double) v.getSodioPorcaoMg()).isFinite();
            assertThat((double) v.getEnergiaPorcaoKcal()).isFinite();
        }

        @Test
        @DisplayName("pesoTotalG é a soma das quantidades dos ingredientes encontrados")
        void pesoTotalGCorreto() {
            ItemTACO item = itemTaco(100.0, 10.0, 10.0, 5.0, 2.0, 1.0, 100.0);
            Receita receita = receitaCom(150.0, item, 50.0);

            ValoresNutricionais v = service.calcular(receita);

            assertThat(v.getPesoTotalG())
                    .as("Peso total deve ser igual à quantidade do ingrediente")
                    .isEqualTo(150.0);
        }

        @Test
        @DisplayName("Valores por porção são proporcionais a valores por 100g")
        void valoresPorPorcaoProporcionaisA100g() {
            // 100g do item, porção 50g → valor por porção = valor por 100g / 2
            ItemTACO item = itemTaco(200.0, 20.0, 30.0, 10.0, 5.0, 3.0, 500.0);
            Receita receita = receitaCom(100.0, item, 50.0);

            ValoresNutricionais v = service.calcular(receita);

            // Carboidrato 100g = 30.0, por porção (50g) = 15.0
            assertThat(v.getCarboidratoPorcao())
                    .as("Carboidrato por porção = carboidrato 100g × (porcao/100)")
                    .isEqualTo(15.0);
        }
    }
}