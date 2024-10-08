package checks.jspecify;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.meta.When;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.beans.factory.annotation.Value;

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleB {

  @org.jspecify.annotations.NonNull // Noncompliant {{Remove redundant nullability annotation Optional[@NonNull] as already annotated with Optional[@NullMarked at class level].}}
  @Value("${my.property_jspecify}")
  private String myProperty_jspecify;

  public void methodNonNullParam(@javax.annotation.Nonnull(when= When.ALWAYS) Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull(when=ALWAYS) as already annotated with @NullMarked at class level.}}
    // ...
  }

  @NotNull // Noncompliant {{Remove redundant nullability annotation @NotNull as already annotated with @NullMarked at class level.}}
  public Integer methodJXNonNullReturn(Object o) {
    return 0;
  }

  @javax.annotation.Nonnull(when= When.ALWAYS) // Noncompliant {{Remove redundant nullability annotation @Nonnull(when=ALWAYS) as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @Nullable // Compliant
  public Boolean methodBooleanNullable(@Nullable Object o) { // Compliant
    return false;
  }

  @org.jspecify.annotations.Nullable // Compliant
  public Boolean methodBooleanJNullable(@org.jspecify.annotations.Nullable Object o) { // Compliant
    return false;
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

  @NullUnmarked // Compliant
  static class InnerClassUnmarked {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleC {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

class RedundantNullabilityAnnotationsCheckSampleNoAnnotation {

  public void methodNonNullParam(@NonNull Object o) { // Compliant
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Compliant
    // ..
  }

  @NonNull // Compliant
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Compliant
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSample {

  @Nullable
  private String myProperty_jspecify_okay;

  public void methodNonNullParamNullable(@Nullable Object o) { // Compliant
    // ...
  }

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}
    // ..
  }

  @Nullable // Compliant
  public Integer methodNullableReturn(Object o) {
    return 0;
  }

  @NonNull // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleMix {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
record RedundantNullabilityAnnotationsCheckSampleRecord(Integer id) {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}
    // ..
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
interface RedundantNullabilityAnnotationsCheckSampleInterface {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o); // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}

  public void methodNonNullParamTyped(List<@NonNull Object> o); // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o);

  public void methodOkay(Object o);

  @org.jspecify.annotations.NonNull // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static interface InnerClass {

    public void methodOkay(Object o);

  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at class level.}}
  static interface InnerRecord {

    public void methodOkay(Object o);

  }

}

enum TEST_COVERAGE {
  ABACUS,
  BABA,
  CIRCUS
}
