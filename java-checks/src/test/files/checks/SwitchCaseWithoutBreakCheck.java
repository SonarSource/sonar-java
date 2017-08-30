class A {

  private A(boolean test) {
    if (test)
      throw new Exception();
  }

  private void f(byte b, char c) {
    int myVariable = 0;
    switch (myVariable) {
      case 0:
      case 1: // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=14]]
        System.out.println("Test");
      case 2: // Compliant
        break;
      case (8 | 2): // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=20]]
        System.out.println("Test");
      case 3: // Compliant
        return;
      case 4: // Compliant
        throw new IllegalStateException();
      case 5: // Noncompliant
        System.out.println();
      default: // Noncompliant
        System.out.println();
      case 6: // Noncompliant
        int a = 0;
      case 8: { // Compliant
        if (false) {
          break;
        } else {
          break;
        }
      }
      case 12: // Compliant
        try {
          return new A(true);
        } catch (Exception e) {
          throw new RuntimeException("Wrapping", e);
        }
      case 13: // Noncompliant
        try {
          return new A(true);
        } catch (Exception e) {
          System.out.println("Error");
        }
      case 14: // Noncompliant
        try {
          int i = i / b;
        } catch (Exception e) {
          throw new RuntimeException("Wrapping", e);
        }
      case 9: // Compliant
    }

    for (int i = 0; i < 1; i++) {
      switch (myVariable) {
        case 0: // Compliant
          continue; // belongs to for loop
        case 1:
          break;
      }

    }

    switch (myVariable) {
      case 0: // Noncompliant
        switch (myVariable) {
          case 0:
          case 1: // Noncompliant
            System.out.println();
            switch (myVariable){
              case 0:
              case 1:
                break;
            }
          case 2: // Compliant
            break;
        }
        System.out.println();
      case 1: // Compliant
        switch (myVariable) {
          case 0:
          case 1: // Compliant
            System.out.println();
            switch (myVariable){
              case 0: // Noncompliant
                System.out.println();
              case 1:
                break;
            }
            break;
          case 2: // Compliant
            break;
        }
        break;
      case 2: // Compliant
    }

    switch(myVariable) {

    }

    switch (b) {
      case (byte) 0: // Compliant
        break;
      case (byte) 1: // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=21]]
        System.out.println("Test");
      case 2: // Compliant
        break;
      case 3: // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=14]]
        System.out.println("Test 2");
      case 4:
        break;
    }

    switch (c) {
      case 'c': // Compliant
        break;
      case 'a': // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=16]]
        System.out.println("Test");
      case 'd': // Compliant
        break;
      case 'e': // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}} [[sc=7;ec=16]]
        System.out.println("Test 2");
      case 'x':
        break;
    }
  }
}

class S128 {

  void compliantFlows() {
    int i = 0;
    switch (i) {
      case 0:
        System.out.println("OK");
        break;
      case 1: // Noncompliant
        System.out.println("No floor ):");
      default:
        System.out.println("ERROR");
        break;
      case 2:
        System.out.println("WARN");
        break;
    }
  }

}

class Conditions {
  public void test(int j) {
    int i = 0;
    switch (i) {
      case 0: // Compliant
        if (j == 0) {
          break;
        } else {
          break;
        }
      case 1: { // Compliant
        if (j != 0) {
          break;
        } else {
          break;
        }
      }
      case 2: // Compliant
        if (j == 0)
          break;
        else
          break;
      case 3: { // Compliant
        if (j != 0)
          break;
        else
          break;
      }
      case 4: // Noncompliant
        if (j == 0)
          break;
      case 5: // Compliant
        break;
      case 6: // Noncompliant
        if (j == 0) {
          System.out.println(j);
        } else {
          break;
        }
      case 8:
        break;
      default: // Noncompliant
        if (j == 1)
          break;
      case 9:
        break;
      case 7: // Noncompliant
        if (j == 0)
          System.out.println(j);
        else
          break;
      case 12: // Compliant, last case in statement
        System.out.println(j);
    }
  }


  private fallThroughComments(int i) {
    switch (i) {
      case 1:
        System.out.println(i);
        // fall-through
      case 2:
        // FALL-THROUGH
        System.out.println(i);
      case 3:
        // fallthrough
        System.out.println(i);
      case 4:
        //$FALL-THROUGH$
        System.out.println(i);
      case 5:
        // fallthru
        System.out.println(i);
      case 6:
        //falls-through
        System.out.println(i);
      case 7:
        System.out.println("foo");
        break;
    }
  }
}
