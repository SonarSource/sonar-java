/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.VariableReadExtractor;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.ucfg.Expression;
import org.sonar.ucfg.Label;
import org.sonar.ucfg.LocationInFile;
import org.sonar.ucfg.UCFG;
import org.sonar.ucfg.UCFGBuilder;
import org.sonar.ucfg.UCFGBuilder.BlockBuilder;
import org.sonar.ucfg.UCFGtoProtobuf;

import static org.sonar.plugins.java.api.tree.Tree.Kind.ASSIGNMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_INVOCATION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_CLASS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PLUS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PLUS_ASSIGNMENT;
import static org.sonar.ucfg.UCFGBuilder.call;
import static org.sonar.ucfg.UCFGBuilder.constant;
import static org.sonar.ucfg.UCFGBuilder.variableWithId;

public class UCFGJavaVisitor extends BaseTreeVisitor implements JavaFileScanner {
  private static final Logger LOG = Loggers.get(JavaSquid.class);
  private final File protobufDirectory;
  String javaFileKey;
  private int index = 0;

  public UCFGJavaVisitor(File workdir) {
    this.protobufDirectory = new File(new File(workdir, "ucfg"), "java");
    if (protobufDirectory.exists()) {
      // set index to the number of files, to avoid overwriting previous files
      index = protobufDirectory.list().length;
    } else {
      protobufDirectory.mkdirs();
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.javaFileKey = context.getFileKey();
    if (context.getSemanticModel() == null) {
      return;
    }
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    Symbol.MethodSymbol methodSymbol = tree.symbol();
    List<Type> types = new ArrayList<>(methodSymbol.parameterTypes());
    if (!tree.is(CONSTRUCTOR)) {
      types.add(methodSymbol.returnType().type());
    }
    if (tree.block() != null && types.stream().noneMatch(Type::isUnknown)) {
      CFG cfg = CFG.build(tree);
      serializeUCFG(tree, cfg);
    }
  }

  @VisibleForTesting
  protected void serializeUCFG(MethodTree tree, CFG cfg) {
    try {
      UCFG uCFG = buildUCfg(tree, cfg);
      UCFGtoProtobuf.toProtobufFile(uCFG, ucfgFilePath());
    } catch (Exception e) {
      String msg = String.format("Cannot generate ucfg for file %s for method at line %d", javaFileKey, tree.firstToken().line());
      LOG.error(msg, e);
      throw new AnalysisException(msg, e);
    }
  }

  private String ucfgFilePath() {
    String absolutePath = new File(protobufDirectory, "ucfg_" + index + ".proto").getAbsolutePath();
    index++;
    return absolutePath;
  }

  private UCFG buildUCfg(MethodTree methodTree, CFG cfg) {
    String signature = signatureFor(methodTree.symbol());

    IdentifierGenerator idGenerator = new IdentifierGenerator(methodTree);
    UCFGBuilder builder = UCFGBuilder.createUCFGForMethod(signature);

    methodTree.parameters().stream()
      .map(p -> idGenerator.lookupIdFor(p.symbol()))
      .map(UCFGBuilder::variableWithId)
      .forEach(builder::addMethodParam);

    BlockBuilder entryBlockBuilder = buildBasicBlock(cfg.entry(), methodTree, idGenerator);

    if (getAnnotatedParameters(methodTree).count() > 0) {
      builder.addStartingBlock(buildParameterAnnotationsBlock(methodTree, idGenerator, cfg));
      builder.addBasicBlock(entryBlockBuilder);
    } else {
      builder.addStartingBlock(entryBlockBuilder);
    }

    cfg.blocks().stream()
      .filter(b -> !b.equals(cfg.entry()))
      .forEach(b -> builder.addBasicBlock(buildBasicBlock(b, methodTree, idGenerator)));
    return builder.build();
  }

  private BlockBuilder buildParameterAnnotationsBlock(MethodTree methodTree, IdentifierGenerator idGenerator, CFG cfg) {
    LocationInFile parametersLocation = location(methodTree.openParenToken(), methodTree.closeParenToken());
    UCFGBuilder.BlockBuilder blockBuilder = UCFGBuilder.newBasicBlock("paramAnnotations", parametersLocation);

    getAnnotatedParameters(methodTree).forEach(parameter -> buildBlockForParameter(parameter, blockBuilder, idGenerator));

    Label nextBlockLabel = UCFGBuilder.createLabel(Integer.toString(cfg.entry().id()));
    blockBuilder.jumpTo(nextBlockLabel);
    return blockBuilder;
  }

  private void buildBlockForParameter(VariableTree parameter, BlockBuilder blockBuilder, IdentifierGenerator idGenerator) {
    Expression.Variable parameterVariable = variableWithId(idGenerator.lookupIdFor(parameter.symbol()));
    List<AnnotationTree> annotationList = parameter.modifiers().annotations();
    List<Expression> annotationVariables = new ArrayList<>();

    annotationList.forEach(annotationTree -> {
      Expression.Variable var = variableWithId(idGenerator.newIdFor(annotationTree));
      annotationVariables.add(var);
      blockBuilder.assignTo(var, call(annotationTree.annotationType().symbolType().fullyQualifiedName()).withArgs(parameterVariable), location(annotationTree));
    });

    Expression[] args = annotationVariables.toArray(new Expression[annotationVariables.size()]);
    blockBuilder.assignTo(parameterVariable, call("__annotation").withArgs(args), location(parameter.simpleName()));
  }

  private static Stream<VariableTree> getAnnotatedParameters(MethodTree methodTree) {
    return methodTree.parameters().stream().filter(parameter -> isObject(parameter.type().symbolType())).filter(parameter -> !parameter.modifiers().annotations().isEmpty());
  }

  private UCFGBuilder.BlockBuilder buildBasicBlock(CFG.Block javaBlock, MethodTree methodTree, IdentifierGenerator idGenerator) {
    UCFGBuilder.BlockBuilder blockBuilder = UCFGBuilder.newBasicBlock(String.valueOf(javaBlock.id()), location(javaBlock));

    javaBlock.elements().forEach(e -> buildCall(e, blockBuilder, idGenerator));

    Tree terminator = javaBlock.terminator();
    if (terminator != null && terminator.is(Tree.Kind.RETURN_STATEMENT)) {
      ExpressionTree returnedExpression = ((ReturnStatementTree) terminator).expression();
      Expression retExpr = constant(IdentifierGenerator.CONST);
      if (methodTree.returnType() != null && isObject(methodTree.returnType().symbolType())) {
        retExpr = idGenerator.lookupExpressionFor(returnedExpression);
      }
      blockBuilder.ret(retExpr, location(terminator));
      return blockBuilder;
    }

    Set<CFG.Block> successors = javaBlock.successors();
    if (!successors.isEmpty()) {
      blockBuilder.jumpTo(successors.stream().map(b -> UCFGBuilder.createLabel(Integer.toString(b.id()))).toArray(Label[]::new));
      return blockBuilder;
    }
    Preconditions.checkState(javaBlock.id() == 0);
    blockBuilder.ret(constant("implicit return"), location(methodTree.lastToken()));
    return blockBuilder;
  }

  private void buildCall(Tree element, UCFGBuilder.BlockBuilder blockBuilder, IdentifierGenerator idGenerator) {
    if (isObjectVarDeclaration(element)) {
      VariableTree variableTree = (VariableTree) element;

      String lhs = idGenerator.lookupIdFor(variableTree.simpleName());
      if (!idGenerator.isConst(lhs)) {
        ExpressionTree initializer = variableTree.initializer();
        String source = idGenerator.lookupIdFor(initializer);
        blockBuilder.assignTo(variableWithId(lhs), UCFGBuilder.call("__id").withArgs(variableWithId(source)), location(element));
      }
      return;
    }

    if (element.is(METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) element;
      buildMethodInvocation(blockBuilder, idGenerator, methodInvocationTree);
    } else if (element.is(NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) element;
      buildConstructorInvocation(blockBuilder, idGenerator, newClassTree);
    } else if (element.is(PLUS, PLUS_ASSIGNMENT, ASSIGNMENT) && isString(((ExpressionTree) element).symbolType())) {
      if (element.is(PLUS)) {
        BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) element;
        Expression lhs = idGenerator.lookupExpressionFor(binaryExpressionTree.leftOperand());
        Expression rhs = idGenerator.lookupExpressionFor(binaryExpressionTree.rightOperand());
        Expression.Variable var = variableWithId(idGenerator.newIdFor(binaryExpressionTree));
        blockBuilder.assignTo(var, call("__concat").withArgs(lhs, rhs), location(element));
      } else if (element.is(PLUS_ASSIGNMENT)) {
        Expression var = idGenerator.lookupExpressionFor(((AssignmentExpressionTree) element).variable());
        Expression expr = idGenerator.lookupExpressionFor(((AssignmentExpressionTree) element).expression());
        if (!var.isConstant()) {
          idGenerator.varForExpression(element, ((Expression.Variable) var).id());
          blockBuilder.assignTo((Expression.Variable) var, call("__concat").withArgs(var, expr), location(element));
        }
      } else if (element.is(ASSIGNMENT)) {
        Expression var = idGenerator.lookupExpressionFor(((AssignmentExpressionTree) element).variable());
        Expression expr = idGenerator.lookupExpressionFor(((AssignmentExpressionTree) element).expression());
        if (!var.isConstant()) {
          blockBuilder.assignTo((Expression.Variable) var, call("__id").withArgs(expr), location(element));
        }
      }
    }
  }

  private void buildConstructorInvocation(BlockBuilder blockBuilder, IdentifierGenerator idGenerator, NewClassTree tree) {
    Symbol constructorSymbol = tree.constructorSymbol();
    if (!constructorSymbol.isMethodSymbol()) {
      return;
    }

    if(isObject(constructorSymbol.owner().type()) || tree.arguments().stream().map(ExpressionTree::symbolType).anyMatch(UCFGJavaVisitor::isObject)) {
      List<Expression> arguments = argumentIds(idGenerator, tree.arguments());
      buildAssignCall(blockBuilder, idGenerator, arguments, tree, (Symbol.MethodSymbol) constructorSymbol);
    }
  }

  private void buildMethodInvocation(UCFGBuilder.BlockBuilder blockBuilder, IdentifierGenerator idGenerator, MethodInvocationTree tree) {
    if (tree.symbol().isUnknown()) {
      return;
    }

    List<Expression> arguments = null;

    if (isObject(tree.symbol().owner().type()) ) {
      arguments = new ArrayList<>();
      if (tree.methodSelect().is(MEMBER_SELECT)) {
        arguments.add(idGenerator.lookupExpressionFor(((MemberSelectExpressionTree) tree.methodSelect()).expression()));
      }
      arguments.addAll(argumentIds(idGenerator, tree.arguments()));
    } else if (isObject(tree.symbolType())
        || tree.arguments().stream().map(ExpressionTree::symbolType).anyMatch(UCFGJavaVisitor::isObject)) {

      arguments = argumentIds(idGenerator, tree.arguments());
    }

    if (arguments != null) {
      buildAssignCall(blockBuilder, idGenerator, arguments, tree, (Symbol.MethodSymbol) tree.symbol());
    }
  }

  private void buildAssignCall(BlockBuilder blockBuilder, IdentifierGenerator idGenerator,  List<Expression> arguments, Tree tree, Symbol.MethodSymbol symbol) {
    String destination = idGenerator.newIdFor(tree);
    blockBuilder.assignTo(variableWithId(destination), UCFGBuilder.call(signatureFor(symbol)).withArgs(arguments.toArray(new Expression[0])), location(tree));
  }

  private static List<Expression> argumentIds(IdentifierGenerator idGenerator, Arguments arguments) {
    return arguments.stream().map(idGenerator::lookupExpressionFor).collect(Collectors.toList());
  }

  private static String signatureFor(Symbol.MethodSymbol methodSymbol) {
    return ((JavaSymbol.MethodJavaSymbol) methodSymbol).completeSignature();
  }


  @Nullable
  private LocationInFile location(CFG.Block javaBlock) {
    Tree firstTree = null;
    List<Tree> elements = javaBlock.elements();
    if (!elements.isEmpty()) {
      firstTree = elements.get(0);
    } else if (javaBlock.terminator() != null) {
      firstTree = javaBlock.terminator();
    }
    if (firstTree == null) {
      return null;
    }
    return location(firstTree);
  }


  private LocationInFile location(Tree tree) {
    return location(tree.firstToken(), tree.lastToken());
  }

  private LocationInFile location(SyntaxToken firstToken, SyntaxToken lastToken) {
    return new LocationInFile(
      javaFileKey,
      firstToken.line(), firstToken.column(),
      lastToken.line(), lastToken.column() + lastToken.text().length()
    );
  }

  private static boolean isObjectVarDeclaration(Tree tree) {
    if (!tree.is(Tree.Kind.VARIABLE)) {
      return false;
    }

    VariableTree var = (VariableTree) tree;
    return isObject(var.type().symbolType());
  }

  private static boolean isString(Type type) {
    return type.is("java.lang.String");
  }

  private static boolean isObject(Type type) {
    return type.isSubtypeOf("java.lang.Object");
  }

  public static class IdentifierGenerator {

    private static final String CONST = "\"\"";

    private final Map<Symbol, String> vars;
    private final Map<Tree, String> temps;
    private int counter;

    public IdentifierGenerator(MethodTree methodTree) {
      List<Symbol> parameters = methodTree.parameters().stream().map(VariableTree::symbol).collect(Collectors.toList());
      VariableReadExtractor variableReadExtractor = new VariableReadExtractor(methodTree.symbol(), false);
      methodTree.accept(variableReadExtractor);
      Set<Symbol> locals = variableReadExtractor.usedVariables().stream().filter(s -> s.type().is("java.lang.String")).collect(Collectors.toSet());
      vars = Sets.union(new HashSet<>(parameters), locals).stream().collect(Collectors.toMap(s -> s, Symbol::name));
      temps = new HashMap<>();
      counter = 0;
    }

    public boolean isConst(String id) {
      return CONST.equals(id);
    }

    public String newIdFor(@Nullable Tree tree) {
      String id = lookupIdFor(tree);
      if (isConst(id)) {
        return temps.computeIfAbsent(tree, t -> newId());
      } else {
        return id;
      }
    }

    private String newId() {
      String result = "%" + counter;
      counter++;
      return result;
    }

    public Expression lookupExpressionFor(@Nullable Tree tree) {
      String id = lookupIdFor(tree);
      if (isConst(id)) {
        if (tree != null && tree.is(Tree.Kind.STRING_LITERAL)) {
          return constant(LiteralUtils.trimQuotes(((LiteralTree) tree).value()));
        }
        return constant(id);
      }
      return variableWithId(id);
    }

    public String lookupIdFor(@Nullable Tree tree) {
      if (tree == null) {
        return CONST;
      } else if (tree.is(Tree.Kind.IDENTIFIER)) {
        return lookupIdFor(((IdentifierTree) tree).symbol());
      } else {
        return temps.getOrDefault(tree, CONST);
      }
    }

    public String lookupIdFor(Symbol symbol) {
      return vars.getOrDefault(symbol, CONST);
    }

    public void varForExpression(Tree element, String id) {
      temps.put(element, id);
    }
  }

}
