package com.NexGen.nutriiftm.service;

import com.NexGen.nutriiftm.model.TabNutElemento;
import com.NexGen.nutriiftm.model.TabelaNutricional;
import com.NexGen.nutriiftm.repository.TabelaNutricionalRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TabelaNutricionalService {

    private final TabelaNutricionalRepository repo;

    public List<TabelaNutricional> listarTodos() {
        return repo.findAllComElementos();
    }

    public TabelaNutricional buscarPorId(Long id) {
        return repo.findByIdComElementos(id).orElseThrow();
    }

    public TabelaNutricional salvar(TabelaNutricional t) { return repo.save(t); }
    public void deletar(Long id) { repo.deleteById(id); }

    /**
     * Gera PDF do rótulo nutricional conforme IN 75/2020.
     * Usa os mesmos dados persistidos no banco — sem recalcular.
     */
    public byte[] gerarPdf(Long id) throws Exception {
        TabelaNutricional tabela = buscarPorId(id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A5);
        doc.setMargins(20, 20, 20, 20);

        PdfFont bold  = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont plain = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // Indexar elementos pelo eleCodigo para lookup O(1)
        Map<Long, TabNutElemento> tneMap = tabela.getTneElementos() == null
                ? Map.of()
                : tabela.getTneElementos().stream()
                    .filter(tne -> tne.getElemento() != null)
                    .collect(Collectors.toMap(
                            tne -> tne.getElemento().getEleCodigo(),
                            tne -> tne,
                            (a, b) -> a)); // manter primeiro em caso de duplicata

        String unidade = tabela.getUnidadeMedida() != null
                ? tabela.getUnidadeMedida().getUndNome()
                : "g";

        double porcao      = tabela.getTabPorcao();
        double totalPorc  = tabela.getTabTotalPorcao() != null ? tabela.getTabTotalPorcao() : 0.0;
        double energPorcao= tabela.getTabValorEnergeticoPorcao() != null ? tabela.getTabValorEnergeticoPorcao() : 0.0;
        double energ100   = tabela.getTabValorEnergetico()       != null ? tabela.getTabValorEnergetico()       : 0.0;
        double vd         = tabela.getTabVD()                    != null ? tabela.getTabVD()                    : 0.0;

        // ── Título do produto ──────────────────────────────────────────────────
        if (tabela.getProduto() != null) {
            doc.add(new Paragraph(tabela.getProduto().getProNome())
                    .setFont(bold).setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            if (tabela.getProduto().getProNomeFantasia() != null) {
                doc.add(new Paragraph(tabela.getProduto().getProNomeFantasia())
                        .setFont(plain).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            }
        }

        // ── Rótulo ANVISA ──────────────────────────────────────────────────────
        Table rotulo = new Table(UnitValue.createPercentArray(new float[]{50, 20, 20, 10}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(2));

        // Cabeçalho "Informação Nutricional"
        rotulo.addCell(headerCell("INFORMAÇÃO NUTRICIONAL", bold, 4));

        // Porção
        String porcaoTxt = "Porção de " + fmt(porcao) + " " + unidade
                + "   (Porções por embalagem: " + (int) totalPorc + ")";
        rotulo.addCell(subHeaderCell(porcaoTxt, plain, 4));

        // Cabeçalho de colunas
        rotulo.addCell(colHeaderCell("", bold));
        rotulo.addCell(colHeaderCell("100 g", bold));
        rotulo.addCell(colHeaderCell(fmt(porcao) + " " + unidade, bold));
        rotulo.addCell(colHeaderCell("%VD*", bold));

        // Energia
        int energPorcInt = (int) Math.round(energPorcao);
        int energ100Int  = (int) Math.round(energ100);
        int kjPorc       = (int) Math.round(energPorcao * 4.184);
        int kj100        = (int) Math.round(energ100    * 4.184);
        int vdInt        = (int) Math.round(vd);

        rotulo.addCell(nutriCell("Valor Energético (kcal/kJ)", bold));
        rotulo.addCell(nutriCell(energ100Int + " kcal / " + kj100 + " kJ", plain));
        rotulo.addCell(nutriCell(energPorcInt + " kcal / " + kjPorc + " kJ", plain));
        rotulo.addCell(nutriCell(vdInt + "%", plain));

        // Nutrientes: carboidratos(1), açúcares totais(2), proteínas(4),
        //             gorduras totais(5), gorduras saturadas(6), gorduras trans(7),
        //             fibras(15), sódio(16)
        addNutrienteRow(rotulo, tneMap, 1L,  "Carboidratos (g)",      plain, bold, false, false);
        addNutrienteRow(rotulo, tneMap, 2L,  "  Açúcares Totais (g)", plain, bold, true,  false);
        // Açúcares adicionados — N.D.
        rotulo.addCell(nutriCell("  Açúcares Adicionados (g)", plain));
        rotulo.addCell(nutriCell("N.D.", plain));
        rotulo.addCell(nutriCell("N.D.", plain));
        rotulo.addCell(nutriCell("**", plain));

        addNutrienteRow(rotulo, tneMap, 4L,  "Proteínas (g)",           plain, bold, false, false);
        addNutrienteRow(rotulo, tneMap, 5L,  "Gorduras Totais (g)",     plain, bold, false, false);
        addNutrienteRow(rotulo, tneMap, 6L,  "  Gorduras Saturadas (g)",plain, bold, true,  false);
        // Gorduras Trans — sempre 0
        rotulo.addCell(nutriCell("  Gorduras Trans (g)", plain));
        rotulo.addCell(nutriCell("0", plain));
        rotulo.addCell(nutriCell("0", plain));
        rotulo.addCell(nutriCell("VD não estabel.", plain));

        addNutrienteRow(rotulo, tneMap, 15L, "Fibra Alimentar (g)",    plain, bold, false, false);
        addNutrienteRow(rotulo, tneMap, 16L, "Sódio (mg)",             plain, bold, false, true);

        // Rodapé
        String rodape = "* Percentual de Valores Diários fornecidos pela porção.\n"
                + "** VD não estabelecido.\nN.D. Não disponível na TBCA.\n"
                + "Valores Diários com base em dieta de 2000 kcal ou 8400 kJ.";
        Cell rodapeCell = new Cell(1, 4)
                .add(new Paragraph(rodape).setFont(plain).setFontSize(7))
                .setBorderTop(new SolidBorder(2))
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(2));
        rotulo.addCell(rodapeCell);

        doc.add(rotulo);

        // Informações do produto abaixo do rótulo
        if (tabela.getProduto() != null) {
            var prod = tabela.getProduto();
            doc.add(new Paragraph(" ").setFontSize(6));
            if (prod.getProIngredientes() != null) {
                doc.add(new Paragraph("Ingredientes: " + prod.getProIngredientes())
                        .setFont(plain).setFontSize(8));
            }
            if (prod.getProRecomendacoes() != null) {
                doc.add(new Paragraph("Recomendações: " + prod.getProRecomendacoes())
                        .setFont(plain).setFontSize(8));
            }
            if (prod.getFabricante() != null) {
                doc.add(new Paragraph("Produzido por: " + prod.getFabricante().getFabNome()
                        + " | " + prod.getFabricante().getFabEndereco())
                        .setFont(plain).setFontSize(8));
            }
        }

        doc.close();
        return baos.toByteArray();
    }

    // ── Helpers de célula ──────────────────────────────────────────────────────

    private Cell headerCell(String text, PdfFont font, int colspan) {
        return new Cell(1, colspan)
                .add(new Paragraph(text).setFont(font).setFontSize(14).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorderBottom(new SolidBorder(3))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(3));
    }

    private Cell subHeaderCell(String text, PdfFont font, int colspan) {
        return new Cell(1, colspan)
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(1));
    }

    private Cell colHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(2));
    }

    private Cell nutriCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.5f));
    }

    /**
     * Adiciona linha de nutriente ao rótulo PDF.
     * sodioMg=true → formata como inteiro (mg), senão 1 decimal (g).
     */
    private void addNutrienteRow(Table t, Map<Long, TabNutElemento> tneMap,
            Long eleId, String label, PdfFont plain, PdfFont bold,
            boolean subItem, boolean sodioMg) {

        TabNutElemento tne = tneMap.get(eleId);

        String v100, vPorc, vd;
        if (tne != null) {
            if (sodioMg) {
                v100  = String.valueOf((int) Math.round(tne.getTneValorPadrao()));
                vPorc = String.valueOf((int) Math.round(tne.getTneValor()));
            } else {
                v100  = fmt(tne.getTneValorPadrao());
                vPorc = fmt(tne.getTneValor());
            }
            // VD%: 0 também é válido (ex: sódio = 0 mg)
            vd = (tne.getTneVD() >= 0 && tne.getElemento().getEleValorRecomendado() > 0)
                    ? ((int) Math.round(tne.getTneVD())) + "%"
                    : "**";
        } else {
            v100 = "—"; vPorc = "—"; vd = "**";
        }

        PdfFont labelFont = subItem ? plain : bold;
        t.addCell(nutriCell(label, labelFont));
        t.addCell(nutriCell(v100,  plain));
        t.addCell(nutriCell(vPorc, plain));
        t.addCell(nutriCell(vd,    plain));
    }

    /** Formata double com 1 casa decimal para macros em gramas. */
    private String fmt(double val) {
        return String.format("%.1f", val);
    }
}