package checks;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@interface AnotherAnnotation {
}

@CheckForNull
@interface MyCheckForNull {
}

abstract class PrimitivesMarkedNullableCheckSample {

  @CheckForNull
//^^^^^^^^^^^^^>
  abstract int getInt0(); // Noncompliant {{"@CheckForNull" annotation should not be used on primitive types}}
//         ^^^

  abstract int getInt1();

  @AnotherAnnotation
  abstract int getInt2();

  @Nullable
  protected abstract boolean isBool(); // Noncompliant {{"@Nullable" annotation should not be used on primitive types}} [[quickfixes=qf1]]
//                   ^^^^^^^
  // fix@qf1 {{Remove "@Nullable"}}
  // edit@qf1 [[sl=-1;el=+0;sc=3;ec=3]] {{}}

  @javax.annotation.CheckForNull
  public double getDouble0() { return 0.0; } // Noncompliant {{"@CheckForNull" annotation should not be used on primitive types}} [[quickfixes=qf2]]
//       ^^^^^^
  // fix@qf2 {{Remove "@CheckForNull"}}
  // edit@qf2 [[sl=-1;el=+0;sc=3;ec=3]] {{}}

  @javax.annotation.Nullable
  public double getDouble1() { return 0.0; } // Noncompliant {{"@Nullable" annotation should not be used on primitive types}}

  public double getDouble2() { return 0.0; }

  @MyCheckForNull
  public double getDouble2_1() { return 0.0; } // Compliant, Nullable meta annotation are not taken into account

  @Nonnull
  public double getDouble2_2() { return 0.0; } // Compliant, Nonnull is useless, but is accepted as it can be added for consistency

  @javax.annotation.Nullable
  public Double getDouble3() { return 0.0; }

  @Nullable
  public Double getDouble4() { return 0.0; }

  @Nullable
  public Object getObj0() { return null; }

  @CheckForNull
  public Object getObj1() { return null; }

  public Object getObj2() { return null; }

  protected abstract @Nullable boolean isBool2(); // Noncompliant {{"@Nullable" annotation should not be used on primitive types}} [[quickfixes=qf3]]
//                             ^^^^^^^
  // fix@qf3 {{Remove "@Nullable"}}
  // edit@qf3 [[sc=22;ec=32]] {{}}

  @CheckForNull
  Object containsAnonymousClass() {
    return new PrimitivesMarkedNullableCheckSampleParent() {
      int getInt0() {
        return 0;
      }
    };
  }

}

abstract class PrimitivesMarkedNullableCheckSampleChild extends PrimitivesMarkedNullableCheckSampleParent {

  abstract int getInt0(); // Compliant, not directly marked as CheckForNull, an issue will be on the parent if needed.

}
