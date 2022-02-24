package checks;

class ConcatenationWithStringValueOfCheck {
  Object o;
  Object[] arr;
  ConcatenationWithStringValueOfCheck valueOf;

  private void f(int buf, ConcatenationWithStringValueOfCheck a) {
    System.out.println("" + String.valueOf(0)); // Noncompliant [[sc=29;ec=46]] {{Directly append the argument of String.valueOf().}}
    System.out.println("" + String.valueOf(null, 0, 0)); // Compliant
    System.out.println(String.valueOf(0)); // Compliant
    System.out.println("" + ""); // Compliant
    System.out.println(String.valueOf(0) + ""); // Compliant
    System.out.println("" + "foo" +
      String.valueOf('a') + // Noncompliant
      String.valueOf('b') + // Noncompliant
      "");
    System.out.println("" + "".length()); // Compliant
    System.out.println("" + a.bar(0)); // Compliant
    System.out.println("" + Integer.valueOf(0)); // Compliant
    System.out.println("" + a.o); // Compliant
    System.out.println("" + a.arr[0]); // Compliant
    System.out.println("" + a.valueOf.bar(0)); // Compliant

    o = ("" + String.valueOf('a')) + ""; // Noncompliant
    o = "" + ("" + String.valueOf('a')); // Noncompliant

    o = 0 + String.valueOf('a'); // Compliant
    int position = 1;
    o = buf + "tab @" + String.valueOf(position); // Noncompliant
    char[] chars = new char[] {'a', 'b'};
    System.out.println(""+String.valueOf(chars)); // compliant char[].toString != String.valueOf(char[])

    // coverage on targeted binary expressions
    o = 42 - 2;
  }

  int bar(int i) { return 0; }
}
