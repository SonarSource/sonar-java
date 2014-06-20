class A {
  String str1 = "blue";
  String str2 = "blue";
  String[] strArray1 = {"blue"};
  String[] strArray2 = {"blue"};
  private void method() {
    if (str1 == str2) {}
    if(str1 == "green") {}
    if (str1.equals(str2)) {}
    if(strArray1 == strArray2) {}
    if(null == str1){ }
    if(str1 == null){ }
  }
}

class B {
  String[][] strArray2 = {{"blue"}};
  private void method() {
    if(strArray2 == strArray2){}
    if(strArray2[0] == strArray2[1]){}
    if(strArray2[0][0] == strArray2[1][1]){}
    byte[] bits;
    if(bits [0] == bits [1]) {}
    if(Foo.FOO == Foo.BAR) {}
  }
enum Foo {
  FOO,
  BAR;
}
}
