class A { // Noncompliant {{message}}

  int i; // Noncompliant {{message1}}
  
  void foo() { // test method
    // Noncompliant@+1 {{message2}}
    int j;
    int k;
    // Noncompliant@-1 2 {{message3}}
    int l; // Noncompliant 2 {{message4}}
    int m; // Noncompliant
  }
}
