static final ScopedValue myScopedValue = ScopedValue.newInstance();
static final ScopedValue myScopedValue2 = ScopedValue.newInstance();

void main() {
  ScopedValue.where(myScopedValue, "Simple chained with get").get(myScopedValue); // Noncompliant
  ScopedValue.where(myScopedValue, "Simple chained with run").run(() -> {
  }); // Compliant, the result is used immediately
  ScopedValue.where(myScopedValue, "Chained two").where(myScopedValue2, "times with run").run(() -> {
  }); // Compliant, the result is used immediately
  ScopedValue.where(myScopedValue, "Simple carrier creation"); // Noncompliant
  var myUnusedCarrier = ScopedValue.where(myScopedValue, "Unused carrier in variable"); // Noncompliant
  var myUnused2LevelCarrier = ScopedValue.where(myScopedValue, "Unused carrrier in variable").where(myScopedValue2, "2 levels of where"); // Noncompliant
  var myUsedCarrier = ScopedValue.where(myScopedValue, "Used carrier in variable"); // Compliant, the result is assigned to a variable and used
  var myUsed2LevelCarrier = ScopedValue.where(myScopedValue, "Used carrier in variable").where(myScopedValue2, "2 levels of where"); // Compliant, the result is assigned to a variable and used
  myUsedCarrier.run(() -> {
  });
  myUsed2LevelCarrier.run(() -> {
  });
}

void escapedCarrierFunctionCall() {
  var carrier = ScopedValue.where(myScopedValue, "Escape a carrier with a function call"); // compliant - the result escapes
  usedCarrierArgument(carrier);
}

void usedCarrierArgument(ScopedValue.Carrier carrier) { // compliant - the carrier is used in the function
  carrier.run(() -> {
  });
}

void unusedCarrierArgument(ScopedValue.Carrier carrier) { // Noncompliant
}

ScopedValue.Carrier escapedCarrierReturn() {
  var carrier = ScopedValue.where(myScopedValue, "Escape a carrier with a return"); // compliant - the result escapes
  return carrier;
}

void escapedCarrierArgument(ScopedValue.Carrier carrier) { // compliant - the carrier is used in the function
  usedCarrierArgument(carrier);
}
