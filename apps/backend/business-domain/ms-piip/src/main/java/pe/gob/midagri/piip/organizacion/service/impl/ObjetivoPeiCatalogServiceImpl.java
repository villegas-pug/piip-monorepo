package pe.gob.midagri.piip.organizacion.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.organizacion.dto.*;
import pe.gob.midagri.piip.organizacion.entity.*;
import pe.gob.midagri.piip.organizacion.repository.*;
import pe.gob.midagri.piip.organizacion.service.ObjetivoPeiCatalogService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

@Service
public class ObjetivoPeiCatalogServiceImpl implements ObjetivoPeiCatalogService {
 private final ObjetivoPeiRepository itemRepository; private final ObjetivoPeiVersionRepository versionRepository;
 private final DocumentoService documentoService; private final AutorizacionEfectivaService autorizacion; private final AuditService auditoria;
 public ObjetivoPeiCatalogServiceImpl(ObjetivoPeiRepository i, ObjetivoPeiVersionRepository v, DocumentoService d, AutorizacionEfectivaService a, AuditService au) { itemRepository=i;versionRepository=v;documentoService=d;autorizacion=a;auditoria=au; }
 @Override @Transactional(readOnly=true) public List<PlaneamientoOption> listar(String q, LocalDate fecha, PlaneamientoAuthContext c) { var a=autorizarConsulta(c); var r=itemRepository.buscarVigentes(q==null?"":q, fecha==null?LocalDate.now():fecha).stream().map(this::opcion).toList(); auditar(a,c,"CONSULTAR_OBJETIVOS_PEI",null,Map.of("total",String.valueOf(r.size()))); return r; }
 @Override @Transactional public PlaneamientoVersionDetail crearVersion(PlaneamientoVersionRequest r, PlaneamientoAuthContext c) { var a=autorizar(c); validarSolicitud(r,"PEI"); if(versionRepository.existsByCodigoVersion(r.codigoVersion())) throw error("PEI_VERSION_DUPLICATE"); if(!documentoService.validarDocumentoInstitucionalAprobatorio(r.documentoAprobacionVersionId()).valido()) throw error("PEI_APPROVAL_REQUIRED"); var nueva=new ObjetivoPeiVersionEntity(); nueva.setCodigoVersion(r.codigoVersion().trim()); nueva.setDocumentoAprobacionId(r.documentoAprobacionVersionId()); nueva.setOficinaAprobadora(r.oficinaAprobadora().trim()); nueva.setVigenteDesde(r.vigenteDesde()); nueva.setVigenteHasta(r.vigenteHasta()); nueva.setActiva("S"); nueva.setCreadoPor(c.actorSub()); nueva.setVersionAnteriorId(versionRepository.findFirstByOrderByIdDesc().map(ObjetivoPeiVersionEntity::getId).orElse(null)); final var v=versionRepository.save(nueva); var items=r.items().stream().map(x->{var e=new ObjetivoPeiEntity();e.setVersionId(v.getId());e.setCodigo(x.codigo().trim());e.setDescripcion(x.descripcion().trim());e.setVigenteDesde(x.vigenteDesde());e.setVigenteHasta(x.vigenteHasta());e.setActivo("S");return itemRepository.save(e);}).toList(); auditar(a,c,"VERSIONAR_OBJETIVOS_PEI",v.getId(),Map.of("codigoVersion",v.getCodigoVersion(),"oficinaAprobadora",v.getOficinaAprobadora())); return detalle(v,items); }
 @Override @Transactional(readOnly=true) public List<PlaneamientoVersionDetail> listarVersiones(PlaneamientoAuthContext c) { autorizar(c); return versionRepository.findAll().stream().map(v->detalle(v,itemRepository.findAll().stream().filter(i->v.getId().equals(i.getVersionId())).toList())).toList(); }
 @Override @Transactional(readOnly=true) public PlaneamientoVersionDetail obtenerVersion(Long id, PlaneamientoAuthContext c) { autorizar(c); var v=versionRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"PEI_VERSION_NOT_FOUND")); return detalle(v,itemRepository.findAll().stream().filter(i->v.getId().equals(i.getVersionId())).toList()); }
 private AutorizacionEfectivaService.AsignacionEfectiva autorizar(PlaneamientoAuthContext c) { if(c==null) throw error("ASSIGNMENT_SCOPE_DENIED"); return autorizacion.revalidarParaOperacionSensible(c.actorSub(),c.asignacionEfectivaId(),"GlobalAdmin",c.unidadEfectivaId()); }
 private AutorizacionEfectivaService.AsignacionEfectiva autorizarConsulta(PlaneamientoAuthContext c) { if(c==null) throw error("ASSIGNMENT_SCOPE_DENIED"); return autorizacion.revalidarAsignacionInstitucional(c.actorSub(),c.asignacionEfectivaId(),c.unidadEfectivaId()); }
 private void validarSolicitud(PlaneamientoVersionRequest r,String tipo){ if(r.vigenteHasta()!=null&&r.vigenteHasta().isBefore(r.vigenteDesde()))throw error(tipo+"_APPROVAL_MISMATCH"); for(var i:r.items())if(i.vigenteHasta()!=null&&i.vigenteHasta().isBefore(i.vigenteDesde()))throw error(tipo+"_APPROVAL_MISMATCH"); }
 private PlaneamientoOption opcion(ObjetivoPeiEntity e){return new PlaneamientoOption(e.getId(),e.getCodigo(),e.getDescripcion(),e.getVigenteDesde(),e.getVigenteHasta(),"S".equals(e.getActivo()));}
 private PlaneamientoVersionDetail detalle(ObjetivoPeiVersionEntity v,List<ObjetivoPeiEntity> i){return new PlaneamientoVersionDetail(v.getId(),v.getCodigoVersion(),v.getDocumentoAprobacionId(),v.getOficinaAprobadora(),v.getVigenteDesde(),v.getVigenteHasta(),i.stream().map(this::opcion).toList());}
 private void auditar(AutorizacionEfectivaService.AsignacionEfectiva a,PlaneamientoAuthContext c,String o,Long id,Map<String,String> cambios){auditoria.registrarExito(new AuditService.AuditCommand(c.correlacionId(),a.usuarioId(),null,a.id(),a.perfil(),a.unidadId(),o,"ORGANIZACION","OBJETIVO_PEI",id,"SUCCESS",cambios,"INTERNO"));}
 private ResponseStatusException error(String code){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,code);}
}
