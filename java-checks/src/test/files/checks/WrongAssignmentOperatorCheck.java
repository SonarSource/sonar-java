class A {
  int target = -5;
  boolean a, b, c;
  int num = 3;

  void fun() {
    target =-num; // Noncompliant {{Was "-=" meant instead?}} [[sc=12;ec=14]]
    target =
            -num;
    target = -num; // Compliant intent to assign inverse value of num is clear
    target =--num;

    target += num;
    target =+ num; // Noncompliant {{Was "+=" meant instead?}} [[sc=12;ec=14]]
    target =
            + num;
    target =
            +num;
    target = +num;
    target =++num;
    target=+num; // Compliant - no spaces between variable, operator and expression

    a = b != c;
    a = b =! c; // Noncompliant {{Was "!=" meant instead?}} [[sc=11;ec=13]]
    a = b =!! c; // Noncompliant
    a = b = !c;
    a =! c; // Noncompliant {{Add a space between "=" and "!" to avoid confusion.}}
    a = ! c;
    a = !c;
    a =
       !c;
  }
}
