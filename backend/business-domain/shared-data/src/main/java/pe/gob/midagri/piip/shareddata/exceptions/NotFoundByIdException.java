package pe.gob.midagri.piip.shareddata.exceptions;

public class NotFoundByIdException extends RuntimeException {

   public NotFoundByIdException(Long id) {
      super(id.toString());
   }

}
