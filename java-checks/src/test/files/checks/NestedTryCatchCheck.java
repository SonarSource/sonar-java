class A {
  public void f() {
    try {
    } finally {
    }

    try {
      try {             // Compliant
      } finally {
      }
    } catch (Exception e) {
    }

    try {
    } catch (Exception e) {
      try {             // Compliant
      } catch (Exception e) {
      }
    }

    try {
    } catch (Exception e) {
      try {             // Compliant
      } finally {
      }
    }

    try {
    } catch (Exception e) {
      try {             // Compliant
      } catch (Exception e) {
      }
    } finally {
      try {             // Compliant
      } catch (Exception e) {
      }
    }

    try {
      try {             // Non-Compliant
      } catch (Exception e) {
      }

      try {             // Non-Compliant
      } catch (Exception e) {
        try {           // Non-Compliant

        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
    }

    try (Resource r = new Resource()) {    // Compliant
      try (Resource r = new Resource()) {  // Compliant
        try {                              // Compliant
        } finally {
        }
      }
    }

    try {
      try {                                // Compliant
      } catch (Exception e) {
      }
    } finally {
    }

    try {
      try{
        try (Resource r = new Resource()){
        }
      }catch (Exception e){}
    } catch (Exception e) {}
  }
}
