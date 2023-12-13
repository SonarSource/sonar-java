/* No detection of commented-out code in header
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */
package checks.ml;

/**
 * No detection of commented-out code in Javadoc for class
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */
public class CommentedCodeML {
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
  public CommentedCodeML(int field) {
    this.field = field;
    // This is a comment, but next line is a commented-out code

    // Noncompliant@+2 {{This block of commented-out lines of code should be removed.}}

    // for (Visitor visitor : visitors) {
    //   continue;
    // }

    // Noncompliant@+1
    /*
    This is a comment, but next line is a commented-out code
    for (Visitor visitor : visitors) {
      continue;
    }
    */


    // Noncompliant@+4
    // Noncompliant@+4
    /* This is a comment, but next line is a commented-out code */
    /* for (Visitor visitor : visitors) { */
    /*   continue; */
    /* } */


    /* Limitation: only the first line of a commented block is highlighted
     * for (Visitor visitor : visitors) {
     *   continue;
     * }
     */

    // Noncompliant FP
    // Noncompliant@+3 [[sc=5;ec=47]]
    // Noncompliant@+2 [[sc=52;ec=94]]
    // Only one issue is highlighted per list of consecutive comments
    /* for (Visitor visitor : visitors) { } */     /* for (Visitor visitor : visitors) { } */

    // Noncompliant@+3
    // Noncompliant@+2
    // Several issues are highlighted for non consecutive comments
    if (/* for (Visitor visitor : visitors) { } */field == 0) { /* for (Visitor visitor : visitors) { } */
    }

    // Noncompliant@+2 [[sc=5;ec=65]]
    // Comment prefix and suffix are not highlighted
    /*         for (Visitor visitor : visitors) { }           */


    /* Leading star is not highlighted
     *   for (Visitor visitor : visitors) { }
     */


    /* Leading spaces are not highlighted
         for (Visitor visitor : visitors) { }
     */


    // Leading spaces are not highlighted
    //         for (Visitor visitor : visitors) {
    //         }

    // TODO
    /**
     * This is not Javadoc, even if it looks like Javadoc and before declaration of variable
     * for (Visitor visitor : visitors) {
     *   continue;
     * }
     */
    int a;
  }

  // Noncompliant@+12 FP
  // Noncompliant@+7 FP
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

  /*
   * This is not a documentation comment
   * for (Visitor visitor : visitors) {
   * continue;
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

  /*
   * FIXME: the following method calls {@link CommentedCode#method(String)}
   */
  public void foo() {
    // FIXME: the following line calls {@link CommentedCode#method(String)} - javadoc links are accepted in comments
    method("");
  }
  // C++
}
