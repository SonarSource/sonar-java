
void main() { // just to make the file is a compact source file
}

class A { // Compliant, FP raised here
}

class C {
  class D { // Noncompliant
  }
}
