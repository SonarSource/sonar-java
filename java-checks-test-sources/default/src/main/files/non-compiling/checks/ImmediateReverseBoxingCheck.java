package checks;

abstract class ImmediateReverseBoxingCheck {
  void intBoxingAndUnboxing(int int1, Integer integer1, String string, Double double1) {
    int int2 = new Integer(1); // Noncompliant
    Integer.valueOf(double1); // Compliant
    int2 = new Unknown();
    int2 = unknownMethod(1);
  }
  void otherThanInts(byte b, double d, float f, long l, short s, char c) {
    Character c1 = new Character(); // Compliant - Assuming that there is an Character class in the same package, which is not "java.lang.Character".
  }
}
