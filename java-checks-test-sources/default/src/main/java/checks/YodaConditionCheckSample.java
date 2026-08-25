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
    if ((0) == count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
    if (((null)) == count) { } // Noncompliant {{Put the variable on the left side of this comparison.}}
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

  void testNonComparisonContexts() {
    int count = 0;
    int a = 1;
    int b = 2;
    count = 0; // Compliant - assignment
    int sum = a + 5; // Compliant - arithmetic
    int product = 5 * b; // Compliant - arithmetic
    Object obj = Math.max(5, 10); // Compliant - method call
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
}
