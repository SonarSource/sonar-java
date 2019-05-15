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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4510")
public class XmlDeserializationCheck extends AbstractMethodDetection {

  private static final MethodMatcher READ_OBJECT = MethodMatcher.create().typeDefinition("java.beans.XMLDecoder")
    .name("readObject").withAnyParameters();
  private static final String MESSAGE = "Make sure deserializing with XMLDecoder is safe here.";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create().typeDefinition("java.beans.XMLDecoder").name("<init>").withAnyParameters()
    );
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    List<JavaFileScannerContext.Location> secondaries = collectSecondaryLocations(newClassTree);
    reportIssue(newClassTree.identifier(), MESSAGE, secondaries, null);
  }

  private static List<JavaFileScannerContext.Location> collectSecondaryLocations(NewClassTree newClassTree) {
    Tree parentMethodOrClass = parentMethod(newClassTree);
    if (parentMethodOrClass == null) {
      return Collections.emptyList();
    }
    List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    parentMethodOrClass.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        if (READ_OBJECT.matches(tree)) {
          secondaries.add(new JavaFileScannerContext.Location("Possible data execution", tree));
        }
      }
    });
    return secondaries;
  }

  @CheckForNull
  private static Tree parentMethod(NewClassTree newClassTree) {
    Tree parent = newClassTree.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD, Tree.Kind.CLASS)) {
      parent = parent.parent();
    }
    return parent;
  }

}
