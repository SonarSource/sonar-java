package my.pkg;

class Base {
  public int b;
}

class MyClass extends Base {

  public String f;

  public String returnConstLiteral(String a) {
    return "foo";
  }

  public String returnParam(String a) {
    return a;
  }

  public String returnField() {
    return f;
  }

  public String returnParamThroughLocalVariable(String a) {
    String v = a;
    String v2;
    return v;
  }

  public String returnOfUninitializedLocalVariable() {
    String v;
    return v;
  }

  public String returnOneOfTwoParams(boolean flag, String a, String b) {
    if (flag) {
      return a;
    } else {
      return b;
    }
  }

  public String returnParamThroughTwoPaths(boolean flag, String a) {
    String v = a;
    if (flag) {
      return a;
    } else {
      return v;
    }
  }

}
