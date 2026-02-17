package checks;

import java.util.concurrent.ScopedValue;

class ScopedValueGetOutsideBindingScopeCheckSample {

  static final ScopedValue<String> USER = ScopedValue.newInstance();
  static final ScopedValue<Integer> COUNT = ScopedValue.newInstance();

  void noncompliant_getOutsideRunBlock() {
    ScopedValue.where(USER, "Alice");
    System.out.println(USER.get()); // Noncompliant {{Move this "get()" call inside a "run()" or "call()" block where the ScopedValue is bound.}}
  }

  void noncompliant_getWithoutAnyBinding() {
    System.out.println(USER.get()); // Noncompliant {{Move this "get()" call inside a "run()" or "call()" block where the ScopedValue is bound.}}
  }

  void noncompliant_getAfterRunBlock() {
    ScopedValue.where(USER, "Alice").run(() -> {
      // value is bound here
    });
    System.out.println(USER.get()); // Noncompliant {{Move this "get()" call inside a "run()" or "call()" block where the ScopedValue is bound.}}
  }

  void compliant_getInsideRunBlock() {
    ScopedValue.where(USER, "Alice").run(() -> {
      System.out.println(USER.get()); // Compliant
    });
  }

  void compliant_getInsideCallBlock() throws Exception {
    String result = ScopedValue.where(USER, "Alice").call(() -> {
      return USER.get(); // Compliant
    });
  }

  void compliant_getInsideNestedRunBlock() {
    ScopedValue.where(USER, "Alice").run(() -> {
      ScopedValue.where(COUNT, 42).run(() -> {
        System.out.println(USER.get()); // Compliant
        System.out.println(COUNT.get()); // Compliant
      });
    });
  }

  void compliant_multipleWhereChained() {
    ScopedValue.where(USER, "Alice").where(COUNT, 42).run(() -> {
      System.out.println(USER.get()); // Compliant
      System.out.println(COUNT.get()); // Compliant
    });
  }

  void compliant_isBound() {
    if (USER.isBound()) {
      System.out.println(USER.get()); // Compliant - guarded by isBound()
    }
  }

  void compliant_orElse() {
    String value = USER.orElse("default"); // Compliant - orElse does not throw
  }
}
