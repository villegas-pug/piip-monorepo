package pe.gob.midagri.piip.reportes.dto;

/**
 * Contenido institucional listo para descargar.
 * Mantiene fuera del controlador la lectura del
 * snapshot, el renderizado y la resolución de
 * metadatos del archivo.
 */
public record ReporteArchivoDescarga(
        byte[] contenido,
        String nombreArchivo,
        String contentType,
        String etag) {
}
