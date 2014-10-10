/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;


@Rule(
    key = UnusedMethodParameterCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class UnusedMethodParameterCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1172";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    this.semanticModel = (SemanticModel) context.getSemanticModel();
    if (semanticModel != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    if (tree.block() != null && !isExcluded(tree)) {
      List<String> unused = Lists.newArrayList();
      for (VariableTree var : tree.parameters()) {
        Symbol sym = semanticModel.getSymbol(var);
        if (sym != null && semanticModel.getUsages(sym).isEmpty()) {
          unused.add(var.simpleName().name());
        }
      }
      if (!unused.isEmpty()) {
        context.addIssue(tree, ruleKey, "Remove the unused method parameter(s) \"" + Joiner.on(",").join(unused) + "\".");
      }
    }
  }

  private boolean isExcluded(MethodTree tree) {
    return ((MethodTreeImpl) tree).isMainMethod() || isOverriding(tree) || isSerializableMethod(tree) || isDesignedForExtension(tree);
  }

  private boolean isDesignedForExtension(MethodTree tree) {
    return !tree.modifiers().modifiers().contains(Modifier.PRIVATE) && isEmptyOrThrowStatement(tree.block());
  }

  private boolean isEmptyOrThrowStatement(BlockTree block) {
    return block.body().isEmpty() || (block.body().size()==1 && block.body().get(0).is(Tree.Kind.THROW_STATEMENT));
  }

  private boolean isSerializableMethod(MethodTree methodTree) {
    boolean result = false;
    //FIXME detect methods based on type of arg and throws, not arity.
    if (methodTree.modifiers().modifiers().contains(Modifier.PRIVATE) && methodTree.parameters().size() == 1) {
      result |= "writeObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 1;
      result |= "readObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 2;
    }
    return result;
  }

  private boolean isOverriding(MethodTree tree) {
    //if overriding cannot be determined, we consider it is overriding to avoid FP.
    return !BooleanUtils.isFalse(((MethodTreeImpl) tree).isOverriding());
  }
}
