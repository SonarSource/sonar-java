package checks;

class GetClassLoaderCheckSample {
  void foo(){
    ClassLoader cl = this.getClass()
      .getClassLoader(); // Noncompliant {{Use "Thread.currentThread().getContextClassLoader()" instead.}}
//     ^^^^^^^^^^^^^^
  }
}
