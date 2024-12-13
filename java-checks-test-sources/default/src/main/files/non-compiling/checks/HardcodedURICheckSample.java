package checks;

public class HardcodedURICheckSample {

  String path = "/home/path/to/my/file.txt"; // Noncompliant

  String aVarPath = "/home/path/to/my/file.txt"; // FN, missing semantics
  @MyAnnotation(aVarPath = "")
  int x = 0;
}
