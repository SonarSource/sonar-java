package checks;

public class SimpleStringLiteralForSingleLineStringsCheck {
  
  public void str() { // Noncompliant@+1 [[sc=23;endColumn=43]]{{Use simple literal for a single-line string.}}
    String question = """
              What's the point, really?""";

    String question1 = "What's the point, really?"; // Compliant

    String question2 = """
              What's the point, really
              ?"""; // Compliant, 3 lines

    String question3 = """
              What's the point, really\n?"""; // Noncompliant@-1 [[sc=24;endColumn=45]]{{Use simple literal for a single-line string.}}

  }
}
