import java.util.NoSuchElementException;
import java.util.Optional;
import javax.annotation.Nonnull;

class A {

  void foo0() {
    nseeIfCalled(getOptional()); // Noncompliant {{"NoSuchElementException" will be thrown when invoking method "nseeIfCalled()" without verifying Optional parameter.}}
  }

  void foo1() {
    Optional<String> o = getOptional();
    nseeIfCalled(o); // Noncompliant {{"NoSuchElementException" will be thrown when invoking method "nseeIfCalled()" without verifying Optional parameter.}}
  }

  void foo2() {
    Optional<String> o = getOptional();
    if (o.isPresent()) {
      nseeIfCalled(o); // Compliant
    }
  }

  void foo3() {
    Optional<String> o = getOptional();
    ok(o); // Compliant
  }

  void foo5() {
    Optional<String> o = getOptional();
    Optional<String> o2 = o;
    try {
      nseeIfCalled(o); // Noncompliant {{"NoSuchElementException" will be thrown when invoking method "nseeIfCalled()" without verifying Optional parameter.}}
    } catch (MyCheckedException e) {
      o2 = o;
    }
    o2.get(); // Compliant
  }

  void foo6(boolean b) {
    Optional<String> o = getOptional(); // flow@foo6 [[order=1]] {{'getOptional()' can return not null.}} flow@foo6 [[order=2]] {{Implies 'o' can be not null.}}
    if (b) { // flow@foo6 [[order=3]] {{Implies 'b' is true.}}
      A.nseeIfCalledAndTrue(b, o); // Noncompliant [[flows=foo6]] {{"NoSuchElementException" will be thrown when invoking method "nseeIfCalledAndTrue()" without verifying Optional parameter.}} flow@foo6 [[order=4]] {{'o' is passed to 'nseeIfCalledAndTrue()'.}}
    } else {
      nseeIfCalledAndTrue(b, o); // Compliant
    }
  }

  void foo7() {
    Optional<String> o = getOptional();
    Optional<String> o2 = o;
    try {
      nseeIfCalled(o); // Compliant - Exception is catched
    } catch (NoSuchElementException e) {
      o2 = o;
    }
    o2.get(); // Noncompliant {{Call "o2.isPresent()" before accessing the value.}}
  }

  static void nseeIfCalled(Optional<String> o) {
    o.get(); // Noncompliant {{Call "o.isPresent()" before accessing the value.}}
  }

  static void nseeIfCalledAndTrue(boolean b, Optional<String> o) {
    if (b) {
      o.get(); // Noncompliant {{Call "o.isPresent()" before accessing the value.}} flow@foo6 [[order=5]] {{Implies 'o' is not null.}} flow@foo6 [[order=6]] {{'NoSuchElementException' is thrown.}}
    } else if (o.isPresent()) {
      o.get();
    }
  }

  static void ok(Optional<String> o) {
    if (o.isPresent()) {
      o.get(); // Compliant
    }
  }

  @Nonnull
  abstract Optional<String> getOptional();

  static class MyCheckedException extends Exception { }
}
