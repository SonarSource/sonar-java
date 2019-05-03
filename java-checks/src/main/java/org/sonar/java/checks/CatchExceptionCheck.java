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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S2221")
public class CatchExceptionCheck extends IssuableSubscriptionVisitor {

  private final ThrowsExceptionVisitor throwsExceptionVisitor = new ThrowsExceptionVisitor();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatement = (TryStatementTree) tree;
    if (!tryStatement.resourceList().isEmpty()) {
      // classes implementing AutoCloseable interface implements 'close()' methods which throws Exception
      return;
    }
    for (CatchTree catchTree : tryStatement.catches()) {
      TypeTree catchType = catchTree.parameter().type();
      if (catchesException(catchType, tryStatement.block())) {
        reportIssue(catchType, "Catch a list of specific exception subtypes instead.");
      }
    }
  }

  private boolean catchesException(TypeTree catchType, BlockTree block) {
    if (catchType.is(Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) catchType;
      for (TypeTree typeAlternative : unionTypeTree.typeAlternatives()) {
        if (catchesExceptionCheck(block, typeAlternative)) {
          return true;
        }
      }
    } else if (catchesExceptionCheck(block, catchType)) {
      return true;
    }
    return false;
  }

  private boolean catchesExceptionCheck(BlockTree block, TypeTree catchType) {
    return isJavaLangException(catchType.symbolType()) && !throwsExceptionVisitor.containsExplicitThrowsException(block);
  }

  private static boolean isJavaLangException(Type type) {
    return type.is("java.lang.Exception");
  }

  private static class ThrowsExceptionVisitor extends BaseTreeVisitor {
    private boolean containsExplicitThrowsException;

    boolean containsExplicitThrowsException(Tree tree) {
      containsExplicitThrowsException = false;
      tree.accept(this);
      return containsExplicitThrowsException;
    }

    @Override
    protected void scan(@Nullable Tree tree) {
      if(containsExplicitThrowsException) {
        return;
      }
      super.scan(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isThrowingJavaLangException(tree.symbol())) {
        return;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (isThrowingJavaLangException(tree.constructorSymbol())) {
        return;
      }
      super.visitNewClass(tree);
    }

    private boolean isThrowingJavaLangException(Symbol symbol) {
      containsExplicitThrowsException |= symbol.isUnknown() || ((Symbol.MethodSymbol) symbol).thrownTypes().stream().anyMatch(CatchExceptionCheck::isJavaLangException);
      return containsExplicitThrowsException;
    }
  }

}
