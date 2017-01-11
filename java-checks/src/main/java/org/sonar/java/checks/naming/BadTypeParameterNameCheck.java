/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

import java.util.List;
import java.util.regex.Pattern;

@Rule(key = "S00119")
@RspecKey("S119")
public class BadTypeParameterNameCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_FORMAT = "^[A-Z][0-9]?$";

  @RuleProperty(
      key = "format",
      description = "Regular expression used to check the type parameter names against.",
      defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;
  private Pattern pattern = null;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.TYPE_PARAMETER);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    pattern = Pattern.compile(format, Pattern.DOTALL);
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    IdentifierTree identifier = ((TypeParameterTree) tree).identifier();
    if (!pattern.matcher(identifier.name()).matches()) {
      reportIssue(identifier, "Rename this generic name to match the regular expression '" + format + "'.");
    }
  }
}
