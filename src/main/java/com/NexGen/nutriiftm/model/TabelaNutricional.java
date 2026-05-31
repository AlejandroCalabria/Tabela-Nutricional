package com.NexGen.nutriiftm.model;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "tabelanutricional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TabelaNutricional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tabCodigo")
    private Long tabCodigo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proCodigo")
    private Produto produto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "undCodigo")
    private UnidadeMedida unidadeMedida;

    @Column(name = "tabValorEnergetico")
    private Double tabValorEnergetico;

    @Column(name = "tabValorenergeticoPorcao")
    private Double tabValorEnergeticoPorcao;

    @Getter(lombok.AccessLevel.NONE)
    @Column(name = "tabPorcao")
    private Double tabPorcao;

    @Column(name = "tabTotalPorcao")
    private Double tabTotalPorcao;

    @Column(name = "tabTotalColheres")
    private Double tabTotalColheres;

    @Column(name = "tabVD")
    private Double tabVD;

    @Column(name = "tabPorcaoPadrao")
    private Double tabPorcaoPadrao;

    @Column(name = "tabMedidaCaseira")
    private String tabMedidaCaseira;

    @Column(name = "tabUnidadeMedidasColheres")
    private Double tabUnidadeMedidasColheres;

    @OneToMany(
            mappedBy = "tabelaNutricional",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<TabNutElemento> tneElementos;

    /**
     * Helper seguro — evita NPE quando tabPorcao for null.
     * Usado por TabNutElemento.getTabPorcao().
     */
    public double getTabPorcao() {
        return tabPorcao != null ? tabPorcao : 0.0;
    }
}