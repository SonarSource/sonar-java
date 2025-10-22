package checks;

class UnusedPrivateMethodCheckShouldMakeExceptionsForParameterAnnotations {
  static class ForUnknownAnnotationTypes {
    static class ShouldRaiseFor {
      private void unrecognized_simple_name(@UnknownAnnotation Object object) { // Noncompliant
      }

      private void unrecognized_fqn(@somepackage.ObservesAsync Object object) { // Noncompliant
      }

    }

    static class ShouldNotRaiseFor {
      private void recognized_simple_name(@ObservesAsync Object object) { // Compliant
      }

      private void recognized_fqn(@jakarta.enterprise.event.ObservesAsync Object object) { // Compliant
      }
    }
  }
}
