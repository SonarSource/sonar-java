package checks;

public class CommentsMustStartWithCorrectNumberOfSlashesCheckBeforeJava23 {

  // This is a comment
  public void twoSlashes() {}
  // Noncompliant@+1 {{Do not use more than two slashes in a comment.}}
  /// This is a comment
//^^^
  public void threeSlashes() {}
  // Noncompliant@+1 {{Do not use more than two slashes in a comment.}}
  //// This is a comment
//^^^
  public void fourSlashes() {}


  // //
  public void twoTimeTwoSlashes() {}

  // /// //// /////
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
   *
   * @param input A string input to be processed.
   */
  public void javadoc(String input){
  }

}
