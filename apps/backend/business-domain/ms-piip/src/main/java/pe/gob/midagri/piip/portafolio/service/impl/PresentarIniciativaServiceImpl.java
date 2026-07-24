package pe.gob.midagri.piip.portafolio.service.impl;

import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.CreateInitiativeRequest;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.repository.UnidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.PresentarIniciativaService;

/**
 * Implementación de presentación de iniciativa.
 * Valida los campos oficiales 1, 5-13 y 22, exige exactamente un titular, un objetivo PEI, una
 * actividad POI y una unidad principal, exige la ficha con SHA-256 (validada en admisibilidad
 * por el Evaluador y registrada como {@code fichaDocumentoVersionId}), genera el código bajo
 * {@code PESSIMISTIC_WRITE}, fija el estado {@code PRESENTADO} y registra auditoría exitosa
 * en la misma transacción. La operación se ejecuta bajo {@link IdempotencyService} con clave
 * aportada por el cliente y hash canónico del payload.
 */
@Service
public class PresentarIniciativaServiceImpl implements PresentarIniciativaService {

    private static final String CONSUMIDOR_IDEMPOTENCIA = "PORTAFOLIO";
    private static final String OPERACION_PRESENTAR = "PRESENTAR_INICIATIVA";
    private static final String RECURSO_TIPO = "REGISTRO";

    private final RegistroPortafolioRepository registroRepository;
    private final UnidadResponsableRepository unidadResponsableRepository;
    private final TitularidadResponsableRepository titularidadRepository;
    private final CatalogoUnidadReader catalogoUnidadReader;
    private final CodigoProyectoService codigoProyectoService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PresentarIniciativaServiceImpl(
            RegistroPortafolioRepository registroRepository,
            UnidadResponsableRepository unidadResponsableRepository,
            TitularidadResponsableRepository titularidadRepository,
            CatalogoUnidadReader catalogoUnidadReader,
            CodigoProyectoService codigoProyectoService,
            AuditService auditService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.registroRepository = registroRepository;
        this.unidadResponsableRepository = unidadResponsableRepository;
        this.titularidadRepository = titularidadRepository;
        this.catalogoUnidadReader = catalogoUnidadReader;
        this.codigoProyectoService = codigoProyectoService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public InitiativeDetail presentar(CreateInitiativeRequest comando, PortafolioAuthContext contexto,
            String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_KEY_REQUIRED",
                    "La presentación exige el header Idempotency-Key.");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La presentación exige el cuerpo serializado para calcular el hash canónico.");
        }

        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR_IDEMPOTENCIA, OPERACION_PRESENTAR, idempotencyKey, payloadJson,
                contexto.actorSub() == null ? "unknown" : contexto.actorSub());

        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request,
                () -> ejecutarPresentacion(comando, contexto));

        return deserializarDetalle(resultado.respuestaJson());
    }

    @Transactional
    IdempotencyService.IdempotencyResponse ejecutarPresentacion(
            CreateInitiativeRequest comando, PortafolioAuthContext contexto) {
        validarCamposOficiales(comando);
        validarCardinalidades(comando);

        String nombreTrim = comando.nombre().trim();
        String problemaTrim = comando.problemaPublico().trim();
        String solucionTrim = comando.solucionPropuesta() != null ? comando.solucionPropuesta().trim() : null;
        String detalleFuenteTrim = comando.detalleFuente() != null ? comando.detalleFuente().trim() : null;
        String detalleDigitalTrim = comando.detalleComponenteDigital() != null
                ? comando.detalleComponenteDigital().trim() : null;
        String notaTrim = comando.nota() != null ? comando.nota().trim() : null;

        Long unidadPrincipalId = comando.unidades().stream()
                .filter(CreateInitiativeRequest.UnidadResponsableItem::principal)
                .map(CreateInitiativeRequest.UnidadResponsableItem::unidadId)
                .findFirst()
                .orElseThrow(PortafolioValidationException::unidadPrincipalCardinality);

        String prefijo = catalogoUnidadReader.prefijoUnidad(unidadPrincipalId)
                .orElseThrow(PortafolioValidationException::prefijoNoDisponible);

        Integer anio = Year.now().getValue();
        String codigo = codigoProyectoService.generarCodigo(anio, unidadPrincipalId, prefijo);

        RegistroPortafolioEntity registro = new RegistroPortafolioEntity();
        registro.setCodigo(codigo);
        registro.setCodigoPrefijo(prefijo);
        registro.setCodigoOrigen(null);
        registro.setTipoRegistro(TipoRegistro.INICIATIVA);
        registro.setNombre(nombreTrim);
        registro.setTipoSolucion(comando.tipoSolucion());
        registro.setFuenteOrigen(comando.fuenteOrigen());
        registro.setDetalleFuente(detalleFuenteTrim);
        registro.setDescripcion(problemaTrim);
        registro.setProblemaPublico(problemaTrim);
        registro.setSolucionPropuesta(solucionPropuestaOVacia(solucionTrim));
        registro.setObjetivoPeiId(comando.objetivoPeiId());
        registro.setActividadPoiId(comando.actividadPoiId());
        registro.setEstado(EstadoIniciativa.PRESENTADO);
        registro.setFechaInicio(LocalDate.now());
        registro.setUnidadEjecutoraId(unidadPrincipalId);
        registro.setResponsableId(comando.responsableId());
        registro.setComponenteDigital(Boolean.TRUE.equals(comando.componenteDigital()) ? "S" : "N");
        registro.setDetalleComponenteDigital(detalleDigitalTrim);
        registro.setNota(notaTrim);
        registro.setCreadoPor(contexto.actorSub());
        registro.setSubsanacionActiva("N");

        // Texto legacy PEI/POI queda nullable para nuevos registros.
        registro.setObjetivoPei(null);
        registro.setActividadPoi(null);

        registro = registroRepository.save(registro);

        for (CreateInitiativeRequest.UnidadResponsableItem item : comando.unidades()) {
            UnidadResponsableEntity unidad = new UnidadResponsableEntity();
            unidad.setRegistroPortafolioId(registro.getId());
            unidad.setNroOrden(item.unidadId().intValue());
            unidad.setDescripcion("Unidad " + item.unidadId());
            unidad.setAbreviatura("U" + item.unidadId());
            unidadResponsableRepository.save(unidad);
        }

        TitularidadResponsableEntity titularidad = new TitularidadResponsableEntity();
        titularidad.setRegistroPortafolioId(registro.getId());
        titularidad.setUsuarioId(comando.responsableId());
        titularidad.setInicio(LocalDate.now());
        titularidad.setCreadoPor(contexto.actorSub());
        titularidadRepository.save(titularidad);

        // Auditamos dentro de la misma transacción de negocio.
        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OPERACION_PRESENTAR,
                CONSUMIDOR_IDEMPOTENCIA,
                RECURSO_TIPO,
                registro.getId(),
                "SUCCESS",
                Map.of(
                        "codigo", codigo,
                        "tipoRegistro", TipoRegistro.INICIATIVA.name(),
                        "unidadPrincipalId", String.valueOf(unidadPrincipalId)),
                "INTERNO"));

        String etag = "\"" + registro.getId() + "-" + registro.getVersion() + "\"";

        InitiativeDetail detalle = new InitiativeDetail(
                registro.getId(),
                registro.getTipoRegistro(),
                registro.getCodigo(),
                registro.getCodigoOrigen(),
                registro.getFechaInicio(),
                registro.getNombre(),
                registro.getTipoSolucion(),
                registro.getFuenteOrigen(),
                registro.getDetalleFuente(),
                registro.getResponsableId(),
                registro.getProblemaPublico(),
                registro.getSolucionPropuesta(),
                registro.getObjetivoPeiId(),
                registro.getActividadPoiId(),
                java.util.List.of(),
                registro.getEstado(),
                "S".equals(registro.getComponenteDigital()),
                registro.getDetalleComponenteDigital(),
                registro.getNota(),
                registro.getVersion(),
                etag,
                registro.getFechaCreacion());

        String detalleJson = serializarDetalle(detalle);
        return new IdempotencyService.IdempotencyResponse(RECURSO_TIPO, registro.getId(), detalleJson);
    }

    private void validarCamposOficiales(CreateInitiativeRequest comando) {
        if (comando.nombre() == null || comando.nombre().isBlank()) {
            throw PortafolioValidationException.campoRequerido(5, "Nombre");
        }
        if (comando.tipoSolucion() == null) {
            throw PortafolioValidationException.campoRequerido(6, "Tipo de solución");
        }
        if (comando.fuenteOrigen() == null) {
            throw PortafolioValidationException.campoRequerido(7, "Fuente u origen");
        }
        Set<String> catalogosActivos = Set.of(FuenteOrigen.FICHA_INICIATIVA.name(),
                FuenteOrigen.CONCURSO_INTERNO.name(), FuenteOrigen.INNOVACION_ABIERTA.name(),
                FuenteOrigen.PROPUESTA_JEFATURA.name(), FuenteOrigen.OTROS.name());
        if (!catalogosActivos.contains(comando.fuenteOrigen().name())) {
            throw new PortafolioValidationException("CATALOG_NOT_ACTIVE",
                    "El valor de Fuente u origen no pertenece al catálogo canónico 019.");
        }
        if (comando.fuenteOrigen() == FuenteOrigen.OTROS
                && (comando.detalleFuente() == null || comando.detalleFuente().isBlank())) {
            throw PortafolioValidationException.fuenteOtrosSinDetalle();
        }
        if (comando.responsableId() == null) {
            throw PortafolioValidationException.campoRequerido(8, "Responsable");
        }
        if (comando.problemaPublico() == null || comando.problemaPublico().isBlank()) {
            throw PortafolioValidationException.campoRequerido(9, "Descripción (problema público)");
        }
        if (comando.objetivoPeiId() == null) {
            throw PortafolioValidationException.campoRequerido(10, "Objetivo PEI");
        }
        if (comando.actividadPoiId() == null) {
            throw PortafolioValidationException.campoRequerido(11, "Actividad POI");
        }
        if (comando.unidades() == null || comando.unidades().isEmpty()) {
            throw PortafolioValidationException.campoRequerido(12, "Unidades responsables");
        }
        if (comando.componenteDigital() == null) {
            throw PortafolioValidationException.campoRequerido(22, "Componente digital");
        }
        if (Boolean.TRUE.equals(comando.componenteDigital())
                && (comando.detalleComponenteDigital() == null || comando.detalleComponenteDigital().isBlank())) {
            throw PortafolioValidationException.componenteDigitalSinDetalle();
        }
        if (comando.fichaDocumentoVersionId() == null) {
            throw PortafolioValidationException.campoRequerido(13, "Ficha de iniciativa");
        }
        if (comando.nombre().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El nombre no puede exceder 500 caracteres.");
        }
        if (comando.problemaPublico().length() > 2000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La descripción del problema público no puede exceder 2000 caracteres.");
        }
        if (comando.solucionPropuesta() != null && comando.solucionPropuesta().length() > 2000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La solución propuesta no puede exceder 2000 caracteres.");
        }
        if (comando.detalleFuente() != null && comando.detalleFuente().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El detalle de la fuente no puede exceder 500 caracteres.");
        }
        if (comando.detalleComponenteDigital() != null && comando.detalleComponenteDigital().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El detalle del componente digital no puede exceder 500 caracteres.");
        }
        if (comando.nota() != null && comando.nota().length() > 1000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La nota no puede exceder 1000 caracteres.");
        }
    }

    private void validarCardinalidades(CreateInitiativeRequest comando) {
        long principalCount = comando.unidades().stream()
                .filter(CreateInitiativeRequest.UnidadResponsableItem::principal)
                .count();
        if (principalCount != 1) {
            throw PortafolioValidationException.unidadPrincipalCardinality();
        }
        long unidadesDistintas = comando.unidades().stream()
                .map(CreateInitiativeRequest.UnidadResponsableItem::unidadId)
                .distinct()
                .count();
        if (unidadesDistintas != comando.unidades().size()) {
            throw new PortafolioValidationException("UNIT_DUPLICATE",
                    "Una unidad responsable no puede aparecer más de una vez en la presentación.");
        }
    }

    private String solucionPropuestaOVacia(String solucionTrim) {
        return (solucionTrim == null || solucionTrim.isBlank()) ? null : solucionTrim;
    }

    private String serializarDetalle(InitiativeDetail detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la iniciativa para la respuesta idempotente.",
                    exception);
        }
    }

    private InitiativeDetail deserializarDetalle(String json) {
        try {
            return objectMapper.readValue(json, InitiativeDetail.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la iniciativa.", exception);
        }
    }
}


