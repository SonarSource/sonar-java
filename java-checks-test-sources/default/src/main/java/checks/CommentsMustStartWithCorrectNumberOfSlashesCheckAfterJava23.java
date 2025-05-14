package checks;

public class CommentsMustStartWithCorrectNumberOfSlashesCheckAfterJava23 {
  // This is a comment
  public void twoSlashes() {}
  /// This is a javadoc
  public void threeSlashes() {}
  // Noncompliant@+1
  //// This is javadoc using markdown {{Do not use more than three slashes in a comment.}}
//^^^^
  public void fourSlashes() {}

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
   *
   * @param input A string input to be processed.
   */
  public void javadoc(String input){
  }

  // Noncompliant@+3
  // Noncompliant@+4
  /// This is a javadoc
	//// invalid
  ///
  //// invalid
//^^^^
  public void markdownJavadoc() {
  }

  /// Calculates the sum of two integers.
  ///
  /// - @param `a` the first integer to add
  /// - @param `b` the second integer to add
  /// - @return the sum of the two integers
  /// - @throws `IllegalArgumentException` if either of the integers is null
  public int calculateSum(Integer a, Integer b) {
    return a + b;
  }
}
