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
package org.sonar.java.model;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

final class PsiImportDiagnostics {
  private final PsiJavaFile file;
  private final Set<PsiElement> used = new HashSet<>();

  PsiImportDiagnostics(PsiJavaFile file) {
    this.file = file;
  }

  void reference(PsiJavaCodeReferenceElement reference) {
    if (reference.getQualifier() != null || PsiTreeUtil.getParentOfType(reference, PsiImportStatementBase.class) != null) {
      return;
    }
    var result = reference.advancedResolve(false);
    if (result.isValidResult() && result.getCurrentFileResolveScope() instanceof PsiImportStatementBase statement) {
      used.add(statement);
    }
  }

  void collect(BiConsumer<PsiElement, String> warning, Set<JProblem> problems) {
    if (file.getImportList() == null) {
      return;
    }
    var statements = file.getImportList().getAllImportStatements();
    boolean unresolved = false;
    for (var statement : statements) {
      if (statement.getImportReference() != null && statement.resolve() == null) {
        unresolved = true;
        var reference = statement.getImportReference();
        while (reference.getQualifier() instanceof PsiJavaCodeReferenceElement prefix && prefix.resolve() == null) {
          reference = prefix;
        }
        problems.add(new JProblem("The import " + reference.getQualifiedName() + " cannot be resolved", JProblem.Type.UNDEFINED_TYPE));
      }
    }
    if (unresolved || file.getModuleDeclaration() != null) {
      return;
    }
    var seen = new HashSet<String>();
    for (var statement : statements) {
      var reference = statement.getImportReference();
      if (statement instanceof com.intellij.psi.PsiImportModuleStatement module) {
        String name = module.getReferenceName();
        if ((!seen.add("module " + name) || !used.contains(module)) && module.getModuleReference() != null) {
          warning.accept(module.getModuleReference(), "The import " + name + " is never used");
        }
        continue;
      }
      if (reference == null) {
        continue;
      }
      String name = reference.getQualifiedName();
      String key = (statement instanceof PsiImportStaticStatement ? "static " : "") + name + statement.isOnDemand();
      boolean duplicate = !seen.add(key);
      if (!duplicate && ownDeclaration(statement)) {
        continue;
      }
      if (duplicate || redundantWildcard(statement) || !used.contains(statement)) {
        warning.accept(reference, "The import " + name + " is never used");
      }
    }
  }

  private boolean ownDeclaration(PsiImportStatementBase statement) {
    return statement instanceof PsiImportStatement && !statement.isOnDemand()
      && java.util.Arrays.asList(file.getClasses()).contains(statement.resolve());
  }

  private boolean redundantWildcard(PsiImportStatementBase statement) {
    return statement instanceof PsiImportStatement && statement.isOnDemand()
      && (statement.getImportReference().getQualifiedName().equals("java.lang") || statement.getImportReference().getQualifiedName().equals(file.getPackageName()));
  }
}
