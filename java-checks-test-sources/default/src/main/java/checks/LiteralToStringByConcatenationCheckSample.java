package checks;

class LiteralToStringByConcatenationCheckSample {

  private static final String EMPTY = "";
  static final String CONSTANT = "" + 42;
  static final String PARENTHESIZED_CONSTANT = ("" + 42);
  final String instanceField = "" + 42; // Noncompliant
  static String staticField = "" + 42; // Noncompliant
  static final Runnable LAMBDA = () -> System.out.println("" + 42); // Noncompliant

  interface Constants {
    String INTERFACE_CONSTANT = "" + 42;
  }

  @SuppressWarnings("" + 1)
  void annotated() {
  }

  String switchOnString(String input) {
    switch (input) {
      case "" + 42:
        return "" + 1; // Noncompliant
      default:
        return input;
    }
  }

  void loopVariable() {
    for (String s = "" + 0; s.isEmpty(); ) { // Noncompliant
      break;
    }
  }

  String convertLiterals(int value) {
    String itemCount = "" + 123; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                     ^^^^^^^^
    String enabled = "" + true; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^^
    String ratio = "" + 3.14f; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                 ^^^^^^^^^^
    String initial = "" + 'A'; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^
    String timeout = 42L + ""; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^
    String rate = ("") + (2.5d); // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                ^^^^^^^^^^^^^
    return itemCount + enabled + ratio + initial + timeout + rate;
  }

  String explicitOrOrdinaryConcatenation(int value) {
    String explicitInteger = Integer.toString(123);
    String explicitBoolean = Boolean.toString(true);
    String explicitGeneric = String.valueOf(3.14f);
    String prefixed = "items=" + 123;
    String variable = "" + value;
    String absent = "" + null;
    String text = "" + "ready";
    String constant = EMPTY + 123;
    String computed = "" + (1 + 2);
    return explicitInteger + explicitBoolean + explicitGeneric + prefixed + variable + absent + text + constant + computed;
  }
}
