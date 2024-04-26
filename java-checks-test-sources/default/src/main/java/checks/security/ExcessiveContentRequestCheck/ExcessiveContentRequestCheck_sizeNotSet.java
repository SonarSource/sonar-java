package checks.security.ExcessiveContentRequestCheck;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class ExcessiveContentRequestCheck_sizeNotSet {

  public static CommonsMultipartResolver getCommonsMultipartResolver() {
    CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(); // Noncompliant {{Make sure not setting any maximum content length limit is safe here.}}
//                                               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    return multipartResolver;
  }

  public static MultipartConfigFactory getMultipartConfigElement() {
    MultipartConfigFactory factory = new MultipartConfigFactory(); // Noncompliant {{Make sure not setting any maximum content length limit is safe here.}}
//                                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    return factory;
  }
}
