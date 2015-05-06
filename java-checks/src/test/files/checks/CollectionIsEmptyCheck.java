class A {
  private void foo() {
    myCollection.size() == 0; // Noncompliant {{Use isEmpty() to check whether the collection is empty or not.}}
    myCollection.size() != 0; // Noncompliant
    myCollection.size() > 0; // Noncompliant
    myCollection.size() >= 1; // Noncompliant
    myCollection.size() < 1; // Noncompliant
    myCollection.size() <= 0; // Noncompliant

    0 == myCollection.size(); // Noncompliant
    0 != myCollection.size(); // Noncompliant
    0 < myCollection.size(); // Noncompliant
    1 <= myCollection.size(); // Noncompliant
    1 > myCollection.size(); // Noncompliant
    0 >= myCollection.size(); // Noncompliant

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
