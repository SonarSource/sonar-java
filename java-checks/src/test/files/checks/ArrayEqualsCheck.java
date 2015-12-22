class A {
  int[] a, b;
  String c, d;
  void fun() {
    if(a.equals(b)){ } // Noncompliant {{Use the '==' operator instead of calling the equals() method to prevent any misunderstandings}} [[sc=10;ec=16]]
    else if (a == b) {}
    else if (c.equals(d)) {}
    else if (c.equals(d)) {}
    else if (method().equals(b)) {} // Noncompliant {{Use the '==' operator instead of calling the equals() method to prevent any misunderstandings}} [[sc=23;ec=29]]
    else if ((matrix()[0].c).equals(d)) {}
  }

  int[] method(){
    return a;
  }
   A[] matrix(){
   return new A[10];
  }

}