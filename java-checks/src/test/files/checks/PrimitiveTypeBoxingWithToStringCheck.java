class A {
  void foo() {
    int myInt = 4;
    boolean myBoolean = true;

    new Integer(myInt).toString(); // Noncompliant {{Use "Integer.toString" instead.}}
    Integer.toString(myInt); // Compliant
    A.returnInteger(myInt).toString(); // Compliant
    Integer.valueOf(myInt).toString(); // Noncompliant {{Use "Integer.toString" instead.}}
    bar(new Integer(myInt).toString()); // Noncompliant {{Use "Integer.toString" instead.}}

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

    String myString = 4 + ""; // Noncompliant {{Use "Integer.toString" instead.}}
    myString = "" + 4; // Noncompliant {{Use "Integer.toString" instead.}}
    myString = "foo" + 4; // compliant
    myString = "foo" + 4.0; // compliant
    myString = "" + 4.0; // Noncompliant {{Use "Double.toString" instead.}}
    myString = "" + true; // Noncompliant {{Use "Boolean.toString" instead.}}
    foo("" + true); // Noncompliant {{Use "Boolean.toString" instead.}}
    foo("Foo" + "" + true); // compliant true is added to the result of the concatenation of Foo and ""
    foo("" + true + "Foo"); // Noncompliant {{Use "Boolean.toString" instead.}}

  }

  static Integer returnInteger(int value) {
    return Integer.valueOf(value);
  }

  @Foo(value = "" + 12) // Compliant
  void bar(String s) {
    s += "";
    s += 12; // Noncompliant
  }
}
