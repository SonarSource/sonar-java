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
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Defines scopes and symbols.
 */
public class FirstPass extends BaseTreeVisitor {

  private static final String CONSTRUCTOR_NAME = "<init>";
  private final SemanticModel semanticModel;

  private final List<JavaSymbol> uncompleted = new ArrayList<>();
  private final SecondPass completer;
  private final Symbols symbols;
  private final ParametrizedTypeCache parametrizedTypeCache;
  private Resolve resolve;

  /**
   * Environment.
   * {@code env.scope.symbol} - enclosing symbol.
   */
  private Resolve.Env env;

  public FirstPass(SemanticModel semanticModel, Symbols symbols, Resolve resolve, ParametrizedTypeCache parametrizedTypeCache, TypeAndReferenceSolver typeAndReferenceSolver) {
    this.semanticModel = semanticModel;
    this.resolve = resolve;
    this.completer = new SecondPass(semanticModel, symbols, parametrizedTypeCache, typeAndReferenceSolver);
    this.symbols = symbols;
    this.parametrizedTypeCache = parametrizedTypeCache;
  }

  private void restoreEnvironment(Tree tree) {
    if (env.next == null) {
      // Invariant: env.next == null for CompilationUnit
      Preconditions.checkState(tree.is(Tree.Kind.COMPILATION_UNIT));
    } else {
      env = env.next;
    }
  }

  public void completeSymbols() {
    for (JavaSymbol symbol : uncompleted) {
      symbol.complete();
    }
    uncompleted.clear();
  }


  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    JavaSymbol.PackageJavaSymbol compilationUnitPackage = symbols.defaultPackage;

    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration != null) {
      ExpressionTree packageName = packageDeclaration.packageName();
      PackageResolverVisitor packageResolver = new PackageResolverVisitor();
      packageName.accept(packageResolver);
      compilationUnitPackage = (JavaSymbol.PackageJavaSymbol) resolve.findIdentInPackage(compilationUnitPackage, packageResolver.packageName, JavaSymbol.PCK);
      semanticModel.associateSymbol(packageName, compilationUnitPackage);
    }
    compilationUnitPackage.members = new Scope(compilationUnitPackage);

    env = new Resolve.Env();
    env.packge = compilationUnitPackage;
    env.scope = compilationUnitPackage.members;
    env.namedImports = new Scope.ImportScope(compilationUnitPackage);
    env.starImports = resolve.createStarImportScope(compilationUnitPackage);
    env.staticStarImports = resolve.createStaticStarImportScope(compilationUnitPackage);
    semanticModel.associateEnv(tree, env);

    scan(tree.types());
    restoreEnvironment(tree);
    resolveImports(tree.imports());
    completeSymbols();
  }

  private static class PackageResolverVisitor extends BaseTreeVisitor {
    private String packageName;

    public PackageResolverVisitor() {
      packageName = "";
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (!packageName.isEmpty()) {
        packageName += ".";
      }
      packageName += tree.name();
    }
  }

  private void resolveImports(List<ImportClauseTree> imports) {
    ImportResolverVisitor importResolverVisitor = new ImportResolverVisitor();
    for (ImportClauseTree importClauseTree : imports) {
      importClauseTree.accept(importResolverVisitor);
    }
  }

  private class ImportResolverVisitor extends BaseTreeVisitor {
    private JavaSymbol currentSymbol;
    private List<JavaSymbol> resolved;
    private boolean isStatic;

    @Override
    public void visitImport(ImportTree tree) {
      //reset currentSymbol to default package
      currentSymbol = symbols.defaultPackage;
      isStatic = tree.isStatic();
      tree.qualifiedIdentifier().accept(this);
      //Associate symbol only if found.
      if (currentSymbol.kind < JavaSymbol.ERRONEOUS) {
        enterSymbol(currentSymbol, tree);
      } else if (isStatic) {
        resolved.stream()
          //add only static fields
          //TODO accessibility should be checked : package/public
          .filter(symbol -> Flags.isFlagged(symbol.flags, Flags.STATIC))
          //TODO only the first symbol found will be associated with the tree.
          .forEach(symbol -> enterSymbol(symbol, tree));
      }
    }

    private void enterSymbol(JavaSymbol symbol, ImportTree tree) {
      env.namedImports.enter(symbol);
      //FIXME We add all symbols to named Imports for static methods, but only the first one will be resolved as we don't handle arguments.
      //FIXME That is why we only add the first symbol so we resolve references at best for now.
      //add to semantic model only the first symbol.
      //twice the same import : ignore the duplication JLS8 7.5.1.
      if (semanticModel.getSymbol(tree) == null) {
        semanticModel.associateSymbol(tree, symbol);
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (JavaPunctuator.STAR.getValue().equals(tree.name())) {
        //star import : we save the current symbol
        if (isStatic) {
          env.staticStarImports.enter(currentSymbol);
        } else {
          env.starImports.enter(currentSymbol);
        }
        //we set current symbol to not found to do not put it in named import scope.
        currentSymbol = new Resolve.JavaSymbolNotFound();
      } else {
        if (currentSymbol.kind == JavaSymbol.PCK) {
          currentSymbol = resolve.findIdentInPackage(currentSymbol, tree.name(), JavaSymbol.PCK | JavaSymbol.TYP);
          resolved = Collections.emptyList();
        } else if (currentSymbol.kind == JavaSymbol.TYP) {
          resolved = ((JavaSymbol.TypeJavaSymbol) currentSymbol).members().lookup(tree.name());
          currentSymbol = resolve.findIdentInType(env, (JavaSymbol.TypeJavaSymbol) currentSymbol, tree.name(), JavaSymbol.TYP | JavaSymbol.VAR).symbol();
        } else {
          //Site symbol is not found so we won't be able to resolve the import.
          currentSymbol = new Resolve.JavaSymbolNotFound();
          resolved = Collections.emptyList();
        }
      }
    }

  }

  @Override
  public void visitClass(ClassTree tree) {
    int flag = 0;
    boolean anonymousClass = tree.simpleName() == null;
    String name = "";
    if (!anonymousClass) {
      name = tree.simpleName().name();
      flag = computeClassFlags(tree);
    }
    JavaSymbol.TypeJavaSymbol symbol = new JavaSymbol.TypeJavaSymbol(flag, name, env.scope.owner);
    symbol.declaration = tree;
    ((ClassTreeImpl) tree).setSymbol(symbol);
    //Only register classes that can be accessible, so classes owned by a method are not registered.
    //TODO : register also based on flags ?
    if (!anonymousClass) {
      if (env.scope.owner.kind == JavaSymbol.TYP || env.scope.owner.kind == JavaSymbol.PCK) {
        resolve.registerClass(symbol);
      }
      enterSymbol(tree, symbol);
    }
    symbol.members = new Scope(symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    //Define type parameters:
    TypeParameters typeParameterTrees = tree.typeParameters();
    createNewEnvironment(typeParameterTrees);
    // Save current environment to be able to complete class later
    semanticModel.saveEnv(symbol, env);
    for (TypeParameterTree typeParameterTree : typeParameterTrees) {
      JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol(typeParameterTree.identifier().name(), symbol);
      symbol.addTypeParameter((TypeVariableJavaType) typeVariableSymbol.type);
      enterSymbol(typeParameterTree, typeVariableSymbol);
    }
    if(!typeParameterTrees.isEmpty()) {
      symbol.type = parametrizedTypeCache.getParametrizedTypeType(symbol, new TypeSubstitution());
    }
    symbol.typeParameters = env.scope;
    Resolve.Env classEnv = env.dup();
    classEnv.outer = env;
    classEnv.enclosingClass = symbol;
    classEnv.scope = symbol.members;
    env = classEnv;
    semanticModel.associateEnv(tree, env);
    scan(tree.modifiers());
    //skip type parameters
    scan(tree.superClass());
    scan(tree.superInterfaces());
    scan(tree.members());

    if (tree.is(Tree.Kind.ENUM)) {
      // implicit methods from enum: JLS8 : 8.9.2
      // add 'public static E[] values()'
      JavaSymbol.MethodJavaSymbol valuesMethod = new JavaSymbol.MethodJavaSymbol((symbol.flags & Flags.ACCESS_FLAGS) | Flags.STATIC, "values", symbol);
      ArrayJavaType enumArrayType = new ArrayJavaType(symbol.type, symbols.arrayClass);
      MethodJavaType valuesMethodType = new MethodJavaType(Collections.emptyList(), enumArrayType, Collections.emptyList(), symbol);
      valuesMethod.setMethodType(valuesMethodType);
      valuesMethod.parameters = new Scope(valuesMethod);
      classEnv.scope.enter(valuesMethod);

      // add 'public static E valueOf(String name)'
      JavaSymbol.MethodJavaSymbol valueOfMethod = new JavaSymbol.MethodJavaSymbol((symbol.flags & Flags.ACCESS_FLAGS) | Flags.STATIC, "valueOf", symbol);
      MethodJavaType valueOfMethodType = new MethodJavaType(Collections.singletonList(symbols.stringType), symbol.type, Collections.emptyList(), symbol);
      valueOfMethod.setMethodType(valueOfMethodType);
      valueOfMethod.parameters = new Scope(valueOfMethod);
      valueOfMethod.parameters.enter(new JavaSymbol.VariableJavaSymbol(0, "name", symbols.stringType, valueOfMethod));
      classEnv.scope.enter(valueOfMethod);
    }
    restoreEnvironment(tree);
    restoreEnvironment(tree);
  }

  private int computeClassFlags(ClassTree tree) {
    int flags = computeFlags(tree.modifiers(), tree);
    if (tree.is(Tree.Kind.INTERFACE)) {
      flags |= Flags.INTERFACE;
    }else if (tree.is(Tree.Kind.ENUM)) {
      flags |= Flags.ENUM;
    }else if (tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      flags |= Flags.INTERFACE | Flags.ANNOTATION;
    }
    return flags;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    String name = tree.returnType() == null ? CONSTRUCTOR_NAME : tree.simpleName().name();
    JavaSymbol.MethodJavaSymbol symbol = new JavaSymbol.MethodJavaSymbol(computeFlags(tree.modifiers(), tree), name, env.scope.owner);
    symbol.declaration = tree;
    if (Flags.isFlagged(env.scope.owner.flags, Flags.ENUM) && tree.returnType() == null) {
      //enum constructors are private.
      symbol.flags |= Flags.PRIVATE;
    }
    enterSymbol(tree, symbol);
    symbol.parameters = new Scope(symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    ((MethodTreeImpl) tree).setSymbol(symbol);
    createNewEnvironment(tree.typeParameters());
    for (TypeParameterTree typeParameterTree : tree.typeParameters()) {
      JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol(typeParameterTree.identifier().name(), symbol);
      symbol.addTypeParameter((TypeVariableJavaType) typeVariableSymbol.type);
      enterSymbol(typeParameterTree, typeVariableSymbol);
    }
    // Save current environment to be able to complete method later
    semanticModel.saveEnv(symbol, env);

    symbol.typeParameters = env.scope;
    // Create new environment - this is required, because new scope is created
    Resolve.Env methodEnv = env.dup();
    methodEnv.scope = symbol.parameters;
    methodEnv.outer = env;
    env = methodEnv;
    scan(tree.modifiers());
    //skip type parameters.
    scan(tree.returnType());
    scan(tree.parameters());
    scan(tree.throwsClauses());
    scan(tree.defaultValue());
    symbol.defaultValue = getDefaultValueFromTree(tree.defaultValue());
    scan(tree.block());
    restoreEnvironment(tree);
    restoreEnvironment(tree);
  }

  @Nullable
  private static Object getDefaultValueFromTree(@Nullable ExpressionTree expressionTree) {
    if(expressionTree == null) {
      return null;
    }
    if (expressionTree.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value());
    }
    if (expressionTree.is(Tree.Kind.INT_LITERAL)) {
      return LiteralUtils.intLiteralValue(expressionTree);
    }
    if (expressionTree.is(Tree.Kind.LONG_LITERAL)) {
      return LiteralUtils.longLiteralValue(expressionTree);
    }
    return null;
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    // JLS-8.9.3
    int flags = Flags.PUBLIC | Flags.STATIC | Flags.FINAL | Flags.ENUM;
    if (hasDeprecatedAnnotation(tree.modifiers().annotations())) {
      flags |= Flags.DEPRECATED;
    }
    declareVariable(flags, tree.simpleName(), (VariableTreeImpl) tree);
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    declareVariable(computeFlags(tree.modifiers(), tree), tree.simpleName(), (VariableTreeImpl) tree);
    super.visitVariable(tree);
  }

  private int computeFlags(ModifiersTree modifiers, Tree tree) {
    int result = 0;
    if (Flags.isFlagged(env.scope.owner.flags, Flags.INTERFACE)) {
      result = computeFlagsForInterfaceMember(tree);
    }
    for (ModifierKeywordTree modifier : modifiers.modifiers()) {
      result |= Flags.flagForModifier(modifier.modifier());
    }
    if(hasDeprecatedAnnotation(modifiers.annotations())) {
      result |= Flags.DEPRECATED;
    }
    return result;
  }

  private static int computeFlagsForInterfaceMember(Tree tree) {
    int result;
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      // JLS9 9.4 A method in the body of an interface may be declared public or private
      if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE)) {
        result = Flags.PRIVATE;
      } else {
        result = Flags.PUBLIC;
      }
      if (methodTree.block() == null) {
        // JLS8 9.4: methods lacking a block are implicitly abstract
        result |= Flags.ABSTRACT;
      }
    } else {
      // JLS7 9.5: member type declarations are implicitly static and public
      result = Flags.PUBLIC | Flags.STATIC;
      if (tree.is(Tree.Kind.VARIABLE)) {
        // JLS7 9.3: fields are implicitly public, static and final
        result |= Flags.FINAL;
      }
    }
    return result;
  }

  private static boolean hasDeprecatedAnnotation(Iterable<AnnotationTree> annotations) {
    for (AnnotationTree annotationTree : annotations) {
      if (isDeprecated(annotationTree)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Tree.Kind.IDENTIFIER) &&
        "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

  private void declareVariable(int flags, IdentifierTree identifierTree, VariableTreeImpl tree) {
    String name = identifierTree.name();
    Object constantValue = semanticModel.constantValue(env.scope.owner, name);
    JavaSymbol.VariableJavaSymbol symbol = new JavaSymbol.VariableJavaSymbol(flags, name, env.scope.owner, constantValue);
    symbol.declaration = tree;
    enterSymbol(tree, symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    tree.setSymbol(symbol);

    // Save current environment to be able to complete variable declaration later
    semanticModel.saveEnv(symbol, env);
  }

  @Override
  public void visitBlock(BlockTree tree) {
    // Create new environment - this is required, because block can declare types
    createNewEnvironment(tree);
    super.visitBlock(tree);
    restoreEnvironment(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    if (!declaresResourceAsVariable(tree.resourceList())) {
      super.visitTryStatement(tree);
    } else {
      //Declare scope for resources
      createNewEnvironment(tree.resourceList());
      scan(tree.resourceList());
      scan(tree.block());
      restoreEnvironment(tree.resourceList());
      scan(tree.catches());
      scan(tree.finallyBlock());
    }
  }

  private static boolean declaresResourceAsVariable(ListTree<Tree> resources) {
    return resources.stream().anyMatch(r -> r.is(Tree.Kind.VARIABLE));
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    // Create new environment - this is required, because new scope is created
    createNewEnvironment(tree);
    super.visitForStatement(tree);
    restoreEnvironment(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    // Create new environment - this is required, because new scope is created
    createNewEnvironment(tree);
    super.visitForEachStatement(tree);
    restoreEnvironment(tree);
  }

  @Override
  public void visitCatch(CatchTree tree) {
    // Create new environment - this is required, because new scope is created
    createNewEnvironment(tree);
    super.visitCatch(tree);
    restoreEnvironment(tree);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    // Create new environment - this is required, because new scope is created
    createNewEnvironment(tree);
    super.visitLambdaExpression(tree);
    restoreEnvironment(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    // Create new environment - this is required, because new scope is created
    createNewEnvironment(tree);
    super.visitSwitchStatement(tree);
    restoreEnvironment(tree);
  }

  private void createNewEnvironment(Tree tree) {
    Scope scope = new Scope(env.scope);
    Resolve.Env newEnv = env.dup();
    newEnv.scope = scope;
    env = newEnv;
    semanticModel.associateEnv(tree, env);
  }

  private void enterSymbol(Tree tree, JavaSymbol symbol) {
    env.scope.enter(symbol);
    semanticModel.associateSymbol(tree, symbol);
  }

}
