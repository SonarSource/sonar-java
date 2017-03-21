import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

class A {

  public static @interface MyAnnotation {
    String stuff() default "none";
    String path() default "/";
  }

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
    new File("", s + "/" + s); // Noncompliant [[sc=22;ec=25]] {{Remove this hard-coded path-delimiter.}}

    new URI("http:https"); // Compliant
    new URI("http://www.mywebsite.com"); // Noncompliant [[sc=13;ec=39]] {{Refactor your code to get this URI from a customizable parameter.}}
    new File("/home/path/to/my/file.txt"); // Noncompliant [[sc=14;ec=41]] {{Refactor your code to get this URI from a customizable parameter.}}
    new File(s, "~\\blah\\blah\\blah.txt"); // Noncompliant [[sc=17;ec=42]] {{Refactor your code to get this URI from a customizable parameter.}}
    new File("/Folder/", s); // Noncompliant [[sc=14;ec=24]] {{Refactor your code to get this URI from a customizable parameter.}}

    String filename;
    String path = "/home/path/to/my/file.txt"; // Noncompliant [[sc=19;ec=46]] {{Refactor your code to get this URI from a customizable parameter.}}
    String fileName = "\\\\blah\\blah\\"; // Noncompliant {{Refactor your code to get this URI from a customizable parameter.}}
    String fileNAME = s; // Compliant
    String stuff = "/home/path/to/my/file.txt"; // Compliant  - requires a variable with adequate name
    this.fileName = "/home/path/to/my/file.txt"; // Noncompliant
    stuffs[0] = "/home/path/to/my/file.txt"; // Compliant - require a variable with adequate name

    fileNAME = s + "//" + s; // Noncompliant {{Remove this hard-coded path-delimiter.}}
    fileNAME = s + "\\\\" + s; // Noncompliant {{Remove this hard-coded path-delimiter.}}t
    fileNAME = s + "hello" + s; // Compliant
    fileNAME = "c:\\blah\\blah\\blah.txt"; // Noncompliant

    int fIleNaMe = 14 - 2;

    String v1 = s + "//" + s; // Compliant - not a file name
  }
}
