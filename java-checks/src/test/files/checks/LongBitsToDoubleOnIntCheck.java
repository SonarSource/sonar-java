class A {
  void foo() {
    byte b = 1;
    short s = 1;
    Double.longBitsToDouble('c');
    Double.longBitsToDouble(s);
    Double.longBitsToDouble(b);
    Double.longBitsToDouble(1);
    Double.longBitsToDouble(1L);
    Double.longBitsToDouble(Long.valueOf(1l));
  }
}