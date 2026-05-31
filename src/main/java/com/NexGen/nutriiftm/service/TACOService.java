package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.ItemTACO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Service de acesso à TBCA (Tabela Brasileira de Composição de Alimentos).
 *
 * Mudanças em relação à versão anterior:
 *  - P-02/S-04: Parser JSON artesanal substituído por Jackson ObjectMapper.
 *    Elimina ~250 linhas de código frágil e resolve problemas com
 *    caracteres especiais, strings com vírgulas e campos ausentes.
 *  - P-09: Falha no carregamento do TACO.json agora loga erro estruturado
 *    via SLF4J (não System.err) e não propaga exceção (comportamento intencional
 *    documentado em CB-01 dos contratos).
 *  - Mapa por código mantido para lookup O(1) via buscarPorCodigo().
 *  - Lógica de busca fuzzy preservada integralmente.
 */
@Slf4j
@Service
public class TACOService {

    private final List<ItemTACO>         itens          = new ArrayList<>();
    private final Map<String, ItemTACO>  itensPorCodigo = new HashMap<>();

    /**
     * DTO interno para desserialização Jackson do TACO.json.
     * @JsonIgnoreProperties(ignoreUnknown = true) garante que campos extras
     * no JSON não causem falha — apenas os campos mapeados são lidos.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ItemTacoJson {
        @JsonProperty("id")              private Integer id;
        @JsonProperty("description")     private String  description;
        @JsonProperty("category")        private String  category;
        @JsonProperty("humidity_percents") private Double humidity;
        @JsonProperty("energy_kcal")     private Double  energyKcal;
        @JsonProperty("protein_g")       private Double  proteinG;
        @JsonProperty("lipid_g")         private Double  lipidG;
        @JsonProperty("cholesterol_mg")  private Double  cholesterolMg;
        @JsonProperty("carbohydrate_g")  private Double  carbohydrateG;
        @JsonProperty("fiber_g")         private Double  fiberG;
        @JsonProperty("calcium_mg")      private Double  calciumMg;
        @JsonProperty("sodium_mg")       private Double  sodiumMg;
        @JsonProperty("magnesium_mg")    private Double  magnesiumMg;
        @JsonProperty("manganese_mg")    private Double  manganeseMg;
        @JsonProperty("phosphorus_mg")   private Double  phosphorusMg;
        @JsonProperty("iron_mg")         private Double  ironMg;
        @JsonProperty("potassium_mg")    private Double  potassiumMg;
        @JsonProperty("copper_mg")       private Double  copperMg;
        @JsonProperty("zinc_mg")         private Double  zincMg;
        @JsonProperty("retinol_mcg")     private Double  retinolMcg;
        @JsonProperty("thiamine_mg")     private Double  thiamineMg;
        @JsonProperty("riboflavin_mg")   private Double  riboflavinMg;
        @JsonProperty("pyridoxine_mg")   private Double  pyridoxineMg;
        @JsonProperty("cobalamin_mcg")   private Double  cobalaminMcg;
        @JsonProperty("vitaminC_mg")     private Double  vitaminCMg;
        @JsonProperty("vitaminD_mcg")    private Double  vitaminDMcg;
        @JsonProperty("vitaminE_mg")     private Double  vitaminEMg;
        @JsonProperty("saturated_g")     private Double  saturatedG;
        @JsonProperty("monounsaturated_g") private Double monounsaturatedG;
        @JsonProperty("polyunsaturated_g") private Double polyunsaturatedG;
        @JsonProperty("ash_g")           private Double  ashG;
    }

    public TACOService() {
        carregarDados();
    }

    /** Retorna o item pelo código exato da TBCA. O(1). */
    public ItemTACO buscarPorCodigo(String codigo) {
        if (codigo == null) return null;
        return itensPorCodigo.get(codigo);
    }

    /** Retorna todos os itens da TBCA. Nunca null — pode ser lista vazia. */
    public List<ItemTACO> buscarTodos() {
        return Collections.unmodifiableList(itens);
    }

    private void carregarDados() {
    String caminho = "/taco/TACO.json";
    try (InputStream is = getClass().getResourceAsStream(caminho)) {
        if (is == null) {
            log.error("TACO.json não encontrado em classpath: {}", caminho);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Trata strings não numéricas ("NA", "Tr", "") como null → safe() converte para 0.0
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

        // Registrar módulo que aceita string → double com fallback null
        mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
            .addDeserializer(Double.class, new com.fasterxml.jackson.databind.deser.std.StdDeserializer<Double>(Double.class) {
                @Override
                public Double deserialize(com.fasterxml.jackson.core.JsonParser p,
                                          com.fasterxml.jackson.databind.DeserializationContext ctx)
                        throws java.io.IOException {
                    String text = p.getText().trim();
                    if (text.isEmpty() || text.equalsIgnoreCase("NA") || text.equalsIgnoreCase("Tr")) return null;
                    try { return Double.parseDouble(text); }
                    catch (NumberFormatException e) { return null; }
                }
            })
        );

        List<ItemTacoJson> jsonItems = mapper.readValue(is, new TypeReference<>() {});

            for (ItemTacoJson j : jsonItems) {
                if (j.getId() == null) continue;
                ItemTACO item = converterParaItemTACO(j);
                itens.add(item);
                if (item.getCodigo() != null) {
                    itensPorCodigo.put(item.getCodigo(), item);
                }
            }

            log.info("TBCA carregada com sucesso: {} itens de {}", itens.size(), caminho);

        } catch (Exception e) {
            log.error("Erro ao carregar TBCA de {}. Calculadora ficará inoperante.", caminho, e);
        }
    }

    /**
     * Converte o DTO de desserialização para o modelo de domínio.
     * Campos null (NA, Tr, ausentes) → 0.0 via safe().
     */
    private ItemTACO converterParaItemTACO(ItemTacoJson j) {
        int id = j.getId();
        double energiaKcal = safe(j.getEnergyKcal());
        // kJ calculado a partir de kcal conforme NutricionalConstants
        double energiaKj = Math.round(energiaKcal * 4.184 * 10.0) / 10.0;

        return new ItemTACO(
                id,
                String.valueOf(id),
                j.getDescription(),
                j.getCategory(),
                safe(j.getHumidity()),
                energiaKcal,
                energiaKj,
                safe(j.getProteinG()),
                safe(j.getLipidG()),
                safe(j.getCholesterolMg()),
                safe(j.getCarbohydrateG()),
                0.0,                        // acucarTotal — campo inexistente no TACO.json
                safe(j.getFiberG()),
                safe(j.getSodiumMg()),
                safe(j.getCalciumMg()),
                safe(j.getMagnesiumMg()),
                safe(j.getManganeseMg()),
                safe(j.getPhosphorusMg()),
                safe(j.getIronMg()),
                safe(j.getPotassiumMg()),
                safe(j.getCopperMg()),
                safe(j.getZincMg()),
                safe(j.getRetinolMcg()),
                safe(j.getThiamineMg()),
                safe(j.getRiboflavinMg()),
                safe(j.getPyridoxineMg()),
                safe(j.getCobalaminMcg()),
                safe(j.getVitaminCMg()),
                safe(j.getVitaminDMcg()),
                safe(j.getVitaminEMg()),
                safe(j.getSaturatedG()),
                safe(j.getMonounsaturatedG()),
                safe(j.getPolyunsaturatedG()),
                safe(j.getAshG())
        );
    }

    /** Converte null → 0.0 e valores não-finitos → 0.0. */
    private static double safe(Double value) {
        if (value == null) return 0.0;
        return Double.isFinite(value) ? value : 0.0;
    }

    // ── Busca fuzzy — preservada integralmente ────────────────────────────────

    public ItemTACO buscarPorNome(String nomeBusca) {
        if (nomeBusca == null || nomeBusca.isBlank()) return null;
        String normalizado = normalizar(nomeBusca);
        double melhorPontuacao = 0;
        ItemTACO melhor = null;
        for (ItemTACO item : itens) {
            double p = similaridade(normalizado, normalizar(item.getDescricao()));
            if (p > melhorPontuacao) {
                melhorPontuacao = p;
                melhor = item;
            }
        }
        return melhorPontuacao > 0.35 ? melhor : null;
    }

    public List<ItemTACO> buscarSugestoes(String nomeBusca, int maximo) {
        if (nomeBusca == null || nomeBusca.isBlank()) return new ArrayList<>();
        String normalizado = normalizar(nomeBusca);
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            double p = similaridade(normalizado, normalizar(itens.get(i).getDescricao()));
            if (p > 0.15) scored.add(new double[]{i, p});
        }
        scored.sort((a, b) -> Double.compare(b[1], a[1]));
        List<ItemTACO> resultado = new ArrayList<>();
        for (int i = 0; i < Math.min(scored.size(), maximo); i++) {
            resultado.add(itens.get((int) scored.get(i)[0]));
        }
        return resultado;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase()
                .replace("á", "a").replace("ã", "a").replace("ä", "a")
                .replace("à", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e").replace("è", "e")
                .replace("í", "i").replace("ó", "o").replace("õ", "o")
                .replace("ö", "o").replace("ò", "o").replace("ô", "o")
                .replace("ú", "u").replace("ü", "u").replace("ç", "c")
                .replace("ñ", "n").replace("-", " ").replace(",", " ")
                .replace(".", " ").trim().replaceAll("\\s+", " ");
    }

    private double similaridade(String busca, String candidato) {
        String[] tokensBusca = busca.split("\\s+");
        String[] tokensCand  = candidato.split("\\s+");
        if (tokensBusca.length == 0 || tokensCand.length == 0) return 0;
        Set<String> setBusca = new HashSet<>(Arrays.asList(tokensBusca));
        Set<String> setCand  = new HashSet<>(Arrays.asList(tokensCand));
        int cobertos = 0;
        for (String tb : tokensBusca) if (setCand.contains(tb)) cobertos++;
        double cobertura = (double) cobertos / tokensBusca.length;
        Set<String> uniao = new HashSet<>(setBusca);
        uniao.addAll(setCand);
        Set<String> intersecao = new HashSet<>(setBusca);
        intersecao.retainAll(setCand);
        double jaccard = uniao.isEmpty() ? 0 : (double) intersecao.size() / uniao.size();
        String buscaSemEspaco = busca.replace(" ", "");
        String candSemEspaco  = candidato.replace(" ", "");
        double substringBonus = 0;
        if (!buscaSemEspaco.isEmpty() && candSemEspaco.contains(buscaSemEspaco)) substringBonus = 0.3;
        else if (!candSemEspaco.isEmpty() && buscaSemEspaco.contains(candSemEspaco)) substringBonus = 0.2;
        else if (!busca.isEmpty() && candidato.contains(busca)) substringBonus = 0.25;
        return Math.min(cobertura * 0.6 + jaccard * 0.1 + substringBonus, 1.0);
    }
}