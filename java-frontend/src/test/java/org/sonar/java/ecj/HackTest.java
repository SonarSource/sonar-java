package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HackTest {

  private final AST ast = createAST();

  @Test
  public void test() {
    assertThat(Hack.resolveType(ast, "int"))
      .isNotNull();

    assertThat(Hack.resolveType(ast, "java.lang.IllegalStateException"))
      .isNotNull();

    assertThat(Hack.resolveType(ast, "java.util.Map$Entry"))
      .isNotNull();

    assertThat(Hack.resolveType(ast, "java.lang.Object[]"))
      .isNotNull();

    // TODO assert that not recovered?
  }

  private static AST createAST() {
    ASTParser parser = ASTParser.newParser(AST.JLS11);
    parser.setResolveBindings(true);
    parser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    parser.setSource("".toCharArray());
    parser.setUnitName("Example.java");
    return parser.createAST(null).getAST();
  }

}
