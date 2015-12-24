class FieldAfterConstructor {
  FieldAfterConstructor() {
  }
  int field; // Noncompliant [[sc=7;ec=12]] {{Move this variable to comply with Java Code Conventions.}}
}

class ConstructorAfterMethod {
  void method() {
  }
  FieldAfterConstructor() { // Noncompliant [[sc=3;ec=24]]
  }
}

class Ok {
  int field;

  Ok() {
  }

  void method() {
  }
}
