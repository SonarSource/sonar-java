
class A {
  void foo(){
    ClassLoader cl = this.getClass()
      .getClassLoader(); // Noncompliant [[sc=8;ec=22]] {{Use "Thread.currentThread().getContextClassLoader()" instead.}}
  }
}
