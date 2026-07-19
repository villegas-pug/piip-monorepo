package pe.gob.midagri.piip.shareddata.services;

public abstract class BaseReportingService {

   public final BaseApachePOIReportingService apachePOIReportingService;
   public final BaseJasperReportingService jasperReportingService;

   public BaseReportingService() {
      this.apachePOIReportingService = new BaseApachePOIReportingService();
      this.jasperReportingService = new BaseJasperReportingService(this.getBaseTemplatePath());
   }

   protected abstract String getBaseTemplatePath();

}
