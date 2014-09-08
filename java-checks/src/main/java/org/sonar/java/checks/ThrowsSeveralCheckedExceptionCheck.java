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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

@Rule(key = "S1160", priority = Priority.MAJOR, tags = {"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ThrowsSeveralCheckedExceptionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isPublic(methodTree) && !isPublicStaticVoidMain(methodTree)) {
      List<String> thrownCheckedExceptions = getThrownCheckedExceptions(methodTree);
      if (thrownCheckedExceptions.size() > 1 && isNotOverriden(methodTree)) {
        addIssue(methodTree, "Refactor this method to throw at most one checked exception instead of: " + Joiner.on(", ").join(thrownCheckedExceptions));
      }
    }
  }

  private boolean isPublicStaticVoidMain(MethodTree methodTree) {
    return "main".equals(methodTree.simpleName().name()) && hasStringArrayParam(methodTree) && returnsVoid(methodTree) && isStatic(methodTree);
  }

  private boolean hasStringArrayParam(MethodTree methodTree) {
    if(methodTree.parameters().size()==1){
      Tree argType = methodTree.parameters().get(0).type();
      if(argType.is(Tree.Kind.ARRAY_TYPE) && ((ArrayTypeTree) argType).type().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) ((ArrayTypeTree) argType).type();
        return "String".equals(identifierTree.name()) || "java.lang.String".equals(identifierTree.name());
      }
    }
    return false;
  }

  private boolean returnsVoid(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    if(returnType != null) {
      return returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    }
    return false;
  }

  private boolean isNotOverriden(MethodTree methodTree) {
    //Static method are necessarily not overriden, no need to check.
    return isStatic(methodTree) || BooleanUtils.isFalse(isOverriden(methodTree));
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
    Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) methodTree).getSymbol();

    Boolean result = false;
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
    return result;
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

  private boolean isAnnotatedOverride(MethodTree methodTree) {
    for (AnnotationTree annotationTree : methodTree.modifiers().annotations()) {
      if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) annotationTree.annotationType();
        if (Override.class.getSimpleName().equals(identifier.name())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isPublic(MethodTree methodTree) {
    return ((MethodTreeImpl) methodTree).getSymbol().isPublic();
  }

  private List<String> getThrownCheckedExceptions(MethodTree methodTree) {
    List<Symbol.TypeSymbol> thrownClasses = ((MethodTreeImpl) methodTree).getSymbol().getThrownTypes();
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (Symbol.TypeSymbol thrownClass : thrownClasses) {
      if (!isSubClassOfRuntimeException(thrownClass)) {
        builder.add(thrownClass.owner().getName() + "." + thrownClass.getName());
      }
    }
    return builder.build();
  }

  private static boolean isSubClassOfRuntimeException(Symbol.TypeSymbol thrownClass) {
    Symbol.TypeSymbol typeSymbol = thrownClass;
    while (typeSymbol != null) {
      if (isRuntimeException(typeSymbol)) {
        return true;
      }
      Type superType = typeSymbol.getSuperclass();
      if (superType == null) {
        typeSymbol = null;
      } else {
        typeSymbol = ((Type.ClassType) superType).getSymbol();
      }
    }
    return false;
  }

  private static boolean isRuntimeException(Symbol.TypeSymbol thrownClass) {
    return "RuntimeException".equals(thrownClass.getName()) && "java.lang".equals(thrownClass.owner().getName());
  }

}
