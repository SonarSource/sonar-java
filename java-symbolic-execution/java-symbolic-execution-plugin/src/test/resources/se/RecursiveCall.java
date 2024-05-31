class RecursiveCall {

  static int foo(int a) {
    if(a == 0) {
      return a;
    }
    return foo(a - 1);
  }


}