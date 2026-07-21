package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MissingPathVariableAnnotationCheck_InactiveConfiguration {

  static class VersionedController {

    @GetMapping(value = "/api/{ordinaryVariable}/resource", version = "2") // Noncompliant
    String missingOrdinaryVariable() {
      return "";
    }
  }

  static class ApiVersionConfiguration implements WebMvcConfigurer {

    void unusedHelper(ApiVersionConfigurer configurer) {
      configurer.usePathSegment(1);
    }
  }
}
