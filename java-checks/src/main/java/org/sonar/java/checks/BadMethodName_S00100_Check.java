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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Rule(
    key = "S00100",
    priority = Priority.MAJOR,
    tags = {"convention"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class BadMethodName_S00100_Check extends SubscriptionBaseVisitor {

  private static final String DEFAULT_FORMAT = "^[a-z][a-zA-Z0-9]*$";

  @RuleProperty(
      key = "format",
      defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    super.scanFile(context);
  }


  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isNotOverriden(methodTree) && !pattern.matcher(methodTree.simpleName().name()).matches()) {
      addIssue(tree, "Rename this method name to match the regular expression '" + format + "'.");
    }
  }


  private boolean isNotOverriden(MethodTree methodTree) {
    //Static method are necessarily not overriden, no need to check.
    return isStatic(methodTree) || isPrivate(methodTree) || BooleanUtils.isFalse(isOverriden(methodTree));
  }

  private boolean isPrivate(MethodTree methodTree) {
    return methodTree.modifiers().modifiers().contains(Modifier.PRIVATE);
  }

  private boolean isStatic(MethodTree methodTree) {
    return methodTree.modifiers().modifiers().contains(Modifier.STATIC);
  }

  /**
   * Check if a methodTree is overriden.
   *
   * @param methodTree the methodTree to check.
   * @return true if overriden, null if some super types are unknown.
   */
  private Boolean isOverriden(MethodTree methodTree) {
    if (isAnnotatedOverride(methodTree)) {
      return true;
    }
    Boolean result = null;
    if (hasSemantic()) {
      result = false;
      Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) methodTree).getSymbol();

      Symbol.TypeSymbol enclosingClass = methodSymbol.enclosingClass();
      if (StringUtils.isEmpty(enclosingClass.getName())) {
        //FIXME : SONARJAVA-645 : exclude methods within anonymous classes
        return null;
      }
      for (Type.ClassType type : superTypes(enclosingClass)) {
        Boolean overrideFromType = overrideMethodFromSymbol(methodSymbol, type);
        if (overrideFromType == null) {
          result = null;
        } else if (BooleanUtils.isTrue(overrideFromType)) {
          return true;
        }
      }
    }
    return result;
  }

  private boolean isAnnotatedOverride(MethodTree tree) {
    for (AnnotationTree annotationTree : tree.modifiers().annotations()) {
      if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) annotationTree.annotationType();
        if (Override.class.getSimpleName().equals(identifier.name())) {
          return true;
        }
      }
    }
    return false;
  }

  private Set<Type.ClassType> superTypes(Symbol.TypeSymbol enclosingClass) {
    ImmutableSet.Builder<Type.ClassType> types = ImmutableSet.builder();
    Type.ClassType superClassType = (Type.ClassType) enclosingClass.getSuperclass();
    types.addAll(interfacesOfType(enclosingClass));
    while (superClassType != null) {
      types.add(superClassType);
      Symbol.TypeSymbol superClassSymbol = superClassType.getSymbol();
      types.addAll(interfacesOfType(superClassSymbol));
      superClassType = (Type.ClassType) superClassSymbol.getSuperclass();
    }
    return types.build();
  }

  private Set<Type.ClassType> interfacesOfType(Symbol.TypeSymbol typeSymbol) {
    ImmutableSet.Builder<Type.ClassType> builder = ImmutableSet.builder();
    for (Type type : typeSymbol.getInterfaces()) {
      Type.ClassType classType = (Type.ClassType) type;
      builder.add(classType);
      builder.addAll(interfacesOfType(classType.getSymbol()));
    }
    return builder.build();
  }

  private Boolean overrideMethodFromSymbol(Symbol.MethodSymbol methodSymbol, Type.ClassType classType) {
    Boolean result = false;
    if (classType.isTagged(Type.UNKNOWN)) {
      return null;
    }
    List<Symbol> symbols = classType.getSymbol().members().lookup(methodSymbol.getName());
    for (Symbol symbol : symbols) {
      if (symbol.isKind(Symbol.MTH) && isOverridableBy((Symbol.MethodSymbol) symbol, methodSymbol)) {
        Boolean isOverriding = isOverriding(methodSymbol, (Symbol.MethodSymbol) symbol);
        if (isOverriding == null) {
          result = null;
        } else if (BooleanUtils.isTrue(isOverriding)) {
          return true;
        }
      }
    }
    return result;
  }

  /**
   * Methods have the same name and overidee is in a supertype of the enclosing class of overrider.
   */
  private boolean isOverridableBy(Symbol.MethodSymbol overridee, Symbol.MethodSymbol overrider) {
    if (overridee.isPackageVisibility()) {
      return overridee.outermostClass().owner().equals(overrider.outermostClass().owner());
    }
    return !overridee.isPrivate();
  }

  private Boolean isOverriding(Symbol.MethodSymbol overrider, Symbol.MethodSymbol overridee) {
    //same number and type of formal parameters
    if (overrider.getParametersTypes().size() != overridee.getParametersTypes().size()) {
      return false;
    }
    for (int i = 0; i < overrider.getParametersTypes().size(); i++) {
      Type paramOverrider = overrider.getParametersTypes().get(i);
      if (paramOverrider.isTagged(Type.UNKNOWN)) {
        //FIXME : complete symbol table should not have unknown types.
        return null;
      }
      if (!paramOverrider.equals(overridee.getParametersTypes().get(i))) {
        return false;
      }
    }
    //we assume code is compiling so no need to check return type at this point.
    return true;
  }
}
