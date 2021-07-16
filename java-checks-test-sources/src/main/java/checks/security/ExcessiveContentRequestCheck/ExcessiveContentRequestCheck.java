package checks.security.ExcessiveContentRequestCheck;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * 8 MB = 8388608 bytes
 */
public class ExcessiveContentRequestCheck {

  private static final int MAX_SIZE_COMPLIANT = 8_388_608;
  private static final int MAX_SIZE_NON_COMPLIANT = 8_388_609;
  private static final long MAX_SIZE_COMPLIANT_LONG = 8_388_608L;
  private static final long MAX_SIZE_NON_COMPLIANT_LONG = 8_388_609L;
  private static final String MAX_SIZE_COMPLIANT_STRING = "8388608";
  private static final String MAX_SIZE_NON_COMPLIANT_STRING = "8388609";

  void springCommonsMultipartResolver() {
    CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
    multipartResolver.setMaxUploadSize(8388609); // Noncompliant [[sc=5;ec=48]] {{The content length limit of 8388609 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
    multipartResolver.setMaxUploadSize(8388609l); // Noncompliant
    multipartResolver.setMaxUploadSize(8388609L); // Noncompliant
    multipartResolver.setMaxUploadSize(10); // Compliant
    multipartResolver.setMaxUploadSize(8388608); // Compliant
    multipartResolver.setMaxUploadSize(8388608l); // Compliant
    multipartResolver.setMaxUploadSize(8388608L); // Compliant
  }

  void springMultipartConfigFactory() {
    MultipartConfigFactory factory = new MultipartConfigFactory();

    // 2.0.2
    factory.setMaxFileSize(8388609); // Noncompliant
    factory.setMaxFileSize(8388609l); // Noncompliant
    factory.setMaxFileSize(8388609L); // Noncompliant
    factory.setMaxFileSize(8388608); // Compliant
    factory.setMaxFileSize(8388608l); // Compliant
    factory.setMaxFileSize(8388608L); // Compliant

    factory.setMaxFileSize("8388609"); // Noncompliant
    factory.setMaxFileSize("9MB"); // Noncompliant {{The content length limit of 9437184 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
    factory.setMaxFileSize("9Mb"); // Noncompliant
    factory.setMaxFileSize("9000KB"); // Noncompliant
    factory.setMaxFileSize("8193KB"); // Noncompliant

    factory.setMaxFileSize("8000KB"); // Compliant
    factory.setMaxFileSize("8001KB"); // Compliant, still less than 8MB
    factory.setMaxFileSize("8192KB"); // Compliant, equals 8MB
    factory.setMaxFileSize("900KB"); // Compliant
    factory.setMaxFileSize("9KB"); // Compliant
    factory.setMaxFileSize("8388608"); // Compliant
    factory.setMaxFileSize("8MB"); // Compliant
    factory.setMaxFileSize("8Mb"); // Compliant

    factory.setMaxFileSize(MAX_SIZE_NON_COMPLIANT); // Noncompliant
    factory.setMaxFileSize(MAX_SIZE_NON_COMPLIANT_STRING); // Noncompliant
    factory.setMaxFileSize(MAX_SIZE_NON_COMPLIANT_LONG); // Noncompliant
    factory.setMaxFileSize(MAX_SIZE_COMPLIANT); // Compliant
    factory.setMaxFileSize(MAX_SIZE_COMPLIANT_STRING); // Compliant
    factory.setMaxFileSize(MAX_SIZE_COMPLIANT_LONG); // Compliant

    factory.setMaxFileSize("SomethingElse"); // Compliant

    factory.setMaxFileSize("80000KB123"); // Compliant

    factory.setMaxRequestSize(8388609); // Noncompliant
    factory.setMaxRequestSize("8388609"); // Noncompliant
    factory.setMaxRequestSize(8388608);
    factory.setMaxRequestSize("8388608");

    String s = new String("9000KB"); // Compliant
  }

}
