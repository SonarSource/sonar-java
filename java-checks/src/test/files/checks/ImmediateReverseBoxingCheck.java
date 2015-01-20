abstract class A {
  
  public void processInt(String s, int a) {}
  public void processInteger(String s, Integer... a) {}
  
  void intBoxingAndUnboxing(int int1, Integer integer1, String string) {
    new Integer(int1).intValue(); // Noncompliant
    new Integer(1 + 2).intValue(); // Noncompliant
    Integer.valueOf(int1).intValue(); // Noncompliant
    Integer.valueOf(1 + 2).intValue(); // Noncompliant
    processInt(string, new Integer(int1)); // Noncompliant
    new Integer(int1).toString();
    integer1.intValue();
    int int2 = new Integer(1); // Noncompliant
    int2 = new Integer(1); // Noncompliant
    int2 = new Integer(string);
    Integer integer2 = new Integer(1);
    integer2 = new Integer(1);
    int2 = new Unknown();
    int2 = unknownMethod(1);
  }
  
  void intUnboxingAndBoxing(int int1, Integer integer1, String string, Number number) {
    new Integer(integer1.intValue()); // Noncompliant
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
  
  void otherThanInts(byte b, double d, float f, long l, short s) {
    new Byte(b).byteValue(); // Noncompliant
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
  }
  
}