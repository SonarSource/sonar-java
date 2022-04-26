package checks.security.ExcessiveContentRequestCheck.caching;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class Sanitizer {

  public CommonsMultipartResolver sanitize(CommonsMultipartResolver resolver) {
    resolver.setMaxUploadSize(1L);
    return resolver;
  }

  public MultipartConfigFactory sanitize(MultipartConfigFactory factory) {
    factory.setMaxFileSize(1L);
    factory.setMaxRequestSize(1L);
    return factory;
  }
}
