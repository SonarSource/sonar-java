void main() {
  int a = 5;
  int x = 10; // Noncompliant {{Rename "x" which hides the field declared at line 11.}}
//    ^
  float b = 3.14f;
  System.out.println(a);
  System.out.println(x);
  System.out.println(b);
}

int x = 20;
