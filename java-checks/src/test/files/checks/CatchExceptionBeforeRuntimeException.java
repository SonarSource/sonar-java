public class TestClass {
  
  public void method() {
    try {
    } catch(Exception e) { // Noncompliant {{Catch a list of specific exception subtypes instead.}}
    }
    try {
    } catch(RuntimeException e) { // Compliant
    } catch(Exception e) { // Compliant, after RuntimeException
    }
  }

}