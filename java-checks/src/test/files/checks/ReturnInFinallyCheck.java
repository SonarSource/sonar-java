class B {
  private void f() {
    try {
      return; // Compliant
    } catch (Exception e) {
      return; // Compliant
    } finally {
      return; // Noncompliant [[sc=7;ec=13]] {{Remove this return statement from this finally block.}}
    }

    try {
    } finally {
      new Foo() {
        public void foo() {
          return; // Compliant
        }
      };
    }

    try {
      return; // Compliant
    } finally {
    }
    try {
    } finally {
      try {
      } catch (Exception e) {
        return; // Noncompliant {{Remove this return statement from this finally block.}}
      }
    }


    for (int i = 0; i <1; i++) {
      try {
        return; // Compliant
      } catch (Exception e) {
        return; // Compliant
      } finally {
        continue; // Noncompliant [[sc=9;ec=17]] {{Remove this continue statement from this finally block.}}
      }
      try {
        return; // Compliant
      } catch (Exception e) {
        return; // Compliant
      } finally {
        break; // Noncompliant [[sc=9;ec=14]] {{Remove this break statement from this finally block.}}
      }
    }

    try {
      return; // Compliant
    } catch (Exception e) {
      return; // Compliant
    } finally {
      throw new Exception(); // Noncompliant [[sc=7;ec=12]] {{Remove this throw statement from this finally block.}}
    }
  }
}

enum A {
  A;

  {
    return;
  }

}

class FPs {

  int i;

  void f() {
    try {

    } finally {
      switch (i) {
        case 1:
          break; // Compliant
        case 2:
          return; // Noncompliant
        case 3:
          throw new RuntimeException(); // Noncompliant
        default:
          break; // Compliant
      }
      for (;;) {
        if (i > 0) continue; // Compliant
        break; // Compliant
        return; // Noncompliant
        throw new RuntimeException(); // Noncompliant
      }
      while (true) {
        if (i > 0) continue; // Compliant
        break; // Compliant
        return; // Noncompliant
        throw new RuntimeException(); // Noncompliant
      }
      do {
        if (i > 0) continue; // Compliant
        break; // Compliant
        return; // Noncompliant
        throw new RuntimeException(); // Noncompliant
      } while (true);
    }
  }

  void g() {
    for (;;) {
      try {
        throw new IllegalStateException();
      } finally {
        continue; // Noncompliant
        break; // Noncompliant
      }
    }
  }

  void fp() {
    outer:
    for (;;) {
      try {
        throw new IllegalStateException();
      } finally {
        while (true) {
          continue outer; // FN - requires CFG to detect this, but let's not overcomplicate this rule
          break; // Compliant
        }
      }
    }
  }
}
