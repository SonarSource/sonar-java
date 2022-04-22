package checks.security.ExcessiveContentRequestCheck.caching;

import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class Instantiator {
  public void createCommonsMultipartResolver() {
    new CommonsMultipartResolver(); // Compliant
  }
}
