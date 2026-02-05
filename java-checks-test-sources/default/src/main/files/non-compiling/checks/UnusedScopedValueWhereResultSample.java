static final ScopedValue myScopedValue = ScopedValue.newInstance();

void main() {
  var unusedCarrierGet = ScopedValue.where(myScopedValue, "hello").get(myScopedValue); // Noncompliant
  ScopedValue.where(myScopedValue, "hello").run(() -> {
  }); // Compliant, the result is used immediately
  ScopedValue.where(myScopedValue, "hello"); // Noncompliant
  var myUnusedCarrier = ScopedValue.where(myScopedValue, "hello"); // Noncompliant
  var myUnused2LevelCarrier = ScopedValue.where(myScopedValue, "hello").where(ScopedValue.newInstance(), "hello"); // Noncompliant
  var myUsedCarrier = ScopedValue.where(myScopedValue, "hello"); // Compliant, the result is assigned to a variable and used
  myUsedCarrier.run(() -> {
  });
}

void escapedCarrierFunctionCall() {
  var carrier = ScopedValue.where(myScopedValue, "hello"); // ccompliant - the result escapes
  usedCarrierArgument(carrier);
}

void usedCarrierArgument(ScopedValue.Carrier carrier) { // compliant - the carrier is used in the function
  carrier.run(() -> {
  });
}

void unusedCarrierArgument(ScopedValue.Carrier carrier) { // Noncompliant
}

ScopedValue.Carrier escapedCarrierReturn() {
  var carrier = ScopedValue.where(myScopedValue, "hello"); // compliant - the result escapes
  return carrier;
}

void escapedCarrierArgument(ScopedValue.Carrier carrier) { // compliant - the carrier is used in the function
  usedCarrierArgument(carrier);
}
