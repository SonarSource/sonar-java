class A {
  public void f() {
    try {
    } finally {
    }

    try {
      try {             // Non-Compliant
      } finally {
      }
    } finally {
    }

    try {
    } finally {
      try {             // Compliant
      } finally {
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
      } finally {
      }
    } finally {
      try {             // Compliant
      } finally {
      }
    }

    try {
      try {             // Non-Compliant
      } finally {
      }

      try {             // Non-Compliant
      } catch (Exception e) {
        try {           // Non-Compliant

        } finally {
        }
      }
    } finally {
    }

    try (Resource r = new Resource()) {    // Compliant
      try (Resource r = new Resource()) {  // Compliant
        try {                              // Compliant
        } finally {
        }
      }
    }
  }
}
