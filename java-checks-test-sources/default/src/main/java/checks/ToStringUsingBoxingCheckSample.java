package checks;

class ToStringUsingBoxingCheckSample {
  private void f() {
    new Byte((byte) 12).toString(); // Noncompliant {{Call the static method Byte.toString(...) instead of instantiating a temporary object.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    new Short((short) 0).toString(); // Noncompliant {{Call the static method Short.toString(...) instead of instantiating a temporary object.}}
    new Integer(0).toString(); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object.}}
    new java.lang.Integer(0).toString(); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object.}}
    new Long(0L).toString(); // Noncompliant {{Call the static method Long.toString(...) instead of instantiating a temporary object.}}
    new Float(0f).toString(); // Noncompliant {{Call the static method Float.toString(...) instead of instantiating a temporary object.}}
    new Double(0d).toString(); // Noncompliant {{Call the static method Double.toString(...) instead of instantiating a temporary object.}}
    new Character('a').toString(); // Noncompliant {{Call the static method Character.toString(...) instead of instantiating a temporary object.}}
    new Boolean(false).toString(); // Noncompliant {{Call the static method Boolean.toString(...) instead of instantiating a temporary object.}}
    new Integer(0).toString(1); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object.}}
    new Integer(0).toString(1, 2); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object.}}
    new Integer(0).compareTo(0); // Noncompliant {{Call the static method Integer.compare(...) instead of instantiating a temporary object.}}
    new Boolean(false).compareTo(true); // Noncompliant {{Call the static method Boolean.compare(...) instead of instantiating a temporary object.}}

    int myInt = 4;
    new Integer(myInt).toString(); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object.}}
    new Integer(myInt).compareTo(0); // Noncompliant {{Call the static method Integer.compare(...) instead of instantiating a temporary object.}}
    Integer.valueOf(myInt).compareTo(0); // Noncompliant {{Call the static method Integer.compare(...) instead of instantiating a temporary object.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Integer.toString(0); // Compliant
    Integer.toString(0, 2); // Compliant

    boolean myBoolean = true;
    new Boolean(myBoolean).toString(); // Noncompliant {{Call the static method Boolean.toString(...) instead of instantiating a temporary object.}}
    Boolean.valueOf(myBoolean).toString(); // Noncompliant {{Call the static method Boolean.toString(...) instead of instantiating a temporary object.}}
    Boolean.valueOf(myBoolean).compareTo(false); // Noncompliant {{Call the static method Boolean.compare(...) instead of instantiating a temporary object.}}
    Boolean.toString(myBoolean); // Compliant
    Boolean.compare(myBoolean, true); // Compliant

    new RuntimeException("").toString(); // Compliant
    Integer.toString(0); // Compliant
    Integer.toString(0); // Compliant
    new Integer(0).getClass().toString(); // Compliant

    new int[0].toString(); // Compliant

    Integer integer = Integer.valueOf(12);
    integer++; // Compliant
    (integer).toString(); // Compliant
    toString();
  }

  private void trueNegative(String[] array, int i) {
    // When the type of the argument of the wrapper creation is not the same as the argument, the operation requires a cast.
    new Byte("1").toString();
    new Long("2").toString();
    Integer.valueOf("1").compareTo(2);
    Integer.valueOf("1").toString();
    Integer.valueOf(array[i]).compareTo(Integer.valueOf(array[i + 1]));

    // We can not directly use toString here, we have to add "f" or cast it.
    // The quick fix will not compile, but it seems like a corner case
    // due to the arguable choice to have a Float constructor taking a double in the first place...
    new Float(1.2).toString(); // Noncompliant
    Float.toString(1.2f);
    Float.toString((float) 1.2);
    // With the non-deprecated way, "valueOf" with double does not exist, everything will work just fine
    Float.valueOf(1.2f).toString(); // Noncompliant

    // Some types can be automatically casted to others
    new Long(0).toString(); // Noncompliant
    new Float(0).toString(); // Noncompliant
    new Double(0).toString(); // Noncompliant
    new Double(1.2f).toString(); // Noncompliant
    byte myByte = 4;
    new Short(myByte).toString(); // Noncompliant
  }

  private void quickFixes(boolean myBoolean) {
    new Byte((byte) 1).toString(); // Noncompliant [[quickfixes=qf1]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Use Byte.toString(...) instead}}
    // edit@qf1 [[sc=5;ec=14]] {{Byte.toString(}}
    // edit@qf1 [[sc=22;ec=34]] {{)}}
    new Short((short) 0).toString(); // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf2 {{Use Short.toString(...) instead}}
    // edit@qf2 [[sc=5;ec=15]] {{Short.toString(}}
    // edit@qf2 [[sc=24;ec=36]] {{)}}
    new java.lang.Integer(0).toString(); // Noncompliant [[quickfixes=qf3]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf3 {{Use Integer.toString(...) instead}}
    // edit@qf3 [[sc=5;ec=27]] {{Integer.toString(}}
    // edit@qf3 [[sc=28;ec=40]] {{)}}
    new Integer(0).compareTo(1); // Noncompliant [[quickfixes=qf4]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf4 {{Use Integer.compare(...) instead}}
    // edit@qf4 [[sc=5;ec=17]] {{Integer.compare(}}
    // edit@qf4 [[sc=18;ec=30]] {{, }}

    Boolean.valueOf(myBoolean).compareTo(false); // Noncompliant [[quickfixes=qf5]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf5 {{Use Boolean.compare(...) instead}}
    // edit@qf5 [[sc=5;ec=21]] {{Boolean.compare(}}
    // edit@qf5 [[sc=30;ec=42]] {{, }}

    // Special cases, the creation of Integer is in fact completely useless.
    new Integer(0).toString(1); // Noncompliant [[quickfixes=qf6]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf6 {{Use Integer.toString(...) instead}}
    // edit@qf6 [[sc=5;ec=19]] {{Integer}}
    new Integer(0).toString(1, 2); // Noncompliant [[quickfixes=qf7]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf7 {{Use Integer.toString(...) instead}}
    // edit@qf7 [[sc=5;ec=19]] {{Integer}}
  }
}
