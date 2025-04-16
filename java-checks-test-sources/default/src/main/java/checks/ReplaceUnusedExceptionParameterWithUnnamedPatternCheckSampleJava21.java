package checks;

import java.util.List;

public class ReplaceUnusedExceptionParameterWithUnnamedPatternCheckSampleJava21 {
  public void simpleUnUsedParameter() {

    List<String> elements = List.of();
    int value = 0;
    try {
      var elem = elements.get(10);
      value = Integer.parseInt(elem);
    } catch (NumberFormatException nfe) { // compliant java 21
      System.err.println("Wrong number format");
    } catch (IndexOutOfBoundsException ioob) { // compliant java 21
      System.err.println("No such element");
    }
  }
}
