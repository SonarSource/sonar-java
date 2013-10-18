class FieldAfterConstructor {
  FieldAfterConstructor() {
  }
  int field;
}

class ConstructorAfterMethod {
  void method() {
  }
  FieldAfterConstructor() {
  }
}

class Ok {
  int field;

  Ok() {
  }

  void method() {
  }
}
