package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para registrar la planificacion de un proyecto
 * (US4, Constitucion 5.0.0, contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y DDL {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>La planificacion es append-only: la primera invocacion crea la
 * version 1; cada correccion crea una nueva fila en
 * {@code PLANIFICACION_PROYECTO} conservando la anterior. Los campos
 * siguen las limitaciones canonicas de la tabla:
 * {@code ALCANCE VARCHAR2(2000)}, {@code OBJETIVOS VARCHAR2(2000)},
 * {@code ENTREGABLES CLOB} y {@code PERIODOS CLOB} (este ultimo
 * contiene la lista separada por {@code ;} de periodos quincenales
 * con formato canonico {@code AAAA-Qn-Sn}).
 */
public record PlanificacionRequest(
        @NotBlank @Size(max = 2000) String alcance,
        @NotBlank @Size(max = 2000) String objetivos,
        @Size(max = 100000) String entregables,
        @Size(max = 100000) String periodos) {
}
