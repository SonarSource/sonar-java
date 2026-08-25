package checks;

class YodaConditionCheckSample {

  void testIntLiteral() {
    int count = 0;
    int x = 5;
    if (0 == count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (5 != x) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (count == 0) { } // Compliant
    if (x != 5) { } // Compliant
  }

  void testNullLiteral() {
    Object obj = null;
    Object myObject = null;
    if (null == obj) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^^
    if (null != myObject) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^^
    if (obj == null) { } // Compliant
    if (myObject != null) { } // Compliant
    if (null == null) { } // Compliant
  }

  void testBooleanLiteral() {
    boolean flag = true;
    boolean result = false;
    if (true == flag) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^^
    if (false != result) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^^^
    if (flag == true) { } // Compliant
    if (result != false) { } // Compliant
  }

  void testStringLiteral() {
    String str = "hello";
    String value = "";
    if ("hello" == str) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^^^^^
    if ("" != value) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^
    if (str == "hello") { } // Compliant
    if (value != "") { } // Compliant
  }

  void testCharLiteral() {
    char ch = 'a';
    if ('a' == ch) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^
    if (ch == 'a') { } // Compliant
  }

  void testFloatingPointLiteral() {
    double doubleValue = 0.0;
    if (0.0 == doubleValue) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^^^
    if (doubleValue == 0.0) { } // Compliant
  }

  void testNestedParentheses() {
    int count = 0;
    Object obj = new Object();
    if ((0) == count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
    if (((null)) == obj) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//        ^^^^
  }

  void testLessThanGreaterThan() {
    int count = 0;
    int x = 5;
    if (0 < count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (5 > x) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (count > 0) { } // Compliant
    if (x < 5) { } // Compliant
  }

  void testLessThanOrEqualGreaterThanOrEqual() {
    int count = 0;
    int x = 5;
    if (0 <= count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (5 >= x) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (count >= 0) { } // Compliant
    if (x <= 5) { } // Compliant
  }

  void testNonComparisonContexts() {
    int count = 0;
    int a = 1;
    int b = 2;
    count = 0; // Compliant - assignment
    int sum = a + 5; // Compliant - arithmetic
    int product = 5 * b; // Compliant - arithmetic
  }

  void testTernaryOperator() {
    boolean condition = true;
    int result = condition ? 5 : 10; // Compliant
    if (condition) { } // Compliant
  }

  void testArrayAccess() {
    int[] array = {1, 2, 3};
    if (0 == array[0]) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
//      ^
    if (array[0] == 0) { } // Compliant
  }

  void testBothLiterals() {
    if (0 == 0) { } // Compliant - both sides are literals
    if (5 != 10) { } // Compliant - both sides are literals
    if (true == false) { } // Compliant - both sides are literals
  }

  void testBothVariables() {
    int count = 0;
    int otherCount = 0;
    Object obj1 = null;
    Object obj2 = null;
    if (count == otherCount) { } // Compliant - both are variables
    if (obj1 == obj2) { } // Compliant - both are variables
  }

  void testConstantMathCalls() {
    int max = Math.max(5, 10); // Noncompliant {{Replace this call to "max" with the precomputed constant value.}}
//            ^^^^^^^^
    int min = Math.min(3, 7); // Noncompliant {{Replace this call to "min" with the precomputed constant value.}}
//            ^^^^^^^^
    double sqrt = Math.sqrt(16.0); // Noncompliant {{Replace this call to "sqrt" with the precomputed constant value.}}
//                ^^^^^^^^^
    int abs = Math.abs(-5); // Noncompliant {{Replace this call to "abs" with the precomputed constant value.}}
//            ^^^^^^^^
    double pow = Math.pow(2.0, 3.0); // Noncompliant {{Replace this call to "pow" with the precomputed constant value.}}
//               ^^^^^^^^
    long rounded = Math.round(3.14); // Noncompliant {{Replace this call to "round" with the precomputed constant value.}}
//                 ^^^^^^^^^^
    double floor = Math.floor(3.7); // Noncompliant {{Replace this call to "floor" with the precomputed constant value.}}
//                 ^^^^^^^^^^
    double ceil = Math.ceil(3.2); // Noncompliant {{Replace this call to "ceil" with the precomputed constant value.}}
//                ^^^^^^^^^
  }

  void testConstantMathCallsCompliant() {
    int x = 5;
    int y = 10;
    int max = Math.max(x, 10); // Compliant - x is not a literal
    int min = Math.min(3, y); // Compliant - y is not a literal
    double sqrt = Math.sqrt(x); // Compliant - x is not a literal
    int abs = Math.abs(x); // Compliant - x is not a literal
    int maxVar = Math.max(x, y); // Compliant - neither is a literal
  }

  void testConstantMathCallsWithUnaryMinus() {
    int abs = Math.abs(-10); // Noncompliant {{Replace this call to "abs" with the precomputed constant value.}}
//            ^^^^^^^^
    int max = Math.max(-5, -3); // Noncompliant {{Replace this call to "max" with the precomputed constant value.}}
//            ^^^^^^^^
    double sqrt = Math.sqrt(+4.0); // Noncompliant {{Replace this call to "sqrt" with the precomputed constant value.}}
//                ^^^^^^^^^
  }

  void testNonMathMethodCalls() {
    String result = String.valueOf(5); // Compliant - not a Math method
    int hash = Integer.hashCode(42); // Compliant - not a Math method
  }
}
