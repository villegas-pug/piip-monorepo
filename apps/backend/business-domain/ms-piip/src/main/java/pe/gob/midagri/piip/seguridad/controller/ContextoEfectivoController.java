package pe.gob.midagri.piip.seguridad.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.midagri.piip.seguridad.dto.EffectiveAssignmentOption;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Expone los contextos de asignación pertenecientes exclusivamente a la identidad autenticada. */
@RestController
@RequestMapping("/api/v1/seguridad/me")
public class ContextoEfectivoController {

    private final AutorizacionEfectivaService autorizacionEfectivaService;

    public ContextoEfectivoController(AutorizacionEfectivaService autorizacionEfectivaService) {
        this.autorizacionEfectivaService = autorizacionEfectivaService;
    }

    @GetMapping("/asignaciones")
    public List<EffectiveAssignmentOption> listarAsignacionesPropias(Principal principal) {
        return autorizacionEfectivaService.listarAsignacionesPropias(principal.getName());
    }
}
