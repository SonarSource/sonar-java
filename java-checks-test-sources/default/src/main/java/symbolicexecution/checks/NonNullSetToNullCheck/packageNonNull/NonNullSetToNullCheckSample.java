package symbolicexecution.checks.NonNullSetToNullCheck.packageNonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NonNullSetToNullCheckSample {
  Boolean notInitialized;
  Integer initialized;

  // 1.1: field not assigned in constructor
  public NonNullSetToNullCheckSample() { // Noncompliant {{"notInitialized" is marked "@NonNullByDefault at package level" but is not initialized in this constructor.}}
    initialized = 200;
  }

  // 1.2: field assigned
  public void setInitialized() {
    this.initialized = null; // Noncompliant {{"initialized" is marked "@NonNullByDefault at package level" but is set to null.}}
  }

  // 2. return values
  public String returnNull() {
    return null; // Noncompliant {{This method's return value is marked "@NonNullByDefault at package level" but null is returned.}}
  }

  // 3.
  public void notNullArgument(Object o) {
    notNullArgument(null);  // Compliant - will be reported in S4449
  }

  public void nonNullMethod(@Nonnull String s) {
    nonNullMethod(null); // Noncompliant {{Parameter 1 to this call is marked "@Nonnull" but null could be passed.}}
  }

  public void nullableMethod(@Nullable String s) {
    nullableMethod(null); // Compliant
  }

  public void jakartaNonNullMethod(@jakarta.annotation.Nonnull String s) {
    nonNullMethod(null); // Noncompliant {{Parameter 1 to this call is marked "@Nonnull" but null could be passed.}}
  }

  public void jakartaNullableMethod(@jakarta.annotation.Nonnull String s) {
    nullableMethod(null); // Compliant
  }
}

class HandleNonNullByDefaultPrimitives {
  boolean falseByDefault;

  public HandleNonNullByDefaultPrimitives() { // Compliant, primitive are not reported
  }
}
