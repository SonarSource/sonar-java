package checks;
// To help keep "guava" and "jdk" tests in sync, this file is identical to its counterpart except for the import of class "Optional"

import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nullable;

interface NullShouldNotBeUsedWithOptionalCheck_guava {

  @Nullable // Noncompliant {{Methods with an "Optional" return type should not be "@Nullable".}}
//^^^^^^^^^
  public Optional<String> getOptionalKo();

}

class NullShouldNotBeUsedWithOptionalCheck_guavaClassA {

  public NullShouldNotBeUsedWithOptionalCheck_guavaClassA() {
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

    Optional<String> optional2 = getOptionalOk();
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
  private Optional<String> field;

  public void doSomething6(@Nullable Optional<String> arg) { // Noncompliant {{"Optional" variables should not be "@Nullable".}}
//                         ^^^^^^^^^
  }

  public void doSomething7() {
    @Nullable // Noncompliant {{"Optional" variables should not be "@Nullable".}}
//  ^^^^^^^^^
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
