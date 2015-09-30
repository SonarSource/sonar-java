class FieldAfterConstructor {
  FieldAfterConstructor() {
  }
  int field; // Noncompliant {{Move this variable to comply with Java Code Conventions.}}
}

class ConstructorAfterMethod {
  void method() {
  }
  FieldAfterConstructor() { // Noncompliant
  }
}

class Ok {
  int field;

  Ok() {
  }

  void method() {
  }
}
