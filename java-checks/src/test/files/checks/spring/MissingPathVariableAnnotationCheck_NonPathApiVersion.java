package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MissingPathVariableAnnotationCheck_NonPathApiVersion {

  static class VersionedController {

    @GetMapping(value = "/api/{ordinaryVariable}/resource/{id}", version = "2") // Noncompliant {{Bind template variable "ordinaryVariable" to a method parameter.}}
    String missingOrdinaryVariable(@PathVariable String id) {
      return id;
    }
  }

  static class ApiVersionConfiguration implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      configurer.usePathSegment(1, path -> path.value().startsWith("/versioned/"));
      configurer.useRequestHeader("API-Version");
    }
  }
}
