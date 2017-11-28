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

}
