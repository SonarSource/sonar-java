package checks.security.ExcessiveContentRequestCheck.caching;

import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class Unsafe {
  public void createCommonsMultipartResolver() {
    new CommonsMultipartResolver(); // Noncompliant
  }
}
