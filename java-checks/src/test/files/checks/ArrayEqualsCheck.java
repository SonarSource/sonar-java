class A {
  int[] a, b;
  String c, d;
  void fun() {
    if(a.equals(b)){ } // Noncompliant {{Use the '==' operator instead of calling the equals() method to prevent any misunderstandings}}
    else if (a == b) {}
    else if (c.equals(d)) {}
    else if (c.equals(d)) {}
    else if (method().equals(b)) {} // Noncompliant {{Use the '==' operator instead of calling the equals() method to prevent any misunderstandings}}
    else if ((matrix()[0].c).equals(d)) {}
  }

  int[] method(){
    return a;
  }
   A[] matrix(){
   return new A[10];
  }

}