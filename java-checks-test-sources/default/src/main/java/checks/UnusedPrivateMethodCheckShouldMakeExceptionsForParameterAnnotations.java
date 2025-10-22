package checks;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.event.Reception;

public class UnusedPrivateMethodCheckShouldMakeExceptionsForParameterAnnotations {
  @interface CustomAnnotation {
    ObservesAsync[] value() default {};
  }

  private void control() { // Noncompliant
  }

  private void should_raise_for_unrecognized_annotation(@CustomAnnotation Object o) { // Noncompliant

  }

  static class ForObservesAsync {
    static class DontRaiseWhen {
      private void standalone(@ObservesAsync Object o) { // Compliant

      }

      private void among_other_parameters_01(Object o1, @ObservesAsync Object o2) { // Compliant

      }

      private void among_other_parameters_02(@ObservesAsync Object o1, Object o2) { // Compliant

      }

      private void among_other_parameters_03(@ObservesAsync Object o1, @Observes Object o2) { // Compliant

      }

      private void stacking_other_annotations_01(@ObservesAsync @CustomAnnotation Object o1) { // Compliant

      }

      private void stacking_other_annotations_02(@CustomAnnotation @ObservesAsync Object o1) { // Compliant

      }

      private void notify_observer_is_specified(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) Object o1) { // Compliant

      }
    }

    static class RaiseWhen {
      private void fqn_is_unusual(@Other.ObservesAsync Object o) { // Noncompliant

      }

      private void applied_to_wrong_element(@CustomAnnotation(@ObservesAsync) Object o1) { // Noncompliant

      }
    }
  }
}

class Other {
  @interface ObservesAsync {
  }
}
