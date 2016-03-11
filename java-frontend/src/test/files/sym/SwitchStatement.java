class A {

  void foo() { // This construction should not be accepted.
    int b = 2;
    switch (12) {
      case 1:
        int b = 2; // this declaration hides the local var a and as such does not compile cf SONARJAVA-218
      default:
        System.out.println(b);
    }
  }

  void bar() {
    switch (12) {
      case 1:
        int a = 2;
      default:
        System.out.println(a);
    }
    int a = 2;
    System.out.println(a);
  }
}