package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.IngredienteConsultaDTO;
import com.NexGen.nutriiftm.model.ItemTACO;
import com.NexGen.nutriiftm.model.NutrienteConsultaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("IngredienteConsultaService")
class IngredienteConsultaServiceTest {

    private final IngredienteConsultaService service = new IngredienteConsultaService();

    /** Item TBCA fictício, valores redondos para facilitar a conferência manual. */
    private ItemTACO itemFicticio() {
        ItemTACO item = new ItemTACO();
        item.setId(1);
        item.setCodigo("1");
        item.setDescricao("Alimento de Teste");
        item.setCategoria("Categoria Teste");
        item.setEnergia(200.0);
        item.setEnergiaKj(200.0 * 4.184);
        item.setCarboidrato(30.0);
        item.setProteina(10.0);
        item.setLipideos(5.0);
        item.setAcidoGraxoSaturado(2.0);
        item.setFibra(4.0);
        item.setSodio(120.0);
        item.setAcucarTotal(0.0);
        item.setCalcio(50.0);
        item.setFerro(1.234);
        return item;
    }

    @Nested
    @DisplayName("montar(item, quantidade)")
    class Montar {

        @Test
        @DisplayName("Quantidade padrão (100g) → valor selecionado igual ao valor padrão")
        void quantidadeIgualA100_valoresIguais() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);

            assertThat(dto.quantidadePadraoG()).isEqualTo(100.0);
            assertThat(dto.quantidadeSelecionadaG()).isEqualTo(100.0);
            assertThat(dto.energiaPadraoKcal()).isEqualTo(dto.energiaSelecionadaKcal());

            NutrienteConsultaDTO carboidrato = buscarPorNome(dto, "Carboidratos");
            assertThat(carboidrato.valorPadrao()).isEqualTo(carboidrato.valorSelecionado());
        }

        @Test
        @DisplayName("Quantidade selecionada é proporcional ao valor padrão (fator linear)")
        void quantidadeProporcional() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 50.0);

            // 30g de carboidrato por 100g → 15g em 50g
            NutrienteConsultaDTO carboidrato = buscarPorNome(dto, "Carboidratos");
            assertThat(carboidrato.valorSelecionado()).isEqualTo(15.0);

            // 200 kcal por 100g → 100 kcal em 50g
            assertThat(dto.energiaSelecionadaKcal()).isEqualTo(100);
        }

        @Test
        @DisplayName("Quantidade selecionada maior que 100g escala corretamente")
        void quantidadeMaiorQue100() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 200.0);

            NutrienteConsultaDTO sodio = buscarPorNome(dto, "Sódio");
            assertThat(sodio.valorPadrao()).isEqualTo(120.0);
            assertThat(sodio.valorSelecionado()).isEqualTo(240.0);
        }

        @Test
        @DisplayName("Quantidade zero ou negativa cai para o padrão (100g)")
        void quantidadeInvalidaCaiParaPadrao() {
            IngredienteConsultaDTO dtoZero = service.montar(itemFicticio(), 0.0);
            IngredienteConsultaDTO dtoNegativo = service.montar(itemFicticio(), -10.0);

            assertThat(dtoZero.quantidadeSelecionadaG()).isEqualTo(100.0);
            assertThat(dtoNegativo.quantidadeSelecionadaG()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Identificação do ingrediente é preservada no DTO")
        void identificacaoPreservada() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);

            assertThat(dto.codigo()).isEqualTo("1");
            assertThat(dto.descricao()).isEqualTo("Alimento de Teste");
            assertThat(dto.categoria()).isEqualTo("Categoria Teste");
        }

        @Test
        @DisplayName("Açúcares Totais não tem %VD estabelecido (null em ambas as colunas)")
        void acucaresTotaisSemVD() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 150.0);

            NutrienteConsultaDTO acucares = buscarPorNome(dto, "Açúcares Totais");
            assertThat(acucares.vdPadrao()).isNull();
            assertThat(acucares.vdSelecionado()).isNull();
        }

        @Test
        @DisplayName("Macronutrientes com VD de referência têm %VD calculado (não nulo)")
        void macronutrientesComVDPreenchido() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);

            assertThat(buscarPorNome(dto, "Carboidratos").vdPadrao()).isNotNull();
            assertThat(buscarPorNome(dto, "Proteínas").vdPadrao()).isNotNull();
            assertThat(buscarPorNome(dto, "Gorduras Totais").vdPadrao()).isNotNull();
            assertThat(buscarPorNome(dto, "Gorduras Saturadas").vdPadrao()).isNotNull();
            assertThat(buscarPorNome(dto, "Fibra Alimentar").vdPadrao()).isNotNull();
            assertThat(buscarPorNome(dto, "Sódio").vdPadrao()).isNotNull();
        }

        @Test
        @DisplayName("Informações complementares nunca têm %VD (sempre null)")
        void complementaresNuncaTemVD() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);

            assertThat(dto.complementares())
                    .allSatisfy(n -> {
                        assertThat(n.vdPadrao()).isNull();
                        assertThat(n.vdSelecionado()).isNull();
                    });
        }

        @Test
        @DisplayName("Lista de macronutrientes não vem vazia nem nula")
        void listaMacronutrientesPreenchida() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);
            assertThat(dto.macronutrientes()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Lista de informações complementares não vem vazia nem nula")
        void listaComplementaresPreenchida() {
            IngredienteConsultaDTO dto = service.montar(itemFicticio(), 100.0);
            assertThat(dto.complementares()).isNotNull().isNotEmpty();
        }
    }

    private NutrienteConsultaDTO buscarPorNome(IngredienteConsultaDTO dto, String nome) {
        Optional<NutrienteConsultaDTO> encontrado = java.util.stream.Stream
                .concat(dto.macronutrientes().stream(), dto.complementares().stream())
                .filter(n -> n.nome().equals(nome))
                .findFirst();
        assertThat(encontrado).as("Nutriente '%s' deveria existir no resultado", nome).isPresent();
        return encontrado.get();
    }
}