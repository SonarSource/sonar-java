class A {

  int f;
  static int field;

  void compoundAssignment(int a) {
    a += 1;
    this.f += 1;
    A.field += 1;
    a -= 1;
    this.f -= 1;
    A.field -= 1;
    a *= 1;
    this.f *= 1;
    A.field *= 1;
    a /= 1;
    this.f /= 1;
    A.field /= 1;
    a ^= 1;
//    this.f ^= 1; logical compound assignment with this is not supported see SONARJAVA-2242
    a &= 1;
//    this.f &= 1; logical compound assignment with this is not supported
    a |= 1;
//    this.f |= 1; logical compound assignment with this is not supported
    a %= 1;
    this.f %= 1;
    A.field %= 1;
    a <<= 1;
    this.f <<= 1;
    A.field <<= 1;
    a >>= 1;
    this.f >>= 1;
    A.field >>= 1;
    a >>>= 1;
    this.f >>>= 1;
    A.field >>>= 1;
  }
}
