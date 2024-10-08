package checks.jspecify.nullmarked;

import java.util.List;
import javax.annotation.meta.When;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;

@NullMarked
class RedundantNullabilityAnnotationsCheckRedundantClass {

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at class level.}}
    // ...
  }

}

// NullMarked at the package level
class RedundantNullabilityAnnotationsCheckSample {

  @org.jspecify.annotations.NonNull // Noncompliant {{Remove redundant nullability annotation Optional[@NonNull] as already annotated with Optional[@NullMarked at package level].}}
  @Value("${my.property_jspecify}")
  private String myProperty_jspecify;

  private String myProperty_jspecify_okay;

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at package level.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at package level.}}
    // ..
  }

  @NonNull // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at package level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at package level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

  static class UnMarkedInnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

  enum INNER_TEST_COVERAGE {
    ABACUS,
    BABA,
    CIRCUS
  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleB {

  public void methodNonNullParam(@javax.annotation.Nonnull(when= When.ALWAYS) Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull(when=ALWAYS) as already annotated with @NullMarked at class level.}}
    // ...
  }

  @javax.annotation.Nonnull(when= When.ALWAYS) // Noncompliant {{Remove redundant nullability annotation @Nonnull(when=ALWAYS) as already annotated with @NullMarked at class level.}}
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


// NullMarked at the package level
class RedundantNullabilityAnnotationsCheckSampleMix {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at package level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

// NullMarked at the package level
record RedundantNullabilityAnnotationsCheckSampleRecord(Integer id) {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at package level.}}
    // ..
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at package level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

// NullMarked at the package level
interface RedundantNullabilityAnnotationsCheckSampleInterface {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o); // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}

  public void methodNonNullParamTyped(List<@NonNull Object> o); // Noncompliant {{Remove redundant nullability annotation @NonNull as already annotated with @NullMarked at package level.}}

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation @Nonnull as already annotated with @NullMarked at package level.}}
  public Integer methodNonNullReturn(Object o);

  public void methodOkay(Object o);

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at package level.}}
  static interface InnerClass {

    public void methodOkay(Object o);

  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation @NullMarked at class level as already annotated with @NullMarked at package level.}}
  static interface InnerRecord {

    public void methodOkay(Object o);

  }

}

enum TEST_COVERAGE {
  ABACUS,
  BABA,
  CIRCUS
}
