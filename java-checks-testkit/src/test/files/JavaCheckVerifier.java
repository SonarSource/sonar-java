class A { // Noncompliant {{message}}

  int i; // Noncompliant {{message1}}
  
  void foo() { // test method
    // Noncompliant@+1 {{message2}}
    int j;
    int k;
    // Noncompliant@-1 {{message3}}
    int l; // Noncompliant {{message4}}
    int m; // Noncompliant
  }
}
