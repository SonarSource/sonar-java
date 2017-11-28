package my.pkg;

class Base {
  public int b;
}

class MyClass extends Base {

  public String returnConstLiteral(String a) {
    return "foo";
  }

  public String returnParam(String a) {
    return a;
  }

  public String returnOneOfTwoParams(boolean flag, String a, String b) {
    if (flag) {
      return a;
    } else {
      return b;
    }
  }

}
