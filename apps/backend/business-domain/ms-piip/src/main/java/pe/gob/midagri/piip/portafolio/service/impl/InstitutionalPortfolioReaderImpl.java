package pe.gob.midagri.piip.portafolio.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.entity.TransicionEstadoEntity;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.RelacionIniciativaProyectoRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.repository.TransicionEstadoRepository;
import pe.gob.midagri.piip.portafolio.repository.UnidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipantePersonaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.ProyectoParticipantePersonaRepository;
import pe.gob.midagri.piip.portafolio.service.InstitutionalPortfolioReader;
import pe.gob.midagri.piip.portafolio.entity.RelacionIniciativaProyectoEntity;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;

/**
 * Implementación JPA del lector de portafolio para la consulta
 * institucional. Aplica los filtros neutros declarados en
 * {@link InstitutionalPortfolioQuery} y limita los resultados a
 * las unidades explícitamente autorizadas. Devuelve
 * exclusivamente DTOs del módulo; las entidades JPA nunca
 * abandonan esta capa.
 */
@Service
public class InstitutionalPortfolioReaderImpl implements InstitutionalPortfolioReader {

    private final RegistroPortafolioRepository registros;
    private final UnidadResponsableRepository unidades;
    private final TitularidadResponsableRepository titularidades;
    private final TransicionEstadoRepository transiciones;
    private final RelacionIniciativaProyectoRepository relaciones;
    private final ProyectoParticipantePersonaRepository participantesPersona;
    private final UnidadEjecutoraRepository unidadesEjecutoras;
    private final InstitutionalPortfolioMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public InstitutionalPortfolioReaderImpl(
            RegistroPortafolioRepository registros,
            UnidadResponsableRepository unidades,
            TitularidadResponsableRepository titularidades,
            TransicionEstadoRepository transiciones,
            RelacionIniciativaProyectoRepository relaciones,
            ProyectoParticipantePersonaRepository participantesPersona,
            UnidadEjecutoraRepository unidadesEjecutoras) {
        this.registros = registros;
        this.unidades = unidades;
        this.titularidades = titularidades;
        this.transiciones = transiciones;
        this.relaciones = relaciones;
        this.participantesPersona = participantesPersona;
        this.unidadesEjecutoras = unidadesEjecutoras;
        this.mapper = new InstitutionalPortfolioMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionalPortfolioPage buscar(InstitutionalPortfolioQuery consulta) {
        Objects.requireNonNull(consulta, "La consulta no puede ser nula.");
        Set<Long> visibles = normalizarVisibles(consulta.unidadesVisibles());
        if (visibles.isEmpty()) {
            return new InstitutionalPortfolioPage(List.of(), consulta.paginaNormalizada(),
                    consulta.tamanioNormalizado(), 0L, 0);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegistroPortafolioEntity> cq = cb.createQuery(RegistroPortafolioEntity.class);
        Root<RegistroPortafolioEntity> root = cq.from(RegistroPortafolioEntity.class);
        List<Predicate> predicados = new ArrayList<>();
        predicados.add(root.get("unidadEjecutoraId").in(visibles));
        if (consulta.tipoRegistro() != null) {
            TipoRegistro tipo = parseTipoRegistro(consulta.tipoRegistro());
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipoRegistro"), tipo));
            }
        }
        if (consulta.codigo() != null && !consulta.codigo().isBlank()) {
            predicados.add(cb.equal(cb.upper(root.get("codigo")),
                    consulta.codigo().trim().toUpperCase()));
        }
        if (consulta.nombre() != null && !consulta.nombre().isBlank()) {
            predicados.add(cb.like(cb.upper(root.get("nombre")),
                    "%" + consulta.nombre().trim().toUpperCase() + "%"));
        }
        if (consulta.estado() != null && !consulta.estado().isBlank()) {
            EstadoIniciativa estado = parseEstado(consulta.estado());
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
        }
        if (consulta.unidadId() != null) {
            predicados.add(cb.equal(root.get("unidadEjecutoraId"), consulta.unidadId()));
        }
        if (consulta.responsableId() != null) {
            predicados.add(cb.equal(root.get("responsableId"), consulta.responsableId()));
        }
        if (consulta.fechaDesde() != null) {
            predicados.add(cb.greaterThanOrEqualTo(root.get("fechaInicio"), consulta.fechaDesde()));
        }
        if (consulta.fechaHasta() != null) {
            predicados.add(cb.lessThanOrEqualTo(root.get("fechaInicio"), consulta.fechaHasta()));
        }
        cq.where(cb.and(predicados.toArray(new Predicate[0])));
        cq.orderBy(ordenamiento(cb, root, consulta.ordenSeguro()));
        TypedQuery<RegistroPortafolioEntity> query = entityManager.createQuery(cq);
        int pagina = consulta.paginaNormalizada();
        int tamanio = consulta.tamanioNormalizado();
        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);
        List<RegistroPortafolioEntity> filas = query.getResultList();
        long total = contar(cb, predicados);
        int totalPaginas = tamanio == 0 ? 0 : (int) Math.ceil((double) total / (double) tamanio);
        List<InstitutionalPortfolioPage.InstitutionalPortfolioSummary> resumenes = filas.stream()
                .map(this::aResumen)
                .toList();
        return new InstitutionalPortfolioPage(resumenes, pagina, tamanio, total, totalPaginas);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstitutionalPortfolioProjection> obtener(Long id, List<Long> unidadesVisibles) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        Set<Long> visibles = normalizarVisibles(unidadesVisibles);
        if (visibles.isEmpty()) {
            return Optional.empty();
        }
        return registros.findById(id)
                .filter(registro -> visibles.contains(registro.getUnidadEjecutoraId()))
                .map(this::aProyeccion);
    }

    private long contar(CriteriaBuilder cb, List<Predicate> predicados) {
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<RegistroPortafolioEntity> root = cq.from(RegistroPortafolioEntity.class);
        cq.select(cb.count(root));
        cq.where(cb.and(predicados.toArray(new Predicate[0])));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private Order[] ordenamiento(CriteriaBuilder cb, Root<RegistroPortafolioEntity> root, String orden) {
        return switch (orden) {
            case "nombre" -> new Order[] { cb.asc(cb.upper(root.get("nombre"))), cb.asc(root.get("id")) };
            case "estado" -> new Order[] { cb.asc(root.get("estado")), cb.asc(root.get("id")) };
            case "fechaInicio" -> new Order[] { cb.asc(root.get("fechaInicio")), cb.asc(root.get("id")) };
            default -> new Order[] { cb.asc(root.get("codigo")), cb.asc(root.get("id")) };
        };
    }

    private InstitutionalPortfolioPage.InstitutionalPortfolioSummary aResumen(RegistroPortafolioEntity entity) {
        UnidadEjecutoraEntity unidad = entity.getUnidadEjecutoraId() == null ? null
                : unidadesEjecutoras.findById(entity.getUnidadEjecutoraId()).orElse(null);
        String etag = "\"" + entity.getId() + "-" + entity.getVersion() + "\"";
        return new InstitutionalPortfolioPage.InstitutionalPortfolioSummary(
                entity.getId(),
                entity.getTipoRegistro(),
                entity.getCodigo(),
                entity.getCodigoOrigen(),
                entity.getNombre(),
                entity.getEstado() == null ? null : entity.getEstado().name(),
                entity.getFechaInicio(),
                entity.getUnidadEjecutoraId(),
                unidad == null ? null : unidad.getNombre(),
                unidad == null ? null : unidad.getCodigo(),
                entity.getResponsableId(),
                entity.getVersion(),
                etag);
    }

    private InstitutionalPortfolioProjection aProyeccion(RegistroPortafolioEntity entity) {
        Long id = entity.getId();
        List<UnidadResponsableEntity> filasUnidad = unidades.findByRegistroPortafolioId(id);
        List<TitularidadResponsableEntity> titularidadesRegistro = titularidades
                .findByRegistroPortafolioIdOrderByInicioDesc(id);
        // Las transiciones no tienen un finder por proyecto, se cargan con criteria
        List<TransicionEstadoEntity> transicionesRegistro = entityManager
                .createQuery("select t from TransicionEstadoEntity t "
                        + "where t.registroPortafolioId = :id order by t.fechaTransicion asc",
                        TransicionEstadoEntity.class)
                .setParameter("id", id)
                .getResultList();
        // Relación iniciativa-proyecto: la iniciativa es la fila que tiene
        // iniciativaId = id; el derivado es la fila con proyectoId = id.
        RelacionIniciativaProyectoEntity relacion = relaciones.findByIniciativaId(id)
                .or(() -> relaciones.findByProyectoId(id))
                .orElse(null);
        Long iniciativaOrigenId = relacion == null ? null
                : id.equals(relacion.getIniciativaId()) ? id : relacion.getIniciativaId();
        Long proyectoDerivadoId = relacion == null ? null
                : id.equals(relacion.getProyectoId()) ? id : relacion.getProyectoId();
        List<ProyectoParticipantePersonaEntity> participantes = participantesPersona
                .findByIdProyectoOrderByInicioAsc(id);
        return mapper.proyectar(entity, filasUnidad, titularidadesRegistro, transicionesRegistro,
                participantes, iniciativaOrigenId, proyectoDerivadoId);
    }

    private static Set<Long> normalizarVisibles(List<Long> unidades) {
        if (unidades == null) {
            return Set.of();
        }
        return unidades.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    private static EstadoIniciativa parseEstado(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return EstadoIniciativa.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static TipoRegistro parseTipoRegistro(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return TipoRegistro.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Mapper estático anidado que convierte las entidades JPA
     * cargadas en DTOs del módulo {@code portafolio}.
     */
    private static final class InstitutionalPortfolioMapper {

        InstitutionalPortfolioProjection proyectar(RegistroPortafolioEntity entity,
                List<UnidadResponsableEntity> unidadesRegistradas,
                List<TitularidadResponsableEntity> titularidades,
                List<TransicionEstadoEntity> transiciones,
                List<ProyectoParticipantePersonaEntity> participantes,
                Long iniciativaOrigenId,
                Long proyectoDerivadoId) {
            List<InstitutionalPortfolioProjection.InstitutionalUnidadProjection> unidadesDto = unidadesRegistradas
                    .stream()
                    .sorted(Comparator.comparing(
                            UnidadResponsableEntity::getNroOrden,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(unidad -> new InstitutionalPortfolioProjection.InstitutionalUnidadProjection(
                            unidad.getId(),
                            null,
                            unidad.getDescripcion(),
                            unidad.getAbreviatura(),
                            unidad.getNroOrden()))
                    .toList();
            List<InstitutionalPortfolioProjection.InstitutionalTitularidadProjection> titularidadesDto = titularidades
                    .stream()
                    .map(titular -> new InstitutionalPortfolioProjection.InstitutionalTitularidadProjection(
                            titular.getId(),
                            titular.getUsuarioId(),
                            titular.getInicio(),
                            titular.getFin(),
                            titular.getMotivoSustitucion(),
                            titular.getActorSustitucionId()))
                    .toList();
            List<InstitutionalPortfolioProjection.InstitutionalTransitionProjection> transicionesDto = transiciones
                    .stream()
                    .map(transicion -> new InstitutionalPortfolioProjection.InstitutionalTransitionProjection(
                            transicion.getId(),
                            transicion.getEstadoAnterior() == null ? null
                                    : transicion.getEstadoAnterior().name(),
                            transicion.getEstadoNuevo() == null ? null
                                    : transicion.getEstadoNuevo().name(),
                            transicion.getUsuarioId(),
                            transicion.getRolEfectivoId(),
                            transicion.getUnidadEfectivaId(),
                            transicion.getFechaTransicion(),
                            transicion.getObservaciones(),
                            transicion.getDocumentoRefId()))
                    .toList();
            List<InstitutionalPortfolioProjection.InstitutionalParticipanteProjection> participantesDto = participantes
                    .stream()
                    .map(participante -> new InstitutionalPortfolioProjection.InstitutionalParticipanteProjection(
                            participante.getId(),
                            participante.getIdParticipante(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            participante.getInicio(),
                            participante.getFin()))
                    .toList();
            return new InstitutionalPortfolioProjection(
                    entity.getId(),
                    entity.getTipoRegistro() == null ? null : entity.getTipoRegistro().name(),
                    entity.getCodigo(),
                    entity.getCodigoOrigen(),
                    entity.getFechaInicio(),
                    entity.getFechaCierre(),
                    entity.getNombre(),
                    entity.getTipoSolucion() == null ? null : entity.getTipoSolucion().name(),
                    entity.getFuenteOrigen() == null ? null : entity.getFuenteOrigen().name(),
                    entity.getDetalleFuente(),
                    entity.getResponsableId(),
                    entity.getProblemaPublico(),
                    entity.getSolucionPropuesta(),
                    entity.getObjetivoPeiId(),
                    entity.getActividadPoiId(),
                    entity.getUnidadEjecutoraId(),
                    entity.getEstado() == null ? null : entity.getEstado().name(),
                    entity.getComponenteDigital(),
                    entity.getDetalleComponenteDigital(),
                    entity.getNota(),
                    entity.getResultadosClave(),
                    entity.getVersion(),
                    entity.getFechaCreacion(),
                    iniciativaOrigenId,
                    proyectoDerivadoId,
                    unidadesDto,
                    titularidadesDto,
                    transicionesDto,
                    participantesDto);
        }
    }
}
