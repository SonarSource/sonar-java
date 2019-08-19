interface I {
  void foo();
}
class A implements I {
  void foo(){} // Compliant - the annotation does not exists in java 4
}
