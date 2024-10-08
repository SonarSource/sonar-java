package checks.jspecify.nullmarked;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

// NullMarked at the package level
class ChangeMethodContractCheck {

  @interface MyAnnotation {}

  @NullUnmarked
  String annotatedUnmarked(Object a) { return null; }
}

class ChangeMethodContractCheck_B extends ChangeMethodContractCheck {

  @NullMarked
  @Override
  String annotatedUnmarked(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @NullMarked to honor @NullUnmarked of the overridden method.}}

}

