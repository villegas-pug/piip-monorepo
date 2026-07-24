package pe.gob.midagri.piip.reportes.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReporteDetail;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;
import pe.gob.midagri.piip.reportes.dto.TotalDimensionItem;

/**
 * Renderizador XLSX de detalle (US8, BR-124). Produce
 * el archivo de detalle que consume el mismo
 * {@code snapshotId}, parámetros y versión que el
 * PDF oficial; nunca reconstruye el corte desde
 * datos operativos posteriores.
 *
 * <p>La implementación usa Apache POI (declarado
 * como dependencia del backend) y respeta la
 * regla BR-122 del denominador cero: cuando el
 * indicador no aplica, la celda de porcentaje
 * queda vacía para que el receptor del archivo
 * no infiera un valor.
 */
@Component
public class XlsxReportRenderer {

    /** Renderiza un XLSX de detalle a partir de un snapshot canónico. */
    public byte[] render(ReporteDetail detalle, JsonNode snapshot)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle cabecera = estiloCabecera(workbook);
            CellStyle porcentaje = estiloPorcentaje(workbook);
            CellStyle texto = estiloTexto(workbook);

            hojaMetadatos(workbook, detalle, snapshot, cabecera, texto);
            hojaIndicadores(workbook, detalle.indicadores(), cabecera,
                    porcentaje, texto);
            int hojaTotales = 2;
            if (detalle.totales() != null) {
                for (TotalDimension dimension : detalle.totales()) {
                    hojaTotales(workbook, hojaTotales++,
                            dimension, cabecera, texto);
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        }
    }

    private void hojaMetadatos(Workbook libro, ReporteDetail detalle,
            JsonNode snapshot, CellStyle cabecera, CellStyle texto) {
        Sheet hoja = libro.createSheet("Metadatos");
        int fila = 0;
        fila = escribirCabecera(libro, hoja, fila, cabecera,
                "Reporte institucional PIIP");
        fila = escribirFilaKV(hoja, fila, texto,
                "idReporte", String.valueOf(detalle.idReporte()));
        fila = escribirFilaKV(hoja, fila, texto,
                "tipo", detalle.tipo());
        fila = escribirFilaKV(hoja, fila, texto,
                "periodo", detalle.periodo());
        fila = escribirFilaKV(hoja, fila, texto,
                "anio", String.valueOf(detalle.anio()));
        fila = escribirFilaKV(hoja, fila, texto,
                "semestre", String.valueOf(detalle.semestre()));
        fila = escribirFilaKV(hoja, fila, texto,
                "fechaCorte", detalle.fechaCorte().toString());
        fila = escribirFilaKV(hoja, fila, texto,
                "versionDatos", String.valueOf(detalle.versionDatos()));
        fila = escribirFilaKV(hoja, fila, texto,
                "clasificacion", detalle.clasificacion());
        fila = escribirFilaKV(hoja, fila, texto,
                "idSnapshot", String.valueOf(detalle.idSnapshot()));
        fila = escribirFilaKV(hoja, fila, texto,
                "hashSnapshot", detalle.hashSnapshot());
        fila = escribirFilaKV(hoja, fila, texto,
                "estadoTecnico", detalle.estadoTecnico());
        fila = escribirFilaKV(hoja, fila, texto,
                "snapshotBytes", String.valueOf(
                        snapshot == null ? 0 : snapshot.toString().length()));
        fila = escribirFilaKV(hoja, fila, texto,
                "fechaGeneracion",
                detalle.fechaGeneracion() == null
                        ? "-" : detalle.fechaGeneracion().toString());
        hoja.autoSizeColumn(0);
        hoja.autoSizeColumn(1);
    }

    private void hojaIndicadores(Workbook libro,
            List<IndicadorReporte> indicadores, CellStyle cabecera,
            CellStyle porcentaje, CellStyle texto) {
        Sheet hoja = libro.createSheet("Indicadores BR-122");
        Row filaCabecera = hoja.createRow(0);
        escribirCelda(filaCabecera, 0, "Indicador", cabecera);
        escribirCelda(filaCabecera, 1, "Numerador", cabecera);
        escribirCelda(filaCabecera, 2, "Denominador", cabecera);
        escribirCelda(filaCabecera, 3, "Porcentaje", cabecera);
        escribirCelda(filaCabecera, 4, "Aplicable", cabecera);
        escribirCelda(filaCabecera, 5, "Detalle", cabecera);
        int fila = 1;
        for (IndicadorReporte indicador : indicadores) {
            Row row = hoja.createRow(fila++);
            escribirCelda(row, 0, indicador.nombre(), texto);
            escribirCelda(row, 1,
                    String.valueOf(indicador.numerador()), texto);
            escribirCelda(row, 2,
                    String.valueOf(indicador.denominador()), texto);
            if (indicador.aplicable()) {
                escribirCelda(row, 3,
                        String.format("%.2f %%", indicador.porcentaje()),
                        porcentaje);
            } else {
                escribirCelda(row, 3, "no aplicable", texto);
            }
            escribirCelda(row, 4,
                    indicador.aplicable() ? "SI" : "NO", texto);
            escribirCelda(row, 5,
                    indicador.detalle() == null ? "" : indicador.detalle(),
                    texto);
        }
        for (int col = 0; col <= 5; col++) {
            hoja.autoSizeColumn(col);
        }
    }

    private void hojaTotales(Workbook libro, int indice,
            TotalDimension dimension, CellStyle cabecera, CellStyle texto) {
        Sheet hoja = libro.createSheet(
                "Totales " + sanitize(dimension.dimension()));
        Row filaCabecera = hoja.createRow(0);
        escribirCelda(filaCabecera, 0, dimension.dimension(), cabecera);
        escribirCelda(filaCabecera, 1, "Etiqueta", cabecera);
        escribirCelda(filaCabecera, 2, "Total", cabecera);
        int fila = 1;
        if (dimension.items() != null) {
            for (TotalDimensionItem item : dimension.items()) {
                Row row = hoja.createRow(fila++);
                escribirCelda(row, 0,
                        item.clave() == null ? "" : item.clave(), texto);
                escribirCelda(row, 1,
                        item.etiqueta() == null ? "" : item.etiqueta(),
                        texto);
                escribirCelda(row, 2,
                        String.valueOf(item.total()), texto);
            }
        }
        for (int col = 0; col <= 2; col++) {
            hoja.autoSizeColumn(col);
        }
    }

    private int escribirCabecera(Workbook libro, Sheet hoja, int fila,
            CellStyle cabecera, String titulo) {
        Row row = hoja.createRow(fila++);
        Cell celda = row.createCell(0);
        celda.setCellValue(titulo);
        celda.setCellStyle(cabecera);
        return fila;
    }

    private int escribirFilaKV(Sheet hoja, int fila, CellStyle texto,
            String clave, String valor) {
        Row row = hoja.createRow(fila++);
        Cell celdaClave = row.createCell(0);
        celdaClave.setCellValue(clave);
        celdaClave.setCellStyle(texto);
        Cell celdaValor = row.createCell(1);
        celdaValor.setCellValue(valor);
        celdaValor.setCellStyle(texto);
        return fila;
    }

    private void escribirCelda(Row fila, int columna, String valor,
            CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private static String sanitize(String nombre) {
        if (nombre == null) {
            return "Dimension";
        }
        return nombre.replaceAll("[^A-Za-z0-9]+", " ").trim();
    }

    private CellStyle estiloCabecera(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle estiloPorcentaje(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setAlignment(HorizontalAlignment.RIGHT);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle estiloTexto(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }
}
