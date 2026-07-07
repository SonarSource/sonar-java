package files.checks.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MissingPathVariableAnnotationCheck_DynamicIndex {

  static class VersionedController {

    @GetMapping(value = "/api/v{apiVersion}/resource", version = "2") // Noncompliant
    String ambiguousVersionSegment() {
      return "";
    }
  }

  static class ApiVersionConfiguration implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      configurer.usePathSegment(1);
      configurer.usePathSegment(dynamicIndex());
    }

    private static int dynamicIndex() {
      return 2;
    }
  }
}
