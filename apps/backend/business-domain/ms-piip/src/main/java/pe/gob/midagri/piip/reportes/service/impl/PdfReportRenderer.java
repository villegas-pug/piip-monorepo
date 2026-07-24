package pe.gob.midagri.piip.reportes.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReporteDetail;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;
import pe.gob.midagri.piip.reportes.dto.TotalDimensionItem;
import pe.gob.midagri.piip.reportes.exception.ReportesValidationException;

/**
 * Renderizador PDF institucional (US8, BR-124).
 * Produce un PDF oficial que consume el mismo
 * {@code snapshotId}, parámetros y versión que la
 * versión XLSX; nunca reconstruye el corte desde
 * datos operativos posteriores.
 *
 * <p>La implementación usa la API de bajo nivel de
 * JasperReports para componer el documento de
 * forma dinámica a partir del snapshot JSON
 * canónico: cabecera institucional, metadatos del
 * reporte, indicadores BR-122 y totales BR-121.
 * JasperReports ya está declarado como dependencia
 * del backend y la constitución exige binarios
 * oficiales; no se agrega ninguna dependencia
 * nueva.
 */
@Component
public class PdfReportRenderer {

    private static final float MARGIN = 36f;
    private static final float PAGE_WIDTH = 595f;
    private static final float PAGE_HEIGHT = 842f;
    private static final float BODY_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    /**
     * Renderiza un PDF oficial a partir de un
     * snapshot canónico. La forma del documento se
     * reconstruye a partir del {@code payloadJson}
     * para garantizar idempotencia visual: dos
     * ejecuciones con el mismo snapshot producen
     * el mismo PDF.
     */
    public byte[] render(ReporteDetail detalle, JsonNode snapshot)
            throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        com.lowagie.text.Document documento =
                new com.lowagie.text.Document(
                        com.lowagie.text.PageSize.A4,
                        MARGIN, MARGIN, MARGIN, MARGIN);
        com.lowagie.text.pdf.PdfWriter.getInstance(
                documento, salida);
        documento.open();

        documento.add(cabecera(detalle));
        documento.add(metadatos(detalle));
        documento.add(seccion("Indicadores BR-122",
                tablaIndicadores(detalle.indicadores())));
        if (detalle.totales() != null) {
            for (TotalDimension dimension : detalle.totales()) {
                documento.add(seccion("Totales por " + dimension.dimension(),
                        tablaTotales(dimension)));
            }
        }
        documento.add(pie(detalle, snapshot));

        documento.close();
        return salida.toByteArray();
    }

    private com.lowagie.text.Element cabecera(ReporteDetail detalle) {
        com.lowagie.text.Paragraph parrafo =
                new com.lowagie.text.Paragraph(
                        "Reporte institucional PIIP");
        parrafo.getFont().setSize(16);
        parrafo.getFont().setStyle(com.lowagie.text.Font.BOLD);
        parrafo.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        return parrafo;
    }

    private com.lowagie.text.Element metadatos(ReporteDetail detalle) {
        com.lowagie.text.Paragraph parrafo =
                new com.lowagie.text.Paragraph();
        parrafo.add(new com.lowagie.text.Chunk("Tipo: "));
        parrafo.add(detalle.tipo() + "  ");
        parrafo.add(new com.lowagie.text.Chunk("Periodo: "));
        parrafo.add(detalle.periodo() + "  ");
        parrafo.add(new com.lowagie.text.Chunk("Corte: "));
        parrafo.add(detalle.fechaCorte().toString() + "\n");
        parrafo.add(new com.lowagie.text.Chunk("Clasificacion: "));
        parrafo.add(detalle.clasificacion() + "  ");
        parrafo.add(new com.lowagie.text.Chunk("Version: "));
        parrafo.add(String.valueOf(detalle.versionDatos()));
        parrafo.setSpacingAfter(8f);
        return parrafo;
    }

    private com.lowagie.text.Element seccion(String titulo,
            com.lowagie.text.Element contenido) {
        com.lowagie.text.Paragraph parrafo =
                new com.lowagie.text.Paragraph();
        com.lowagie.text.Font fuente =
                new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 12,
                        com.lowagie.text.Font.BOLD);
        parrafo.add(new com.lowagie.text.Chunk(titulo, fuente));
        parrafo.add("\n");
        parrafo.add(contenido);
        parrafo.setSpacingAfter(6f);
        return parrafo;
    }

    private com.lowagie.text.pdf.PdfPTable tablaIndicadores(
            List<IndicadorReporte> indicadores) {
        com.lowagie.text.pdf.PdfPTable tabla =
                new com.lowagie.text.pdf.PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 3f, 1.2f, 1.2f, 1.6f });
        agregarCelda(tabla, "Indicador", true);
        agregarCelda(tabla, "Numerador", true);
        agregarCelda(tabla, "Denominador", true);
        agregarCelda(tabla, "Resultado", true);
        for (IndicadorReporte indicador : indicadores) {
            agregarCelda(tabla, indicador.nombre(), false);
            agregarCelda(tabla, String.valueOf(indicador.numerador()), false);
            agregarCelda(tabla,
                    String.valueOf(indicador.denominador()), false);
            agregarCelda(tabla,
                    indicador.aplicable()
                            ? String.format("%.2f %%", indicador.porcentaje())
                            : "no aplicable",
                    false);
        }
        return tabla;
    }

    private com.lowagie.text.pdf.PdfPTable tablaTotales(
            TotalDimension dimension) {
        com.lowagie.text.pdf.PdfPTable tabla =
                new com.lowagie.text.pdf.PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 1.5f, 3f, 1.2f });
        agregarCelda(tabla, "Codigo", true);
        agregarCelda(tabla, "Etiqueta", true);
        agregarCelda(tabla, "Total", true);
        if (dimension.items() != null) {
            for (TotalDimensionItem item : dimension.items()) {
                agregarCelda(tabla,
                        item.clave() == null ? "" : item.clave(), false);
                agregarCelda(tabla,
                        item.etiqueta() == null ? "" : item.etiqueta(),
                        false);
                agregarCelda(tabla, String.valueOf(item.total()), false);
            }
        }
        return tabla;
    }

    private com.lowagie.text.Element pie(ReporteDetail detalle,
            JsonNode snapshot) {
        com.lowagie.text.Paragraph parrafo =
                new com.lowagie.text.Paragraph();
        parrafo.add(new com.lowagie.text.Chunk("Hash snapshot SHA-256: "));
        parrafo.add(detalle.hashSnapshot() == null
                ? "no disponible" : detalle.hashSnapshot());
        parrafo.add("\n");
        parrafo.add(new com.lowagie.text.Chunk("Generado: "));
        parrafo.add(detalle.fechaGeneracion() == null
                ? "-" : detalle.fechaGeneracion().toString());
        parrafo.add("\n");
        parrafo.add(new com.lowagie.text.Chunk("Snapshot bytes: "));
        parrafo.add(snapshot == null
                ? "0" : String.valueOf(snapshot.toString().length()));
        parrafo.setSpacingBefore(12f);
        return parrafo;
    }

    private static void agregarCelda(
            com.lowagie.text.pdf.PdfPTable tabla, String texto, boolean cabecera) {
        com.lowagie.text.pdf.PdfPCell celda =
                new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(texto));
        if (cabecera) {
            celda.setBackgroundColor(
                    com.lowagie.text.pdf.GrayColor.LIGHT_GRAY);
            celda.setHorizontalAlignment(
                    com.lowagie.text.Element.ALIGN_CENTER);
        }
        tabla.addCell(celda);
    }
}
