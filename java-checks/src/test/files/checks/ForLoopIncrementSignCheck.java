class A {
  void myMethod(int x, int y, int z) {
    int j = 0, k = 0;
    for (int i = x; i < y; i++) {}
    for (int i = x; i > y; i++) {} // Noncompliant [[sc=21;ec=26]] {{"i" is incremented and will never reach "stop condition".}}
    for (int i = x; i >=y; i++) {} // Noncompliant
    for (int i = x; i > y; i--) {}
    for (int z = x; z < y; z--) {} // Noncompliant [[sc=21;ec=26]] {{"z" is decremented and will never reach "stop condition".}}
    for (int i = x; i <=y; i--) {} // Noncompliant
    for (int i = x; y > i; i++) {}
    for (int i = x; y < i; i++) {} // Noncompliant
    for (int i = x; y <=i; i++) {} // Noncompliant
    for (int i = x; y < i; i--) {}
    for (int i = x; y > i; i--) {} // Noncompliant
    for (int i = x; y >=i; i--) {} // Noncompliant
    for (int i = x; x < y; i--) {}
    for (int i = x; x > y; i--) {}
    for (int i = x; i > y; i-=1 ) {}
    for (int i = x; i > y; i+=1 ) {} // Noncompliant
    for (int i = x; i > y; i-=+1) {}
    for (int i = x; i > y; i+=-x) {}
    for (int i = x; i > y; i+=z ) {}
    for (int i = x; i > y; i=i-1) {}
    for (int i = x; i > y; i=i+1) {} // Noncompliant
    for (int i = x; i > y; i=i+z) {}
    for (int i = x; i > y; i=z+1) {}
    for (int i = x; i > y; i=i*2) {}
    for (int i = x; i > y; i=i-z) {}
    for (int i = x; i > y; object.x = i + 1) {}
    for (int i = x; i+1 < y; i++) {}
    for (int i = x; i < y; ) {}
    for (int i = x; i > y; update()) {}
    for (int i = x; condition(); i++) {}
    for (int i = x; ; i++) {}
  }
}
