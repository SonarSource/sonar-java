class A { // Noncompliant {{message}}

  int i; // Noncompliant {{message1}}

  void foo() { // test method
    // Noncompliant@+1 {{message2}}
    int j;
    int k;
    // Noncompliant@-1 {{message3}}
    int l; // Noncompliant {{message4}} [[sc=9;endColumn=10;secondary=3,4,5]] bla bla bla
    int m; // Noncompliant [[effortToFix=4]]
    int m; // Noncompliant [[effortToFix=4]]
    // Noncompliant@-5
    System // Noncompliant [[sc==5;el=+1;ec=11]]
      .out.println();
    // Noncompliant@+1 blabla
    int z;
  }
}
