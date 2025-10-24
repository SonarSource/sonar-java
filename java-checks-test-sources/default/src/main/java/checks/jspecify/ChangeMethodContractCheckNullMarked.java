package checks.jspecify;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

@NullMarked
class ChangeMethodContractCheck {

  @interface MyAnnotation {}

  @NullUnmarked
  String annotatedUnmarked(Object a) { return null; }
}

class ChangeMethodContractCheck_B extends ChangeMethodContractCheck {

  @NullMarked
  @Override
  String annotatedUnmarked(Object a) { return null; } // Compliant - NullUnmarked doesn't add any information about nullability

}

