class B {
  private void f() {
    try {
      return; // Compliant
    } catch (Exception e) {
      return; // Compliant
    } finally {
      return; // Noncompliant {{Remove this return statement from this finally block.}}
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
    }finally {
      try {
      }catch (Exception e){
        return; // Noncompliant {{Remove this return statement from this finally block.}}
      }
    }
  }
}

enum A {
  A;

  {
    return;
  }

}
