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
