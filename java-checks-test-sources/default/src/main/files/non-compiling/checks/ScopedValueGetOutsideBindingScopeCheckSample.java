static final ScopedValue<String> USER = ScopedValue.newInstance();
static final ScopedValue<Integer> COUNT = ScopedValue.newInstance();

// === NONCOMPLIANT ===

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

void noncompliant_getInSeparateMethod() {
  doGet(); // The get() is in another method, not in a run/call lambda
}

void doGet() {
  System.out.println(USER.get()); // Noncompliant {{Move this "get()" call inside a "run()" or "call()" block where the ScopedValue is bound.}}
}

// === COMPLIANT ===

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

void compliant_carrierVariableUsedWithRun() {
  var carrier = ScopedValue.where(USER, "Alice");
  carrier.run(() -> {
    System.out.println(USER.get()); // Compliant
  });
}

void compliant_carrierVariableUsedWithCall() throws Exception {
  var carrier = ScopedValue.where(USER, "Alice");
  String result = carrier.call(() -> {
    return USER.get(); // Compliant
  });
}

void noncompliant_getInsideNonCarrierLambda() {
  Runnable r = () -> {
    System.out.println(USER.get()); // Noncompliant {{Move this "get()" call inside a "run()" or "call()" block where the ScopedValue is bound.}}
  };
}

void compliant_getInsideNestedLambdaInsideRun() {
  ScopedValue.where(USER, "Alice").run(() -> {
    Runnable r = () -> {
      System.out.println(USER.get()); // Compliant - still within the run block's execution scope
    };
  });
}
