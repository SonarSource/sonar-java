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
        System.out.println(i); //fall through
      case 8:
        System.out.println("foo");
        break;
    }
  }
}

class NestedSwitches {
  public enum E {
    A, B
  }

  public int test1(String s, E e, int i) {
    switch (s) {
      case "FOO":
        return 0;
      case "BAR": // Compliant - all cases in the following switch have unconditional termination statements
        switch (e) {
          case A:
            switch (i) {
              case A:
              case B:
                return 1;
              default:
                return 2;
            }
          case B:
            return 2;
          default:
            throw new IllegalArgumentException();
        }
      default:
        throw new IllegalArgumentException();
    }
  }

  public int test2(String s, E e) {
    switch (s) {
      case "FOO":
        return 0;
      case "BAR": // Noncompliant
        switch (e) {
          case A:
            return 1;
          case B:
            return 2;
        }
      default:
        throw new IllegalArgumentException();
    }
    return 0;
  }

  public int test3(String s, E e) {
    int result = 0;
    switch (s) {
      case "FOO":
        return 0;
      case "BAR": // Noncompliant
        switch (e) {
          case A: // Noncompliant
            result = 0;
          case B:
            return 2;
        }
      default:
        throw new IllegalArgumentException();
    }
    return result;
  }
}

class SwitchesAndLoops {
  public enum E {
    A, B
  }

  private void nestedSwitchesAndLoops(String s, E e) {
    int result = 0;

    for (int i = 0; i < 1; i++) {
      switch (myVariable) {
        case 0:
          continue label1;
        case 1:
          switch (s) {
            case "FOO":
              for (int j = 0; j < 1; j++) {
                switch (j) {
                  case 0:
                  case 1:
                    throw new IllegalArgumentException();
                  case 2:
                    continue;
                  case 3: // Noncompliant
                    label1:
                    result = 0;
                  case 4:
                    result = 1;
                    // fallthrough
                  case 5: // Noncompliant
                    result = 1;
                  case 6:
                  case 7:
                  case 8:
                    result = 6;
                }
                continue;
              }
              continue;
              return 0;
            case "BAR":
              switch (e) {
                case A: // Noncompliant
                  result = 0;
                case B:
                  return 2;
              }
          }
          break;
        case 2:
          result = 2;
          continue;
        default:
          result = 0;
      }
    }
  }

  public void infiniteLoopCheck() {
    int value = 0;
    switch (value) {
      case 0: // Noncompliant
        for (int i = 0; i < 1; i++) { }
      case 1:
    }
  }
}
