package checks;

public class RestrictedIdentifiersUsageCheckSample {

  void noncompliant() {
    String var = "var"; // Noncompliant {{Rename this variable to not match a restricted identifier.}}
//         ^^^
    var = "what is this?";

    String record = "record"; // Noncompliant {{Rename this variable to not match a restricted identifier.}}
//         ^^^^^^
  }

  void yield(int i) { // Noncompliant {{Rename this method to not match a restricted identifier.}}
//     ^^^^^
    switch (i) {
      case 1:
        System.out.println(1);
      default:
        System.out.println(i - 1);
    }
  }

  void method(String var) { // Noncompliant {{Rename this variable to not match a restricted identifier.}}
    String myVariable = "var";
    String myRecord = "record";

    yield(3); // is allowed if Java version <= 12, otherwise won't compile
  }

  void metho1(String yield) { // Noncompliant {{Rename this variable to not match a restricted identifier.}}
  }

  void method2(String record) { // Noncompliant {{Rename this variable to not match a restricted identifier.}}
  }

  void record() { // Noncompliant {{Rename this method to not match a restricted identifier.}}
  }

  void var() { // Noncompliant {{Rename this method to not match a restricted identifier.}}
  }

  int minusOne(int i) {
    switch (i) {
      case 1:
        System.out.println(0);
      default:
        System.out.println(i - 1);
    }
    return i - 1;
  }

}
