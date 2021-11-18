package symbolicexecution.checks.NonNullSetToNullCheck.packageNonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NonNullSetToNullCheck {
  Boolean notInitialized;
  Integer initialized;

  // 1.1: field not assigned in constructor
  public NonNullSetToNullCheck() { // Compliant - FN - {{"notInitialized" is marked "org.eclipse.jdt.annotation.NonNullByDefault" but is not initialized in this constructor.}}
    initialized = 200;
  }

  // 1.2: field assigned
  public void setInitialized() {
    this.initialized = null; // Compliant - FN - {{"initialized" is marked "org.eclipse.jdt.annotation.NonNullByDefault" but is set to null.}}
  }

  // 2. return values
  public String returnNull() {
    return null; // Compliant - FN - {{This method's return value is marked "org.eclipse.jdt.annotation.NonNullByDefault" but null is returned.}}
  }

  // 3.
  public void notNullArgument(Object o) {
    notNullArgument(null);  // Compliant - FN
  }

  public void nonNullMethod(@Nonnull String s) {
    nonNullMethod(null); // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public void nullableMethod(@Nullable String s) {
    nullableMethod(null); // Compliant
  }
}

class HandleNonNullByDefaultPrimitives {
  boolean falseByDefault;

  public HandleNonNullByDefaultPrimitives() { // Compliant, primitive are not reported
  }
}
