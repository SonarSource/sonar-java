package checks.security.ExcessiveContentRequestCheck;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class ExcessiveContentRequestCheck_max8000000 {


  void springCommonsMultipartResolver() {
    CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
    multipartResolver.setMaxUploadSize(8000001); // Noncompliant [[sc=5;ec=48]] {{The content length limit of 8000001 bytes is greater than the defined limit of 8000000; make sure it is safe here.}}
    multipartResolver.setMaxUploadSize(8000000); // Compliant
  }

  void springMultipartConfigFactory() {
    MultipartConfigFactory factory = new MultipartConfigFactory();

    factory.setMaxFileSize(8000001); // Noncompliant
    factory.setMaxFileSize(8000000); // Compliant

    // The prefix used is the binary prefix (power of two).
    factory.setMaxFileSize("8MB"); // Noncompliant [[sc=5;ec=34]] {{The content length limit of 8388608 bytes is greater than the defined limit of 8000000; make sure it is safe here.}}
    factory.setMaxFileSize("8000KB"); // Noncompliant
    // 7900KB = 8089600 bytes
    factory.setMaxFileSize("7900KB"); // Noncompliant

    factory.setMaxFileSize("900KB"); // Compliant
    factory.setMaxFileSize("9KB"); // Compliant
    factory.setMaxFileSize("8000000"); // Compliant

    factory.setMaxRequestSize(8000001); // Noncompliant
    factory.setMaxRequestSize("8000001"); // Noncompliant
    factory.setMaxRequestSize(8000000);
    factory.setMaxRequestSize("8000000");
  }

}
