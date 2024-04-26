/* No detection of commented-out code in header
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */
package checks;

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


    /* Limitation: only the first line of a commented block is highlighted
     * for (Visitor visitor : visitors) { // Noncompliant@+2
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     *   continue;
     * }
     */


    // Only one issue is highlighted per list of consecutive comments
    /* for (Visitor visitor : visitors) { } */     /* for (Visitor visitor : visitors) { } */ // Noncompliant@+2
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

 // Noncompliant@+3
 // Noncompliant@+2
    // Several issues are highlighted for non consecutive comments
    if (/* for (Visitor visitor : visitors) { } */field == 0) { /* for (Visitor visitor : visitors) { } */
    }


    // Comment prefix and suffix are not highlighted
    /*         for (Visitor visitor : visitors) { }           */ // Noncompliant@+2
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    /* Leading star is not highlighted
     *   for (Visitor visitor : visitors) { } // Noncompliant@+2
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     */


    /* Leading spaces are not highlighted
         for (Visitor visitor : visitors) { } // Noncompliant@+2
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     */


    // Leading spaces are not highlighted
    //         for (Visitor visitor : visitors) { // Noncompliant@+2
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //         }
//  ^^^<

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


  /*
   * This is not a documentation comment
   * for (Visitor visitor : visitors) { // Noncompliant@+3
//   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
   *   continue;
//  ^^^<
   * }
//  ^^^<
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
