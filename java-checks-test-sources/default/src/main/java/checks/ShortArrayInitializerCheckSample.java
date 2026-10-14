package checks;

import java.util.List;

public class ShortArrayInitializerCheckSample {

  private static final String[] SAFE_METHODS = new String[] {"GET", "HEAD", "OPTIONS"}; // Noncompliant {{Remove the redundant array creation and keep only the array initializer.}} [[quickfixes=qf1]]
  //                                           ^^^^^^^^^^^^
  // fix@qf1 {{Remove the redundant array creation}}
  // edit@qf1 [[sc=48;ec=61]] {{}}

  private static final int[] EMPTY = new int[] {}; // Noncompliant
  //                                 ^^^^^^^^^

  private final char[] separators = new char[] {',', ';'}; // Noncompliant
  //                                ^^^^^^^^^^

  private static final int[][] GRID = new int[][] {{1, 2}, {3, 4}}; // Noncompliant [[quickfixes=qf2]]
  //                                  ^^^^^^^^^^^
  // fix@qf2 {{Remove the redundant array creation}}
  // edit@qf2 [[sc=39;ec=51]] {{}}

  private static final List[] RAW_LISTS = new List[] {}; // Noncompliant
  //                                      ^^^^^^^^^^

  private static final String[] NO_INITIALIZER = new String[3];

  private static final String[] SHORTHAND = {"GET", "HEAD"};

  int[] retryDelaysMillis() {
    int[] delays = new int[] {100, 500, 2_000}; // Noncompliant [[quickfixes=qf3]]
    //             ^^^^^^^^^
    // fix@qf3 {{Remove the redundant array creation}}
    // edit@qf3 [[sc=20;ec=30]] {{}}
    return delays;
  }

  void multipleDeclarators() {
    int[] first = new int[] {1}, // Noncompliant
    //            ^^^^^^^^^
      second = new int[] {2}; // Noncompliant
      //       ^^^^^^^^^
  }

  void alreadyShorthand() {
    int[] delays = {100, 500};
    String[] methods = {"GET"};
  }

  void sizedCreation() {
    int[] delays = new int[3];
    int[][] grid = new int[2][2];
    int[][] partial = new int[2][];
  }

  void assignmentToExistingVariable() {
    int[] delays;
    delays = new int[] {1, 2};
    delays = new int[0];
  }

  void methodArgument() {
    accepts(new String[] {"GET"});
    acceptsVarargs(new String[] {"GET"});
  }

  String[] returnStatement() {
    return new String[] {"GET"};
  }

  void inferredType() {
    var methods = new String[] {"GET"};
    var grid = new int[][] {{1}};
  }

  void widerDeclaredType() {
    Object[] values = new String[] {"a"};
    Object single = new String[] {"a"};
  }

  void nestedExplicitCreation() {
    int[][] grid = {new int[] {1, 2}, new int[] {3}};
  }

  @Methods(value = {"GET", "HEAD"})
  void annotationShorthand() {
  }

  @Methods(value = new String[] {"GET", "HEAD"})
  void annotationExplicit() {
  }

  void accepts(String[] methods) {
  }

  void acceptsVarargs(String... methods) {
  }

  @interface Methods {
    String[] value();
  }
}
