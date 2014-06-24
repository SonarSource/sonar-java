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
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.List;

public abstract class JavaTree implements Tree {

  private final AstNode astNode;

  public JavaTree(AstNode astNode) {
    this.astNode = astNode;
  }

  public AstNode getAstNode() {
    return astNode;
  }

  public int getLine() {
    return astNode.getTokenLine();
  }

  @Override
  public final boolean is(Kind kind) {
    return getKind() == null ? false : getKind() == kind;
  }

  public abstract Kind getKind();

  public static class CompilationUnitTreeImpl extends JavaTree implements CompilationUnitTree {
    @Nullable
    private final ExpressionTree packageName;
    private final List<ImportTree> imports;
    private final List<Tree> types;
    private final List<AnnotationTree> packageAnnotations;

    public CompilationUnitTreeImpl(AstNode astNode, @Nullable ExpressionTree packageName, List<ImportTree> imports, List<Tree> types, List<AnnotationTree> packageAnnotations) {
      super(astNode);
      this.packageName = packageName;
      this.imports = Preconditions.checkNotNull(imports);
      this.types = Preconditions.checkNotNull(types);
      this.packageAnnotations = Preconditions.checkNotNull(packageAnnotations);
    }

    @Override
    public Kind getKind() {
      return Kind.COMPILATION_UNIT;
    }

    @Override
    public List<AnnotationTree> packageAnnotations() {
      return packageAnnotations;
    }

    @Nullable
    @Override
    public ExpressionTree packageName() {
      return packageName;
    }

    @Override
    public List<ImportTree> imports() {
      return imports;
    }

    @Override
    public List<Tree> types() {
      return types;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCompilationUnit(this);
    }
  }

  public static class ImportTreeImpl extends JavaTree implements ImportTree {
    private final boolean isStatic;
    private final Tree qualifiedIdentifier;

    public ImportTreeImpl(AstNode astNode, boolean aStatic, Tree qualifiedIdentifier) {
      super(astNode);
      isStatic = aStatic;
      this.qualifiedIdentifier = qualifiedIdentifier;
    }

    @Override
    public Kind getKind() {
      return null;
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    @Override
    public Tree qualifiedIdentifier() {
      return qualifiedIdentifier;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitImport(this);
    }
  }

  public static class ClassTreeImpl extends JavaTree implements ClassTree {
    private final Kind kind;
    private final ModifiersTree modifiers;
    private final IdentifierTree simpleName;
    @Nullable
    private final Tree superClass;
    private final List<Tree> superInterfaces;
    private final List<Tree> members;
    @Nullable
    private Symbol.TypeSymbol symbol;

    public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, @Nullable IdentifierTree simpleName, @Nullable Tree superClass, List<Tree> superInterfaces,
                         List<Tree> members) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.simpleName = simpleName;
      this.superClass = superClass;
      this.superInterfaces = Preconditions.checkNotNull(superInterfaces);
      this.members = Preconditions.checkNotNull(members);
    }

    // TODO remove:
    public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, List<Tree> members) {
      this(astNode, kind, modifiers, null, null, ImmutableList.<Tree>of(), members);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Nullable
    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Override
    public List<Tree> typeParameters() {
      // TODO implement
      return ImmutableList.of();
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Nullable
    @Override
    public Tree superClass() {
      return superClass;
    }

    @Override
    public List<Tree> superInterfaces() {
      return superInterfaces;
    }

    @Override
    public List<Tree> members() {
      return members;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitClass(this);
    }

    @Nullable
    public Symbol.TypeSymbol getSymbol() {
      return symbol;
    }

    public void setSymbol(Symbol.TypeSymbol symbol) {
      this.symbol = symbol;
    }
  }

  public static class MethodTreeImpl extends JavaTree implements MethodTree {
    private final ModifiersTree modifiers;
    @Nullable
    private final Tree returnType;
    private final IdentifierTree simpleName;
    private final List<VariableTree> parameters;
    @Nullable
    private final BlockTree block;
    private final List<ExpressionTree> throwsClauses;
    private final ExpressionTree defaultValue;

    public MethodTreeImpl(AstNode astNode, ModifiersTree modifiers, @Nullable Tree returnType, IdentifierTree simpleName, List<VariableTree> parameters,
                          @Nullable BlockTree block,
                          List<ExpressionTree> throwsClauses, @Nullable ExpressionTree defaultValue) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.returnType = returnType;
      this.simpleName = Preconditions.checkNotNull(simpleName);
      this.parameters = Preconditions.checkNotNull(parameters);
      this.block = block;
      this.throwsClauses = Preconditions.checkNotNull(throwsClauses);
      this.defaultValue = defaultValue;
    }

    @Override
    public Kind getKind() {
      return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Override
    public List<Tree> typeParameters() {
      // TODO implement
      return ImmutableList.of();
    }

    @Nullable
    @Override
    public Tree returnType() {
      return returnType;
    }

    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Override
    public List<VariableTree> parameters() {
      return parameters;
    }

    @Override
    public List<ExpressionTree> throwsClauses() {
      return throwsClauses;
    }

    @Nullable
    @Override
    public BlockTree block() {
      return block;
    }

    @Nullable
    @Override
    public ExpressionTree defaultValue() {
      return defaultValue;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitMethod(this);
    }
  }

  public static class EnumConstantTreeImpl extends VariableTreeImpl implements EnumConstantTree {
    public EnumConstantTreeImpl(AstNode astNode, ModifiersTree modifiers, Tree type, IdentifierTree simpleName, ExpressionTree initializer) {
      super(astNode, modifiers, type, simpleName, Preconditions.checkNotNull(initializer));
    }

    @Override
    public Kind getKind() {
      return Kind.ENUM_CONSTANT;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitEnumConstant(this);
    }
  }

  public static class VariableTreeImpl extends JavaTree implements VariableTree {
    private final ModifiersTree modifiers;
    private final Tree type;
    private final IdentifierTree simpleName;
    @Nullable
    private final ExpressionTree initializer;
    private Symbol.VariableSymbol symbol;

    public VariableTreeImpl(AstNode astNode, ModifiersTree modifiers, Tree type, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.type = Preconditions.checkNotNull(type);
      this.simpleName = Preconditions.checkNotNull(simpleName);
      this.initializer = initializer;
    }

    @Override
    public Kind getKind() {
      return Kind.VARIABLE;
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Nullable
    @Override
    public ExpressionTree initializer() {
      return initializer;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitVariable(this);
    }

    public Symbol.VariableSymbol getSymbol() {
      return symbol;
    }

    public void setSymbol(Symbol.VariableSymbol symbol) {
      this.symbol = symbol;
    }
  }

  public static class WildcardTreeImpl extends JavaTree implements WildcardTree {
    private final Kind kind;
    @Nullable
    private final Tree bound;

    public WildcardTreeImpl(AstNode astNode, Kind kind, @Nullable Tree bound) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
      this.bound = bound;
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Nullable
    @Override
    public Tree bound() {
      return bound;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitWildcard(this);
    }
  }

  public static class UnionTypeTreeImpl extends JavaTree implements UnionTypeTree {
    private final List<Tree> typeAlternatives;

    public UnionTypeTreeImpl(AstNode astNode, List<Tree> typeAlternatives) {
      super(astNode);
      this.typeAlternatives = Preconditions.checkNotNull(typeAlternatives);
    }

    @Override
    public Kind getKind() {
      return Kind.UNION_TYPE;
    }

    @Override
    public List<Tree> typeAlternatives() {
      return typeAlternatives;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitUnionType(this);
    }
  }

  public static class ModifiersTreeImpl extends JavaTree implements ModifiersTree {
    // TODO remove:
    public static final ModifiersTreeImpl EMPTY = new ModifiersTreeImpl(null, ImmutableList.<Modifier>of(), ImmutableList.<AnnotationTree>of());

    private final List<Modifier> modifiers;
    private final List<AnnotationTree> annotations;

    public ModifiersTreeImpl(AstNode astNode, List<Modifier> modifiers, List<AnnotationTree> annotations) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.annotations = Preconditions.checkNotNull(annotations);
    }

    @Override
    public Kind getKind() {
      return Kind.MODIFIERS;
    }

    @Override
    public List<Modifier> modifiers() {
      return modifiers;
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitModifier(this);
    }
  }

  public static class NotImplementedTreeImpl extends AbstractTypedTree implements ExpressionTree{
    private String name;

    public NotImplementedTreeImpl(AstNode astNode, String name) {
      super(astNode);
      this.name = name;
    }

    @Override
    public Kind getKind() {
      return Kind.OTHER;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitOther(this);
    }

    public String getName() {
      return name;
    }
  }

  public static class PrimitiveTypeTreeImpl extends AbstractTypedTree implements PrimitiveTypeTree {
    public PrimitiveTypeTreeImpl(AstNode astNode) {
      super(astNode);
    }

    @Override
    public Kind getKind() {
      return Kind.PRIMITIVE_TYPE;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitPrimitiveType(this);
    }
  }

  public static class ParameterizedTypeTreeImpl extends AbstractTypedTree implements ParameterizedTypeTree, ExpressionTree {
    private final ExpressionTree type;
    private final List<Tree> typeArguments;

    public ParameterizedTypeTreeImpl(AstNode child, ExpressionTree type, List<Tree> typeArguments) {
      super(child);
      this.type = Preconditions.checkNotNull(type);
      this.typeArguments = Preconditions.checkNotNull(typeArguments);
    }

    @Override
    public Kind getKind() {
      return Kind.PARAMETERIZED_TYPE;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public List<Tree> typeArguments() {
      return typeArguments;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitParameterizedType(this);
    }
  }

  public static class AnnotationTreeImpl extends AbstractTypedTree implements AnnotationTree {

    private final List<ExpressionTree> arguments;
    private final Tree annotationType;

    public AnnotationTreeImpl(AstNode astNode, Tree annotationType, List<ExpressionTree> arguments) {
      super(astNode);
      this.annotationType = annotationType;
      this.arguments = arguments;
    }

    @Override
    public Tree annotationType() {
      return annotationType;
    }

    @Override
    public List<ExpressionTree> arguments() {
      return arguments;
    }

    @Override
    public Kind getKind() {
      return Kind.ANNOTATION;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitAnnotation(this);

    }
  }

  public static class ArrayTypeTreeImpl extends AbstractTypedTree implements ArrayTypeTree {
    private final Tree type;

    public ArrayTypeTreeImpl(AstNode astNode, Tree type) {
      super(astNode);
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public Kind getKind() {
      return Kind.ARRAY_TYPE;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayType(this);
    }
  }
}
