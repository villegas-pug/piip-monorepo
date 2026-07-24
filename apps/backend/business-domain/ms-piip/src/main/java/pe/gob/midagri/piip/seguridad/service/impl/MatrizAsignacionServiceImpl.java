package pe.gob.midagri.piip.seguridad.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.MatrixAuthContext;
import pe.gob.midagri.piip.seguridad.dto.MatrixCombination;
import pe.gob.midagri.piip.seguridad.dto.MatrixCombinationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixDeactivationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixFunctionRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixFunction;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionDetail;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionRequest;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionalVersionEntity;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionalVersionRepository;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.MatrizAsignacionService;

/** Fuente única de versionado append-only de la matriz funcional aprobada externamente. */
@Service
public class MatrizAsignacionServiceImpl implements MatrizAsignacionService {
    private static final String GLOBAL_ADMIN = "GlobalAdmin";
    private static final String AUTORIDAD = "Autoridad";
    private final MatrizFuncionalVersionRepository versiones;
    private final MatrizFuncionRepository funciones;
    private final MatrizFuncionPerfilUnidadRepository combinaciones;
    private final RolRepository roles;
    private final UnidadEjecutoraRepository unidades;
    private final UsuarioRepository usuarios;
    private final UsuarioRolUnidadRepository asignaciones;
    private final DocumentoService documentos;
    private final AutorizacionEfectivaService autorizacion;
    private final AuditService auditoria;

    public MatrizAsignacionServiceImpl(MatrizFuncionalVersionRepository versiones,
            MatrizFuncionRepository funciones, MatrizFuncionPerfilUnidadRepository combinaciones,
            RolRepository roles, UnidadEjecutoraRepository unidades, UsuarioRepository usuarios,
            UsuarioRolUnidadRepository asignaciones, DocumentoService documentos,
            AutorizacionEfectivaService autorizacion, AuditService auditoria) {
        this.versiones = versiones; this.funciones = funciones; this.combinaciones = combinaciones;
        this.roles = roles; this.unidades = unidades; this.usuarios = usuarios;
        this.asignaciones = asignaciones; this.documentos = documentos;
        this.autorizacion = autorizacion; this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public MatrixVersionDetail crearVersion(MatrixVersionRequest request, MatrixAuthContext contexto) {
        var registrador = autorizarRegistro(contexto);
        validarVersion(request, contexto, registrador);
        if (versiones.findByCodigoVersion(request.codigoVersion().trim()).isPresent()) {
            throw rechazar(contexto, registrador, "MATRIX_VERSION_DUPLICATE", null);
        }
        validarDocumento(request.documentoAprobacionVersionId(), contexto, registrador, null);
        Long anterior = request.versionAnteriorId() != null ? request.versionAnteriorId()
                : versiones.findAllByOrderByIdDesc(PageRequest.of(0, 1)).stream().findFirst()
                        .map(MatrizFuncionalVersionEntity::getId).orElse(null);
        if (anterior != null && versiones.findById(anterior).isEmpty()) {
            throw rechazar(contexto, registrador, "MATRIX_PREVIOUS_VERSION_NOT_FOUND", anterior);
        }
        if (anterior != null && versiones.existsByVersionAnteriorId(anterior)) {
            throw rechazar(contexto, registrador, "MATRIX_VERSION_SUPERSEDED", anterior);
        }
        var version = nuevaVersion(request.codigoVersion(), anterior, request.documentoAprobacionVersionId(),
                request.vigenteDesde(), request.vigenteHasta(), contexto.actorSub());
        version = versiones.save(version);
        final Long versionId = version.getId();
        Map<String, MatrizFuncionEntity> porCodigo = guardarFunciones(versionId, request.funciones(),
                contexto, registrador);
        validarCombinacionesUnicas(request.combinaciones(), contexto, registrador);
        var guardadas = request.combinaciones().stream().map(c -> guardarCombinacion(versionId,
                porCodigo, c, registrador, contexto)).toList();
        auditarExito(contexto, registrador, "CREAR_VERSION_MATRIZ", versionId,
                Map.of("codigoVersion", version.getCodigoVersion(), "combinaciones", String.valueOf(guardadas.size())));
        return detalle(version, funciones.findByVersionIdOrderByCodigoAsc(versionId), guardadas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatrixFunction> listarFunciones(MatrixAuthContext contexto) {
        var consultor = autorizarConsulta(contexto);
        var resultado = funciones.findAll().stream().sorted(java.util.Comparator
                .comparing(MatrizFuncionEntity::getVersionId).reversed()
                .thenComparing(MatrizFuncionEntity::getCodigo))
                .map(f -> new MatrixFunction(f.getId(), f.getVersionId(), f.getCodigo(), f.getDescripcion(),
                        "S".equals(f.getActiva()))).toList();
        auditarExito(contexto, consultor, "CONSULTAR_FUNCIONES_MATRIZ", null,
                Map.of("total", String.valueOf(resultado.size())));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatrixVersionDetail> listarVersiones(int pagina, int tamanio, MatrixAuthContext contexto) {
        var consultor = autorizarConsulta(contexto);
        var resultado = versiones.findAllByOrderByIdDesc(PageRequest.of(pagina, tamanio))
                .map(v -> detalle(v, funciones.findByVersionIdOrderByCodigoAsc(v.getId()),
                        combinaciones.findByVersionIdOrderByIdAsc(v.getId())));
        auditarExito(contexto, consultor, "CONSULTAR_VERSIONES_MATRIZ", null,
                Map.of("total", String.valueOf(resultado.getTotalElements())));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatrixCombination> listarCombinaciones(Long versionId, MatrixAuthContext contexto) {
        var consultor = autorizarConsulta(contexto);
        if (versiones.findById(versionId).isEmpty()) {
            registrarDenegacion(contexto, consultor, "MATRIX_VERSION_NOT_FOUND", versionId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MATRIX_VERSION_NOT_FOUND");
        }
        Map<Long, String> funcion = funciones.findByVersionIdOrderByCodigoAsc(versionId).stream()
                .collect(java.util.stream.Collectors.toMap(MatrizFuncionEntity::getId, MatrizFuncionEntity::getCodigo));
        var resultado = combinaciones.findByVersionIdOrderByIdAsc(versionId).stream().map(c -> combinacion(c,
                funcion.get(c.getFuncionId()), nombreRol(c.getRolId()))).toList();
        auditarExito(contexto, consultor, "CONSULTAR_COMBINACIONES_MATRIZ", versionId,
                Map.of("total", String.valueOf(resultado.size())));
        return resultado;
    }

    @Override
    @Transactional
    public MatrixVersionDetail inactivarCombinacion(Long combinacionId, MatrixDeactivationRequest request,
            MatrixAuthContext contexto) {
        var registrador = autorizarRegistro(contexto);
        var original = combinaciones.findById(combinacionId).orElseThrow(() ->
                rechazar(contexto, registrador, "MATRIX_COMBINATION_NOT_FOUND", combinacionId));
        if (versiones.findByCodigoVersion(request.codigoNuevaVersion().trim()).isPresent()) {
            throw rechazar(contexto, registrador, "MATRIX_VERSION_DUPLICATE", null);
        }
        validarDocumento(request.documentoAprobacionVersionId(), contexto, registrador, combinacionId);
        validarAprobador(request.aprobadorUsuarioId(), original.getRolId(), original.getUnidadId(), contexto,
                registrador, combinacionId);
        var fuente = versiones.findById(original.getVersionId()).orElseThrow(() ->
                rechazar(contexto, registrador, "MATRIX_VERSION_NOT_FOUND", original.getVersionId()));
        if (versiones.existsByVersionAnteriorId(fuente.getId())) {
            throw rechazar(contexto, registrador, "MATRIX_VERSION_SUPERSEDED", fuente.getId());
        }
        var nueva = versiones.save(nuevaVersion(request.codigoNuevaVersion(), fuente.getId(),
                request.documentoAprobacionVersionId(), LocalDate.now(), null, contexto.actorSub()));
        Map<Long, MatrizFuncionEntity> funcionesNuevas = new HashMap<>();
        for (var funcion : funciones.findByVersionIdOrderByCodigoAsc(fuente.getId())) {
            var copia = new MatrizFuncionEntity(); copia.setVersionId(nueva.getId()); copia.setCodigo(funcion.getCodigo());
            copia.setDescripcion(funcion.getDescripcion()); copia.setActiva(funcion.getActiva());
            funcionesNuevas.put(funcion.getId(), funciones.save(copia));
        }
        var copias = combinaciones.findByVersionIdOrderByIdAsc(fuente.getId()).stream().map(c -> {
            var copia = copiarCombinacion(c, nueva.getId(), funcionesNuevas.get(c.getFuncionId()).getId(),
                    registrador.usuarioId(), contexto.actorSub());
            if (c.getId().equals(combinacionId)) {
                copia.setActiva("N"); copia.setDocumentoAprobacionId(request.documentoAprobacionVersionId());
                copia.setAprobadorId(request.aprobadorUsuarioId());
            }
            return combinaciones.save(copia);
        }).toList();
        auditarExito(contexto, registrador, "INACTIVAR_COMBINACION_MATRIZ", nueva.getId(),
                Map.of("combinacionAnteriorId", combinacionId.toString(), "motivo", request.motivo().trim()));
        return detalle(nueva, funciones.findByVersionIdOrderByCodigoAsc(nueva.getId()), copias);
    }

    private Map<String, MatrizFuncionEntity> guardarFunciones(Long versionId, List<MatrixFunctionRequest> entradas,
            MatrixAuthContext contexto, AutorizacionEfectivaService.AsignacionEfectiva registrador) {
        Set<String> codigos = new HashSet<>(); Map<String, MatrizFuncionEntity> resultado = new HashMap<>();
        for (var entrada : entradas) {
            String codigo = entrada.codigo().trim();
            if (!codigos.add(codigo)) throw rechazar(contexto, registrador, "MATRIX_FUNCTION_DUPLICATE", null);
            var funcion = new MatrizFuncionEntity(); funcion.setVersionId(versionId); funcion.setCodigo(codigo);
            funcion.setDescripcion(entrada.descripcion().trim()); funcion.setActiva("S");
            resultado.put(codigo, funciones.save(funcion));
        }
        return resultado;
    }

    private MatrizFuncionPerfilUnidadEntity guardarCombinacion(Long versionId,
            Map<String, MatrizFuncionEntity> funcionesPorCodigo, MatrixCombinationRequest entrada,
            AutorizacionEfectivaService.AsignacionEfectiva registrador, MatrixAuthContext contexto) {
        if (entrada.vigenteHasta() != null && entrada.vigenteHasta().isBefore(entrada.vigenteDesde())) {
            throw rechazar(contexto, registrador, "INVALID_VALIDITY_PERIOD", null);
        }
        var funcion = funcionesPorCodigo.get(entrada.funcionCodigo().trim());
        if (funcion == null) throw rechazar(contexto, registrador, "MATRIX_FUNCTION_NOT_FOUND", null);
        var rol = roles.findByNombre(entrada.perfil().trim()).orElseThrow(() ->
                rechazar(contexto, registrador, "MATRIX_PROFILE_NOT_FOUND", null));
        if (!unidades.existsById(entrada.unidadId())) throw rechazar(contexto, registrador,
                "MATRIX_CONCRETE_UNIT_MISMATCH", null);
        validarDocumento(entrada.documentoAprobacionVersionId(), contexto, registrador, null);
        validarAprobador(entrada.aprobadorUsuarioId(), rol.getId(), entrada.unidadId(), contexto, registrador, null);
        var combinacion = new MatrizFuncionPerfilUnidadEntity();
        combinacion.setVersionId(versionId); combinacion.setFuncionId(funcion.getId()); combinacion.setRolId(rol.getId());
        combinacion.setUnidadId(entrada.unidadId()); combinacion.setAprobadorId(entrada.aprobadorUsuarioId());
        combinacion.setRegistradorId(registrador.usuarioId());
        combinacion.setDocumentoAprobacionId(entrada.documentoAprobacionVersionId());
        combinacion.setVigenteDesde(entrada.vigenteDesde()); combinacion.setVigenteHasta(entrada.vigenteHasta());
        combinacion.setActiva("S"); combinacion.setEsBootstrap("N"); combinacion.setCreadoPor(contexto.actorSub());
        return combinaciones.save(combinacion);
    }

    private void validarCombinacionesUnicas(List<MatrixCombinationRequest> entradas, MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador) {
        Set<String> claves = new HashSet<>();
        for (var entrada : entradas) {
            String clave = entrada.funcionCodigo().trim() + "|" + entrada.perfil().trim() + "|" + entrada.unidadId();
            if (!claves.add(clave)) throw rechazar(contexto, registrador, "MATRIX_COMBINATION_DUPLICATE", null);
        }
    }

    private void validarAprobador(Long aprobadorId, Integer rolAfectado, Long unidadId, MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador, Long recursoId) {
        UsuarioEntity aprobador = usuarios.findById(aprobadorId).orElseThrow(() ->
                rechazar(contexto, registrador, "MATRIX_APPROVER_INVALID", recursoId));
        if (!"S".equals(aprobador.getActivo()) || aprobadorId.equals(registrador.usuarioId())) {
            throw rechazar(contexto, registrador, "MATRIX_APPROVER_INVALID", recursoId);
        }
        Integer autoridadId = roles.findByNombre(AUTORIDAD).map(RolEntity::getId).orElseThrow(() ->
                rechazar(contexto, registrador, "MATRIX_APPROVER_INVALID", recursoId));
        if (!asignaciones.existsAsignacionVigente(aprobadorId, autoridadId, unidadId)) {
            throw rechazar(contexto, registrador, "MATRIX_APPROVER_INVALID", recursoId);
        }
    }

    private void validarVersion(MatrixVersionRequest request, MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador) {
        if (request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw rechazar(contexto, registrador, "INVALID_VALIDITY_PERIOD", null);
        }
    }

    private void validarDocumento(Long documentoId, MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador, Long recursoId) {
        if (!documentos.validarDocumentoInstitucionalAprobatorio(documentoId).valido()) {
            throw rechazar(contexto, registrador, "MATRIX_APPROVAL_REQUIRED", recursoId);
        }
    }

    private MatrizFuncionalVersionEntity nuevaVersion(String codigo, Long anterior, Long documento, LocalDate desde,
            LocalDate hasta, String actor) {
        var version = new MatrizFuncionalVersionEntity(); version.setCodigoVersion(codigo.trim());
        version.setVersionAnteriorId(anterior); version.setDocumentoAprobacionId(documento);
        version.setVigenteDesde(desde); version.setVigenteHasta(hasta); version.setActiva("S");
        version.setCreadoPor(actor); return version;
    }

    private MatrizFuncionPerfilUnidadEntity copiarCombinacion(MatrizFuncionPerfilUnidadEntity origen, Long versionId,
            Long funcionId, Long registradorId, String actor) {
        var copia = new MatrizFuncionPerfilUnidadEntity(); copia.setVersionId(versionId); copia.setFuncionId(funcionId);
        copia.setRolId(origen.getRolId()); copia.setUnidadId(origen.getUnidadId());
        copia.setAprobadorId(origen.getAprobadorId()); copia.setRegistradorId(registradorId);
        copia.setDocumentoAprobacionId(origen.getDocumentoAprobacionId()); copia.setVigenteDesde(origen.getVigenteDesde());
        copia.setVigenteHasta(origen.getVigenteHasta()); copia.setActiva(origen.getActiva());
        copia.setEsBootstrap("N"); copia.setCreadoPor(actor); return copia;
    }

    private MatrixVersionDetail detalle(MatrizFuncionalVersionEntity version, List<MatrizFuncionEntity> funcionesVersion,
            List<MatrizFuncionPerfilUnidadEntity> combinacionesVersion) {
        Map<Long, String> porFuncion = funcionesVersion.stream().collect(java.util.stream.Collectors.toMap(
                MatrizFuncionEntity::getId, MatrizFuncionEntity::getCodigo));
        return new MatrixVersionDetail(version.getId(), version.getCodigoVersion(), version.getVersionAnteriorId(),
                version.getDocumentoAprobacionId(), version.getVigenteDesde(), version.getVigenteHasta(),
                "S".equals(version.getActiva()), funcionesVersion.stream().map(f -> new MatrixFunctionRequest(
                        f.getCodigo(), f.getDescripcion())).toList(), combinacionesVersion.stream().map(c ->
                        combinacion(c, porFuncion.get(c.getFuncionId()), nombreRol(c.getRolId()))).toList());
    }

    private MatrixCombination combinacion(MatrizFuncionPerfilUnidadEntity c, String funcion, String perfil) {
        return new MatrixCombination(c.getId(), c.getVersionId(), funcion, perfil, c.getUnidadId(),
                c.getVigenteDesde(), c.getVigenteHasta(), "S".equals(c.getActiva()),
                c.getDocumentoAprobacionId(), c.getAprobadorId(), c.getRegistradorId());
    }

    private String nombreRol(Integer rolId) { return roles.findById(rolId).map(RolEntity::getNombre).orElse("DESCONOCIDO"); }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizarRegistro(MatrixAuthContext contexto) {
        try { return autorizacion.revalidarParaOperacionSensible(contexto.actorSub(), contexto.asignacionEfectivaId(),
                GLOBAL_ADMIN, contexto.unidadEfectivaId()); }
        catch (ResponseStatusException ex) { auditarDenegacion(contexto, ex.getReason()); throw ex; }
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizarConsulta(MatrixAuthContext contexto) {
        try { return autorizacion.revalidarAsignacionInstitucional(contexto.actorSub(), contexto.asignacionEfectivaId(),
                contexto.unidadEfectivaId()); }
        catch (ResponseStatusException ex) { auditarDenegacion(contexto, ex.getReason()); throw ex; }
    }

    private ResponseStatusException rechazar(MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador, String codigo, Long recursoId) {
        registrarDenegacion(contexto, registrador, codigo, recursoId);
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, codigo);
    }

    private void registrarDenegacion(MatrixAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva registrador, String codigo, Long recursoId) {
        auditoria.registrarDenegacion(new AuditService.AuditCommand(contexto.correlacionId(), registrador.usuarioId(),
                contexto.actorSub(), registrador.id(), registrador.perfil(), registrador.unidadId(), "GESTIONAR_MATRIZ",
                "SEGURIDAD", "MATRIZ_FUNCIONAL", recursoId, codigo, Map.of(), "INTERNO"));
    }

    private void auditarDenegacion(MatrixAuthContext contexto, String codigo) {
        auditoria.registrarDenegacion(new AuditService.AuditCommand(contexto == null ? null : contexto.correlacionId(),
                null, contexto == null ? null : contexto.actorSub(), contexto == null ? null : contexto.asignacionEfectivaId(),
                null, contexto == null ? null : contexto.unidadEfectivaId(), "GESTIONAR_MATRIZ", "SEGURIDAD",
                "MATRIZ_FUNCIONAL", null, codigo, Map.of(), "INTERNO"));
    }

    private void auditarExito(MatrixAuthContext contexto, AutorizacionEfectivaService.AsignacionEfectiva registrador,
            String operacion, Long recursoId, Map<String, String> cambios) {
        auditoria.registrarExito(new AuditService.AuditCommand(contexto.correlacionId(), registrador.usuarioId(), null,
                registrador.id(), registrador.perfil(), registrador.unidadId(), operacion, "SEGURIDAD",
                "MATRIZ_FUNCIONAL_VERSION", recursoId, "SUCCESS", cambios, "INTERNO"));
    }
}
