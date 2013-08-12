import com.google.common.base.Throwables;

class A {

  private void f() {
    try {
    } catch (Exception e) { // Non-Compliant
      throw e;
    }

    Exception h;

    try {
    } catch (Exception e) { // Compliant
      throw h;
    }

    try {
    } catch (Exception e) { // Compliant
      Throwables.propagate(e);
    }

    try {
    } catch (Exception e) { // Compliant
      throw new RuntimeException(e);
    }

    try {
    } catch (RuntimeException e) { // Compliant
      System.err.println("ERR!");
      throw e;
    }

    try {
    } catch (Throwable e) { // Compliant
      int a;
    }

    try {
    } catch (RuntimeException e) { // Compliant - is propagation
      throw e;
    } catch (Exception e) { // Compliant
    } catch (Throwable e) { // Non-Compliant
      throw e;
    }

    try {
    } catch (Exception e) { // Non-Compliant
      throw e;
    } finally {
    }

  }

}
