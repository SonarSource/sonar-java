class A {
  void foo() {
    String myStr = "Hello World";
    int pos = myStr.indexOf("W"); // Noncompliant  [[sc=29;ec=32]] {{Put single-quotes around 'W' to use the faster "indexOf(char)" method.}}
    pos = myStr.lastIndexOf("W"); // Noncompliant  [[sc=29;ec=32]] {{Put single-quotes around 'W' to use the faster "lastIndexOf(char)" method.}}
    pos = myStr.lastIndexOf("\""); // Noncompliant  [[sc=29;ec=33]] {{Put single-quotes around '\"' to use the faster "lastIndexOf(char)" method.}}
  }

  void bar(String param) {
    String myStr = "Hello World";
    int pos = myStr.indexOf('W');
    pos = myStr.indexOf("WR");
    pos = myStr.lastIndexOf("WR");
    pos = myStr.indexOf(param);
    pos = myStr.lastIndexOf("abc");
    if (myStr.charAt(0) == 'A') {
    }
    if (myStr.startsWith("AB")) {

    }
  }
}
