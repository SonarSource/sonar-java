package checks.security.ExcessiveContentRequestCheck;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static checks.security.ExcessiveContentRequestCheck.ExcessiveContentRequestCheck_sizeNotSet.getCommonsMultipartResolver;
import static checks.security.ExcessiveContentRequestCheck.ExcessiveContentRequestCheck_sizeNotSet.getMultipartConfigElement;

public class ExcessiveContentRequestCheck_setSize {

  public void setSizeForCommonsMultipartResolver() {
    CommonsMultipartResolver commonsMultipartResolver = getCommonsMultipartResolver();
    commonsMultipartResolver.setMaxUploadSize(1234);
  }

  public void setSizeForMultipartConfigElement() {
    MultipartConfigFactory multipartConfigFactory = getMultipartConfigElement();
    multipartConfigFactory.setMaxFileSize(1234);
  }

}
