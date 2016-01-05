class A {
  void foo() {
    try {
    } catch (IOException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant [[sc=12;ec=26;secondary=4]] {{Combine this catch with the one at line 4, which has the same body.}}
      doCleanup();
      logger.log(e);
    }
    catch (TimeoutException e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
   catch (IllegalArgumentException e) {  // Noncompliant [[sc=11;ec=37;secondary=4]] {{Combine this catch with the one at line 4, which has the same body.}}
    doCleanup();
    logger.log(e);
  }

    try {
    } catch (IOException | java.io.FileNotFoundException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant [[sc=12;ec=26;secondary=22]] {{Combine this catch with the one at line 22, which has the same body.}}
      doCleanup();
      logger.log(e);
    }
    catch (TimeoutException e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
  }
}
