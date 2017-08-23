class BadLocalVariableName {
  void method(
    int BAD_FORMAL_PARAMETER
  ) {
    int BAD;
    int good;

    for (int I = 0; I < 10; I++) {
      int DVAR;
    }

    for (good = 0; good < 10; good++) {
    }

    try (Closeable BAD_RESOURCE = open()) {
    } catch (Exception BAD_EXCEPTION) {
    } catch (Exception e) {
    }
  }

  Object FIELD_SHOULD_NOT_BE_CHECKED = new Object(){
    {
      int BAD;
    }
  };

  void forEachMethod() {
    for (byte C : "".getBytes()) {
      int DVAR;
    }
  }

}
