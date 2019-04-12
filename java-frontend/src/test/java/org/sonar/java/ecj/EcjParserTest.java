package org.sonar.java.ecj;

import org.junit.Test;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class EcjParserTest {

  @Test
  public void declarations() {
    CompilationUnitTree t;

    t = parse("class Example { }");
    assertThat(t.types()).hasSize(1);

    t = parse("interface Example { }");
    assertThat(t.types()).hasSize(1);

    t = parse("enum Example { }");
    assertThat(t.types()).hasSize(1);

    t = parse("class Example { int field; }");
    assertThat(t.types()).hasSize(1);
  }

  @Test
  public void statements() {
    parseStatement(";");

    parseStatement("for (;;) break;");
    parseStatement("while (true) break;");

    {
      IfStatementTree t = (IfStatementTree) parseStatement("if (true) ; else ;");
      assertThat(t.condition()).isNotNull();
      assertThat(t.thenStatement()).isNotNull();
      assertThat(t.elseStatement()).isNotNull();
    }
  }

  @Test
  public void expressions() {
    parseExpression("true");
    parseExpression("'c'");
    parseExpression("\"s\"");
    parseExpression("42");
    parseExpression("null");

    // TODO parseExpression("this");
    parseExpression("i");
    parseExpression("(42)");
    parseExpression("i++");
    parseExpression("--i");
    parseExpression("true == false");
    parseExpression("new Object()");
    parseExpression("(double) 42");
    parseExpression("true ? 1 : 2");
  }

  private static void parseExpression(String source) {
    parse(
      "class Example { void expression(int i) { String.valueOf(" + source + "); } }"
    );
  }

  private static StatementTree parseStatement(String source) {
    CompilationUnitTree compilationUnitTree = parse(
      "class Example { void example() { " + source + " } }"
    );
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    BlockTree blockTree = methodTree.block();
    assertNotNull(blockTree);
    return blockTree.body().get(0);
  }

  private static CompilationUnitTree parse(String source) {
    return (CompilationUnitTree) EcjParser.parse(source);

//    ASTParser astParser = ASTParser.newParser(AST.JLS11);
//    astParser.setResolveBindings(true);
//    astParser.setEnvironment(
//      new String[]{},
//      new String[]{},
//      new String[]{},
//      true
//    );
//    astParser.setUnitName("Example.java");
//
//    char[] sourceChars = source.toCharArray();
//    astParser.setSource(sourceChars);
//
//    CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);
//
//    List<String> problems = Arrays.stream(compilationUnit.getProblems())
//      .map(IProblem::getMessage)
//      .filter(msg -> !msg.equals("Dead code"))
//      .collect(Collectors.toList());
//    assertThat(problems).isEmpty();
//
//    return (CompilationUnitTree) new EcjParser().convert(compilationUnit);
  }

}
