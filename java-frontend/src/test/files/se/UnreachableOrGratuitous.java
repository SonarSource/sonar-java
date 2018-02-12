package javax.annotation;


public class Class {

  void f(boolean a) {
    if (!true) { // Noncompliant

    }
    if (false && a) { // Noncompliant

    }
    if (a && false) { // Compliant - gratuitous expr

    }
    if (true) { // Compliant - gratuitous expr

    }
    if (!false) { // Noncompliant

    } else {

    }
    if (true || a) { // Noncompliant

    } else {

    }
    if (a || true) { // Compliant - gratuitous expr

    } else {

    }
  }

  void paren(boolean a) {
    if ((((false)))) { // Noncompliant

    }
    if (((((false))) && ((a)))) { // Noncompliant

    }
  }

  void loops() {
    boolean f = false; // using vars, because literals are ignored in while loop
    boolean t = true;
    while (f) {} // Noncompliant
    while (t) {}

    // do-while body will always execute at least once
    do { } while (t);
    do { } while (f);

    // for loop condition is not checked in ExplodedGraphWalker
    for (int i = 0; f; i++) { }
    for (int i = 0; t; i++) { }
  }

}
