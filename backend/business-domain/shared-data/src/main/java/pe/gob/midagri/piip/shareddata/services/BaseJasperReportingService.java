package pe.gob.midagri.piip.shareddata.services;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import lombok.AllArgsConstructor;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@AllArgsConstructor
public class BaseJasperReportingService {

   private final String baseTemplatePath;

   public byte[] generatePdfReport(String templatePath, Map<String, Object> params) throws JRException {

      // * Cargar archivo: jrxml, desde resource
      JasperReport report = this.loadTemplate(templatePath);

      // * Llenar el reporte
      JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, new JREmptyDataSource());

      // * Exportar a PDF
      return JasperExportManager.exportReportToPdf(jasperPrint);

   }

   public JRDataSource createDataSource(Collection<?> data) {
      return data == null ? new JREmptyDataSource() : new JRBeanCollectionDataSource(data);
   }

   private JasperReport loadTemplate(String templatePath) throws JRException {

      // * Cargar plantilla desde resource
      InputStream resource = getClass().getResourceAsStream(this.baseTemplatePath + templatePath);

      // * Compilar archivo `jrxml`
      JasperReport report = JasperCompileManager.compileReport(resource);
      return report;
   }

}
