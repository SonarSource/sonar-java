package checks.jspecify;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

@interface AnotherAnnotation {
}

@NullMarked
@interface MyCheckForNull {
}

@NullMarked
abstract class PrimitivesMarkedNullableCheckSample {

  abstract int getInt1();

  @AnotherAnnotation
  abstract int getInt2();

  @NullUnmarked
  protected abstract boolean isBool(); // Compliant

  @NullUnmarked
  public double getDouble1() { return 0.0; } // Compliant

  public double getDouble2() { return 0.0; }

  @MyCheckForNull
  public double getDouble2_1() { return 0.0; } // Compliant, Nullable meta annotation are not taken into account

  @NullMarked
  public double getDouble2_2() { return 0.0; } // Compliant, Nonnull is useless, but is accepted as it can be added for consistency

  @NullUnmarked
  public Double getDouble3() { return 0.0; }

  @NullUnmarked
  public Double getDouble4() { return 0.0; }

  @NullUnmarked
  public Object getObj0() { return null; }

  @NullUnmarked
  public Object getObj1() { return null; }

  public Object getObj2() { return null; }

  protected abstract @NullUnmarked boolean isBool2(); // Compliant

  @NullUnmarked
  Object containsAnonymousClass() {
    return new PrimitivesMarkedNullableCheckNullMarkedParent() {
      int getInt0() {
        return 0;
      }
    };
  }

}

abstract class PrimitivesMarkedNullableCheckNullMarkedChild extends PrimitivesMarkedNullableCheckNullMarkedParent {

  abstract int getInt0(); // Compliant, not directly marked as CheckForNull, an issue will be on the parent if needed.

}
