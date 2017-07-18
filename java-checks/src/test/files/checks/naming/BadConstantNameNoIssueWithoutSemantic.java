import java.util.function.Supplier;

class BadConstantName {

  static final int CONST = 42; // Compliant

  void foo() {
    double myOtherLocalConst = 10.5; // Compliant
    final int myLocalConst = 21; // Compliant
    final int MY_LOCAL_CONST = 42; // Compliant
    final String MY_OTHER_LOCAL_CONST = "helloWorld"; // Compliant
    final java.util.List<String> myList = new java.util.ArrayList<>(); // Compliant

    final java.util.function.Supplier<String> supplier = () -> { 
      final int myValue = 42; 
      return myValue + "";
    };
  }

}
