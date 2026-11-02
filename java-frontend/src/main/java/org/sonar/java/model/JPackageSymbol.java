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

import com.intellij.psi.PsiPackage;
import org.sonar.plugins.java.api.semantic.Symbol;

final class JPackageSymbol extends JSymbol {
  private final PsiPackage pkg;

  JPackageSymbol(JSema sema, PsiPackage pkg, String key) {
    super(sema, pkg, key);
    this.pkg = pkg;
  }

  @Override
  public String name() {
    return sema.read(pkg::getQualifiedName);
  }

  @Override
  public Symbol owner() {
    return Symbol.ROOT_PACKAGE;
  }

  @Override
  public Symbol.TypeSymbol enclosingClass() {
    return null;
  }
}
