package pe.gob.midagri.piip.portafolio.service;

import pe.gob.midagri.piip.portafolio.dto.*;
import java.util.List;

/**
 * Servicio de aplicación para la incorporación individual de información existente.
 * Registra, corrige, detecta conflictos y resuelve duplicados.
 */
public interface IncorporacionRegistroService {

    /**
     * Registra una nueva incorporación individual.
     */
    IncorporacionDetail registrar(CreateIncorporacionRequest comando, PortafolioAuthContext contexto);

    /**
     * Registra una corrección a una incorporación pendiente.
     */
    IncorporacionDetail corregir(IncorporacionCorreccionRequest comando, PortafolioAuthContext contexto);

    /**
     * Resuelve un conflicto de incorporación.
     */
    IncorporacionDetail resolverConflicto(IncorporacionResolucionConflictoRequest comando, PortafolioAuthContext contexto);

    /**
     * Valida o rechaza una incorporación pendiente.
     */
    IncorporacionDetail validar(IncorporacionValidacionRequest comando, PortafolioAuthContext contexto);
}
