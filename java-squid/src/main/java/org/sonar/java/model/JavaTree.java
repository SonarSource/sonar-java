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
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public abstract class JavaTree implements Tree {

  protected final AstNode astNode;

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

  /**
   * Creates iterator for children of this node.
   * Note that iterator may contain {@code null} elements.
   *
   * @throws java.lang.UnsupportedOperationException if {@link #isLeaf()} returns {@code true}
   */
  public abstract Iterator<Tree> childrenIterator();

  public boolean isLeaf() {
    return false;
  }

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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(
        Iterators.singletonIterator(packageName),
        imports.iterator(),
        types.iterator(),
        packageAnnotations.iterator()
      );
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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.singletonIterator(
        qualifiedIdentifier
      );
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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.singletonIterator(
        bound
      );
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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(
        // (Godin): workaround for generics
        Iterators.<Tree>emptyIterator(),
        typeAlternatives.iterator()
      );
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

    @Override
    public Iterator<Tree> childrenIterator() {
      throw new UnsupportedOperationException();
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

    @Override
    public SyntaxToken keyword() {
      return new InternalSyntaxToken(astNode.getLastToken());
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    public Iterator<Tree> childrenIterator() {
      throw new UnsupportedOperationException();
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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(
        Iterators.singletonIterator(type),
        typeArguments.iterator()
      );
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

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.singletonIterator(type);
    }
  }
}
