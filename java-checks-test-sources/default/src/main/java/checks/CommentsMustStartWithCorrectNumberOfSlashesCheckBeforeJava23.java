package checks;

public class CommentsMustStartWithCorrectNumberOfSlashesCheckBeforeJava23 {

  // This is a comment
  public void twoSlashes() {}
  // Noncompliant@+1 {{A single-line comment should start with exactly two slashes, no more.}}
    ///This is a comment
  //^^^
  public void threeSlashes() {}
  // Noncompliant@+1 {{A single-line comment should start with exactly two slashes, no more.}}
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
  //
  ///
  ////
   */

}
