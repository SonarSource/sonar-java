class A {
  void foo() {
    int myInt = 4;
    boolean myBoolean = true;

    new Integer(myInt).toString(); // Noncompliant; creates and discards an Integer object
    Integer.toString(myInt); // Compliant
    A.returnInteger(myInt).toString(); // Compliant
    Integer.valueOf(myInt).toString(); // Noncompliant
    bar(new Integer(myInt).toString()); // Noncompliant

    new Boolean(myBoolean).toString(); // Noncompliant
    Boolean.toString(myBoolean); // Compliant
    Boolean.valueOf(myBoolean).toString(); // Noncompliant

    Integer myInteger = returnInteger(myInt);
    myInteger.toString(); // Compliant

    new Byte((byte) 0).toString(); // Noncompliant
    new Character('c').toString(); // Noncompliant
    new Short((short) 0).toString(); // Noncompliant
    new Long(0L).toString(); // Noncompliant
    new Float(0.0F).toString(); // Noncompliant
    new Double(0.0).toString(); // Noncompliant

    Integer.valueOf("4").toString(); // Compliant
    new Integer("4").toString(); // Noncompliant
  }

  static Integer returnInteger(int value) {
    return Integer.valueOf(value);
  }

  void bar(String s) {
  }
}