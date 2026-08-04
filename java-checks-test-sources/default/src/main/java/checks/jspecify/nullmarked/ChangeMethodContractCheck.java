package checks.jspecify.nullmarked;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

// NullMarked at the package level
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

class NullableGenericParent {
  protected @Nullable List<Object> handle(@Nullable Object body) {
    return null;
  }
}

class NullableGenericChild_WithNullable extends NullableGenericParent {
  @Override
  protected @Nullable List<Object> handle(@Nullable Object body) { // Compliant
    return null;
  }
}

class NullableGenericChild_NoNullable extends NullableGenericParent {
  @Override
  protected List<Object> handle(@Nullable Object body) { // Compliant
    return new java.util.ArrayList<>();
  }
}




