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
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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
    this.completer = new SecondPass(semanticModel, resolve);
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
    //Default package
    //TODO default package name is null or empty as in openJDK?
    Symbol.PackageSymbol compilationUnitPackage = symbols.defaultPackage;
    compilationUnitPackage.members = new Scope(compilationUnitPackage);

    env = new Resolve.Env();
    env.packge = compilationUnitPackage;
    env.scope = compilationUnitPackage.members;
    env.namedImports = new Scope(compilationUnitPackage);
    semanticModel.associateEnv(tree, env);

    super.visitCompilationUnit(tree);
    restoreEnvironment(tree);
    completeSymbols();
  }

  @Override
  public void visitImport(ImportTree tree) {
    //an import is always of the form : TypeName/PackName.Identifier
    Preconditions.checkArgument(tree.qualifiedIdentifier().is(Tree.Kind.MEMBER_SELECT));
    ImportResolverVisitor importResolverVisitor = new ImportResolverVisitor();
    tree.accept(importResolverVisitor);
    super.visitImport(tree);
  }

  private class ImportResolverVisitor extends BaseTreeVisitor {
    private Symbol currentSymbol;
    private List<Symbol> resolved;

    public ImportResolverVisitor() {
      currentSymbol = symbols.defaultPackage;
    }

    @Override
    public void visitImport(ImportTree tree) {
      super.visitImport(tree);
      //Associate symbol only if found.
      if (currentSymbol.kind < Symbol.ERRONEOUS) {
        enterSymbol(currentSymbol, tree);
      } else {
        if (tree.isStatic()) {
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
      if (semanticModel.getSymbol(tree) == null && semanticModel.getTree(symbol) == null) {
        //TODO handle correctly on demand import so java.util.List and java.util.List.* are not resolved to the same symbol and we won't need check : semanticModel.getTree(symbol)==null
        semanticModel.associateSymbol(tree, symbol);
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (currentSymbol.kind == Symbol.PCK) {
        currentSymbol = resolve.findIdentInPackage(env, currentSymbol, tree.name(), Symbol.PCK | Symbol.TYP);
        resolved = Collections.emptyList();
      } else if (currentSymbol.kind == Symbol.TYP) {
        resolved = ((Symbol.TypeSymbol) currentSymbol).members().lookup(tree.name());
        currentSymbol = resolve.findIdentInType(env, (Symbol.TypeSymbol) currentSymbol, tree.name(), Symbol.TYP);
      } else {
        //Site symbol is not found so we won't be able to resolve the import.
        currentSymbol = new Resolve.SymbolNotFound();
        resolved = Collections.emptyList();
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
    if (!anonymousClass) {
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

  private int computeModifierFlag(AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.MODIFIER));
    final int flag;
    AstNode modifierNode = astNode.getFirstChild();
    if (modifierNode.is(JavaKeyword.PRIVATE)) {
      flag = Flags.PRIVATE;
    } else if (modifierNode.is(JavaKeyword.PROTECTED)) {
      flag = Flags.PROTECTED;
    } else if (modifierNode.is(JavaKeyword.PUBLIC)) {
      flag = Flags.PUBLIC;
    } else {
      flag = 0;
    }
    return flag;
  }

  private int computeFlags(AstNode astNode) {
    int flags = 0;
    AstNode modifierNode = astNode.getPreviousAstNode();
    while (modifierNode != null && modifierNode.is(JavaGrammar.MODIFIER)) {
      flags |= computeModifierFlag(modifierNode);
      modifierNode = modifierNode.getPreviousAstNode();
    }
    return flags;
  }

  private int computeClassFlags(ClassTree tree) {
    AstNode astNode = ((JavaTree) tree).getAstNode();
    int flags = computeFlags(astNode);
    if (astNode.is(JavaGrammar.INTERFACE_DECLARATION)) {
      flags |= Flags.INTERFACE;
    } else if (astNode.is(JavaGrammar.ENUM_DECLARATION)) {
      flags |= Flags.ENUM;
    } else if (astNode.is(JavaGrammar.ANNOTATION_TYPE_DECLARATION)) {
      flags |= Flags.ANNOTATION | Flags.INTERFACE;
    }
    if (env.scope.owner instanceof Symbol.TypeSymbol && ((env.enclosingClass.flags() & Flags.INTERFACE) != 0)) {
      // JLS7 6.6.1: All members of interfaces are implicitly public.
      flags |= Flags.PUBLIC;
    }
    return flags;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    visitMethodDeclaration(tree);
    super.visitMethod(tree);
    restoreEnvironment(tree);
  }

  private void visitMethodDeclaration(MethodTree tree) {
    String name = tree.returnType() == null ? "<init>" : tree.simpleName().name();
    Symbol.MethodSymbol symbol = new Symbol.MethodSymbol(computeMethodFlags(tree), name, env.scope.owner);
    enterSymbol(tree, symbol);
    symbol.parameters = new Scope(symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

    // Save current environment to be able to complete method later
    semanticModel.saveEnv(symbol, env);

    // Create new environment - this is required, because new scope is created
    Resolve.Env methodEnv = env.dup();
    methodEnv.scope = symbol.parameters;
    env = methodEnv;
  }

  private int computeMethodFlags(MethodTree tree) {
    AstNode astNode = ((JavaTree) tree).getAstNode();
    if (astNode.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST, JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)) {
      return computeFlags(astNode.getFirstAncestor(JavaGrammar.MEMBER_DECL));
    } else if (astNode.is(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST, JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST)) {
      // JLS7 6.6.1: All members of interfaces are implicitly public.
      return Flags.PUBLIC;
    } else if (astNode.is(JavaGrammar.ANNOTATION_METHOD_REST)) {
      // JLS7 6.6.1: All members of interfaces are implicitly public.
      return Flags.PUBLIC;
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    declareVariable(Flags.PUBLIC | Flags.ENUM, tree.simpleName(), tree);
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    declareVariable(computeFlags(tree.modifiers()), tree.simpleName(), tree);
    super.visitVariable(tree);
  }

  private int computeFlags(ModifiersTree modifiers) {
    //TODO  JLS7 6.6.1: All members of interfaces are implicitly public. but we should use modifiers to compute flags.
    return 1;
  }

  private void declareVariable(int flags, IdentifierTree identifierTree, Tree tree) {
    Symbol.VariableSymbol symbol = new Symbol.VariableSymbol(flags, identifierTree.name(), env.scope.owner);
    enterSymbol(tree, symbol);
    symbol.completer = completer;
    uncompleted.add(symbol);

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
