/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1220")
public class DefaultPackageCheck implements JavaFileScanner {

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.fileParsed()) {
      CompilationUnitTree cut = context.getTree();
      if (cut.moduleDeclaration() == null && cut.packageDeclaration() == null && !isCompactSource(cut)) {
        context.addIssueOnFile(this, "Move this file to a named package.");
      }
    }
  }

  private static boolean isCompactSource(CompilationUnitTree cu) {
    List<Tree> types = cu.types();
    return types.size() == 1 && types.get(0).is(Tree.Kind.IMPLICIT_CLASS);
  }
}
