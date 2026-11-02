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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

abstract class JSymbol implements Symbol {
  final JSema sema;
  final PsiElement element;
  final String key;
  private SymbolMetadata metadata;

  JSymbol(JSema sema, PsiElement element, String key) {
    this.sema = sema;
    this.element = element;
    this.key = key;
  }

  boolean modifier(String name) {
    return sema.read(() -> element instanceof PsiModifierListOwner owner && owner.hasModifierProperty(name));
  }

  @Override
  public String name() {
    return sema.read(() -> element instanceof PsiNamedElement named ? named.getName() : "");
  }

  @Override
  public Symbol owner() {
    return sema.read(() -> sema.owner(element));
  }

  @Override
  public Type type() {
    return Type.UNKNOWN;
  }

  @Override
  public boolean isVariableSymbol() {
    return this instanceof Symbol.VariableSymbol;
  }

  @Override
  public boolean isTypeSymbol() {
    return this instanceof Symbol.TypeSymbol && !isUnknown();
  }

  @Override
  public boolean isMethodSymbol() {
    return this instanceof Symbol.MethodSymbol;
  }

  @Override
  public boolean isPackageSymbol() {
    return element instanceof PsiPackage;
  }

  @Override
  public boolean isStatic() {
    return modifier(PsiModifier.STATIC);
  }

  @Override
  public boolean isFinal() {
    return modifier(PsiModifier.FINAL);
  }

  @Override
  public boolean isEnum() {
    return sema.read(() -> element instanceof PsiClass cls && cls.isEnum() || element instanceof PsiEnumConstant);
  }

  @Override
  public boolean isInterface() {
    return sema.read(() -> element instanceof PsiClass cls && cls.isInterface() && !cls.isAnnotationType());
  }

  @Override
  public boolean isAbstract() {
    return modifier(PsiModifier.ABSTRACT);
  }

  @Override
  public boolean isPublic() {
    return modifier(PsiModifier.PUBLIC);
  }

  @Override
  public boolean isPrivate() {
    return modifier(PsiModifier.PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return modifier(PsiModifier.PROTECTED);
  }

  @Override
  public boolean isPackageVisibility() {
    return !isPublic() && !isPrivate() && !isProtected();
  }

  @Override public boolean isDeprecated() {
    return sema.read(() -> element instanceof PsiModifierListOwner owner && owner.getModifierList() != null
      && owner.getModifierList().findAnnotation("java.lang.Deprecated") != null
      || element instanceof PsiDocCommentOwner documented && documented.getDocComment() != null && documented.getDocComment().findTagByName("deprecated") != null);
  }

  @Override
  public boolean isVolatile() {
    return modifier(PsiModifier.VOLATILE);
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public SymbolMetadata metadata() {
    return sema.read(() -> {
      if (metadata == null) {
        metadata = JSymbolMetadata.of(sema, this, element);
      }
      return metadata;
    });
  }

  @Override
  public TypeSymbol enclosingClass() {
    return sema.read(() -> {
      var cls = element instanceof PsiClass c ? c : PsiTreeUtil.getParentOfType(element, PsiClass.class);
      return cls == null ? null : (TypeSymbol) sema.symbol(cls);
    });
  }

  @Override
  public List<IdentifierTree> usages() {
    return sema.read(() -> sema.usages.getOrDefault(sema.key(element), List.of()));
  }

  @Override
  public Tree declaration() {
    return sema.read(() -> sema.declarations.get(sema.key(element)));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof JSymbol symbol && getClass() == symbol.getClass() && key.equals(symbol.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return name();
  }
}
