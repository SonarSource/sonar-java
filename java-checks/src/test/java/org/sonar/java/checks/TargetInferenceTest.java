/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class TargetInferenceTest {

  @Test
  public void test() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/TargetInference.java", new SubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
      }

      @Override
      public void visitNode(Tree tree) {
        Symbol.MethodSymbol method = (Symbol.MethodSymbol) ((MethodInvocationTree) tree).symbol();
        Type hashSetA = ((Symbol.TypeSymbol) method.owner()).lookupSymbols("hashSetA").iterator().next().type();
        Type hashSetI = ((Symbol.TypeSymbol) method.owner()).lookupSymbols("hashSetI").iterator().next().type();
        switch (tree.firstToken().line()) {
          case 16:
            assertThat(method.name()).isEqualTo("newHashSet");
            assertThat(method.returnType().type()).isEqualTo(hashSetA);
            break;
          case 18:
            assertThat(method.name()).isEqualTo("useSet");
            assertThat(((MethodInvocationTree) tree).arguments().get(0).symbolType()).isEqualTo(hashSetA);
            break;
          case 19:
            if (method.name().equals("useSet")) {
              assertThat(((MethodInvocationTree) tree).arguments().get(0).symbolType()).isEqualTo(hashSetI);
            }
            if (method.name().equals("newHashSet")) {
              assertThat(method.returnType().type()).isEqualTo(hashSetI);
            }
            break;
        }
      }
    });
  }
}
