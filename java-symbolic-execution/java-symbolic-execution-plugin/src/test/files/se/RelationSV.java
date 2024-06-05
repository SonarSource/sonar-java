class Test {

  Boolean valid;

  void test(Object param1, Object param2) {
    if (valid == null) {
      valid = false;
    }

    String string1 = param1 == null ? null : param1.toString();
    String string2 = param2 == null ? null : param2.toString();

    boolean bool1 = valid && string1 != null;
    boolean bool2 = valid && string2 != null;

    if (bool1 != null) {

    }
    if (bool2 != null) {

    }
  }
}
