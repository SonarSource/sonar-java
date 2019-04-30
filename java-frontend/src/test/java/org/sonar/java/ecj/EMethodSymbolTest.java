package org.sonar.java.ecj;

import org.junit.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

public class EMethodSymbolTest {

  @Test
  public void test() {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) EcjParser.parse("interface I { boolean equals(Object object); }");
    ClassTree cls = (ClassTree) compilationUnit.types().get(0);
    MethodTree method = (MethodTree) cls.members().get(0);

    assertThat(method.symbol().overriddenSymbol()).isNotNull();
  }

}
