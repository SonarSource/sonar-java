/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "UnusedPrivateMethod",
  name = "Unused private method should be removed",
  tags = {"unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedPrivateMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

    private JavaFileScannerContext context;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        if (context.getSemanticModel() != null) {
            scan(context.getTree());
        }
    }

    @Override
    public void visitClass(ClassTree tree) {
        if (tree.is(Tree.Kind.ENUM)) {
            for (Tree member : tree.members()) {
                if (member.is(Tree.Kind.CONSTRUCTOR)) {
                    checkConstructorNotUsed((MethodTree) member);
                }
            }
        }
        super.visitClass(tree);
    }

    @Override
    public void visitMethod(MethodTree tree) {
        checkMethodNotUsed(tree);
        super.visitMethod(tree);
    }

    private void checkMethodNotUsed(MethodTree tree) {
        if (isPrivateUnused(tree)) {
            if (tree.is(Tree.Kind.CONSTRUCTOR)) {
                checkConstructorNotUsed(tree);
            } else {
                if (!isExcludedFromCheck(tree)) {
                    String messageStr = "Private method '" + tree.simpleName().name() + "' is never used.";
                    context.addIssue(tree, this, messageStr);
                }
            }
        }
    }

    private void checkConstructorNotUsed(MethodTree tree) {
        Symbol.MethodSymbol methodSymbol = tree.symbol();
        if (methodSymbol != null && methodSymbol.usages().isEmpty()) {
            String messageStr = "Private constructor '" + tree.simpleName().name() + "(";
            List<String> params = Lists.newArrayList();
            for (Type paramType : methodSymbol.parameterTypes()) {
                params.add(paramType.toString());
            }
            messageStr += Joiner.on(",").join(params) + ")' is never used.";
            context.addIssue(tree, this, messageStr);
        }
    }

    private boolean isPrivateUnused(MethodTree tree) {
        Symbol symbol = tree.symbol();
        if (symbol != null && symbol.usages().isEmpty() && ModifiersUtils.hasModifier(tree.modifiers(), Modifier.PRIVATE)) {
            return true;
        }
        return false;
    }

    private boolean isExcludedFromCheck(MethodTree tree) {
        return SerializableContractMethodTree.methodMatch(tree) || hasExcludingAnnotation(tree);
    }

    private boolean hasExcludingAnnotation(MethodTree method) {
        for (AnnotationTree annotation : method.modifiers().annotations()) {
            Type annotationType = annotation.symbolType();
            if (annotationType == null) {
                return false;
            }
            if (annotationType.isUnknown() || annotationType.isArray()) {
                return false;
            }
            if (annotationType.is("javax.annotation.PostConstruct")
                    || annotationType.is("javax.annotation.PreDestroy")
                    || annotationType.is("javax.enterprise.inject.Produces")
                    || annotationType.is("javax.persistence.PostLoad")
                    || annotationType.is("javax.persistence.PrePersist")
                    || annotationType.is("javax.persistence.PostPersist")
                    || annotationType.is("javax.persistence.PreUpdate")
                    || annotationType.is("javax.persistence.PostUpdate")
                    || annotationType.is("javax.persistence.PreRemove")
                    || annotationType.is("javax.persistence.PostRemove")
                    || annotationType.is("javax.ejb.Remove")
                    || annotationType.is("javafx.fxml.FXML")) {
                return true;
            }

        }
        return false;
    }

}
