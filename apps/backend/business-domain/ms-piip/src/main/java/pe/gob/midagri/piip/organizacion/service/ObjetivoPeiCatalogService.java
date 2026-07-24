package pe.gob.midagri.piip.organizacion.service;
import java.time.LocalDate;
import java.util.List;
import pe.gob.midagri.piip.organizacion.dto.*;
public interface ObjetivoPeiCatalogService {
 List<PlaneamientoOption> listar(String q, LocalDate vigenteEn, PlaneamientoAuthContext contexto);
 PlaneamientoVersionDetail crearVersion(PlaneamientoVersionRequest request, PlaneamientoAuthContext contexto);
 List<PlaneamientoVersionDetail> listarVersiones(PlaneamientoAuthContext contexto);
 PlaneamientoVersionDetail obtenerVersion(Long id, PlaneamientoAuthContext contexto);
}
