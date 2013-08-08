class A {
  private void f() {
    try {
    } catch (Exception e) { // Non-Compliant
    } catch (Exception e) { // Non-Compliant
      System.out.println(e);
    } catch (Exception e) { // Non-Compliant
      System.out.println("foo", e.getMessage());
    } catch (Exception e) { // Compliant
      System.out.println("", e);
    } catch (Exception f) { // Non-Compliant
      System.out.println("", e);
    } catch (Exception f) { // Compliant
      System.out.println("", f);
    } catch (Exception e) { // Compliant
      System.out.println("", e);
      try {
      } catch (Exception f) { // Non-Compliant
      }
    } catch (Exception e) { // Non-Compliant
      try {
      } catch (Exception f) { // Non-Compliant
        System.out.println("", e);
      }
    } catch (RuntimeException e) {
      try {
      } catch (Exception f) { // Compliant
        System.out.println("", f);
      }
      System.out.println("", e);
    }
  }

  private void g() {
    System.out.println();
  }
}
