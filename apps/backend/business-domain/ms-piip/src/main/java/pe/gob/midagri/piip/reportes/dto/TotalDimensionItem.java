package pe.gob.midagri.piip.reportes.dto;

/** Par clave-valor para un total por dimensión (BR-121). */
public record TotalDimensionItem(String clave, String etiqueta, Long total) {
}
