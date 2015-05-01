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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import org.sonar.java.ast.parser.AstNodeReflector;
import org.sonar.java.ast.parser.TypeUnionListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class JavaTree extends AstNode implements Tree {

  private static final AstNodeType NULL_NODE = new AstNodeType() {

    @Override
    public String toString() {
      return "[null]";
    }

  };

  private final AstNode astNode;

  public JavaTree(AstNodeType type) {
    super(type, type.toString(), null);
    this.astNode = this;
  }

  public JavaTree(AstNodeType type, Token token) {
    super(type, type.toString(), token);
    this.astNode = this;
  }

  public JavaTree(@Nullable AstNode astNode) {
    super(
      astNode == null ? NULL_NODE : astNode.getType(),
      astNode == null ? NULL_NODE.toString() : astNode.getType().toString(),
      astNode == null ? null : astNode.getToken());
    this.astNode = astNode;
  }

  public boolean isLegacy() {
    return astNode != this;
  }

  private void prependChild(AstNode astNode) {
    Preconditions.checkState(getAstNode() == this, "Legacy strongly typed node");

    List<AstNode> children = getChildren();
    if (children.isEmpty()) {
      // addChild() will take care of everything
      addChild(astNode);
    } else {
      AstNodeReflector.setParent(astNode, this);
      children.add(0, astNode);

      // Reset the childIndex field of all children
      for (int i = 0; i < children.size(); i++) {
        AstNodeReflector.setChildIndex(children.get(i), i);
      }
    }
  }

  public void prependChildren(AstNode... astNodes) {
    for (int i = astNodes.length - 1; i >= 0; i--) {
      prependChild(astNodes[i]);
    }
  }

  public void prependChildren(List<? extends AstNode> astNodes) {
    prependChildren(astNodes.toArray(new AstNode[astNodes.size()]));
  }

  @Override
  public void addChild(AstNode child) {
    Preconditions.checkState(!isLegacy(), "Children should not be added to legacy nodes");
    super.addChild(child);
  }

  public AstNode getAstNode() {
    return astNode;
  }

  public int getLine() {
    return astNode.getTokenLine();
  }

  @Override
  public final boolean is(Kind... kind) {
    if (getKind() != null) {
      for (Kind kindIter : kind) {
        if (getKind() == kindIter) {
          return true;
        }
      }
    }
    return false;
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
    private final List<ImportClauseTree> imports;
    private final List<Tree> types;
    private final List<AnnotationTree> packageAnnotations;

    public CompilationUnitTreeImpl(@Nullable ExpressionTree packageName, List<ImportClauseTree> imports,
                                   List<Tree> types, List<AnnotationTree> packageAnnotations, List<AstNode> children) {
      super(Kind.COMPILATION_UNIT);
      this.packageName = packageName;
      this.imports = Preconditions.checkNotNull(imports);
      this.types = Preconditions.checkNotNull(types);
      this.packageAnnotations = Preconditions.checkNotNull(packageAnnotations);

      for (AstNode child : children) {
        addChild(child);
      }
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
    public List<ImportClauseTree> imports() {
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

    public String packageNameAsString() {
      if (packageName == null) {
        return "";
      }
      Deque<String> pieces = new LinkedList<String>();
      ExpressionTree expr = packageName;
      while (expr.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
        pieces.push(mse.identifier().name());
        pieces.push(".");
        expr = mse.expression();
      }
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree idt = (IdentifierTree) expr;
        pieces.push(idt.name());
      }

      StringBuilder sb = new StringBuilder();
      for (String piece : pieces) {
        sb.append(piece);
      }
      return sb.toString();
    }

  }

  public static class ImportTreeImpl extends JavaTree implements ImportTree {
    private final boolean isStatic;
    private final Tree qualifiedIdentifier;
    private final SyntaxToken semiColonToken;
    private final SyntaxToken importToken;
    private final SyntaxToken staticToken;

    public ImportTreeImpl(InternalSyntaxToken importToken, @Nullable InternalSyntaxToken staticToken,
                          Tree qualifiedIdentifier, InternalSyntaxToken semiColonToken) {
      super(Kind.IMPORT);
      this.importToken = importToken;
      this.staticToken = staticToken;
      this.qualifiedIdentifier = qualifiedIdentifier;
      this.semiColonToken = semiColonToken;
      isStatic = staticToken != null;
      addChild(importToken);
      if(isStatic) {
        addChild(staticToken);
      }
      addChild((AstNode) qualifiedIdentifier);
      addChild(semiColonToken);

    }

    @Override
    public Kind getKind() {
      return Kind.IMPORT;
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    @Override
    public SyntaxToken importKeyword() {
      return importToken;
    }

    @Nullable
    @Override
    public SyntaxToken staticKeyword() {
      return staticToken;
    }

    @Override
    public Tree qualifiedIdentifier() {
      return qualifiedIdentifier;
    }

    @Override
    public SyntaxToken semicolonToken() {
      return semiColonToken;
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
    private final TypeTree bound;

    public WildcardTreeImpl(Kind kind, InternalSyntaxToken queryToken) {
      super(kind);

      Preconditions.checkArgument(kind == Kind.UNBOUNDED_WILDCARD);

      this.kind = Preconditions.checkNotNull(kind);
      this.bound = null;

      addChild(queryToken);
    }

    public WildcardTreeImpl(Kind kind, InternalSyntaxToken extendsOrSuperToken, List<AnnotationTreeImpl> annotations, TypeTree bound) {
      super(kind);

      Preconditions.checkArgument(kind == Kind.EXTENDS_WILDCARD || kind == Kind.SUPER_WILDCARD);

      this.kind = Preconditions.checkNotNull(kind);
      this.bound = bound;

      addChild(extendsOrSuperToken);
      for (AnnotationTreeImpl annotation : annotations) {
        addChild(annotation);
      }
      addChild((AstNode) bound);
    }

    public WildcardTreeImpl complete(InternalSyntaxToken queryToken) {
      Preconditions.checkState(kind == Kind.EXTENDS_WILDCARD || kind == Kind.SUPER_WILDCARD);
      prependChildren(queryToken);

      return this;
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Nullable
    @Override
    public TypeTree bound() {
      return bound;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitWildcard(this);
    }

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.<Tree>singletonIterator(
        bound
        );
    }
  }

  public static class UnionTypeTreeImpl extends AbstractTypedTree implements UnionTypeTree {
    private final List<TypeTree> typeAlternatives;

    public UnionTypeTreeImpl(TypeUnionListTreeImpl typeAlternatives) {
      super(Kind.UNION_TYPE);
      this.typeAlternatives = Preconditions.checkNotNull(typeAlternatives);

      addChild(typeAlternatives);
    }

    @Override
    public Kind getKind() {
      return Kind.UNION_TYPE;
    }

    @Override
    public List<TypeTree> typeAlternatives() {
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

  public static class NotImplementedTreeImpl extends AbstractTypedTree implements ExpressionTree {
    private final String name;

    public NotImplementedTreeImpl(AstNode... children) {
      super(Kind.OTHER);
      this.name = "TODO";

      for (AstNode child : children) {
        addChild(child);
      }
    }

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

    @Override
    public String getName() {
      return name;
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

  public static class PrimitiveTypeTreeImpl extends AbstractTypedTree implements PrimitiveTypeTree {

    private final InternalSyntaxToken token;

    public PrimitiveTypeTreeImpl(InternalSyntaxToken token, List<AstNode> children) {
      super(Kind.PRIMITIVE_TYPE);
      this.token = token;

      for (AstNode child : children) {
        addChild(child);
      }
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
      return token != null ? token : InternalSyntaxToken.createLegacy(getLastTokenAstNode(getAstNode()));
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    public Iterator<Tree> childrenIterator() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    private static AstNode getLastTokenAstNode(AstNode astNode) {
      if (!astNode.hasToken()) {
        return null;
      }
      AstNode currentNode = astNode;
      while (currentNode.hasChildren()) {
        for (int i = currentNode.getChildren().size() - 1; i >= 0; i--) {
          AstNode child = currentNode.getChildren().get(i);
          if (child.hasToken()) {
            currentNode = child;
            break;
          }
        }
      }
      return currentNode;
    }
  }

  public static class ParameterizedTypeTreeImpl extends AbstractTypedTree implements ParameterizedTypeTree, ExpressionTree {

    private final TypeTree type;
    private final List<Tree> typeArguments;

    public ParameterizedTypeTreeImpl(TypeTree type, TypeArgumentListTreeImpl typeArguments) {
      super(Kind.PARAMETERIZED_TYPE);
      this.type = Preconditions.checkNotNull(type);
      this.typeArguments = Preconditions.checkNotNull(typeArguments);

      addChild((AstNode) type);
      addChild(typeArguments);
    }

    @Override
    public Kind getKind() {
      return Kind.PARAMETERIZED_TYPE;
    }

    @Override
    public TypeTree type() {
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
    private final TypeTree type;

    public ArrayTypeTreeImpl(TypeTree type, List<AstNode> children) {
      super(Kind.ARRAY_TYPE);
      this.type = Preconditions.checkNotNull(type);

      for (AstNode child : children) {
        addChild(child);
      }
    }

    public ArrayTypeTreeImpl(@Nullable AstNode astNode, TypeTree type) {
      super(astNode);
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public Kind getKind() {
      return Kind.ARRAY_TYPE;
    }

    @Override
    public TypeTree type() {
      return type;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayType(this);
    }

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.<Tree>singletonIterator(type);
    }
  }
}
