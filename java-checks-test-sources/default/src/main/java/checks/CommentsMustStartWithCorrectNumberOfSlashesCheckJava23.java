///////////////////////////////////////////////////////////////////////////////////////////////
//
// this a license header, we don't raise on it as it will not generate javadoc
//
///////////////////////////////////////////////////////////////////////////////////////////////
package checks;

public class CommentsMustStartWithCorrectNumberOfSlashesCheckJava23 {
  // This is a comment
  public void twoSlashes() {}
  /// javadoc using markdown
  public void threeSlashes() {}
  // Noncompliant@+1
  ////javadoc using markdown {{Markdown documentation should start with exactly three slashes, no more.}}
//^^^^
  public void fourSlashes() {}
  // Noncompliant@+1
  ///// javadoc using markdown
//^^^^
  public void fiveSlashes() {}

  // //
  public void twoTimeTwoSlashes() {}

  // /// //// /////
  /// // /// ////
  public void seriesOfSlashes(){}

  /*
   * multiline comment
   */

  /*
   * multiline comment with Slashes
   //
   ///
   ////
   */

  /**
   //
   ///
   ////
   * @param input A string input to be processed.
   */
  public void javadoc(String input){
  }

  // Noncompliant@+3
  // Noncompliant@+4
  /// This is a javadoc
  //// invalid
  ///
  ///// invalid
//^^^^
  public void markdownJavadoc() {
  }
}
