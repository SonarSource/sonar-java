package checks.spring.s6856;

import java.util.function.Predicate;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class ApiVersionPathConfiguration implements WebMvcConfigurer {

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    Predicate<RequestPath> versionedPaths = this::isVersionedPath;
    configurer.usePathSegment(1, versionedPaths);
  }

  private boolean isVersionedPath(RequestPath requestPath) {
    String path = requestPath.pathWithinApplication().value();
    return path.startsWith("/api/");
  }
}
