package pe.gob.midagri.piip.documentos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoRequest;
import pe.gob.midagri.piip.documentos.dto.UploadDocumentCommand;
import pe.gob.midagri.piip.documentos.entity.DocumentoClasificacionHistEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoSerieEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.service.DocumentStorage;

class DocumentosContratoTest {
    @Test
    void contratosInternosNoExponenDecisionesNiCamposAntimalwareNiStorageKeys() {
        assertFalse(Arrays.stream(UploadDocumentCommand.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("antimalware") || component.getName().toLowerCase().contains("scan")));
        assertFalse(Arrays.stream(ReclasificarDocumentoRequest.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("antimalware") || component.getName().toLowerCase().contains("scan")));
        assertFalse(Arrays.stream(PublicacionDocumentoDetail.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("contenido") || component.getName().toLowerCase().contains("binar")));

        assertFalse(Arrays.stream(DocumentoSerieEntity.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("antivirus") || field.getName().toLowerCase().contains("storage")));
        assertFalse(Arrays.stream(DocumentoVersionEntity.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("antivirus") || field.getName().toLowerCase().contains("storage")));
        assertFalse(Arrays.stream(DocumentoClasificacionHistEntity.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("password") || field.getName().toLowerCase().contains("token")));

        assertTrue(Arrays.stream(DocumentStorage.class.getDeclaredMethods())
                .map(Method::getName).noneMatch(name -> name.toLowerCase().contains("url") || name.toLowerCase().contains("key")));
        assertTrue(DocumentoAuthorizedContext.class.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnoreType.class));
        // El DTO público de historial no expone la contraseña ni el token.
        assertFalse(Arrays.stream(ClasificacionHistDetalle.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("password") || component.getName().toLowerCase().contains("token")));
    }
}
