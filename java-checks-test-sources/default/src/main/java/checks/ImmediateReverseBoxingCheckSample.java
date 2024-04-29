package checks;

abstract class ImmediateReverseBoxingCheckSample {

  public void processInt(String s, int a) {}
  public void processInteger(String s, Integer... a) {}

  void intBoxingAndUnboxing(int int1, Integer integer1, String string, Double double1) {
    new Integer(int1).intValue(); // Noncompliant {{Remove the boxing of "int1".}}
//  ^^^^^^^^^^^^^^^^^
    new Integer(1 + 2).intValue(); // Noncompliant {{Remove the boxing to "Integer".}}
//  ^^^^^^^^^^^^^^^^^^
    Integer.valueOf(int1).intValue(); // Noncompliant
    Integer.valueOf(1 + 2).intValue(); // Noncompliant
    processInt(string, new Integer(int1)); // Noncompliant
    new Integer(int1).toString();
    integer1.intValue();
    Integer.valueOf(integer1); // Noncompliant {{Remove the boxing to "Integer"; The argument is already of the same type.}}
//  ^^^^^^^^^^^^^^^
    Double.valueOf(double1); // Noncompliant {{Remove the boxing to "Double"; The argument is already of the same type.}}
    Long.valueOf(integer1); // Compliant
    int int2 = new Integer(1); // Noncompliant {{Remove the boxing to "Integer".}}
    int2 = new Integer(1); // Noncompliant
    int2 = new Integer(string);
    Integer integer2 = new Integer(1);
    integer2 = new Integer(1);
  }

  void intUnboxingAndBoxing(int int1, Integer integer1, String string, Number number) {
    new Integer(integer1.intValue()); // Noncompliant {{Remove the unboxing of "integer1".}}
    new Integer(integer1); // Noncompliant {{Remove the boxing to "Integer"; The argument is already of the same type.}}
//      ^^^^^^^
    new Long(integer1); // Compliant
    new Integer(int1);
    new Integer(string.length());
    new Integer(createInteger().intValue()); // Noncompliant
    Integer.valueOf(integer1.intValue()); // Noncompliant
    Integer.valueOf(int1);
    Integer.valueOf(string.length());
    processInteger(string, integer1.intValue()); // Noncompliant
    processInteger(string, integer1);
    processInteger(string);
    Integer integer2 = integer1.intValue(); // Noncompliant
    Integer integer3 = int1;
    integer2 = integer1.intValue(); // Noncompliant
    integer2 = string.length();
    new Integer(number.intValue());
  }

  abstract Integer createInteger();

  void otherThanInts(byte b, double d, float f, long l, short s, char c) {
    new Byte(b).byteValue(); // Noncompliant {{Remove the boxing of "b".}}
    Byte.valueOf(b).byteValue(); // Noncompliant
    new Double(1.).doubleValue(); // Noncompliant
    Double.valueOf(1.).doubleValue(); // Noncompliant
    new Float(1.).floatValue(); // Noncompliant
    Float.valueOf(f).floatValue(); // Noncompliant
    new Long(l).longValue(); // Noncompliant
    Long.valueOf(l).longValue(); // Noncompliant
    new Short(s).shortValue(); // Noncompliant
    Short.valueOf(s).shortValue(); // Noncompliant
    new Boolean(true).booleanValue(); // Noncompliant
    int i1 = new Double(d).intValue(); // Noncompliant
    Character.valueOf(c).charValue(); // Noncompliant
    new Character(c).charValue(); // Noncompliant
  }

  void quickFixes(int int1, Integer integer1, Double double1, double doublePrimitive) {
    // Visit METHOD_INVOCATION
    // valueOf - checkForUnboxing
    Double.valueOf(double1.doubleValue()); // Noncompliant [[quickfixes=qf1]] {{Remove the unboxing of "double1".}}
//                 ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Remove the unboxing}}
    // edit@qf1 [[sc=27;ec=41]] {{}}

    // valueOf - checkForUselessUnboxing
    Double.valueOf(double1); // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^^^^^^^^^
    // fix@qf2 {{Remove the boxing}}
    // edit@qf2 [[sc=5;ec=20]] {{}}
    // edit@qf2 [[sc=27;ec=28]] {{}}

    // isUnboxingMethodInvocation - checkForBoxing
    new Integer(int1).intValue(); // Noncompliant [[quickfixes=qf3]]
//  ^^^^^^^^^^^^^^^^^
    // fix@qf3 {{Remove the boxing}}
    // edit@qf3 [[sc=5;ec=17]] {{}}
    // edit@qf3 [[sc=21;ec=33]] {{}}
    Integer.valueOf(int1).intValue(); // Noncompliant [[quickfixes=qf4]]
//  ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf4 {{Remove the boxing}}
    // edit@qf4 [[sc=5;ec=21]] {{}}
    // edit@qf4 [[sc=25;ec=37]] {{}}

    // else, checkMethodInvocationArguments
    examineInt(Integer.valueOf(int1)); // Noncompliant [[quickfixes=qf5]]
//             ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf5 {{Remove the boxing}}
    // edit@qf5 [[sc=16;ec=32]] {{}}
    // edit@qf5 [[sc=36;ec=37]] {{}}
    examineInteger(integer1.intValue()); // Noncompliant [[quickfixes=qf6]]
//                 ^^^^^^^^^^^^^^^^^^^
    // fix@qf6 {{Remove the unboxing}}
    // edit@qf6 [[sc=28;ec=39]] {{}}

    // Visit VARIABLE
    double d1 = Double.valueOf(doublePrimitive); // Noncompliant [[quickfixes=qf7]]
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf7 {{Remove the boxing}}
    // edit@qf7 [[sc=17;ec=32]] {{}}
    // edit@qf7 [[sc=47;ec=48]] {{}}
    Double d2 = double1.doubleValue(); // Noncompliant [[quickfixes=qf8]]
//              ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf8 {{Remove the unboxing}}
    // edit@qf8 [[sc=24;ec=38]] {{}}

    // Visit ASSIGNMENT
    d1 = Double.valueOf(doublePrimitive); // Noncompliant [[quickfixes=qf9]]
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf9 {{Remove the boxing}}
    // edit@qf9 [[sc=10;ec=25]] {{}}
    // edit@qf9 [[sc=40;ec=41]] {{}}
    d2 = double1.doubleValue(); // Noncompliant [[quickfixes=qf10]]
//       ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf10 {{Remove the unboxing}}
    // edit@qf10 [[sc=17;ec=31]] {{}}

    // Visit NEW_CLASS
    new Integer(integer1.intValue()); // Noncompliant [[quickfixes=qf11]]
//              ^^^^^^^^^^^^^^^^^^^
    // fix@qf11 {{Remove the unboxing}}
    // edit@qf11 [[sc=25;ec=36]] {{}}
    new Integer(integer1); // Noncompliant [[quickfixes=qf12]]
//      ^^^^^^^
    // fix@qf12 {{Remove the boxing}}
    // edit@qf12 [[sc=5;ec=17]] {{}}
    // edit@qf12 [[sc=25;ec=26]] {{}}
  }

  public void examineInt(int a) {
    //...
  }

  public void examineInteger(Integer a) {
    // ...
  }

}
