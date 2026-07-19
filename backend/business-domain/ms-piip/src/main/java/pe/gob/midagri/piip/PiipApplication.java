package pe.gob.midagri.piip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "pe.gob.midagri.piip")
public class PiipApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiipApplication.class, args);
    }
}
