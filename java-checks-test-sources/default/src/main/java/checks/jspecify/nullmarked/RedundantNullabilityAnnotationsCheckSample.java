package checks.jspecify.nullmarked;

import java.util.List;
import javax.annotation.meta.When;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;

@NullMarked
class RedundantNullabilityAnnotationsCheckRedundantClass {

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

}

// NullMarked at the package level
class RedundantNullabilityAnnotationsCheckSample {

  @org.jspecify.annotations.NonNull // Noncompliant {{Remove redundant nullability annotation.}}
  @Value("${my.property_jspecify}")
  private String myProperty_jspecify;

  private String myProperty_jspecify_okay;

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ..
  }

  @NonNull // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
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

  public void methodNonNullParam(@javax.annotation.Nonnull(when= When.ALWAYS) Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

  @javax.annotation.Nonnull(when= When.ALWAYS) // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleC {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}


// NullMarked at the package level
class RedundantNullabilityAnnotationsCheckSampleMix {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

// NullMarked at the package level
record RedundantNullabilityAnnotationsCheckSampleRecord(Integer id) {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ...
  }

  public void methodNonNullParamTyped(List<@NonNull Object> o) { // Noncompliant {{Remove redundant nullability annotation.}}
    // ..
  }

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

// NullMarked at the package level
interface RedundantNullabilityAnnotationsCheckSampleInterface {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o); // Noncompliant {{Remove redundant nullability annotation.}}

  public void methodNonNullParamTyped(List<@NonNull Object> o); // Noncompliant {{Remove redundant nullability annotation.}}

  @javax.annotation.Nonnull // Noncompliant {{Remove redundant nullability annotation.}}
  public Integer methodNonNullReturn(Object o);

  public void methodOkay(Object o);

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static interface InnerClass {

    public void methodOkay(Object o);

  }

  @NullMarked // Noncompliant {{Remove redundant nullability annotation.}}
  static interface InnerRecord {

    public void methodOkay(Object o);

  }

}

enum TEST_COVERAGE {
  ABACUS,
  BABA,
  CIRCUS
}
