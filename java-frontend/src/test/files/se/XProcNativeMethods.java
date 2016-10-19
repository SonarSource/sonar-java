class A {
  boolean b;

  Object bar () {
    if(foo()) {
      return null;
    }
    return new Object();
  }

  boolean foo() {
    return b && nativeMethod();
  }

  private native boolean nativeMethod();
}