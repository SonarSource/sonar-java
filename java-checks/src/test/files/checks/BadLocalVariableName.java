class BadLocalVariableName {
  void method(int BAD_FORMAL_PARAMETER) {
    int BAD;
    int good;

    for (int I; I < 10; I++) {
    }

    try (Closeable BAD_RESOURCE = open()) {
    } catch (Exception BAD_EXCEPTION) {
    }
  }
}
