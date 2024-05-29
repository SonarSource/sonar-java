/**
 * Classes are placed in "androidx.annotation" in order
 * to simulate androidx package (not available through maven central)
 */
package androidx.annotation;

// fake 'androidx.annotation.NonNull' annotation
@interface NonNull { }

// fake 'androidx.annotation.Nullable' annotation
@interface Nullable { }

abstract class AndroidAnnotations {
  void foo(@NonNull Object input) { // flow@cof [[sc=28;ec=33]] {{Implies 'input' can not be null.}}
    // Noncompliant@+1 [[flows=cof]] {{Change this condition so that it does not always evaluate to "false"}}
    if (input == null) {} // flow@cof {{Expression is always false.}}
  }

  void kom(@androidx.annotation.Nullable Object input) { // flow@npe [[sc=49;ec=54]] {{Implies 'input' can be null.}}
    // Noncompliant@+1 [[flows=npe]] {{A "NullPointerException" could be thrown; "input" is nullable here.}}
    input.toString(); // flow@npe {{'input' is dereferenced.}}
  }

  private Integer bar2(@Nullable Integer i) { return i; }
  private void qix2(Integer i) {
    bar2(i).intValue(); // Noncompliant {{A "NullPointerException" could be thrown; "bar2()" can return null.}}
  }

  private void gul(@NonNull Object o) {
    o.toString(); // Compliant
  }

}
