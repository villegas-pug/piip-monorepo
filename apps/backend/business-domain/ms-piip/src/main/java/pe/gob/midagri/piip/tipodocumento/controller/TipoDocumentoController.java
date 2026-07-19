package pe.gob.midagri.piip.tipodocumento.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.shareddata.dtos.responses.ApiResponse;
import pe.gob.midagri.piip.shareddata.enums.ApiResponseStatus;
import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;
import pe.gob.midagri.piip.tipodocumento.service.TipoDocumentoService;

@RestController
@RequestMapping("/tipo-documentos")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoDocumentoResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.<List<TipoDocumentoResponse>>builder()
                .message(ApiResponseStatus.SUCCESS.getMessage())
                .data(this.service.findAll())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TipoDocumentoResponse>> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.<TipoDocumentoResponse>builder()
                .message(ApiResponseStatus.SUCCESS.getMessage())
                .data(this.service.findById(id))
                .build());
    }
}
