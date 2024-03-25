package checks.security.ExcessiveContentRequestCheck;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class ExcessiveContentRequestCheck_max8000000 {

  void setSizeViaDataSizeOf(MultipartProperties multipartProperties) {
    multipartProperties.setMaxFileSize(DataSize.ofTerabytes(1)); // Noncompliant
    multipartProperties.setMaxRequestSize(DataSize.ofTerabytes(1)); // Noncompliant
    multipartProperties.setMaxFileSize(DataSize.ofTerabytes(0));
    multipartProperties.setMaxRequestSize(DataSize.ofTerabytes(0));

    multipartProperties.setMaxFileSize(DataSize.ofGigabytes(1)); // Noncompliant
    multipartProperties.setMaxRequestSize(DataSize.ofGigabytes(1)); // Noncompliant
    multipartProperties.setMaxFileSize(DataSize.ofGigabytes(0));
    multipartProperties.setMaxRequestSize(DataSize.ofGigabytes(0));

    multipartProperties.setMaxFileSize(DataSize.ofBytes(8000001)); // Noncompliant
    multipartProperties.setMaxRequestSize(DataSize.ofBytes(8000001)); // Noncompliant
    multipartProperties.setMaxFileSize(DataSize.ofBytes(8000000));
    multipartProperties.setMaxRequestSize(DataSize.ofBytes(8000000));

    multipartProperties.setMaxFileSize(DataSize.ofKilobytes(7813)); // Noncompliant
    multipartProperties.setMaxRequestSize(DataSize.ofKilobytes(7813)); // Noncompliant
    multipartProperties.setMaxFileSize(DataSize.ofKilobytes(7812));
    multipartProperties.setMaxRequestSize(DataSize.ofKilobytes(7812));

    multipartProperties.setMaxFileSize(DataSize.ofMegabytes(8)); // Noncompliant
    multipartProperties.setMaxRequestSize(DataSize.ofMegabytes(8)); // Noncompliant
    multipartProperties.setMaxFileSize(DataSize.ofMegabytes(7));
    multipartProperties.setMaxRequestSize(DataSize.ofMegabytes(7));
  }

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
