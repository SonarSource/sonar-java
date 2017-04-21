class BadLocalVariableName {
  void method(
    int BAD_FORMAL_PARAMETER // Noncompliant [[sc=9;ec=29]] {{Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
  ) {
    int BAD; // Noncompliant
    int good;

    for (int I = 0; I < 10; I++) {
      int D; // Noncompliant
    }

    for (good = 0; good < 10; good++) {
    }

    try (Closeable BAD_RESOURCE = open()) { // Noncompliant
    } catch (Exception BAD_EXCEPTION) { // Noncompliant
    }
  }

  Object FIELD_SHOULD_NOT_BE_CHECKED = new Object(){
    {
      int BAD; // Noncompliant
    }
  };

  void forEachMethod() {
    for (byte C : "".getBytes()) {
      int D; // Noncompliant
    }
  }

}
