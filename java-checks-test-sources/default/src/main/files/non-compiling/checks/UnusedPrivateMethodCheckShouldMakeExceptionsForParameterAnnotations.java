package checks;

class UnusedPrivateMethodCheckShouldMakeExceptionsForParameterAnnotations {
  static class ForUnknownAnnotationTypes {
    static class ShouldRaiseFor {
      private void unrecognized_simple_name(@UnknownAnnotation Object object) { // Noncompliant
      }

      private void unrecognized_fqn_01(@somepackage.Observes Object object) { // Noncompliant
      }

      private void unrecognized_fqn_02(@somepackage.ObservesAsync Object object) { // Noncompliant
      }
    }

    static class ShouldNotRaiseFor {
      private void recognized_simple_name_02(@ObservesAsync Object object) { // Compliant
      }

      private void recognized_fqn_01(@jakarta.enterprise.event.Observes Object object) { // Compliant
      }

      private void recognized_fqn_02(@jakarta.enterprise.event.ObservesAsync Object object) { // Compliant
      }
    }
  }
}
