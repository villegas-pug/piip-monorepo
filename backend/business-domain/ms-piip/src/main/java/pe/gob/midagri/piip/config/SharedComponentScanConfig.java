package pe.gob.midagri.piip.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "pe.gob.midagri.piip", "pe.gob.midagri.piip.shareddata" })
public class SharedComponentScanConfig {
}
