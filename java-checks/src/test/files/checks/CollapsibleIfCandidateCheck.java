class A {
  void f() {
    if (false) { // Compliant 1
    }

    if (false) { // Compliant  2
    } else {
    }

    if (false) { // Compliant 3
      if (false) { // Noncompliant {{Merge this if statement with the enclosing one.}} [[sc=7;ec=9;secondary=10]]
      }
    }

    if (false) { // Compliant 5
      if (false) { // Compliant 6
      }
      System.out.println();
    }

    if (false) { // Compliant 7
      int a;
      if (a) { // Compliant 8
      }
    }

    if (false) { // Compliant 9
      if (false) { // Compliant 10
      }
    } else {
    }

    if (false) { // Compliant 11
      if (false) { // Compliant 12
      } else {
      }
    }

    if (false) { // Compliant 13
    } else if (false) { // Compliant  14
      if (false) { // Noncompliant {{Merge this if statement with the enclosing one.}} [[sc=7;ec=9;secondary=40]]
      }
    }

    if (false) // Compliant 16
      if (true) { // Noncompliant {{Merge this if statement with the enclosing one.}}
    }

    if (false) { // Compliant 18
      while (true) {
        if (true) { // Compliant  19
        }
      }

      while (true)
        if(true) { // Compliant 20
      }
    }
  }

  {
    if (false) { // Compliant 21
    }
      if (false) {  // 22
        switch ("SELECT") {
          case "SELECT":
            if ("SELECT".equals(token.getValue())) { // Compliant 23
            }
            break;
          }
       }

    if (true) {  // Compliant 24
      if (true) { // Noncompliant {{Merge this if statement with the enclosing one.}}
        int a;
        if (true) {   // Compliant 26
          int b;
        }
      }
    }

    if (false) { // Compliant 27
      while (true) {
        if (true) { // Compliant  28
        }
      }
    }

    if (true) // Compliant 29
      if (false)  // Noncompliant {{Merge this if statement with the enclosing one.}}
        if (true)  // Noncompliant {{Merge this if statement with the enclosing one.}}
          a = 0;
  }

  void testMyFile(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[sc=7;ec=9;quickfixes=qf1]]
        /* ... */
      }
      // fix@qf1 {{Merge this if statement with the (enclosing|nested) one}}
      // edit@qf1 [[sl=+2;el=+2;sc=7;ec=8]] {{}}
    }
  }
}
