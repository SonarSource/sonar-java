package checks;

/// The javadoc in this class uses markdown comment (3 slashes) but uses some HTML tags.
/// It makes some text appear in <b>bold</b>.
public class MarkdownJavadocSyntaxCheckSample { // Noncompliant {{replace HTML syntax with Markdown syntax in javadoc}}

  /// Some text appears in <i>italic</i>.
  public void withItalic() { // Noncompliant
  }

  /// Some text appears in _italic_.
  public void withItalicMarkdown() { // Compliant
  }

  /// Separate <p> paragraphs are created by simply using the {@code <p>} tag.
  public void withParagraph() { // Noncompliant
  }

  /// Separate
  ///
  /// paragraphs are created by simply using the `<p>` tag.
  public void withParagraphMarkdown() { // Compliant
  }

  /// For inline code snippets, it uses the {@code <code>} tag.
  public void withCode() { // Noncompliant
  }

  /// For larger blocks of code, the `<pre>` tag is used:
  /// <pre>
  /// public class Example {
  ///   public static void main(String[] args) {
  ///     System.out.println("Hello, Javadoc!");
  ///   }
  /// }
  /// </pre>
  public void withPreBlock() { // Noncompliant
  }

  /// For larger blocks of code, triple quotes should be used:
  /// ```
  /// public class Example {
  ///   public static void main(String[] args) {
  ///     System.out.println("Hello, <b>Javadoc</b>!");
  ///   }
  /// }
  /// ```
  public void withMarkdownBlock() { // Compliant
  }

  /// {@link String#length()} links to the {@link java.lang.String#length()} method.
  public void withLink() { // Noncompliant
  }

  /// [String#length()] links to the [java.lang.String#length()] method.
  public void withLinkMarkdown() { // Compliant
  }

  /// Here is a list:
  /// <ul>
  ///   <li> Red </li>
  ///   <li> Blue </li>
  ///   <li> Green </li>
  /// </ul>
  public void withList() { // Noncompliant
  }

  /// Here is a list:
  /// * Red
  /// * Blue
  /// * Green
  public void withListMarkdown() { // Compliant
  }

  /// Here is a table:
  /// <table>
  /// <tr>
  /// <th>Header 1</th>
  /// <th>Header 2</th>
  /// </tr>
  /// <tr>
  ///  <td>Row 1, Col 1</td>
  /// <td>Row 1, Col 2</td>
  /// </tr>
  /// <tr>
  /// <td>Row 2, Col 1</td>
  /// <td>Row 2, Col 2</td>
  /// </tr>
  /// </table>
  public void withTable() { // Noncompliant
  }

  /// Here is a table:
  ///
  /// | Header 1     | Header 2     |
  /// |--------------|--------------|
  /// | Row 1, Col 1 | Row 1, Col 2 |
  /// | Row 2, Col 1 | Row 2, Col 2 |
  public void withTableMarkdown() { // Compliant
  }
}
