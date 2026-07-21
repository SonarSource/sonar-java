package checks.spring.s6856;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

public class MissingPathVariableAnnotationCheck_ApiVersionPath {

  static class VersionedController {

    @GetMapping(value = "/api/v{apiVersion}/resource/{id}", version = "2")
    String valid(@PathVariable String id) {
      return id;
    }

    @GetMapping(value = "/api/v{apiVersion}/resource/{id}", version = "2") // Noncompliant {{Bind template variable "id" to a method parameter.}}
    String missingId() {
      return "";
    }

    @GetMapping(value = "/api/v{major}.{minor}/resource", version = "2")
    String multipleVariablesInVersionSegment() {
      return "";
    }

    @GetMapping("/api/v{apiVersion}/unversioned") // Noncompliant {{Bind template variable "apiVersion" to a method parameter.}}
    String versionlessMapping() {
      return "";
    }

    @GetMapping(value = "/api/{*apiVersion}", version = "2") // Noncompliant {{Bind template variable "apiVersion" to a method parameter.}}
    String catchAllVariable() {
      return "";
    }

    @GetMapping(value = {"/api/v{apiVersion}/alternative", "/{apiVersion}/alternative"}, version = "2") // Noncompliant {{Bind template variable "apiVersion" to a method parameter.}}
    String variableAtDifferentIndices() {
      return "";
    }
  }

  @RequestMapping("/api")
  static class TypeLevelPathController {

    @GetMapping(value = "/v{apiVersion}/resource/{id}", version = "2")
    String valid(@PathVariable String id) {
      return id;
    }
  }

}
