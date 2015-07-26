class BadLocalVariableName {
  void method(
    int BAD_FORMAL_PARAMETER // Noncompliant
  ) {
    int BAD; // Noncompliant
    int good; // Compliant

    for (int I = 0; I < 10; I++) { // Compliant, exception
      int D; // Noncompliant
    }

    for (good = 0; good < 10; good++) {
    }

    try (Closeable BAD_RESOURCE = open()) { // Noncompliant
    } catch (Exception BAD_EXCEPTION) { // Noncompliant
    }
  }

  Object FIELD_SHOULD_NOT_BE_CHECKED = new Object(){ // Compliant
    {
      int BAD; // Noncompliant
    }
  };

  void forEachMethod() {
    for (byte C : "".getBytes()) { // Compliant, exception
      int D; // Noncompliant
    }
  }

}
