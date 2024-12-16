package checks;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

class HardcodedURICheckSample {

  public static @interface MyAnnotation {
    String stuff() default "none";
    // we cannot name method path otherwise path is detected as an identifier used in annotation
    //, and it creates clashes (FN) with path variables or fields
    String path() default "/";
  }

  static final String PATH_WITH_EXPANSION_PATTERN = "/.*+\\.[a-z0-9]{2,4}$"; // Compliant
  static final String PATH_WITH_PATTERN_WITH_DOLLAR = "/.{2,4}$"; // Compliant
  static final String PATH_WITH_PATTERN_WITH_STAR = "/.*\\.[a-z0-9]"; // Compliant

  String fileName = "//my-network-drive/folder/file.txt"; // Noncompliant
  String[] stuffs = new String[1];

  @MyAnnotation(stuff = "yolo", path = "/{var}/bulu/stuff") // Compliant - annotations are ignored
  void bar(String var) { }

  @MyAnnotation(stuff = "/{var}/bulu/stuff") // Compliant - not a path assignmnet
  void qix(String var) { }

  @MyAnnotation(path = "/{var}/bulu/stuff") // Compliant - annotations are ignored
  void foo(String s, String var) throws URISyntaxException {
    new Object();

    new URI(s); // Compliant
    new File(s); // Compliant
    new File("", s); // Compliant
    new File("", s + "/" + s); // Noncompliant {{Remove this hard-coded path-delimiter.}}
//                   ^^^
    String path1 = "a" + "/" + "b"; // Noncompliant {{Remove this hard-coded path-delimiter.}}

    new URI("http:https"); // Compliant
    new URI("http://www.mywebsite.com"); // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^
    new File("/home/path/to/my/file.txt"); // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    new File(s, "~\\blah\\blah\\blah.txt"); // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
//              ^^^^^^^^^^^^^^^^^^^^^^^^^
    new File("/Folder/", s); // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
//           ^^^^^^^^^^

    String filename;
    String path = "/home/path/to/my/file.txt"; // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String fileName = "\\\\blah\\blah\\"; // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
    String fileNAME = s; // Compliant
    String stuff = "/home/path/to/my/file.txt"; // Compliant  - requires a variable with adequate name
    this.fileName = "/home/path/to/my/file.txt"; // Noncompliant
    stuffs[0] = "/home/path/to/my/file.txt"; // Compliant - require a variable with adequate name

    fileNAME = s + "//" + s; // Noncompliant {{Remove this hard-coded path-delimiter.}}
    fileNAME = s + "\\\\" + s; // Noncompliant {{Remove this hard-coded path-delimiter.}}
    fileNAME = s + "hello" + s; // Compliant
    fileNAME = "c:\\blah\\blah\\blah.txt"; // Noncompliant

    int fIleNaMe = 14 - 2;

    String v1 = s + "//" + s; // Compliant - not a file name
  }

  @interface MyAnnotation2 {
    String aVar() default "";
  }

  static final String relativePath1 = "/search"; // Compliant, we don't raise issues on short relative uri in constants
  static final String relativePath2 = "/group/members";
  static final String longRelativePath = "/group/members/list.json"; // Noncompliant
  static final String urlPath = "https://www.mywebsite.com"; // Noncompliant
  final String staticIsMissingPath = "/search"; // Noncompliant
  static String finalIsMissingPath = "/search"; // Noncompliant

  static final String default_uri_path = "/a-great/path/for-this-example"; // Compliant, default_uri is constant and is used in an annotation
  String aVarPath = "/a-great/path/for-this-example"; // Noncompliant

  @MyAnnotation2(aVar = default_uri_path)
  void annotated(){}

  @MyAnnotation2()
  String endpoint_url_path = "/a-great/path/for-this-example"; // Compliant, an annotation is applied on the variable

  void reachFullCoverage(){
    int path = 0;
    path = 10;
  }

}
