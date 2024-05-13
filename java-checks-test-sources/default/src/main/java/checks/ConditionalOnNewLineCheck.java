package checks;

class ConditionalOnNewLineCheck {
  void foo(boolean condition1, boolean condition2) {
    if (condition1) {
      // ...
    } if (condition2) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
      //...
    }
    if (condition1) {
      // ...
    } else {

    } if (condition2) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
        //...
      }

    if (condition1) {
      // ...
    } else if(condition2) {

    } if (condition2) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
      //...
    }
    if (condition1) {
      // ...
    }
    if (condition2) {  // compliant, if on next line
      //...
    }

    if (condition1) {
      // ...
    } else if (condition2) {  // compliant, else statement
      //...
    }
    if (condition1) { if(condition2) {} } // compliant

    if (condition1) {

    } if (condition1) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
      // ...
    } else {
      // ...
    }

    if (condition1) {
      // ...
    } else {
      // ...
    } if (condition1) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
        // ...
      } else {
        // ...
      }

    if (condition1) {
      // ...
    } else if(condition2) {
      // ...
    } if (condition1) { // Noncompliant {{Move this "if" to a new line or add the missing "else".}}
//    ^^
//  ^@-1<
      // ...
    } else {
      // ...
    }

    if (condition1) {
      // ...
    } else if(condition2) {
      // ...
    }
    if (condition1) { // Compliant
      // ...
    } else {
      // ...
    }
  }
}
