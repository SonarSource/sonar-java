package org.javac.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.model.declaration.ClassTreeImpl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JavacParserTest {

  private static final JavacParser PARSER = new JavacParser();

  @Test
  void testParse(@TempDir Path tempDir) throws IOException {
    Path myJavaFile = tempDir.resolve("testFile.java");
    String content = """
      package org.example;
      
      import org.junit.jupiter.api.Test;
            
      public class TestFile {
        
        @Test
        void testMethod() {
          System.out.println("Hello, World!");
        }
      }""";
    Files.write(myJavaFile, content.getBytes());

    var compilationUnitTree = PARSER.parse(myJavaFile.toFile());
    assertThat(compilationUnitTree.types()).isNotNull();
    assertThat(compilationUnitTree.types().size()).isEqualTo(1);
    assertThat(compilationUnitTree.types().get(0)).isNotNull();
    ClassTreeImpl firstClass = (ClassTreeImpl) compilationUnitTree.types().get(0);

    assertThat(firstClass.getLine()).isEqualTo(4);

    assertThat(firstClass.simpleName()).isNotNull();
    String simpleName = firstClass.simpleName().name();
    assertThat(simpleName).isNotNull();
    assertThat(simpleName).isEqualTo("TestFile");

  }

//  @Test
//  void testParseSemantics(@TempDir Path tempDir) throws IOException {
//    Path myJavaFile = tempDir.resolve("testFile.java");
//    String content = """
//      package org.example;
//
//      import org.junit.jupiter.api.Test;
//
//      public class TestFile {
//
//        @Test
//        void test() {
//          var myString = "";
//          System.out.println(myString.substring(0, 1));
//        }
//      }
//      """;
//    Files.write(myJavaFile, content.getBytes());
//
//    String classpath = "C:\\Users\\leonardo.pilastri\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter-api\\5.11.2\\junit-jupiter-api-5.11.2.jar"; // Replace with the actual path to the JUnit JAR file
//    var options = Arrays.asList("-classpath", classpath);
//    var compilationUnitTree = PARSER.parse(myJavaFile.toFile(), options);
//
//    ClassTree firstClass = (ClassTree) compilationUnitTree.getTypeDecls().get(0);
//    MethodTree testMethod = (MethodTree) firstClass.getMembers().get(0);
//    AnnotationTree testAnnotation = testMethod.getModifiers().getAnnotations().get(0);
//    assertThat(testAnnotation.getAnnotationType().getKind().asInterface().getSimpleName()).isEqualTo("Test");
//
//  }

}
