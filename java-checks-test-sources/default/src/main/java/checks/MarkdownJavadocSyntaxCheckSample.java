package checks;
// Noncompliant@+3 {{replace HTML syntax with Markdown syntax in javadoc}}

/// The javadoc in this class uses markdown comment (3 slashes) but uses some HTML tags.
/// It makes some text appear in <b>bold</b>.
//                               ^^^
public class MarkdownJavadocSyntaxCheckSample {

  // Noncompliant@+2

  /// Some text appears in <i>italic</i>.
  //                       ^^^
  public void withItalic() {
    // Empty
  }

  /// Some text appears in _italic_.
  public void withItalicMarkdown() { // Compliant
    // Empty
  }

  // Noncompliant@+2

  /// Separate <p> paragraphs are created by simply using the {@code <p>} tag.
  //           ^^^
  public void withParagraph() {
    // Empty
  }

  /// Separate
  ///
  /// paragraphs are created by simply using the `<p>` tag.
  public void withParagraphMarkdown() { // Compliant
    // Empty
  }

  // Noncompliant@+1
  /// For inline code snippets, it uses the {@code <code>} tag.
  //                                        ^^^^^^^^^^^^^^
  public void withCode() {
    // Empty
    }

  // Noncompliant@+2
  /// For larger blocks of code, the `<pre>` tag is used:
  /// <pre>
  /// public class Example {
  ///   public static void main(String[] args) {
  ///     System.out.println("Hello, Javadoc!");
  ///   }
  /// }
  /// </pre>
  public void withPreBlock() {
    // Empty
  }

  /// For larger blocks of code, triple quotes should be used:
  /// ```
  /// public class Example {
  ///   public static void main(String[] args){
  ///     System.out.println("Hello, <b>Javadoc</b>!");
  ///}
  ///}
  ///```
  public void withMarkdownBlock() { // Compliant
    // Empty
  }

  // Noncompliant@+1
  /// {@link String#length()} links to the {@link java.lang.String#length()} method.
  //  ^^^^^^^^^^^^^^^^^^^^^^^
  public void withLink() {
    // Empty
  }

  /// [String#length()] links to the [java.lang.String#length()] method.
  public void withLinkMarkdown() { // Compliant
  }

  // Noncompliant@+2
  /// Here is a list:
  /// <ul>
  ///   <li> Red </li>
  ///   <li> Blue </li>
  ///   <li> Green </li>
  /// </ul>
  public void withList() {
    // Empty
  }

  /// Here is a list:
  /// * Red
  /// * Blue
  /// * Green
  public void withListMarkdown() { // Compliant
    // Empty
  }

  // Noncompliant@+2
  /// An ordered list:
  /// <ol>
  /// <li>one
  /// <li>two
  /// </ol>
  public int withOrderedList() {
    return 0;
  }

  /// An ordered list:
  /// 1. one
  /// 1. two
  public int withOrderedListMarkdown() {
    return 0;
  }

  // Noncompliant@+2
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
  public void withTable() {
    // Empty
  }

  /// Here is a table:
  ///
  /// | Header 1     | Header 2     |
  /// |--------------|--------------|
  /// | Row 1, Col 1 | Row 1, Col 2 |
  /// | Row 2, Col 1 | Row 2, Col 2 |
  public void withTableMarkdown() { // Compliant
    // Empty
  }

  public void danglingComment() {
    // Compliant, because this is not placed where it would be considered by JavaDocs.

    /// Some text in <i>italic</i>
  }
}
