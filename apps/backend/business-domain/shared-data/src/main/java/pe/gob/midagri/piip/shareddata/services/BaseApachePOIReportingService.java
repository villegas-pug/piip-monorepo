package pe.gob.midagri.piip.shareddata.services;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.extern.log4j.Log4j2;
import pe.gob.midagri.piip.shareddata.dtos.projections.GenericReportRecordProjection;

@Log4j2
public class BaseApachePOIReportingService {

   public byte[] generateExcelFile(String sheetName, List<GenericReportRecordProjection> data) {

      try (XSSFWorkbook workbook = new XSSFWorkbook()) {

         // * 1. Cabeceras de columnas:
         List<GenericReportRecordProjection> fieldsHeader = data.stream()
               .filter(record -> record.getRowGroup().equals(1L))
               .toList();

         XSSFSheet sheet = workbook.createSheet(sheetName);
         XSSFRow headerRow = sheet.createRow(0);

         for (int i = 0; i < fieldsHeader.size(); i++) {
            headerRow.createCell(i).setCellValue(fieldsHeader.get(i).getValue());
         }

         // * 2. Cuerpo del reporte:
         Set<Long> rowGroups = data.stream().map(GenericReportRecordProjection::getRowGroup)
               .collect(Collectors.toSet());

         for (Long rowGroup : rowGroups) {

            XSSFRow bodyRow = sheet.createRow(rowGroup.intValue());

            List<GenericReportRecordProjection> fieldsBody = data.stream()
                  .filter(record -> record.getRowGroup().equals(rowGroup))
                  .toList();

            for (int i = 0; i < fieldsBody.size(); i++) {
               bodyRow.createCell(i).setCellValue(fieldsBody.get(i).getValue());
            }

         }

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         workbook.write(outputStream);

         return outputStream.toByteArray();

      } catch (Exception e) {
         log.error("Ocurrión un error al crear el reporte: {}", e.getMessage());
         return new byte[0];
      }

   }

   public byte[] generateDynamicExcelFile(String sheetName, List<Map<String, Object>> data) {

      try (XSSFWorkbook workbook = new XSSFWorkbook()) {

         // * 1. Cabeceras de columnas:
         List<String> fieldsHeader = new ArrayList<>(new LinkedHashSet<>(data.get(0).keySet()));

         XSSFSheet sheet = workbook.createSheet(sheetName);
         XSSFRow headerRow = sheet.createRow(0);

         for (int i = 0; i < fieldsHeader.size(); i++) {
            headerRow.createCell(i).setCellValue(fieldsHeader.get(i) != null ? fieldsHeader.get(i).toUpperCase() : "");
         }

         // * 2. Cuerpo del reporte:

         int iRow = 0;
         for (Map<String, Object> record : data) {
            iRow++;

            XSSFRow bodyRow = sheet.createRow(iRow);

            for (int i = 0; i < record.size(); i++) {
               bodyRow.createCell(i)
                     .setCellValue(
                           Optional.ofNullable(record.get(fieldsHeader.get(i)))
                                 .map(Object::toString)
                                 .orElse("-"));
            }

         }

         // * 3. Aplicar estilos
         this.applyBasicHeaderCellStyle(workbook);
         this.applyBasicBodyCellStyle(workbook);
         sheet.setDisplayGridlines(false);

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         workbook.write(outputStream);

         return outputStream.toByteArray();

      } catch (Exception e) {
         log.error("Ocurrión un error al crear el reporte: {}", e.getMessage());
         return new byte[0];
      }

   }

   private void applyBasicHeaderCellStyle(Workbook workbook) {

      // * 1. Estilo
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);

      // * 1.1 Fuente
      CellStyle cellStyle = workbook.createCellStyle();
      Font fuente = workbook.createFont();
      fuente.setColor(IndexedColors.BLACK.getIndex());
      fuente.setBold(true);
      fuente.setFontHeightInPoints((short) 10);
      fuente.setFontName("Arial");
      cellStyle.setFont(fuente);

      // * 1.2 Border
      this.applyBasicBorderCellStyle(cellStyle);

      // * 1.2 Color fondo
      cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      // * 1.4 Altura de fila
      workbook.getSheetAt(0).getRow(0).setHeightInPoints(25);

      // * 1.5 Fijar fila
      sheet.createFreezePane(0, 1);

      // * 2. Aplicar estilo
      for (int i = 0; i < headerRow.getLastCellNum(); i++) {
         sheet.getRow(0).getCell(i).setCellStyle(cellStyle);
      }

   }

   private void applyBasicBodyCellStyle(Workbook workbook) {

      // * 1. Estilo
      CellStyle cellStyle = workbook.createCellStyle();

      // * 1.1 Borde
      this.applyBasicBorderCellStyle(cellStyle);

      // * 1.4 Ancho columna
      Sheet sheet = workbook.getSheetAt(0);
      for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
         int cellWidth = (sheet.getRow(0).getCell(i).getStringCellValue().length() + 5) * 256;
         // sheet.autoSizeColumn(i);
         // int cellCurrentWidth = sheet.getColumnWidth(i);
         sheet.setColumnWidth(i, cellWidth);
      }

      // * 1.5 Fuente
      Font font = workbook.createFont();
      font.setColor(IndexedColors.BLACK.getIndex());
      font.setFontHeightInPoints((short) 9);
      font.setFontName("Arial");
      cellStyle.setFont(font);

      // * 2. Aplicar estilo
      for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
         for (int j = 0; j < sheet.getRow(i).getLastCellNum(); j++) {
            sheet.getRow(i).getCell(j).setCellStyle(cellStyle);
         }
      }

   }

   private void applyBasicBorderCellStyle(CellStyle cellStyle) {

      // * 1.1 Tipo borde
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderBottom(BorderStyle.THIN);

      // * 1.2 Color borde
      cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
      cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
      cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
      cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

      // * 1.3 Alineación del contenido
      cellStyle.setAlignment(HorizontalAlignment.CENTER);
      cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

   }
}
