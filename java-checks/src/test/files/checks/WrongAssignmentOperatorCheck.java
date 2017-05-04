class A {
  int target = -5;
  boolean a, b, c;
  int num = 3;

  void fun() {
    target =-num; // Noncompliant {{Was "-=" meant instead?}} [[sc=12;ec=14]]
    target = -num; // Compliant intent to assign inverse value of num is clear
    target =--num;

    target += num;
    target =+ num; // Noncompliant {{Was "+=" meant instead?}} [[sc=12;ec=14]]
    target = +num;
    target =++num;

    a = b != c;
    a = b =! c; // Noncompliant {{Was "!=" meant instead?}} [[sc=11;ec=13]]
    a = b =!! c; // Noncompliant
    a = b = !c;
  }
}
