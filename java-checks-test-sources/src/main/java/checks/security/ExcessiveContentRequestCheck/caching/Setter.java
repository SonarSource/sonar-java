package checks.security.ExcessiveContentRequestCheck.caching;

import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class Setter {
  public void setSize() {
    new CommonsMultipartResolver().setMaxUploadSize(1234L);
  }
}
