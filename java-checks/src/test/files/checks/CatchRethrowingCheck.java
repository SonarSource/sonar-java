class A {
  void foo() {
    try {
    } catch (IOException ioe) {
      throw ioe; // Noncompliant {{Add logic to this catch clause or eliminate it and rethrow the exception automatically.}}
//    ^^^^^^^^^^
    }

    try {
    } catch (IOException ioe) {
      logger.log("huho", ioe);
      throw ioe; // compliant
    }
    try {
    } catch (IOException ioe) {
      throw ioe; // compliant
    } catch (Exception e) {
      return;
    }


  }
}
