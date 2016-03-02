class A {
  void foo() {
    String myStr = "Hello World";
    int pos = myStr.indexOf("W"); // Noncompliant  [[sc=29;ec=32]] {{Put single-quotes around 'W' to use the faster "indexOf(char)" method.}}
    pos = myStr.lastIndexOf("W"); // Noncompliant  [[sc=29;ec=32]] {{Put single-quotes around 'W' to use the faster "lastIndexOf(char)" method.}}
    if (myStr.startsWith("A")) { // Noncompliant [[sc=26;ec=29]] {{Use charAt(int) instead}}
    }
    if (myStr.endsWith("A")) {
    }
  }

  void bar() {
    String myStr = "Hello World";
    int pos = myStr.indexOf('W');
    pos = myStr.indexOf("WR");
    pos = myStr.lastIndexOf("WR");
    if (myStr.charAt(0) == 'A') {
    }
    if (myStr.startsWith("AB")) {

    }
  }
}
