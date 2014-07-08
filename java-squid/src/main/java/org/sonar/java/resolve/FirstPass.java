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
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
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
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

/**
 * Defines scopes and symbols.
 */
public class FirstPass extends BaseTreeVisitor {

  private final SemanticModel semanticModel;

  private final List<Symbol> uncompleted = Lists.newArrayList();
  private final SecondPass completer;
  private final Symbols symbols;
  private Resolve resolve;

  /**
   * Environment.
   * {@code env.scope.symbol} - enclosing symbol.
   */
  private Resolve.Env env;

  public FirstPass(SemanticModel semanticModel, Symbols symbols, Resolve resolve) {
    this.semanticModel = semanticModel;
    this.resolve = resolve;
    this.completer = new SecondPass(semanticModel, resolve, symbols);
    this.symbols = symbols;
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
    for (Symbol symbol : uncompleted) {
      symbol.complete();
    }
    uncompleted.clear();
  }


  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    Symbol.PackageSymbol compilationUnitPackage = symbols.defaultPackage;

    ExpressionTree packageName = tree.packageName();
    if (packageName != null) {
      PackageResolverVisitor packageResolver = new PackageResolverVisitor();
      packageName.accept(packageResolver);
      compilationUnitPackage = (Symbol.PackageSymbol) resolve.findIdentInPackage(env, compilationUnitPackage, packageResolver.packageName, Symbol.PCK);
      semanticModel.associateSymbol(packageName, compilationUnitPackage);
    }
    compilationUnitPackage.members = new Scope(compilationUnitPackage);

    env = new Resolve.Env();
    env.packge = compilationUnitPackage;
    env.scope = compilationUnitPackage.members;
    env.namedImports = new Scope(compilationUnitPackage);
    env.starImports = resolve.createStarImportScope(compilationUnitPackage);
    env.staticStarImports = resolve.createStaticStarImportScope(compilationUnitPackage);
    semanticModel.associateEnv(tree, env);

    super.visitCompilationUnit(tree);
    restoreEnvironment(tree);
    resolveImports(tree.imports());
    completeSymbols();
  }

  private class PackageResolverVisitor extends BaseTreeVisitor {
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

  private void resolveImports(List<ImportTree> imports) {
    ImportResolverVisitor importResolverVisitor = new ImportResolverVisitor();
    for (ImportTree importTree : imports) {
      importTree.accept(importResolverVisitor);
    }
  }

  private class ImportResolverVisitor extends BaseTreeVisitor {
    private Symbol currentSymbol;
    private List<Symbol> resolved;
    private boolean isStatic;

    @Override
    public void visitImport(ImportTree tree) {
      //reset currentSymbol to default package
      currentSymbol = symbols.defaultPackage;
      isStatic = tree.isStatic();
      super.visitImport(tree);
      //Associate symbol only if found.
      if (currentSymbol.kind < Symbol.ERRONEOUS) {
        enterSymbol(currentSymbol, tree);
      } else {
        if (isStatic) {
          for (Symbol symbol : resolved) {
            //add only static fields
            //TODO accessibility should be checked : package/public
            if ((symbol.flags & Flags.STATIC) != 0) {
              //TODO only the first symbol found will be associated with the tree.
              enterSymbol(symbol, tree);
            }
          }
        }
      }
    }

    private void enterSymbol(Symbol symbol, ImportTree tree) {
      env.namedImports.enter(symbol);
      //FIXME We add all symbols to named Imports for static methods, but only the first one will be resolved as we don't handle arguments.
      //FIXME That is why we only add the first symbol so we resolve references at best for now.
      //add to semantic model only the first symbol.
      //twice the same import : ignore the duplication JLS8 7.5.1.
      if (semanticModel.getSymbol(tree) == null && semanticModel.getTree(symbol) == null) {
        semanticModel.associateSymbol(tree, symbol);
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (JavaPunctuator.STAR.getValue().equals(tree.name())) {
        //star import : we save the current symbol
        if (isStatic) {
          env.staticStarImports().enter(currentSymbol);
        } else {
          env.starImports().enter(currentSymbol);
        }
        //we set current symbol to not found to do not put it in named import scope.
        currentSymbol = new Resolve.SymbolNotFound();
      } else {
        if (currentSymbol.kind == Symbol.PCK) {
          currentSymbol = resolve.findIdentInPackage(env, currentSymbol, tree.name(), Symbol.PCK | Symbol.TYP);
          resolved = Collections.emptyList();
        } else if (currentSymbol.kind == Symbol.TYP) {
          resolved = ((Symbol.TypeSymbol) currentSymbol).members().lookup(tree.name());
          currentSymbol = resolve.findIdentInType(env, (Symbol.TypeSymbol) currentSymbol, tree.name(), Symbol.TYP | Symbol.VAR);
        } else {
          //Site symbol is not found so we won't be able to resolve the import.
          currentSymbol = new Resolve.SymbolNotFound();
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
    Symbol.TypeSymbol symbol = new Symbol.TypeSymbol(flag, name, env.scope.owner);
    ((ClassTreeImpl) tree).setSymbol(symbol);
    //Only register classes that can be accessible, so classes owned by a method are not registered.
    //TODO : register also based on flags ?
    if (!anonymousClass) {
      if (env.scope.owner.kind == Symbol.TYP || env.scope.owner.kind == Symbol.PCK) {
        resolve.registerClass(symbol);
      }
      enterSymbol(tree, symbol);
    }
    symbol.members = new Scope(symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    // Save current environment to be able to complete class later
    semanticModel.saveEnv(symbol, env);

    Resolve.Env classEnv = env.dup();
    classEnv.outer = env;
    classEnv.enclosingClass = symbol;
    classEnv.scope = symbol.members;
    env = classEnv;

    semanticModel.associateEnv(tree, env);
    super.visitClass(tree);
    restoreEnvironment(tree);
  }

  private int computeClassFlags(ClassTree tree) {
    int flags = computeFlags(tree.modifiers());
    if (tree.is(Tree.Kind.INTERFACE)) {
      flags |= Flags.INTERFACE;
    }else if (tree.is(Tree.Kind.ENUM)) {
      flags |= Flags.ENUM;
    }else if (tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      flags |= Flags.INTERFACE | Flags.ANNOTATION;
    }
    if (env.scope.owner instanceof Symbol.TypeSymbol && ((env.enclosingClass.flags() & Flags.INTERFACE) != 0)) {
      // JLS7 6.6.1: All members of interfaces are implicitly public.
      flags |= Flags.PUBLIC;
    }
    return flags;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    visitMethodDeclaration((MethodTreeImpl) tree);
    super.visitMethod(tree);
    restoreEnvironment(tree);
  }

  private void visitMethodDeclaration(MethodTreeImpl tree) {
    String name = tree.returnType() == null ? "<init>" : tree.simpleName().name();
    Symbol.MethodSymbol symbol = new Symbol.MethodSymbol(computeFlags(tree.modifiers()), name, env.scope.owner);
    enterSymbol(tree, symbol);
    symbol.parameters = new Scope(symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    tree.setSymbol(symbol);

    // Save current environment to be able to complete method later
    semanticModel.saveEnv(symbol, env);

    // Create new environment - this is required, because new scope is created
    Resolve.Env methodEnv = env.dup();
    methodEnv.scope = symbol.parameters;
    env = methodEnv;
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    declareVariable(Flags.PUBLIC | Flags.ENUM, tree.simpleName(), (VariableTreeImpl) tree);
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    declareVariable(computeFlags(tree.modifiers()), tree.simpleName(), (VariableTreeImpl) tree);
    super.visitVariable(tree);
  }

  private int computeFlags(ModifiersTree modifiers) {
    int result = 0;
    //JLS7 6.6.1: All members of interfaces are implicitly public.
    if ((env.scope.owner.flags & Flags.INTERFACE) != 0) {
      result = Flags.PUBLIC;
    }
    for (Modifier modifier : modifiers.modifiers()) {
      result |= Flags.flagForModifier(modifier);
    }
    return result;
  }

  private void declareVariable(int flags, IdentifierTree identifierTree, VariableTreeImpl tree) {
    Symbol.VariableSymbol symbol = new Symbol.VariableSymbol(flags, identifierTree.name(), env.scope.owner);
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

  private void createNewEnvironment(Tree tree) {
    Scope scope = new Scope(env.scope);
    Resolve.Env newEnv = env.dup();
    newEnv.scope = scope;
    env = newEnv;
    semanticModel.associateEnv(tree, env);
  }

  private void enterSymbol(Tree tree, Symbol symbol) {
    env.scope.enter(symbol);
    semanticModel.associateSymbol(tree, symbol);
  }

}
