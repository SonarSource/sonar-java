package checks;

public class ControlCharacterInLiteralCheck {

  String[] data = {
    """
			Compliant tab characters
    """,
    """
      \u200B
    """,
    // Noncompliant@+1 {{Remove the non-escaped \u200B character from this string literal.}}
    """
      U+200B '​' zero width space
    """,
  };

}
