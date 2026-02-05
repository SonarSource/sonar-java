static final ScopedValue myScopedValue = ScopedValue.newInstance();

void main() {
  ScopedValue.where(myScopedValue, "hello").run(() -> {
  }); // Compliant, the result is used immediately
  ScopedValue.where(myScopedValue, "hello"); // Noncompliant
  var myUnusedCarrier = ScopedValue.where(myScopedValue, "hello"); // Noncompliant
  var myUnused2LevelCarrier = ScopedValue.where(myScopedValue, "hello").where(ScopedValue.newInstance(), "hello"); // Noncompliant
  var myUsedCarrier = ScopedValue.where(myScopedValue, "hello"); // Compliant, the result is assigned to a variable and used
  myUsedCarrier.run(() -> {
  });
}
