package pe.gob.midagri.piip.shareddata.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;

public abstract class BaseRestController {

   protected ResponseEntity<byte[]> buildDownloadResponseEntity(String fileName, byte[] data) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", fileName);
      return ResponseEntity.ok().headers(headers).body(data);
   }

}
