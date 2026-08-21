package checks;

class AlmostJavadocCheckSample {

  String name;

  // Noncompliant@+1 [[quickfixes=qf1]]
  /*
   * Computes the factorial of a positive integer.
   * @param n the number to compute factorial for
   * @return the factorial of n
   */
  // fix@qf1 {{Convert to Javadoc comment}}
  // edit@qf1 [[sc=4;ec=4]] {{*}}
  public long factorial(int n) {
    return (n <= 1) ? 1L : n * factorial(n - 1);
  }

  /**
   * Computes the factorial of a positive integer.
   * @param n the number to compute factorial for
   * @return the factorial of n
   */
  public long documentedFactorial(int n) {
    return (n <= 1) ? 1L : n * documentedFactorial(n - 1);
  }

  // Noncompliant@+1 [[quickfixes=qf2]]
  /* Returns the display name as <code>String</code>. */
  // fix@qf2 {{Convert to Javadoc comment}}
  // edit@qf2 [[sc=4;ec=4]] {{*}}
  public String displayName() {
    return name;
  }

  /** Returns the display name as <code>String</code>. */
  public String documentedDisplayName() {
    return name;
  }

  interface Repository {
    // Noncompliant@+1 [[quickfixes=qf3]]
    // Loads the entity. {@link Entity} */
    // fix@qf3 {{Convert to Javadoc comment}}
    // edit@qf3 [[sc=5;ec=7]] {{/**}}
    Entity load(String id);

    /** Loads the entity. {@link Entity} */
    Entity documentedLoad(String id);

    // Noncompliant@+1
    // Loads with trailing space. {@link Entity} */ 
    Entity loadWithTrailingSpace(String id);
  }

  // Noncompliant@+1
  /* {@link AlmostJavadocCheckSample} */
  static class Nested {}

  /** {@link AlmostJavadocCheckSample} */
  static class DocumentedNested {}

  // Noncompliant@+1
  /* @since 1.0 */
  int version;

  /** @since 1.0 */
  int documentedVersion;

  enum Kind {
    // Noncompliant@+1
    /* Foo <em>bar</em>. */
    FOO,
    /** Foo <em>bar</em>. */
    BAR
  }

  /* Regular commentary without tags. */
  void undocumentedOnPurpose() {
  }

  // Regular line comment with {@link tags} is not almost-Javadoc
  void lineCommentWithoutTerminator() {
  }

  /* returns 0 on success */
  int noTagBecauseReturnIsAWord() {
    return 0;
  }

  /* support@param.org is an email, not a Javadoc tag */
  void emailLooksLikeTag() {
  }

  /* @Override is a Java annotation mentioned in a comment */
  void annotationMention() {
  }

  /* List<String> uses generics, not HTML */
  void genericsAreNotHtml() {
    voidWithLocalComment();
  }

  void voidWithLocalComment() {
    /* @param local is not attached to a documentable declaration */
    int local = 1;
  }

  /** Valid Javadoc. */
  /* Extra note with {@link tags}. */
  void alreadyHasJavadoc() {
  }

  /// Markdown documentation with {@link tags}.
  /* Extra note with @param. */
  void alreadyHasMarkdown() {
  }

  @Override
  public String toString() {
    return name;
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 0;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object other) {
    return this == other;
  }

  /* (non-Javadoc)
   * @param ignored this Eclipse stub still contains a Javadoc tag
   */
  public void eclipseMarkerWithoutSee(int ignored) {
  }

  // Noncompliant@+1
  /* @see java.lang.Object#clone() */
  public Object seeWithoutEclipseMarker() {
    return this;
  }

  // Noncompliant@+1
  /* (non-javadoc) @see java.lang.Object#wait() */
  public void lowercaseMarkerLookalike() {
  }

  // (non-Javadoc) {@link Object} */
  public void eclipseMarkerLineComment() {
  }

  record Point(int x, int y) {
    // Noncompliant@+1
    /* @param x the x coordinate */
    Point {
    }
  }

  // Noncompliant@+1
  /* @since 1.0 */
  int a, b, c;

  int trailingField; // trailing note: see @param x */
  void methodAfterTrailingComment(int x) {
  }

  void localClassIsNotDocumentable() {
    /* @param q */
    class Local {
      /* @return r */
      int r;
    }
  }

  static class Entity {
  }
}

// Noncompliant@+1 [[quickfixes=qf4]]
// /** Extra documentation. {@link AlmostJavadocCheckSample.Entity} */
// fix@qf4 {{Convert to Javadoc comment}}
// edit@qf4 [[sc=1;ec=3]] {{}}
class AlmostJavadocCheckSampleSecondType {
}
