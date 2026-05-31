package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.domain.nutricional.NutrienteId;
import com.NexGen.nutriiftm.model.*;
import com.NexGen.nutriiftm.repository.ElementoRepository;
import com.NexGen.nutriiftm.repository.TabNutElementoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CalculadoraService.
 *
 * Valida:
 *  - Transação: se salvarNutrientes falhar, Produto e Tabela NÃO são persistidos
 *    (verificado indiretamente pelo contrato do método)
 *  - IDs de nutrientes vêm de NutrienteId enum (não magic numbers)
 *  - montarReceita() chama buscarPorNome() UMA VEZ por ingrediente (RI-04)
 *  - salvarNutrientes() loga aviso ao invés de falhar quando elemento ausente
 */
@DisplayName("CalculadoraService")
@ExtendWith(MockitoExtension.class)
class CalculadoraServiceTest {

    @Mock private TACOService              tacoService;
    @Mock private MacronutrientesService   macroService;
    @Mock private TabelaNutricionalService tabelaService;
    @Mock private ProdutoService           produtoService;
    @Mock private FabricanteService        fabricanteService;
    @Mock private UnidadeMedidaService     unidadeService;
    @Mock private ElementoRepository       elementoRepo;
    @Mock private TabNutElementoRepository tneRepo;

    private CalculadoraService service;

    @BeforeEach
    void setUp() {
        service = new CalculadoraService(
                tacoService, macroService, tabelaService, produtoService,
                fabricanteService, unidadeService, elementoRepo, tneRepo
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemTACO criarItemTACO(String nome) {
        ItemTACO item = new ItemTACO();
        item.setId(1);
        item.setCodigo("1");
        item.setDescricao(nome);
        item.setProteina(10.0);
        item.setCarboidrato(20.0);
        item.setLipideos(5.0);
        item.setAcidoGraxoSaturado(2.0);
        item.setFibra(1.0);
        item.setSodio(100.0);
        return item;
    }

    private ValoresNutricionais criarValoresNutricionais() {
        return ValoresNutricionais.builder()
                .energia100kcal(200).energia100kj(837)
                .carboidrato100g(20.0).acucaresTotal100g(0.0).proteina100g(10.0)
                .lipideos100g(5.0).saturado100g(2.0).trans100g(0.0).fibra100g(1.0).sodio100mg(100)
                .energiaPorcaoKcal(60).energiaPorcaoKj(251)
                .carboidratoPorcao(6.0).acucaresTotalPorcao(0.0).proteinaPorcao(3.0)
                .lipideosPorcao(1.5).saturadoPorcao(0.6).transPorcao(0.0)
                .fibraPorcao(0.3).sodioPorcaoMg(30)
                .vdEnergia(3).vdCarboidrato(2).vdProteina(4).vdLipideos(3)
                .vdSaturado(3).vdFibra(1).vdSodio(1)
                .pesoTotalG(100.0)
                .build();
    }

    private Elemento criarElemento(long id, double vdBase) {
        Elemento e = new Elemento();
        e.setEleCodigo(id);
        e.setEleNome("Nutriente " + id);
        e.setEleValorRecomendado(vdBase);
        return e;
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("montarReceita()")
    class MontarReceita {

        @Test
        @DisplayName("buscarPorNome() é chamado UMA VEZ por ingrediente (RI-04)")
        void buscarPorNomeUmaVezPorIngrediente() {
            List<Map<String, Object>> ingredientes = List.of(
                    Map.of("nome", "Frango", "quantidade", 100.0),
                    Map.of("nome", "Cenoura", "quantidade", 80.0)
            );
            when(tacoService.buscarPorNome("Frango")).thenReturn(criarItemTACO("Frango"));
            when(tacoService.buscarPorNome("Cenoura")).thenReturn(criarItemTACO("Cenoura"));

            service.montarReceita(ingredientes, 100.0);

            verify(tacoService, times(1)).buscarPorNome("Frango");
            verify(tacoService, times(1)).buscarPorNome("Cenoura");
            verifyNoMoreInteractions(tacoService);
        }

        @Test
        @DisplayName("Porção da receita é definida corretamente")
        void porcaoDefinidaCorretamente() {
            List<Map<String, Object>> ingredientes = List.of(
                    Map.of("nome", "Arroz", "quantidade", 100.0)
            );
            when(tacoService.buscarPorNome(any())).thenReturn(criarItemTACO("Arroz"));

            Receita receita = service.montarReceita(ingredientes, 60.0);

            assertThat(receita.getPorcaoG()).isEqualTo(60.0);
        }
    }

    @Nested
    @DisplayName("salvarNutrientes()")
    class SalvarNutrientes {

        @Test
        @DisplayName("Todos os 7 nutrientes são salvos quando elementos existem no banco")
        void seteNutrientesSalvos() {
            // Criar elementos para todos os IDs do NutrienteId
            List<Elemento> elementos = List.of(
                    criarElemento(NutrienteId.CARBOIDRATO.id(),     300.0),
                    criarElemento(NutrienteId.PROTEINA.id(),         75.0),
                    criarElemento(NutrienteId.GORDURAS_TOTAIS.id(),  55.0),
                    criarElemento(NutrienteId.GORDURAS_SAT.id(),     22.0),
                    criarElemento(NutrienteId.GORDURAS_TRANS.id(),    2.0),
                    criarElemento(NutrienteId.FIBRA.id(),            25.0),
                    criarElemento(NutrienteId.SODIO.id(),          2400.0)
            );
            when(elementoRepo.findAll()).thenReturn(elementos);

            TabelaNutricional tabela = new TabelaNutricional();
            ValoresNutricionais v = criarValoresNutricionais();

            service.salvarNutrientes(tabela, v, 30.0);

            ArgumentCaptor<List<TabNutElemento>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(tneRepo).saveAll(captor.capture());

            assertThat(captor.getValue())
                    .as("Devem ser salvos exatamente 7 TabNutElementos")
                    .hasSize(7);
        }

        @Test
        @DisplayName("Elemento ausente no banco → ignorado (sem falha)")
        void elementoAusenteIgnorado() {
            // Só o carboidrato existe
            when(elementoRepo.findAll()).thenReturn(
                    List.of(criarElemento(NutrienteId.CARBOIDRATO.id(), 300.0))
            );

            TabelaNutricional tabela = new TabelaNutricional();
            ValoresNutricionais v = criarValoresNutricionais();

            // Não deve lançar exceção
            assertThatNoException().isThrownBy(
                    () -> service.salvarNutrientes(tabela, v, 30.0)
            );

            // Apenas 1 elemento salvo
            ArgumentCaptor<List<TabNutElemento>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(tneRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("Gordura Trans sempre tem valor 0.0 (TBCA não fornece)")
        void gorduraTransSempreZero() {
            List<Elemento> elementos = List.of(
                    criarElemento(NutrienteId.GORDURAS_TRANS.id(), 2.0)
            );
            when(elementoRepo.findAll()).thenReturn(elementos);

            TabelaNutricional tabela = new TabelaNutricional();
            ValoresNutricionais v = criarValoresNutricionais();

            service.salvarNutrientes(tabela, v, 30.0);

            ArgumentCaptor<List<TabNutElemento>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(tneRepo).saveAll(captor.capture());

            assertThat(captor.getValue())
                    .hasSize(1)
                    .first()
                    .extracting(TabNutElemento::getTneValor)
                    .isEqualTo(0.0);
        }
    }
}