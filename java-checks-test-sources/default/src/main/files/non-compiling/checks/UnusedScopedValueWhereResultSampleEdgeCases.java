import static java.lang.ScopedValue.where;

class UnusedScopedValueWhereResultSampleEdgeCases {

  static final ScopedValue<String> SCOPED = ScopedValue.newInstance();
  static final ScopedValue<String> SCOPED2 = ScopedValue.newInstance();

  // Field storage - should be flagged as we can't track field usage reliably
  ScopedValue.Carrier carrierField;
  static ScopedValue.Carrier staticCarrierField;



  void chainedWhereBeforeConsumption() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - chained .where() then .run()
    carrier.where(SCOPED2, "world").run(() -> {});
  }

  void chainedWhereBeforeCallConsumption() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - chained .where() then .call()
    carrier.where(SCOPED2, "world").call(() -> "result");
  }

  // ===== CONTROL FLOW =====

  void conditionalUsage(boolean condition) {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used conditionally
    if (condition) {
      carrier.run(() -> {});
    }
  }

  void conditionalUsageNotAllPaths(boolean condition) {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used in one branch (we don't do path-sensitive analysis)
    if (condition) {
      carrier.run(() -> {});
    }
    // else: carrier not used - but we can't detect this without CFG analysis
  }

  void carrierInTryFinally() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used in finally
    try {
      doSomething();
    } finally {
      carrier.run(() -> {});
    }
  }

  void carrierInLoop() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used in loop
    for (int i = 0; i < 3; i++) {
      carrier.run(() -> {});
    }
  }

  // ===== VARIABLE SCENARIOS =====

  void variableReassignedBeforeUse() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Noncompliant
    carrier = ScopedValue.where(SCOPED, "world");
    carrier.run(() -> {});
  }

  void multipleVariablesSameCarrier() {
    var carrier1 = ScopedValue.where(SCOPED, "hello"); // Compliant - used via carrier2 reference... actually no, different objects
    var carrier2 = carrier1; // Carrier2 points to same object
    carrier2.run(() -> {});
  }

  void multipleVariablesOneUnused() {
    var usedCarrier = ScopedValue.where(SCOPED, "hello"); // Compliant
    var unusedCarrier = ScopedValue.where(SCOPED, "world"); // Noncompliant
    usedCarrier.run(() -> {});
  }

  void carrierStoredInField() {
    carrierField = ScopedValue.where(SCOPED, "hello"); // Compliant - way of escaping
  }

  void carrierStoredInStaticField() {
    staticCarrierField = ScopedValue.where(SCOPED, "hello"); // Compliant - way of escaping
  }

  // ===== RETURN VARIATIONS =====

  ScopedValue.Carrier directReturnWithoutVariable() {
    return ScopedValue.where(SCOPED, "hello"); // Compliant - returned directly
  }

  ScopedValue.Carrier ternaryReturn(boolean condition) {
    var carrier1 = ScopedValue.where(SCOPED, "hello"); // Compliant - potentially returned
    var carrier2 = ScopedValue.where(SCOPED, "world"); // Compliant - potentially returned
    return condition ? carrier1 : carrier2;
  }

  // ===== NON-CONSUMING METHOD CALLS =====

  void toStringOnCarrier() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Noncompliant
    carrier.toString();
  }

  void hashCodeOnCarrier() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Noncompliant
    carrier.hashCode();
  }

  void equalsOnCarrier() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Noncompliant
    carrier.equals(null);
  }

  void getOnCarrier() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Noncompliant
    carrier.get(SCOPED);
  }

  // ===== LAMBDA/CLOSURE =====

  void carrierUsedInsideLambda() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used inside lambda
    Runnable r = () -> {
      carrier.run(() -> {});
    };
    r.run();
  }

  void carrierPassedToLambdaConsumer() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - escapes to lambda consumer
    consumeCarrier(c -> c.run(() -> {}), carrier);
  }

  // ===== NESTED ESCAPING =====

  void carrierPassedToMethodThatReturnsIt() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - escapes via method call
    var returned = identity(carrier);
    returned.run(() -> {});
  }

  void carrierPassedToMethodThatReturnsItButUnused() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - escapes via method call (we can't track further)
    var returned = identity(carrier);
    // returned is not used - but carrier escaped so we don't flag
  }

  // ===== STATIC IMPORT =====

  void staticImportWhere() {
    where(SCOPED, "hello"); // Noncompliant
  }

  void staticImportWhereUsed() {
    where(SCOPED, "hello").run(() -> {}); // Compliant - used immediately
  }

  void staticImportWhereVariable() {
    var carrier = where(SCOPED, "hello"); // Noncompliant
  }

  void staticImportWhereVariableUsed() {
    var carrier = where(SCOPED, "hello"); // Compliant - used
    carrier.run(() -> {});
  }

  // ===== CONSTRUCTOR ARGUMENT =====

  void carrierAsConstructorArgument() {
    var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - escapes via constructor
    new CarrierHolder(carrier);
  }

  void carrierAsConstructorArgumentDirect() {
    new CarrierHolder(ScopedValue.where(SCOPED, "hello")); // Compliant - escapes via constructor
  }

  // ===== HELPER METHODS AND CLASSES =====

  void doSomething() {}

  ScopedValue.Carrier identity(ScopedValue.Carrier carrier) {
    return carrier;
  }

  void consumeCarrier(java.util.function.Consumer<ScopedValue.Carrier> consumer, ScopedValue.Carrier carrier) {
    consumer.accept(carrier);
  }

  static class CarrierHolder {
    CarrierHolder(ScopedValue.Carrier carrier) // Noncompliant
    {}
  }
}
