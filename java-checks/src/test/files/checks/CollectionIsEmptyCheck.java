class A {
  private void foo() {
    myCollection.size() == 0; // Non-Compliant
    myCollection.size() != 0; // Non-Compliant
    myCollection.size() > 0; // Non-Compliant
    myCollection.size() >= 1; // Non-Compliant
    myCollection.size() < 1; // Non-Compliant
    myCollection.size() <= 0; // Non-Compliant

    0 == myCollection.size(); // Non-Compliant
    0 != myCollection.size(); // Non-Compliant
    0 < myCollection.size(); // Non-Compliant
    1 <= myCollection.size(); // Non-Compliant
    1 > myCollection.size(); // Non-Compliant
    0 >= myCollection.size(); // Non-Compliant

    myCollection.size() == +0; // Compliant - corner case should be covered by another rule

    myCollection.size(0) == 0; // Compliant

    myCollection.size() == myCollection2.size(); // Compliant

    foo instanceof Object; // Compliant

    myCollection.size() == 3; // Compliant
    myCollection.size() < 3; // Compliant
    myCollection.size() > 3; // Compliant

    0 < 3; // Compliant
    1 + 1 < 3; // Compliant

    myCollection.size < 0; // Compliant

    myCollection.isEmpty();
    !myCollection.isEmpty();
    myCollection.size() == 1;

    1 + 1 == 0; // Compliant
    foo.size[0] == 0; // Compliant

    size() == 0; // Compliant
    foo.size() && 0; // Compliant
    foo.bar() == 0; // Compliant

    foo.bar().baz().size() == 0; // Noncompliant
  }
}
