package pe.gob.midagri.piip.organizacion.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.gob.midagri.piip.organizacion.dto.*;
import pe.gob.midagri.piip.organizacion.service.*;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** API institucional de los catálogos independientes PEI y POI. */
@RestController
@RequestMapping("/api/v1/organizacion")
@Tag(name = "Organización - Planeamiento")
public class PlaneamientoController {
 private final ObjetivoPeiCatalogService pei; private final ActividadPoiCatalogService poi; private final IdempotencyService idempotencia; private final ObjectMapper json;
 public PlaneamientoController(ObjetivoPeiCatalogService pei, ActividadPoiCatalogService poi, IdempotencyService idempotencia, ObjectMapper json){this.pei=pei;this.poi=poi;this.idempotencia=idempotencia;this.json=json;}
 @Operation(summary="Consultar objetivos PEI vigentes")
 @GetMapping("/objetivos-pei") public List<PlaneamientoOption> objetivos(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate vigenteEn,@RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,@RequestHeader("X-Unidad-Efectiva-Id") Long unidad,@RequestHeader(value="X-Correlation-Id",required=false) String correlacion,Principal principal){return pei.listar(q,vigenteEn,contexto(principal,asignacion,unidad,correlacion));}
 @Operation(summary="Consultar actividades POI vigentes")
 @GetMapping("/actividades-poi") public List<PlaneamientoOption> actividades(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate vigenteEn,@RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,@RequestHeader("X-Unidad-Efectiva-Id") Long unidad,@RequestHeader(value="X-Correlation-Id",required=false) String correlacion,Principal principal){return poi.listar(q,vigenteEn,contexto(principal,asignacion,unidad,correlacion));}
 @Operation(summary="Crear versión inmutable de objetivos PEI")
 @PostMapping(value="/objetivos-pei/versiones",consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<PlaneamientoVersionDetail> versionPei(@Valid @RequestBody PlaneamientoVersionRequest r,@RequestHeader("Idempotency-Key") String key,@RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,@RequestHeader("X-Unidad-Efectiva-Id") Long unidad,@RequestHeader(value="X-Correlation-Id",required=false) String correlacion,Principal principal){return ResponseEntity.status(HttpStatus.CREATED).body(idempotente("VERSIONAR_OBJETIVOS_PEI",key,r,()->pei.crearVersion(r,contexto(principal,asignacion,unidad,correlacion)),principal));}
 @Operation(summary="Crear versión inmutable de actividades POI")
 @PostMapping(value="/actividades-poi/versiones",consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<PlaneamientoVersionDetail> versionPoi(@Valid @RequestBody PlaneamientoVersionRequest r,@RequestHeader("Idempotency-Key") String key,@RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,@RequestHeader("X-Unidad-Efectiva-Id") Long unidad,@RequestHeader(value="X-Correlation-Id",required=false) String correlacion,Principal principal){return ResponseEntity.status(HttpStatus.CREATED).body(idempotente("VERSIONAR_ACTIVIDADES_POI",key,r,()->poi.crearVersion(r,contexto(principal,asignacion,unidad,correlacion)),principal));}
 @GetMapping("/objetivos-pei/versiones") public List<PlaneamientoVersionDetail> versionesPei(@RequestHeader("X-Asignacion-Efectiva-Id") Long a,@RequestHeader("X-Unidad-Efectiva-Id") Long u,@RequestHeader(value="X-Correlation-Id",required=false) String c,Principal p){return pei.listarVersiones(contexto(p,a,u,c));}
 @GetMapping("/objetivos-pei/versiones/{id}") public PlaneamientoVersionDetail versionPei(@PathVariable Long id,@RequestHeader("X-Asignacion-Efectiva-Id") Long a,@RequestHeader("X-Unidad-Efectiva-Id") Long u,@RequestHeader(value="X-Correlation-Id",required=false) String c,Principal p){return pei.obtenerVersion(id,contexto(p,a,u,c));}
 @GetMapping("/actividades-poi/versiones") public List<PlaneamientoVersionDetail> versionesPoi(@RequestHeader("X-Asignacion-Efectiva-Id") Long a,@RequestHeader("X-Unidad-Efectiva-Id") Long u,@RequestHeader(value="X-Correlation-Id",required=false) String c,Principal p){return poi.listarVersiones(contexto(p,a,u,c));}
 @GetMapping("/actividades-poi/versiones/{id}") public PlaneamientoVersionDetail versionPoi(@PathVariable Long id,@RequestHeader("X-Asignacion-Efectiva-Id") Long a,@RequestHeader("X-Unidad-Efectiva-Id") Long u,@RequestHeader(value="X-Correlation-Id",required=false) String c,Principal p){return poi.obtenerVersion(id,contexto(p,a,u,c));}
 private PlaneamientoAuthContext contexto(Principal p,Long a,Long u,String c){return new PlaneamientoAuthContext(p==null?null:p.getName(),a,u,c);}
 private PlaneamientoVersionDetail idempotente(String op,String clave,PlaneamientoVersionRequest r,Supplier<PlaneamientoVersionDetail> accion,Principal p){try{String payload=json.writeValueAsString(r);var resultado=idempotencia.execute(new IdempotencyService.IdempotencyRequest("ORGANIZACION",op,clave,payload,p==null?"":p.getName()),()->{var d=accion.get();try{return new IdempotencyService.IdempotencyResponse("PLANEAMIENTO_VERSION",d.id(),json.writeValueAsString(d));}catch(Exception e){throw new IllegalStateException(e);}});return json.readValue(resultado.respuestaJson(),PlaneamientoVersionDetail.class);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("No fue posible procesar idempotencia de planeamiento.",e);}}
}
