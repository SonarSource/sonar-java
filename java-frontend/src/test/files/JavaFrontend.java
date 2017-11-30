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

  public String returnOneOfTwoParams(boolean flag, String a, String b) {
    if (flag) {
      return a;
    } else {
      return b;
    }
  }

}
