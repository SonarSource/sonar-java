package checks;

class MissingDeprecatedCheckMarkdownSample {

  @Deprecated(since = "R1.3")
  public void annotated() {} // Noncompliant

  /// Markdown doc, but not tag.
  @Deprecated(since = "R1.3")
  public void annotatedJavaDoc() {} // Noncompliant

  /// Markdown doc with a tag.
  /// @deprecated for a good reason
  @Deprecated(since = "R1.3")
  public void annotatedJavaDocTag() {} // Compliant

  /// Markdown doc with a tag.
  /// @deprecated for a good reason
  public void justTag() {} // Noncompliant
}
