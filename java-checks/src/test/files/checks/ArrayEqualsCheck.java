class A {
  int[] a, b;
  String c, d;
  void fun() {
    if(a.equals(b)){ }
    else if (a == b) {}
    else if (c.equals(d)) {}
    else if (c.equals(d)) {}
    else if (method().equals(b)) {} //false negative requiring method type resolution
    else if ((matrix()[0].c).equals(d)) {}
  }

  int[] method(){
    return a;
  }
   A[] matrix(){
   return new A[10];
  }

}