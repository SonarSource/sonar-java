package checks;

import org.junit.jupiter.api.Test;

public class NoTestsInProductionFile { // Noncompliant [[sc=14;ec=35]] {{Move this test class to a separate assembly in this solution.}}

  public void not_a_test(){}

}
