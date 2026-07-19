package pe.gob.midagri.piip.shareddata.services;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public abstract class BaseOpenPdfReportingService<T> {

   public final byte[] generatePdf(T data) {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
         Document document = new Document(
               this.getPageSize(),
               this.getMarginLeft(),
               this.getMarginRight(),
               this.getMarginTop(),
               this.getMarginBottom());

         PdfWriter.getInstance(document, outputStream);
         document.open();

         PdfStyleConfig styleConfig = this.createStyleConfig();
         PdfDocumentBuilder builder = new PdfDocumentBuilder(document, styleConfig);
         this.buildPdf(builder, data);

         document.close();
         return outputStream.toByteArray();
      } catch (Exception e) {
         throw new RuntimeException("Error generating PDF", e);
      }
   }

   protected abstract void buildPdf(PdfDocumentBuilder pdf, T data) throws Exception;

   protected Rectangle getPageSize() {
      return PageSize.A4;
   }

   protected float getMarginLeft() {
      return 40f;
   }

   protected float getMarginRight() {
      return 40f;
   }

   protected float getMarginTop() {
      return 60f;
   }

   protected float getMarginBottom() {
      return 40f;
   }

   protected PdfStyleConfig createStyleConfig() {
      Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
      Font subtitleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
      Font keyFont = new Font(Font.HELVETICA, 11, Font.BOLD);
      Font normalFont = new Font(Font.HELVETICA, 10);
      Font tableHeaderFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);

      return new PdfStyleConfig(
            titleFont,
            subtitleFont,
            keyFont,
            normalFont,
            tableHeaderFont,
            Color.DARK_GRAY);
   }

   public static final class PdfStyleConfig {

      private final Font titleFont;
      private final Font subtitleFont;
      private final Font keyFont;
      private final Font normalFont;
      private final Font tableHeaderFont;
      private final Color tableHeaderBackgroundColor;

      public PdfStyleConfig(
            Font titleFont,
            Font subtitleFont,
            Font keyFont,
            Font normalFont,
            Font tableHeaderFont,
            Color tableHeaderBackgroundColor) {
         this.titleFont = titleFont;
         this.subtitleFont = subtitleFont;
         this.keyFont = keyFont;
         this.normalFont = normalFont;
         this.tableHeaderFont = tableHeaderFont;
         this.tableHeaderBackgroundColor = tableHeaderBackgroundColor;
      }

      public Font getTitleFont() {
         return titleFont;
      }

      public Font getSubtitleFont() {
         return subtitleFont;
      }

      public Font getKeyFont() {
         return keyFont;
      }

      public Font getNormalFont() {
         return normalFont;
      }

      public Font getTableHeaderFont() {
         return tableHeaderFont;
      }

      public Color getTableHeaderBackgroundColor() {
         return tableHeaderBackgroundColor;
      }
   }

   public static final class PdfDocumentBuilder {

      private final Document document;
      private final PdfStyleConfig styles;

      private PdfDocumentBuilder(Document document, PdfStyleConfig styles) {
         this.document = document;
         this.styles = styles;
      }

      public PdfDocumentBuilder title(String text) throws Exception {
         Paragraph paragraph = new Paragraph(safeText(text), styles.getTitleFont());
         paragraph.setAlignment(Element.ALIGN_CENTER);
         paragraph.setSpacingAfter(20);
         document.add(paragraph);
         return this;
      }

      public PdfDocumentBuilder subtitle(String text) throws Exception {
         Paragraph paragraph = new Paragraph(safeText(text), styles.getSubtitleFont());
         paragraph.setSpacingAfter(10);
         document.add(paragraph);
         return this;
      }

      public PdfDocumentBuilder paragraph(String text) throws Exception {
         Paragraph paragraph = new Paragraph(safeText(text), styles.getNormalFont());
         document.add(paragraph);
         return this;
      }

      public PdfDocumentBuilder blankLine() throws Exception {
         document.add(new Paragraph(" "));
         return this;
      }

      public KeyValueTableBuilder keyValueTable() {
         return new KeyValueTableBuilder(this.document, this.styles);
      }

      public SimpleTableBuilder table(String... headers) {
         return new SimpleTableBuilder(this.document, this.styles, null, headers);
      }

      public SimpleTableBuilder tableWithTitle(String title, String... headers) {
         return new SimpleTableBuilder(this.document, this.styles, safeText(title), headers);
      }

      public String text(Object value) {
         return safeText(value);
      }
   }

   public static final class KeyValueTableBuilder {

      private final Document document;
      private final PdfStyleConfig styles;
      private final PdfPTable table;

      private KeyValueTableBuilder(Document document, PdfStyleConfig styles) {
         this.document = document;
         this.styles = styles;
         this.table = new PdfPTable(2);
         this.table.setWidthPercentage(100);
      }

      public KeyValueTableBuilder widthPercentage(float widthPercentage) {
         this.table.setWidthPercentage(widthPercentage);
         return this;
      }

      public KeyValueTableBuilder spacingAfter(float spacingAfter) {
         this.table.setSpacingAfter(spacingAfter);
         return this;
      }

      public KeyValueTableBuilder spacingBefore(float spacingBefore) {
         this.table.setSpacingBefore(spacingBefore);
         return this;
      }

      public KeyValueTableBuilder row(String key, Object value) {
         this.table.addCell(createKeyCell(safeText(key), styles.getKeyFont()));
         this.table.addCell(createValueCell(safeText(value), styles.getNormalFont()));
         return this;
      }

      public KeyValueTableBuilder rowIfNotBlank(String key, Object value) {
         String normalized = safeText(value);
         if (!normalized.isBlank()) {
            this.row(key, normalized);
         }
         return this;
      }

      public void add() throws Exception {
         this.document.add(this.table);
      }
   }

   public static final class SimpleTableBuilder {

      private final Document document;
      private final PdfStyleConfig styles;
      private final String title;
      private final PdfPTable table;
      private final int columns;
      private final Set<Integer> centeredColumns;

      private SimpleTableBuilder(Document document, PdfStyleConfig styles, String title, String... headers) {
         if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("At least one header is required");
         }

         this.document = document;
         this.styles = styles;
         this.title = title;
         this.columns = headers.length;
         this.table = new PdfPTable(this.columns);
         this.table.setWidthPercentage(100);
         this.centeredColumns = new HashSet<>();

         for (String header : headers) {
            this.table.addCell(createTableHeaderCell(safeText(header), styles));
         }
      }

      public SimpleTableBuilder widths(float... widths) throws Exception {
         if (widths == null || widths.length != this.columns) {
            throw new IllegalArgumentException("Widths length must match header count");
         }
         this.table.setWidths(widths);
         return this;
      }

      public SimpleTableBuilder widthPercentage(float widthPercentage) {
         this.table.setWidthPercentage(widthPercentage);
         return this;
      }

      public SimpleTableBuilder spacingAfter(float spacingAfter) {
         this.table.setSpacingAfter(spacingAfter);
         return this;
      }

      public SimpleTableBuilder spacingBefore(float spacingBefore) {
         this.table.setSpacingBefore(spacingBefore);
         return this;
      }

      public SimpleTableBuilder centerColumns(int... indexes) {
         if (indexes == null) {
            return this;
         }
         for (int index : indexes) {
            if (index < 0 || index >= this.columns) {
               throw new IllegalArgumentException("Column index out of bounds: " + index);
            }
            this.centeredColumns.add(index);
         }
         return this;
      }

      public SimpleTableBuilder row(Object... values) {
         if (values == null || values.length != this.columns) {
            throw new IllegalArgumentException("Row values length must match header count");
         }

         for (int i = 0; i < values.length; i++) {
            String value = safeText(values[i]);
            if (this.centeredColumns.contains(i)) {
               this.table.addCell(createCenteredCell(value, styles.getNormalFont()));
            } else {
               this.table.addCell(createValueCell(value, styles.getNormalFont()));
            }
         }

         return this;
      }

      public SimpleTableBuilder rows(List<List<Object>> rows) {
         if (rows == null || rows.isEmpty()) {
            return this;
         }

         for (List<Object> row : rows) {
            List<Object> safeRow = row == null ? new ArrayList<>() : row;
            this.row(safeRow.toArray());
         }
         return this;
      }

      public void add() throws Exception {
         if (this.title != null && !this.title.isBlank()) {
            Paragraph titleParagraph = new Paragraph(this.title, styles.getSubtitleFont());
            titleParagraph.setSpacingAfter(10);
            this.document.add(titleParagraph);
         }

         this.document.add(this.table);
      }
   }

   private static PdfPCell createKeyCell(String text, Font font) {
      PdfPCell cell = new PdfPCell(new Phrase(text, font));
      cell.setBorder(Rectangle.NO_BORDER);
      cell.setPadding(5);
      return cell;
   }

   private static PdfPCell createValueCell(String text, Font font) {
      PdfPCell cell = new PdfPCell(new Phrase(text, font));
      cell.setPadding(5);
      return cell;
   }

   private static PdfPCell createCenteredCell(String text, Font font) {
      PdfPCell cell = new PdfPCell(new Phrase(text, font));
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setPadding(5);
      return cell;
   }

   private static PdfPCell createTableHeaderCell(String text, PdfStyleConfig styles) {
      PdfPCell cell = new PdfPCell(new Phrase(text, styles.getTableHeaderFont()));
      cell.setBackgroundColor(styles.getTableHeaderBackgroundColor());
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setPadding(6);
      return cell;
   }

   private static String safeText(Object value) {
      return value == null ? "" : String.valueOf(value).trim();
   }
}
