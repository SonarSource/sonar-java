abstract class A {
  private static final int NUMBER = 42;
  private static int static_number = 42;
  private final int final_number = 42;
  private int field_number = 42;
  private static final String STRING = "string";

  void foo() {
    int myInt = 4;
    boolean myBoolean = true;

    new Integer(myInt).toString(); // Noncompliant [[sc=5;ec=34]] {{Use "Integer.toString" instead.}}
    Integer.toString(myInt); // Compliant
    A.returnInteger(myInt).toString(); // Compliant
    Integer.valueOf(myInt).toString(); // Noncompliant {{Use "Integer.toString" instead.}}
    bar(new Integer(myInt).toString()); // Noncompliant [[sc=9;ec=38]] {{Use "Integer.toString" instead.}}

    new Boolean(myBoolean).toString(); // Noncompliant {{Use "Boolean.toString" instead.}}
    Boolean.toString(myBoolean); // Compliant
    Boolean.valueOf(myBoolean).toString(); // Noncompliant {{Use "Boolean.toString" instead.}}

    Integer myInteger = returnInteger(myInt);
    myInteger.toString(); // Compliant

    new Byte((byte) 0).toString(); // Noncompliant {{Use "Byte.toString" instead.}}
    new Character('c').toString(); // Noncompliant {{Use "Character.toString" instead.}}
    new Short((short) 0).toString(); // Noncompliant {{Use "Short.toString" instead.}}
    new Long(0L).toString(); // Noncompliant {{Use "Long.toString" instead.}}
    new Float(0.0F).toString(); // Noncompliant {{Use "Float.toString" instead.}}
    new Double(0.0).toString(); // Noncompliant {{Use "Double.toString" instead.}}

    Integer.valueOf("4").toString(); // Compliant
    new Integer("4").toString(); // Noncompliant {{Use "Integer.toString" instead.}}

    // Raising issue on string concatenation was removed from rule, kept for historical reason
    String myString = 4 + ""; // Compliant
    myString = "" + 4; // Compliant
    myString = "foo" + 4; // compliant
    myString = "foo" + 4.0; // compliant
    myString = "" + 4.0; // Compliant
    myString = "" + true; // Compliant

    foo("" + true); // Compliant
    foo("Foo" + "" + true); // Compliant
    foo("" + true + "Foo"); // Compliant

    myString = NUMBER + ""; // Compliant
    myString = A.NUMBER + ""; // Compliant
    myString = this.NUMBER + ""; // Compliant
    myString = "" + this.NUMBER; // Compliant
    myString = NUMBER + "foo"; // Compliant

    myString = static_number + ""; // Compliant
    myString = final_number + ""; // Compliant
    myString = field_number + ""; // Compliant

    myString = myInt + ""; // Compliant
    myString = null + ""; // Compliant
    myString = getValue() + ""; // Compliant

    myString = STRING + "";
  }

  static Integer returnInteger(int value) {
    return Integer.valueOf(value);
  }

  abstract int getValue();

  @Foo(value = "" + 12) // Compliant
  void bar(String s) {
    s += "";
    s += 12; // Compliant
  }
}
