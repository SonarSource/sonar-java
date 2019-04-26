package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link Hack}.
 */
public class HackTest {

  private final AST ast = createAST();

  @Test
  public void should_resolve_type_by_name() {
    assertThat(Hack.resolveType(ast, "int"))
      .isNotNull();

    assertThat(Hack.resolveType(ast, "java.lang.IllegalStateException"))
      .isNotNull();
    // unlike
    assertThat(ast.resolveWellKnownType("java.lang.IllegalStateException"))
      .isNull();

    assertThat(Hack.resolveType(ast, "java.util.Map$Entry"))
      .isNotNull();

    ITypeBinding resolved = Hack.resolveType(ast, "java.lang.Object[][]");
    assertThat(resolved).isNotNull();
    assertThat(resolved.getDimensions()).isEqualTo(2);

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
    parser.setUnitName("Test.java");
    return parser.createAST(null).getAST();
  }

}
