class A {

  final int myVal = 42; // Compliant - field

  void foo(final boolean b) {
    double myOtherLocalConst = 10.5; // Compliant
    final int myLocalConst = 21; // Noncompliant
    final int myLocalConst2 = bar(); // compliant - only target literals
    final int MY_LOCAL_CONST = 42; // Compliant
    final String MY_OTHER_LOCAL_CONST = "helloWorld"; // Compliant
    final java.util.List<String> myList = new java.util.ArrayList<>(); // Compliant

    final int myVar; // Compliant - no initilizer
    if (b) {
      myVar = 42;
    } else {
      myVar = 36;
    }

    final java.util.function.Supplier<String> supplier = () -> {
      final int myValue = 42; // Noncompliant
      return myValue + "";
    };
  }
}
