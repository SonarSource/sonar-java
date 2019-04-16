package org.sonar.java.ecj;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class SyntaxErrorsTest {

  @Test
  public void test() throws Exception {
    Files.walk(Paths.get("../java-checks/src/test/files/checks"))
      .filter(Files::isRegularFile)
      .filter(path -> path.toString().endsWith(".java")
        && !path.endsWith("CheckListParseErrorTest.java")
        && !path.endsWith("ParsingError.java")
        && !path.endsWith("UseSwitchExpressionCheck.java") // Java 12
      )
      .forEach(SyntaxErrorsTest::parse);
  }

  private static void parse(Path file) {
    String source;
    try {
      source = new String(Files.readAllBytes(file));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ASTParser astParser = ASTParser.newParser(AST.JLS11);
    astParser.setResolveBindings(true);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("Example.java");
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_SOURCE, "11");
    astParser.setCompilerOptions(options);

    char[] sourceChars = source.toCharArray();
    astParser.setSource(sourceChars);
    CompilationUnit astNode = (CompilationUnit) astParser.createAST(null);

    for (IProblem problem : astNode.getProblems()) {
      if (problem.isError() && problem.getMessage().contains("Syntax error")) {
        System.err.println(file + ":" + problem.getSourceLineNumber() + " " + problem.getMessage());
        return;
      }
    }
  }

}
