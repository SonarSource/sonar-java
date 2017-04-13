class A {

  int f;

  void compoundAssignment(int a) {
    a += 1;
    this.f += 1;
    a -= 1;
    this.f -= 1;
    a *= 1;
    this.f *= 1;
    a /= 1;
    this.f /= 1;
    a ^= 1;
//    this.f ^= 1; logical compound assignment with this is not supported
    a &= 1;
//    this.f &= 1; logical compound assignment with this is not supported
    a %= 1;
//    this.f |= 1; logical compound assignment with this is not supported
    a |= 1;
    this.f %= 1;
    a <<= 1;
    this.f <<= 1;
    a >>= 1;
    this.f >>= 1;
    a >>>= 1;
    this.f >>>= 1;
  }
}
