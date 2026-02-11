import static java.lang.ScopedValue.where;


static final ScopedValue<String> SCOPED = ScopedValue.newInstance();
static final ScopedValue<String> SCOPED2 = ScopedValue.newInstance();

// Field storage - shouldn't be flagged as we can't track field usage reliably
ScopedValue.Carrier carrierField;
static ScopedValue.Carrier staticCarrierField;

// ===== CONTROL FLOW =====

void conditionalUsage(boolean condition) {
  var carrier = ScopedValue.where(SCOPED, "Conditional usage"); // Compliant - used conditionally
  if (condition) {
    carrier.run(() -> {
    });
  }
}

// ===== VARIABLE SCENARIOS =====

void variableReassignedBeforeUse() {
  var carrier = ScopedValue.where(SCOPED, "Initial instance"); // Noncompliant
  carrier = ScopedValue.where(SCOPED, "Second instance"); // Compliant - this instance is used
  carrier.run(() -> {
  });
}

void multipleVariablesSameCarrierUsed() {
  var carrier1 = ScopedValue.where(SCOPED, "Single instance"); // Compliant
  var carrier2 = carrier1; // Carrier2 points to same object
  carrier2.run(() -> {
  });
}

void multipleVariablesSameCarrierUsed2() {
  var carrier1 = ScopedValue.where(SCOPED, "Single instance"); // Compliant
  var carrier2 = carrier1; // Carrier2 points to same object
  carrier1.run(() -> {
  });
}

void multipleVariablesSameCarrierUnused() {
  var carrier1 = ScopedValue.where(SCOPED, "Single instance unused"); // Noncompliant
  var carrier2 = carrier1; // Carrier2 points to same object but is never used
}

void carrierStoredInField() {
  carrierField = ScopedValue.where(SCOPED, "Stored in instance field"); // Compliant - way of escaping
}

void carrierStoredInStaticField() {
  staticCarrierField = ScopedValue.where(SCOPED, "Stored in static field"); // Compliant - way of escaping
}

void escapingInFieldTwice() {
  carrierField = ScopedValue.where(SCOPED, "Instance 1"); // Compliant - escapes, we can't track field usage
  carrierField = ScopedValue.where(SCOPED, "Instance 2"); // Compliant - reassigned but still escapes
}

// ===== RETURN VARIATIONS =====

ScopedValue.Carrier ternaryReturn(boolean condition) {
  var carrier1 = ScopedValue.where(SCOPED, "Ternary return - true"); // Compliant - potentially returned
  var carrier2 = ScopedValue.where(SCOPED, "Ternary return - false"); // Compliant - potentially returned
  return condition ? carrier1 : carrier2;
}

// ===== NON-CONSUMING METHOD CALLS =====

void standardFunctinsOnCarrier() {
  var carrier = ScopedValue.where(SCOPED, "Standard functions"); // Noncompliant
  carrier.toString();
  carrier.hashCode();
  carrier.equals(null);
}

void getOnCarrier() {
  var carrier = ScopedValue.where(SCOPED, "Get function"); // Noncompliant
  carrier.get(SCOPED);
}

// ===== LAMBDA/CLOSURE =====

void carrierUsedInsideLambda() {
  var carrier = ScopedValue.where(SCOPED, "hello"); // Compliant - used inside lambda
  Runnable r = () -> {
    carrier.run(() -> {
    });
  };
  r.run();
}

// ===== STATIC IMPORT =====

void staticImportWhere() {
  where(SCOPED, "Static import unused"); // Noncompliant
}

void staticImportWhereUsed() {
  where(SCOPED, "Static import used").run(() -> {
  }); // Compliant - used immediately
}

void staticImportWhereVariable() {
  var carrier = where(SCOPED, "Static import unused variable"); // Noncompliant
}

void staticImportWhereVariableUsed() {
  var carrier = where(SCOPED, "Static import used variable "); // Compliant - used
  carrier.run(() -> {
  });
}

// ===== CONSTRUCTOR ARGUMENT =====

void carrierAsConstructorArgument() {
  var carrier = ScopedValue.where(SCOPED, "Carrier in constructor argument"); // Compliant - escapes via constructor
  new CarrierHolder(carrier);
}

// ===== HELPER METHODS AND CLASSES =====


static class CarrierHolder {
  CarrierHolder(ScopedValue.Carrier carrier) // Noncompliant
  {
  }

  CarrierHolder(ScopedValue.Carrier carrier, int i) // Compliant
  {
    carrier.run(() -> {
    });
  }
}
