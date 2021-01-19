package checks;

public class TextBlockTabsAndSpaces {
  public final static String compliant1 = """

    this line is indented with spaces

    and this one also with spaces
    """;

  public final static String compliant2 = """
		this line is indented with tabs

		and this one also with tabs
		""";

	// Noncompliant@+1 {{Use only spaces or only tabs for indentation}}
  public final static String nonCompliant1 = """
    this line is indented with spaces

		and this one with tabs
		
		this one also has tabs
    """;

	// Noncompliant@+1
  public final static String nonCompliant2 = """
  	This line is indented with two spaces and a tab
  	""";

  // This could be argued to be an FP because the tab isn't part of the indentation by Java's logic, but
	// it's probably intended to be
  // Noncompliant@+1
  public final static String compliant3 = """
    	This line indented with four spaces and then has a tab that isn't technically part of the indentation
  """;

  public final static String empty = """
""";

  public final static String onlyWhiteSpace = """
		""";

  public final static String noIndent = """
hello
""";

  public final static String tabsOutsideOfIndentation = """
    hello	world (there's a tab between 'hello' and 'world')
    """;
}
