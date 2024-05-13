package checks;
// To help keep "guava" and "jdk" tests in sync, this file is identical to its counterpart except for the import of class "Optional"

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.meta.When;

interface NullShouldNotBeUsedWithOptionalCheck_jdk {

  @Nullable // Noncompliant {{Methods with an "Optional" return type should not be "@Nullable".}}
//^^^^^^^^^
  public Optional<String> getOptionalKo();

}

class NullShouldNotBeUsedWithOptionalCheck_jdkClassA {

  public NullShouldNotBeUsedWithOptionalCheck_jdkClassA() {
  }

  @Nullable // Noncompliant {{Methods with an "Optional" return type should not be "@Nullable".}}
//^^^^^^^^^
  public Optional<String> getOptionalKo() {
    return null; // Noncompliant {{Methods with an "Optional" return type should never return null.}}
//         ^^^^
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
    if (arg == null) { // Noncompliant {{Ensure this "Optional" could never be null and remove this null-check.}}
//      ^^^^^^^^^^^
      return 0;
    }

    Optional<String> optional = getOptionalOk();
    if (optional == null) { // Noncompliant {{Ensure this "Optional" could never be null and remove this null-check.}}
//      ^^^^^^^^^^^^^^^^
      return 0;
    } else if (null != optional) { // Noncompliant {{Ensure this "Optional" could never be null and remove this null-check.}}
//             ^^^^^^^^^^^^^^^^
      return 0;
    }

    Optional<String> optional2 = null; // Noncompliant {{Replace this null literal by an "Optional" object.}}
//                               ^^^^
    String notOptional = null; // Compliant
    optional = null; // Noncompliant {{Replace this null literal by an "Optional" object.}}
//             ^^^^
    optional = Optional.empty(); // Compliant
    notOptional = null; // Compliant
    if (optional == optional2) {
      return 0;
    } else if (null == null) {
      return 0;
    }

    Optional<String> optional3 = getOptionalOk();
    return optional3 == null ? 0 : 1; // Noncompliant {{Ensure this "Optional" could never be null and remove this null-check.}}
//         ^^^^^^^^^^^^^^^^^
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
    return myList.isEmpty() ? Optional.of("hello") : null; // Noncompliant {{Methods with an "Optional" return type should never return null.}}
//                                                   ^^^^
  }

  @Nullable // Noncompliant {{"Optional" variables should not be "@Nullable".}}
//^^^^^^^^^
  private Optional<String> field = null; // Noncompliant {{Replace this null literal by an "Optional" object.}}
//                                 ^^^^

  public void doSomething6(@Nullable Optional<String> arg) { // Noncompliant {{"Optional" variables should not be "@Nullable".}}
//                         ^^^^^^^^^
  }

  public void doSomething7() {
    @Nullable // Noncompliant {{"Optional" variables should not be "@Nullable".}}
//  ^^^^^^^^^
    Optional<String> var;
  }

  public void NonnullWithArgument1() {
    @javax.annotation.Nonnull(when= When.MAYBE) // Noncompliant {{"Optional" variables should not be "@Nonnull(when=MAYBE)".}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Optional<String> var;
  }

  public void NonnullWithArgument2() {
    @javax.annotation.Nonnull(when= When.NEVER) // Noncompliant {{"Optional" variables should not be "@Nonnull(when=NEVER)".}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Optional<String> var;
  }

  public void NonnullWithArgument3() {
    @javax.annotation.Nonnull(when= When.UNKNOWN) // Noncompliant {{"Optional" variables should not be "@Nonnull(when=UNKNOWN)".}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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
