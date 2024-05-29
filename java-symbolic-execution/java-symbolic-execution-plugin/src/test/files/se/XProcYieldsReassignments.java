class A {
  static int foo(String s) {
    s = s.toLowerCase();
    if (s.indexOf("hello") != -1) {
      return 1;
    }

    return 0;
  }
}
