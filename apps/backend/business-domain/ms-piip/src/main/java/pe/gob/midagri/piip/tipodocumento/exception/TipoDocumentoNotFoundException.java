package pe.gob.midagri.piip.tipodocumento.exception;

public class TipoDocumentoNotFoundException extends RuntimeException {

    public TipoDocumentoNotFoundException(Integer id) {
        super("No existe el tipo documental con identificador " + id);
    }
}
