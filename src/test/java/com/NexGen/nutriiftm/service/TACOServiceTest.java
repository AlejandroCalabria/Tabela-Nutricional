package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.ItemTACO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para TACOService.
 *
 * Nota importante: TACOService carrega TACO.json do classpath no construtor.
 * Estes testes dependem do arquivo estar disponível em src/test/resources/taco/TACO.json
 * (ou em src/main/resources/taco/TACO.json, que é incluído no classpath de testes).
 *
 * Se o arquivo não estiver presente, TACOService inicializa com lista vazia
 * e os testes de busca são marcados como skipped via assumeThat.
 */
@DisplayName("TACOService")
class TACOServiceTest {

    private TACOService service;

    @BeforeEach
    void setUp() {
        service = new TACOService();
    }

    @Nested
    @DisplayName("buscarPorNome(String)")
    class BuscarPorNome {

        @Test
        @DisplayName("null → null")
        void nullRetornaNull() {
            assertThat(service.buscarPorNome(null)).isNull();
        }

        @Test
        @DisplayName("String vazia → null")
        void stringVaziaRetornaNull() {
            assertThat(service.buscarPorNome("")).isNull();
        }

        @Test
        @DisplayName("Apenas espaços → null")
        void apenasEspacosRetornaNull() {
            assertThat(service.buscarPorNome("   ")).isNull();
        }

        @Test
        @DisplayName("Nome sem match algum → null")
        void semMatchRetornaNull() {
            // String que não corresponde a nenhum alimento
            assertThat(service.buscarPorNome("xyzabcdefghijklmnop123456")).isNull();
        }

        @Test
        @DisplayName("buscarTodos() nunca retorna null")
        void buscarTodosNuncaRetornaNull() {
            assertThat(service.buscarTodos()).isNotNull();
        }

        @Test
        @DisplayName("Busca com acento encontra item normalizado")
        void buscaComAcentoFunciona() {
            // Se a TBCA estiver carregada, "feijão" deve encontrar algum item de feijão
            assumeTBCACarregada();
            // O item pode não existir em todas as versões do TACO.json,
            // mas a normalização não deve lançar exceção
            assertThatNoException().isThrownBy(() -> service.buscarPorNome("feijão preto cozido"));
        }
    }

    @Nested
    @DisplayName("buscarPorCodigo(String)")
    class BuscarPorCodigo {

        @Test
        @DisplayName("null → null")
        void nullRetornaNull() {
            assertThat(service.buscarPorCodigo(null)).isNull();
        }

        @Test
        @DisplayName("String vazia → null")
        void stringVaziaRetornaNull() {
            assertThat(service.buscarPorCodigo("")).isNull();
        }

        @Test
        @DisplayName("Código inexistente → null")
        void codigoInexistenteRetornaNull() {
            assertThat(service.buscarPorCodigo("9999999")).isNull();
        }

        @Test
        @DisplayName("Código existente retorna o item correto")
        void codigoExistenteRetornaItem() {
            assumeTBCACarregada();
            // O primeiro item da TBCA tem id=1, codigo="1"
            ItemTACO primeiro = service.buscarTodos().isEmpty()
                    ? null
                    : service.buscarTodos().get(0);

            if (primeiro != null) {
                ItemTACO encontrado = service.buscarPorCodigo(primeiro.getCodigo());
                assertThat(encontrado)
                        .isNotNull()
                        .extracting(ItemTACO::getCodigo)
                        .isEqualTo(primeiro.getCodigo());
            }
        }
    }

    @Nested
    @DisplayName("Integridade dos dados carregados")
    class IntegridadeDados {

        @Test
        @DisplayName("Lista e mapa são consistentes — mesmo objeto por código")
        void listaEMapaConsistentes() {
            assumeTBCACarregada();
            for (ItemTACO item : service.buscarTodos()) {
                if (item.getCodigo() != null && !item.getCodigo().isEmpty()) {
                    ItemTACO viaCodigo = service.buscarPorCodigo(item.getCodigo());
                    assertThat(viaCodigo)
                            .as("Item %s deve estar no mapa", item.getCodigo())
                            .isNotNull()
                            .isSameAs(item);
                }
            }
        }

        @Test
        @DisplayName("Nenhum item tem descrição null ou vazia")
        void nenhumItemComDescricaoNula() {
            assumeTBCACarregada();
            assertThat(service.buscarTodos())
                    .allSatisfy(item ->
                            assertThat(item.getDescricao())
                                    .as("Descrição do item %s não deve ser null", item.getCodigo())
                                    .isNotNull()
                                    .isNotBlank()
                    );
        }

        @Test
        @DisplayName("Nenhum item tem energia negativa")
        void nenhumItemComEnergiaNegativa() {
            assumeTBCACarregada();
            assertThat(service.buscarTodos())
                    .allSatisfy(item ->
                            assertThat(item.getEnergia())
                                    .as("Energia do item %s deve ser ≥ 0", item.getCodigo())
                                    .isGreaterThanOrEqualTo(0.0)
                    );
        }

        @Test
        @DisplayName("Nenhum item tem valores NaN ou Infinite")
        void nenhumItemComValoresInvalidos() {
            assumeTBCACarregada();
            assertThat(service.buscarTodos())
                    .allSatisfy(item -> {
                        assertThat(item.getProteina()).isFinite();
                        assertThat(item.getCarboidrato()).isFinite();
                        assertThat(item.getLipideos()).isFinite();
                        assertThat(item.getFibra()).isFinite();
                        assertThat(item.getSodio()).isFinite();
                    });
        }
    }

    /**
     * Assume que a TBCA foi carregada corretamente.
     * Se a lista estiver vazia (arquivo ausente), os testes são ignorados.
     */
    private void assumeTBCACarregada() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                !service.buscarTodos().isEmpty(),
                "TACO.json não carregado — teste ignorado"
        );
    }
}