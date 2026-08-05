package checks;
// To help keep "guava" and "jdk" tests in sync, this file is identical to its counterpart except for the import of class "Optional"

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.meta.When;

interface NullShouldNotBeUsedWithOptionalCheck_jdk {

  @Nullable // FN

  public Optional<String> getOptionalKo();


  @org.jspecify.annotations.Nullable // FN
  Optional<String> getOptional();
}

class NullShouldNotBeUsedWithOptionalCheck_jdkClassA {

  public NullShouldNotBeUsedWithOptionalCheck_jdkClassA() {
  }

  @Nullable // FN

  public Optional<String> getOptionalKo() {
    return null; // Noncompliant

  }

  public Optional<String> getOptionalOk() {
    return Optional.of("hello");
  }

  public Object doSomething1() {
    return null;
  }

  public Optional<String> doSomething2() {
    Worker x = new Worker() {
      public String work() {
        return null;
      }
    };
    return Optional.of("hello");
  }

  public int doSomething3(Optional<String> arg) {
    if (arg == null) { // Noncompliant

      return 0;
    }

    Optional<String> optional = getOptionalOk();
    if (optional == null) { // Noncompliant

      return 0;
    } else if (null != optional) { // Noncompliant

      return 0;
    }

    Optional<String> optional2 = null; // Noncompliant

    String notOptional = null; // Compliant
    optional = null; // Noncompliant

    optional = Optional.empty(); // Compliant
    notOptional = null; // Compliant
    if (optional == optional2) {
      return 0;
    } else if (null == null) {
      return 0;
    }

    Optional<String> optional3 = getOptionalOk();
    return optional3 == null ? 0 : 1; // Noncompliant

  }

  public Optional<String> doSomething4(List<String> myList) {
    myList.stream().map(s -> {
      if (s.length() > 0) {
        return null;
      }
      return s;
    });
    return Optional.of("hello");
  }

  @Deprecated
  public Optional<String> doSomething5(List<String> myList) {
    return myList.isEmpty() ? Optional.of("hello") : null; // Noncompliant

  }

  @Nullable // FN

  private Optional<String> field = null; // Noncompliant


  public void doSomething6(@Nullable Optional<String> arg) { // FN

  }

  public void doSomething6_Jspecify(@org.jspecify.annotations.Nullable Optional<String> arg) { // FN
  }

  public void doSomething7() {
    @Nullable // FN

    Optional<String> var;
  }

  public void doSomething7_jspecify() {
    @org.jspecify.annotations.Nullable // FN
    Optional<String> var;
  }

  public void NonnullWithArgument1() {
    @javax.annotation.Nonnull(when= When.MAYBE) // FN

    Optional<String> var;
  }

  public void NonnullWithArgument2() {
    @javax.annotation.Nonnull(when= When.NEVER) // FN

    Optional<String> var;
  }

  public void NonnullWithArgument3() {
    @javax.annotation.Nonnull(when= When.UNKNOWN) // FN

    Optional<String> var;
  }

  public void NonnullWithArgument4() {
    @javax.annotation.Nonnull(when= When.ALWAYS) // Compliant: when=ALWAYS is Nonnull
    Optional<String> var;
  }

  public void NonnullWithArgument5() {
    @javax.annotation.Nonnull() // Compliant
    Optional<String> var;
  }

  public Optional<String> doSomething8(boolean b) {
    Object obj = b ? null : new Object();
    return Optional.of("hello");
  }

  interface Worker {
    String work();
  }

}
