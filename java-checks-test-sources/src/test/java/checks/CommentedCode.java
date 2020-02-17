/* No detection of commented-out code in header
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */

/**
 * No detection of commented-out code in Javadoc for class
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */
public class CommentedCode {

  /**
   * No detection of commented-out code in Javadoc for field
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  private int field;

  /**
   * No detection of commented-out code in Javadoc for constructor
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  public CommentedCode(int field) {
    this.field = field;
    // This is a comment, but next line is a commented-out code
    // Noncompliant@+1
    // Noncompliant@+2 {{This block of commented-out lines of code should be removed.}}

    // for (Visitor visitor : visitors) {
    //   continue;
    // }

    // Noncompliant@+3
    /*
    This is a comment, but next line is a commented-out code
    for (Visitor visitor : visitors) {
      continue;
    }
    */

    // Noncompliant@+2
    /* This is a comment, but next line is a commented-out code */
    /* for (Visitor visitor : visitors) { */
    /*   continue; */
    /* } */

    // TODO
    /**
     * This is not Javadoc, even if it looks like Javadoc and before declaration of variable
     * for (Visitor visitor : visitors) {
     *   continue;
     * }
     */
    int a;
  }

  // TODo
  /**
   * From GWT documentation:
   * JSNI methods are declared native and contain JavaScript code in a specially formatted comment block
   * between the end of the parameter list and the trailing semicolon.
   */
  public static native void alert(String msg) /* not JSNI comment */ /*-{
    for (i=0;i<=5;i++) {
      $wnd.alert(msg);
    }
  }-*/; /*-{
  This is not JSNI comment block, even if it looks like
  for (Visitor visitor : visitors) {
    continue;
  }
  }-*/

  // Noncompliant@+3
  /*
   * This is not a documentation comment
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  public void method(String s) {
  }

  /**
   * No detection of commented-out code in Javadoc for method
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  public int getField() {
    return field;
  }

  /**
   * FIXME: the following method calls {@link CommentedCode#method(String)}
   */
  public void foo() {
    // FIXME: the following line calls {@link CommentedCode#method(String)} - javadoc links are accepted in comments
    method("");
  }
  // C++
}
