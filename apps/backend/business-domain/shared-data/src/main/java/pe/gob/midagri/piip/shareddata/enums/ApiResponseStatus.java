package pe.gob.midagri.piip.shareddata.enums;

public enum ApiResponseStatus {

   // 200
   SUCCESS(200, "¡Operación exitosa!"),
   SUCCESS_CREATE(201, "¡Entidad creada exitosa!"),
   SUCCESS_UPDATE(201, "¡Entidad actualizada exitosa!"),
   SUCCESS_DELETE(200, "¡Entidad con ID: %s, eliminada correctamente!"),
   SUCCESS_DELETE_ALL(200, "¡Registros eliminados correctamente!"),
   SUCCESS_UPLOAD_FILE(200, "¡Archivo subido correctamente!"),
   SUCCESS_GENERATE_CODE(200, "¡Código generado correctamente!"),
   NO_CONTENT(200, "¡Recurso no encontrado!"),
   NO_CONTENT_BY_ID(200, "¡Entidad con ID: %s, no encontrada!"),
   
   // SUCCESS_NO_CONTENT(204, "¡No se encontraron resultados!"),

   // 400
   BAD_REQUEST(400, "¡Solicitud incorrecta!"),
   BAD_REQUEST_FAIL_VALIDATION(400, "¡Fallo de validación!"),
   UNAUTHORIZED(401, "¡No autorizado!"),
   FORBIDDEN(403, "¡Acceso prohibido!"),
   NOT_FOUND(404, "¡Recurso no encontrado!"),
   NOT_FOUND_FILE(404, "¡Archivo adjunto no encontrado!"),

   // 500
   INTERNAL_SERVER_ERROR(500, "¡Error interno del servidor!"),

   // ...
   AUTH_ERROR(401, "¡Usuario o clave incorrecta!"),
   AUTHORIZED(200, "¡Bienvenido %s!");

   private final int code;
   private String message;

   ApiResponseStatus(int code, String message) {
      this.code = code;
      this.message = message;
   }

   public int getCode() {
      return this.code;
   }

   public String getMessage() {
      return this.message;
   }

   public <T> void setMessage(T additionalMessage) {
      this.message = String.format(this.message, additionalMessage);
   }

}
