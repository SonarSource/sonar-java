package checks;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.sql.SQLException;

class CombineCatchCheck {
  void foo() {
    CombineCatchCheckLogger logger = new CombineCatchCheckLogger();

    try {
      canThrow();
    } catch (IOException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 12, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (IllegalArgumentException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }

    try {
      canThrow();
    } catch (IOException | IllegalArgumentException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 27, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (ArrayStoreException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }

    try {
      canThrow();
    } catch (IOException e) {
      handleException(e);
    } catch (IllegalArgumentException e) { // Compliant, specific type is need in the body
      handleException(e);
    } catch (SQLException e) { // Compliant, not the same body
      handleException(e, "a");
    } catch (Exception e) { // Compliant
      int i;
    } catch (Throwable e) { // Compliant
      doCleanup();
    }

    try {
      canThrow();
    } catch (IOException e) {
      for (int i = 0; i < 1; i++) {};
    } catch (Exception e) { // Update part is not the same.
      for (int i = 0; i < 1; foo()) {};
    }
  }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {
  }

  private void doCleanup() {
  }

  void handleException(IOException io) { }

  void handleException(IllegalArgumentException io) { }

  void handleException(SQLException io, String s) { }
}

class CombineCatchCheckLogger {
  void log(Exception e) {
  }
}

class QuickFix {

  void foo() {
    try {
      canThrow();
    } catch (IOException | java.lang.IllegalArgumentException e) {
      doCleanup();
      System.out.println(e.getMessage());
    }
    catch (SQLException e) {  // Noncompliant [[sc=12;ec=26;secondary=-4;quickfixes=qf1]]
      doCleanup();
      System.out.println(e.getMessage());
    }
    // fix@qf1 {{Combine this catch with the one at line 86}}
    // edit@qf1 [[sl=+0;el=+3;sc=5;ec=6]] {{}}
    // edit@qf1 [[sl=-4;el=-4;sc=14;ec=64;]] {{IOException | java.lang.IllegalArgumentException | SQLException e}}
    catch (ArrayStoreException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
  }

  void foo2() {
    try {
      canThrow();
    } catch (IOException e) {
      doCleanup();
      System.out.println(e.getMessage());
    }
    catch (SQLException | IllegalArgumentException e) {  // Noncompliant [[sc=12;ec=53;secondary=-4;quickfixes=qf2]]
      doCleanup();
      System.out.println(e.getMessage());
    }
    // fix@qf2 {{Combine this catch with the one at line 106}}
    // edit@qf2 [[sl=+0;el=+3;sc=5;ec=6]] {{}}
    // edit@qf2 [[sl=-4;el=-4;sc=14;ec=27]] {{IOException | SQLException | IllegalArgumentException e}}
  }

  void foo3() {
    try {
      canThrow();
    } catch (CharacterCodingException e) {
      doCleanup();
      System.out.println(e.getMessage());
    }
    catch (SQLException | IOException e) {  // Noncompliant [[sc=12;ec=40;secondary=-4;quickfixes=qf3]]
      doCleanup();
      System.out.println(e.getMessage());
    }
    // fix@qf3 {{Combine this catch with the one at line 122}}
    // edit@qf3 [[sl=+0;el=+3;sc=5;ec=6]] {{}}
    // edit@qf3 [[sl=-4;el=-4;sc=14;ec=40]] {{SQLException | IOException e}}
  }

  void foo4() {
    try {
      canThrow3();
    }
    catch (java.nio.charset.MalformedInputException e) {}
    catch (CharacterCodingException e2) {} // Noncompliant 
    catch (IOException e3) {} // Noncompliant [[sc=12;ec=26;quickfixes=qf4]]
    // fix@qf4 {{Combine this catch with the one at line 140}}
    // edit@qf4 [[sc=5;ec=30]] {{}}
    // edit@qf4 [[sl=-1;el=-1;sc=12;ec=39]] {{IOException e2}}
  }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {}

  void canThrow2() throws IOException, SQLException, IllegalArgumentException {}

  void canThrow3() throws IOException {}

  private void doCleanup() {}

}
