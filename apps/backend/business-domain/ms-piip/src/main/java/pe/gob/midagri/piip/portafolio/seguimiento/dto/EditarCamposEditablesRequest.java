package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para editar los campos oficiales editables 17, 19 y 23
 * del proyecto durante {@code PROYECTO_EJECUCION} (US4, regla
 * BR-063 y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>La operacion exige cabecera {@code If-Match}; la implementacion
 * completa del servicio se entrega en T073. T072 declara el DTO y
 * la firma del metodo para que el {@code SeguimientoController}
 * mantenga su contrato estable.
 */
public record EditarCamposEditablesRequest(
        @Size(max = 2000) String documentacionGestion,
        @Size(max = 2000) String resultadosClave,
        @Size(max = 1000) String nota) {
}
