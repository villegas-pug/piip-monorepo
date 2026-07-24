package pe.gob.midagri.piip.portafolio.seguimiento.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloVersionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloProyectoEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloVersionEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ParticipantePersonaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PlanificacionProyectoEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PresentacionProductoFinalEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipantePersonaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipanteUnidadEntity;

/**
 * MapStruct mapper entre entidades JPA de seguimiento del proyecto
 * y sus DTOs HTTP canonicos (US4, Constitucion 5.0.0).
 *
 * <p>El mapper centraliza la composicion de la cabecera ETag a
 * partir del id y la version, garantizando que clientes y
 * servicios converjan en el mismo formato ("id-version" con
 * comillas dobles). Se modela como clase abstracta para que
 * MapStruct genere la implementacion concreta y pueda reutilizar
 * los helpers Named sin colisionar con la API publica.
 *
 * <p>Para los participantes y la presentacion del producto
 * final, el mapper recibe la regla de rol como parametro (no
 * la infiere) porque la inferencia del rol Responsable/Participante
 * depende de la lectura de PROYECTO_RESPONSABLE, que el
 * servicio resuelve antes de invocar al mapper.
 */
@Mapper(componentModel = "spring")
public abstract class SeguimientoMapper {

    @Named("etagPlanificacion")
    protected String etagPlanificacion(PlanificacionProyectoEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "\"" + entity.getId() + "-" + entity.getVersion() + "\"";
    }

    @Named("etagCiclo")
    protected String etagCiclo(CicloProyectoEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "\"" + entity.getId() + "-" + entity.getNumeroVersion() + "\"";
    }

    @Named("etagCicloVersion")
    protected String etagCicloVersion(CicloVersionEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "\"" + entity.getId() + "-" + entity.getNumeroVersion() + "\"";
    }

    @Named("etagParticipantePersona")
    protected String etagParticipantePersona(ProyectoParticipantePersonaEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        long version = entity.getFin() == null ? 0L : entity.getFin().toEpochDay();
        return "\"" + entity.getId() + "-" + version + "\"";
    }

    @Named("etagParticipanteUnidad")
    protected String etagParticipanteUnidad(ProyectoParticipanteUnidadEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        long version = entity.getFin() == null ? 0L : entity.getFin().toEpochDay();
        return "\"" + entity.getId() + "-" + version + "\"";
    }

    @Named("etagPresentacionProductoFinal")
    protected String etagPresentacionProductoFinal(PresentacionProductoFinalEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "\"" + entity.getId() + "-" + entity.getVersion() + "\"";
    }

    @Mapping(target = "idPlanificacion", source = "id")
    @Mapping(target = "idVersionAnterior", source = "idVersionAnterior")
    @Mapping(target = "etag", source = ".", qualifiedByName = "etagPlanificacion")
    public abstract PlanificacionResponse toPlanificacionResponse(
            PlanificacionProyectoEntity entity);

    @Mapping(target = "idCiclo", source = "id")
    @Mapping(target = "numeroVersion", source = "numeroVersion")
    @Mapping(target = "idVersionAnterior", source = "idVersionAnterior")
    @Mapping(target = "etag", source = ".", qualifiedByName = "etagCiclo")
    public abstract CicloResponse toCicloResponse(CicloProyectoEntity entity);

    @Mapping(target = "idVersion", source = "id")
    @Mapping(target = "idCiclo", source = "idCiclo")
    @Mapping(target = "numeroVersion", source = "numeroVersion")
    @Mapping(target = "etag", source = ".", qualifiedByName = "etagCicloVersion")
    public abstract CicloVersionResponse toCicloVersionResponse(CicloVersionEntity entity);

    /**
     * Convierte una participacion de persona a su DTO canonico.
     * El servicio de aplicacion pasa el rol resuelto (Responsable
     * o Participante) segun la regla de cardinalidad vigente.
     */
    @Mapping(target = "idParticipacion", source = "entity.id")
    @Mapping(target = "proyectoId", source = "entity.idProyecto")
    @Mapping(target = "personaId", source = "persona.id")
    @Mapping(target = "unidadId", ignore = true)
    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "nombresCompletos", source = "persona.nombresCompletos")
    @Mapping(target = "institucion", source = "persona.institucion")
    @Mapping(target = "funcion", source = "persona.funcion")
    @Mapping(target = "estado", source = "estadoVigencia")
    @Mapping(target = "fechaAlta", source = "entity.inicio")
    @Mapping(target = "fechaBaja", source = "entity.fin")
    @Mapping(target = "etag", source = "entity", qualifiedByName = "etagParticipantePersona")
    public abstract ParticipanteResponse toParticipantePersonaResponse(
            ProyectoParticipantePersonaEntity entity,
            ParticipantePersonaEntity persona,
            String rol,
            String estadoVigencia);

    /**
     * Convierte una participacion de unidad a su DTO canonico.
     * El rol siempre es Participante (las unidades no asumen el
     * rol de Responsable titular).
     */
    @Mapping(target = "idParticipacion", source = "entity.id")
    @Mapping(target = "proyectoId", source = "entity.idProyecto")
    @Mapping(target = "personaId", ignore = true)
    @Mapping(target = "unidadId", source = "entity.idUnidad")
    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "nombresCompletos", ignore = true)
    @Mapping(target = "institucion", ignore = true)
    @Mapping(target = "funcion", ignore = true)
    @Mapping(target = "estado", source = "estadoVigencia")
    @Mapping(target = "fechaAlta", source = "entity.inicio")
    @Mapping(target = "fechaBaja", source = "entity.fin")
    @Mapping(target = "etag", source = "entity", qualifiedByName = "etagParticipanteUnidad")
    public abstract ParticipanteResponse toParticipanteUnidadResponse(
            ProyectoParticipanteUnidadEntity entity,
            String rol,
            String estadoVigencia);

    /**
     * Convierte la entidad de presentacion del producto final a
     * su DTO canonico. Los campos transient (tipoProductoFinal,
     * documentacionGestion, resultadosClave, nota) se exponen tal
     * cual fueron hidratados por el servicio desde DESCRIPCION.
     */
    @Mapping(target = "idPresentacion", source = "id")
    @Mapping(target = "idProyecto", source = "idProyecto")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "idVersionAnterior", source = "idVersionAnterior")
    @Mapping(target = "tipoProductoFinal", source = "tipoProductoFinal")
    @Mapping(target = "documentacionGestion", source = "documentacionGestion")
    @Mapping(target = "resultadosClave", source = "resultadosClave")
    @Mapping(target = "nota", source = "nota")
    @Mapping(target = "idDocumentoSustenta", source = "idDocumentoSustenta")
    @Mapping(target = "evidenciaIds", ignore = true)
    @Mapping(target = "etag", source = ".", qualifiedByName = "etagPresentacionProductoFinal")
    public abstract PresentacionProductoFinalResponse toPresentacionProductoFinalResponse(
            PresentacionProductoFinalEntity entity);

    // -----------------------------------------------------------------
    // Helpers publicos para composicion de ETag en sitios donde
    // MapStruct no esta disponible (p. ej. pruebas unitarias).
    // -----------------------------------------------------------------

    public static String composeTagParticipantePersona(ProyectoParticipantePersonaEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        LocalDate fin = entity.getFin();
        long version = fin == null ? 0L : fin.toEpochDay();
        return "\"" + entity.getId() + "-" + version + "\"";
    }

    public static String composeTagParticipanteUnidad(ProyectoParticipanteUnidadEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        LocalDate fin = entity.getFin();
        long version = fin == null ? 0L : fin.toEpochDay();
        return "\"" + entity.getId() + "-" + version + "\"";
    }

    public static String composeTagPresentacion(PresentacionProductoFinalEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "\"" + entity.getId() + "-" + entity.getVersion() + "\"";
    }

    public static String composeFechaPresentacion(PresentacionProductoFinalEntity entity) {
        if (entity == null) {
            return null;
        }
        LocalDateTime fecha = entity.getFechaPresentacion();
        return fecha == null ? null : fecha.toString();
    }
}
