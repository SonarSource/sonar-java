package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MissingPathVariableAnnotationCheck_EscapingConfiguration {

  static class VersionedController {

    @GetMapping(value = "/api/{ordinaryVariable}/resource", version = "2") // Noncompliant
    String missingOrdinaryVariable() {
      return "";
    }
  }

  static class ApiVersionConfiguration implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      configurer.usePathSegment(1, path -> path.value().startsWith("/versioned/"));
      configureFallback(configurer);
    }

    private static void configureFallback(ApiVersionConfigurer configurer) {
      configurer.useRequestHeader("API-Version");
    }
  }
}
