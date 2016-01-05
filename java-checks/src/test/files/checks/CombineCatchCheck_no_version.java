class A {
  void foo() {
    try {
    } catch (IOException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 4, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (TimeoutException e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }

    try {
    } catch (IOException | java.io.FileNotFoundException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 18, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (TimeoutException e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
  }
}
