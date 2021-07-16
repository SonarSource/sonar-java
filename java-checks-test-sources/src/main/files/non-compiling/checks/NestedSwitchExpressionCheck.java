package checks;

class NestedSwitchExpressionCheck {
  void foo() {
    int myVar = 0;
    int i = switch (myVar) {  // Compliant
      case 0:
        int iNested = switch (0) {  // Noncompliant
          case 0:
          case 1:
            System.out.println();
            switch (1){ // Noncompliant [[sc=13;ec=19;el=+0]] {{Refactor the code to eliminate this nested "switch".}}
              case 0:
              case 1:
                yield 2;
            }
          case 2:
            yield 2;
          default:
            yield 3;
        };
      case 1:
        int iNested2 = switch (2) { // Noncompliant
          case 0:
          case 1:
            int nestedAgain = switch (3) { // Noncompliant {{Refactor the code to eliminate this nested "switch".}}
              case 0:
                System.out.println();
              case 1:
                yield 2;
              default:
                yield 3;
            };
            yield 2;
          case 2:
            yield 3;
          default:
            yield 4;
        };
        yield 1;
      case 2:
        yield 2;
      default:{
        yield switch(4) {  // Noncompliant
          case 0:
            switch(5) { // Noncompliant
              default:
                break;
            }
            yield 2;
          case 1: {
            yield switch (6) {  // Noncompliant
              case 0:
                yield 4;
              default:
                yield 5;
            };
          }
          default:
            yield 10;
        };
      }
    };

    int j = switch (i) {
      case 1 -> {
        yield switch (4) { // Noncompliant
          case 1 -> 1;
          default -> 2;
        };
      }
      case 2 -> switch (4) { // Noncompliant
        case 1 -> 1;
        default -> 2;
      };
      default -> 4;
    };
  }
}
