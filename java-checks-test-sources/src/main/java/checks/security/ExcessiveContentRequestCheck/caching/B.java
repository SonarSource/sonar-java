package checks.security.ExcessiveContentRequestCheck.caching;

import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class B {
  public void createCommonsMultipartResolver() {
    new CommonsMultipartResolver(); // Compliant
  }
}
