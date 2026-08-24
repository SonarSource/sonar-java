/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks;

class LocalVariablesShouldNotSpanSwitchCaseGroupsCheckSample {

  void statementWithSeveralLaterGroups(int selector) {
    switch (selector) {
      case 0:
      case 1:
        int value = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=18;flows=statementCase2,statementDefault]]
        consume(value);
        break;
      case 2:
        value = 2; // flow@statementCase2 [[sc=9;ec=14]] {{Accessed from this later case group.}}
        value++;
        consume(value);
        break;
      default:
        value = 3; // flow@statementDefault [[sc=9;ec=14]] {{Accessed from this later case group.}}
        consume(value);
    }
  }

  int switchExpression(int selector) {
    return switch (selector) {
      case 0:
        int priority = 1; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=21;secondary=+3]]
        yield priority;
      case 1:
        priority = 2;
        yield priority;
      default:
        yield 0;
    };
  }

  void multipleDeclarations(int selector) {
    switch (selector) {
      case 0:
        int first = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=18;secondary=+5]]
        int second = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=19;secondary=+5]]
        break;
      default:
        first = 1;
        second = 2;
    }
  }

  void accessInsideNestedBlock(int selector) {
    switch (selector) {
      case 0:
        int value = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=18;secondary=+4]]
        break;
      default: {
        value = 1;
        break;
      }
    }
  }

  void nestedDeclarationsAreExcluded(int selector) {
    switch (selector) {
      case 0: {
        int nested = 0;
        consume(nested);
        break;
      }
      default:
        break;
    }
    switch (selector) {
      case 0:
        if (selector == 0) {
          int nested = 0;
          consume(nested);
        }
        break;
      default:
        break;
    }
  }

  void consecutiveLabelsAreOneGroup(int selector) {
    switch (selector) {
      case 0:
      case 1:
        int value = 0;
        consume(value);
        break;
      default:
        break;
    }
  }

  void arrowRulesAreExcluded(int selector) {
    switch (selector) {
      case 0 -> {
        int value = 0;
        consume(value);
      }
      default -> {
        int value = 1;
        consume(value);
      }
    }
  }

  void symbolIdentityIsUsed(int selector) {
    switch (selector) {
      case 0:
        int value = 0;
        consume(value);
        break;
      default:
        class Local {
          int value;

          void increment() {
            value++;
          }
        }
        new Local().increment();
    }
  }

  void nestedSwitchesAreIndependent(int selector) {
    switch (selector) {
      case 0:
        int outer = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=13;ec=18;secondary=+5]]
        break;
      default:
        switch (selector + 1) {
          case 1:
            outer = 1;
            int nested = 0; // Noncompliant {{Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.}} [[sc=17;ec=23;secondary=+3]]
            break;
          default:
            nested = 1;
        }
    }
  }

  void safeDeclarations(int selector) {
    switch (selector) {
      case 0:
        int usedOnlyHere = 0;
        consume(usedOnlyHere);
        break;
      case 1:
        int unused = 0;
        break;
      default:
        int declaredInLastGroup = 0;
        consume(declaredInLastGroup);
    }
  }

  private static void consume(int value) {
    // Nothing to do.
  }
}
