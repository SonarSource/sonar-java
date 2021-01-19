package checks;

public class UnnecessaryEscapeSequencesInTextBlock {

  public void str() { // Noncompliant@+2 {{Remove this unnecessary escape sequence.}}
    String question = """
              What's\tthe\tpoint,\treally?""";

    String question1 = "What's the point, really?"; // Compliant

    String question2 = """
              \"What's the point, really
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question3 = """
              \'What's the point, really\'
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question4 = """
              What's
              the 
              point,
              really\n?"""; // Noncompliant {{Remove this unnecessary escape sequence.}}

    String question5 = """
             \"\"\"What's the point, really
             """; // Noncompliant@-1 {{Use '\"""' to escape """.}}

    String question6 = """
             \"""What's the point, really\"""
             """; // Compliant

    String question7 = """
             What's the point, really
             """; // Compliant

  }
}
