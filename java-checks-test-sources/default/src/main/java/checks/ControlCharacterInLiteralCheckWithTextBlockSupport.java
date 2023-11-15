package checks;

public class ControlCharacterInLiteralCheckWithTextBlockSupport {

  String[] data = {
    """
			Compliant tab characters
    """,
    """
      \u200B
    """,
    // Noncompliant@+1 {{Remove the non-escaped \u200B character from this literal.}}
    """
      U+200B '​' zero width space
    """,

    // Compliant
    """
      U+0009 \u0009 tab here
    """,
    // Noncompliant@+1 {{Remove the non-escaped \u0009 character from this literal.}}
    """
      U+0009 '	' tab here
    """,
    // Compliant, leading tabs
    """
			U+0009 Compliant code
			""",
    
  };

  public String returnWithU0009() {
    // Noncompliant@+1 {{Remove the non-escaped \u0009 character from this literal.}}
    return """
      <html>
  \t 	<head>
      \t\t<title>Main</title>
      \t</head>
      \t<body>
      \t</body>
      </html>
      """;
  }
}
