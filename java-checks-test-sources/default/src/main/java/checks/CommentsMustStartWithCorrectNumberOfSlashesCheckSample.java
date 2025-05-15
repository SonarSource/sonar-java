///////////////////////////////////////////////////////////////////////////////////////////////
//
// this a license header, we don't raise on it as it will not generate javadoc
//
///////////////////////////////////////////////////////////////////////////////////////////////
package checks;


// Noncompliant@+1
/// don't use three slashes
import java.util.List;

// Noncompliant@+1
/// This is a comment, but will be javadoc in java 23
public class CommentsMustStartWithCorrectNumberOfSlashesCheckSample {

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

  public void insideMethod() {
    // Noncompliant@+1
    ///This is a comment
  }


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
