package checks;

public class UnnecessaryEscapeSequencesInTextBlock {

  public void str() {
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

    String question8 = """
              "What's the point, really
              ?"""; // Compliant

    String question9 = """
              \\"What's the point, really
              ?"""; // Compliant

    String question10 = """
              \\\"What's the point, really
              ?"""; // Noncompliant@-1

    String question11 = """
              \\\\"What's the point, really
              ?"""; // Compliant

    String question12 = """
              \\\\\"What's the point, really
              ?"""; // Noncompliant@-1

    String question13 = """
              \\"\"\"What's the point, really
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question14 = """
              \\"\\"\"What's the point, really
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question15 = """
              \\"\\"\\"What's the point, really
              ?"""; // Compliant

    String question16 = """
              \\"\\\"\\"What's the point, really
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question17 = """
              \\\\\\\\'What's the point, really\\\\\\\\'
              ?"""; // Compliant

    String question19 = """
              \\\\\\\\\'What's the point, really\\\\\\\\\'
              ?"""; // Noncompliant@-1 {{Remove this unnecessary escape sequence.}}

    String question20 = """
              What's
              the 
              point,
              really\\n?"""; // Compliant
  }
}
