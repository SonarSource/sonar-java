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

  private void quickfixes() {
    var num = 99;
    var a = "" + String.valueOf(num) + " balloons"; // Noncompliant [[sc=18;ec=37;quickfixes=qf1]] {{Directly append the argument of String.valueOf().}}
    // fix@qf1 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf1 [[sc=18;ec=37]] {{num}}

    a = "" + String.valueOf(num + num); // Noncompliant [[sc=14;ec=39;quickfixes=qf2]]
    // fix@qf2 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf2 [[sc=14;ec=39]] {{(num + num)}}

    a = "" + String.valueOf(num - num); // Noncompliant [[sc=14;ec=39;quickfixes=qf3]]
    // fix@qf3 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf3 [[sc=14;ec=39]] {{(num - num)}}

    a = "" + String.valueOf(-num); // Noncompliant [[sc=14;ec=34;quickfixes=qf4]]
    // fix@qf4 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf4 [[sc=14;ec=34]] {{-num}}

    // Noncompliant@+1 [[sc=14;sl=+1;ec=6;el=+4;quickfixes=qfml]]
    a = "" + String.valueOf(2 +
      3 * 4 -
      5 / 6
    ) + 7;

    // fix@qfml {{Remove redundant String.valueOf() wrapping}}
    // edit@qfml [[sc=14;ec=6;el=+3]] {{(2 +\n      3 * 4 -\n      5 / 6)}}

    var world = "world";
    var b = "Hello, " + String.valueOf(world + world); // Noncompliant [[sc=25;ec=54;quickfixes=qf5]]
    // fix@qf5 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf5 [[sc=25;ec=54]] {{world + world}}

    b = "Hello, " + String.valueOf(world.charAt(2)) + "!"; // Noncompliant [[sc=21;ec=52;quickfixes=qf6]]
    // fix@qf6 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf6 [[sc=21;ec=52]] {{world.charAt(2)}}

    b = "Hello, " + String.valueOf((world.charAt(2))) + "!"; // Noncompliant [[sc=21;ec=54;quickfixes=qf7]]
    // fix@qf7 {{Remove redundant String.valueOf() wrapping}}
    // edit@qf7 [[sc=21;ec=54]] {{(world.charAt(2))}}
  }

  int bar(int i) { return 0; }
}
