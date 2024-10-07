package checks;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
class RedundantNullabilityAnnotationsCheckSample {

  public void methodNonNullParam(@NonNull Object o) { // Noncompliant {{@NonNull is redundant here.}}
    // ...
  }

  @NonNull // Noncompliant {{@NonNull is redundant here.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{@NullMarked is redundant here.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@javax.annotation.Nonnull
class RedundantNullabilityAnnotationsCheckSampleB {

  public void methodNonNullParam(@javax.annotation.Nonnull Object o) { // Noncompliant {{@Nonnull is redundant here.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{@Nonnull is redundant here.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{@Nonnull is redundant here.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@jakarta.annotation.Nonnull
class RedundantNullabilityAnnotationsCheckSampleC {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{@Nonnull is redundant here.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{@Nonnull is redundant here.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @jakarta.annotation.Nonnull // Noncompliant {{@Nonnull is redundant here.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

@NullMarked
class RedundantNullabilityAnnotationsCheckSampleMix {

  public void methodNonNullParam(@jakarta.annotation.Nonnull Object o) { // Noncompliant {{@Nonnull is redundant here.}}
    // ...
  }

  @javax.annotation.Nonnull // Noncompliant {{@Nonnull is redundant here.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @org.jspecify.annotations.NonNull // Noncompliant {{@Nonnull is redundant here.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}
