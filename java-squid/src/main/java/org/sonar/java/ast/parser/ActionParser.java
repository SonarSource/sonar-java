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
package org.sonar.java.ast.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.ast.AstXmlPrinter;
import com.sonar.sslr.impl.matcher.RuleDefinition;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.sonar.java.ast.parser.ActionGrammar.GrammarBuilder;
import org.sonar.java.ast.parser.ActionGrammar.NonterminalBuilder;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.internal.vm.CompilationHandler;
import org.sonar.sslr.internal.vm.FirstOfExpression;
import org.sonar.sslr.internal.vm.Instruction;
import org.sonar.sslr.internal.vm.OneOrMoreExpression;
import org.sonar.sslr.internal.vm.ParsingExpression;
import org.sonar.sslr.internal.vm.SequenceExpression;
import org.sonar.sslr.internal.vm.StringExpression;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import javax.annotation.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public class ActionParser extends Parser {

  private final Field[] fields;
  private final Field parentField;
  private final Field childrenField;

  private final Object action;

  private final GrammarBuilderInterceptor grammarBuilderInterceptor;
  private final Parser parser;

  private AstNode rootNode;

  public ActionParser(Charset charset, LexerlessGrammarBuilder b, Class grammarClass, Object action, GrammarRuleKey rootRule) {
    super(null);

    List<Field> fields = Lists.newArrayList();
    Field parentField = null;
    Field childrenField = null;

    for (Field field : AstNode.class.getDeclaredFields()) {
      if (!"type".equals(field.getName()) && !"name".equals(field.getName())) {
        field.setAccessible(true);
        if ("parent".equals(field.getName())) {
          parentField = field;
        } else if ("children".equals(field.getName())) {
          childrenField = field;
        }
        fields.add(field);
      }
    }

    Preconditions.checkState(parentField != null, "Unable to find the parent field!");
    Preconditions.checkState(childrenField != null, "Unable to find the children field!");

    this.fields = fields.toArray(new Field[fields.size()]);
    this.parentField = parentField;
    this.childrenField = childrenField;

    this.action = action;

    this.grammarBuilderInterceptor = new GrammarBuilderInterceptor(b);
    Enhancer grammarEnhancer = new Enhancer();
    grammarEnhancer.setSuperclass(grammarClass);
    grammarEnhancer.setCallback(grammarBuilderInterceptor);

    ActionMethodInterceptor actionMethodInterceptor = new ActionMethodInterceptor(grammarBuilderInterceptor);
    Enhancer actionEnhancer = new Enhancer();
    actionEnhancer.setSuperclass(action.getClass());
    actionEnhancer.setCallback(actionMethodInterceptor);

    Object grammar = grammarEnhancer.create(
      new Class[] {GrammarBuilder.class, action.getClass()},
      new Object[] {grammarBuilderInterceptor, actionEnhancer.create()});

    for (Method method : grammarClass.getMethods()) {
      if (method.getDeclaringClass().equals(Object.class)) {
        continue;
      }

      try {
        method.invoke(grammar);
      } catch (InvocationTargetException e) {
        throw Throwables.propagate(e);
      } catch (IllegalAccessException e) {
        throw Throwables.propagate(e);
      }
    }

    b.setRootRule(rootRule);
    this.parser = new ParserAdapter<LexerlessGrammar>(charset, b.build());
  }

  @Override
  public AstNode parse(List tokens) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AstNode parse(File file) {
    AstNode astNode = parser.parse(file);

    rootNode = astNode;
    applyActions(astNode);

    return rootNode;
  }

  @Override
  public AstNode parse(String source) {
    AstNode astNode = parser.parse(source);

    rootNode = astNode;
    applyActions(astNode);

    return rootNode;
  }

  private void applyActions(AstNode astNode) {
    AstNode[] children = astNode.getChildren().toArray(new AstNode[astNode.getChildren().size()]);
    for (AstNode childAstNode : children) {
      applyActions(childAstNode);
    }

    Method method = grammarBuilderInterceptor.actionForRuleKey(astNode.getType());
    if (method != null) {
      children = astNode.getChildren().toArray(new AstNode[astNode.getChildren().size()]);
      Object[] convertedChildren = convertTypes(children);

      Preconditions.checkState(
        convertedChildren.length == method.getParameterTypes().length,
        "Argument mismatch! Expected: " + method.getParameterTypes().length + " parameters, but got: " + convertedChildren.length + "\n" +
          AstXmlPrinter.print(astNode));

      try {
        AstNode typedNode = (AstNode) method.invoke(action, convertedChildren);
        replaceAstNode(astNode, typedNode, false);
      } catch (InvocationTargetException e) {
        throw Throwables.propagate(e);
      } catch (IllegalAccessException e) {
        throw Throwables.propagate(e);
      }
    }

    if (grammarBuilderInterceptor.hasMethodForRuleKey(astNode.getType())) {
      children = astNode.getChildren().toArray(new AstNode[astNode.getChildren().size()]);
      Preconditions.checkState(children.length == 1, "Unexpected number of children: " + children.length);
      AstNode typedNode = children[0];
      replaceAstNode(astNode, typedNode, false);
    }
  }

  private void replaceAstNode(AstNode o, AstNode n, boolean overwriteChildren) {
    for (Field field : fields) {
      if (childrenField.equals(field) && !overwriteChildren) {
        continue;
      }

      try {
        field.set(n, field.get(o));
      } catch (IllegalAccessException e) {
        throw Throwables.propagate(e);
      }
    }

    if (overwriteChildren) {
      for (AstNode childAstNode : n.getChildren()) {
        try {
          parentField.set(childAstNode, n);
        } catch (IllegalAccessException e) {
          throw Throwables.propagate(e);
        }
      }
    }

    AstNode parent = n.getParent();
    if (parent != null) {
      List<AstNode> children = parent.getChildren();
      // TODO Replace iteration by childIndex
      for (int i = 0; i < children.size(); i++) {
        if (o.equals(children.get(i))) {
          children.set(i, n);
          break;
        }
      }
    } else {
      rootNode = n;
    }
  }

  private Object[] convertTypes(AstNode[] nodes) {
    List result = Lists.newArrayList();

    ImmutableList.Builder listBuilder = ImmutableList.builder();
    Object listBuilderRepeatedRuleKey = null;

    for (AstNode child : nodes) {
      if (grammarBuilderInterceptor.isRepeatedRule(child.getType())) {
        Object[] converted = convertTypes(child.getChildren().toArray(new AstNode[0]));
        Preconditions.checkState(converted.length == 1, "Unexpected number of children: " + converted.length);

        if (listBuilderRepeatedRuleKey != null && !child.getType().equals(listBuilderRepeatedRuleKey)) {
          result.add(listBuilder.build());
          listBuilder = ImmutableList.builder();
          listBuilderRepeatedRuleKey = null;
        }

        listBuilder.add(converted[0]);
        listBuilderRepeatedRuleKey = child.getType();
      } else {
        if (listBuilderRepeatedRuleKey != null) {
          result.add(listBuilder.build());
          listBuilder = ImmutableList.builder();
          listBuilderRepeatedRuleKey = null;
        }

        result.add(convertType(child));
      }
    }

    if (listBuilderRepeatedRuleKey != null) {
      result.add(listBuilder.build());
      listBuilder = ImmutableList.builder();
      listBuilderRepeatedRuleKey = null;
    }

    return result.toArray();
  }

  private Object convertType(AstNode node) {
    Object result;

    if (grammarBuilderInterceptor.isOptionalRule(node.getType())) {
      Object[] children = convertTypes(node.getChildren().toArray(new AstNode[0]));
      Preconditions.checkState(children.length <= 1, "Unexpected number of children: " + children.length);

      Optional option;
      if (children.length == 1) {
        option = Optional.of(children[0]);
      } else {
        option = Optional.absent();
      }

      result = option;
    } else if (grammarBuilderInterceptor.isRepeatedRule(node.getType())) {
      throw new IllegalStateException("Did not expect a repeated rule: " + node);
    } else {
      result = node;
    }

    return result;
  }

  @Override
  public Grammar getGrammar() {
    return parser.getGrammar();
  }

  @Override
  public void setRootRule(Rule rootRule) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RuleDefinition getRootRule() {
    throw new UnsupportedOperationException();
  }

  public GrammarRuleKey rootRule() {
    return (GrammarRuleKey) parser.getGrammar().getRootRule();
  }

  public static class GrammarBuilderInterceptor implements MethodInterceptor, GrammarBuilder, NonterminalBuilder {

    private final LexerlessGrammarBuilder b;
    private final BiMap<Method, GrammarRuleKey> mapping = HashBiMap.create();
    private final BiMap<Method, GrammarRuleKey> actions = HashBiMap.create();
    private final Set<GrammarRuleKey> optionals = Sets.newHashSet();
    private final Set<GrammarRuleKey> repeated = Sets.newHashSet();

    private Method buildingMethod = null;
    private GrammarRuleKey ruleKey = null;
    private final Deque<ParsingExpression> expressionStack = new ArrayDeque<ParsingExpression>();

    public GrammarBuilderInterceptor(LexerlessGrammarBuilder b) {
      this.b = b;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      if (method.getDeclaringClass().equals(Object.class)) {
        return proxy.invokeSuper(obj, args);
      }

      if (buildingMethod != null) {
        push(new RuleExpression(b, this, method));
        return null;
      }

      buildingMethod = method;
      Object result = proxy.invokeSuper(obj, args);

      return result;
    }

    @Override
    public <T> NonterminalBuilder<T> nonterminal() {
      return nonterminal(new DummyGrammarRuleKey(this.buildingMethod));
    }

    @Override
    public <T> NonterminalBuilder<T> nonterminal(GrammarRuleKey ruleKey) {
      this.ruleKey = ruleKey;
      this.mapping.put(this.buildingMethod, this.ruleKey);
      return this;
    }

    @Override
    public Object is(Object method) {
      Preconditions.checkState(expressionStack.size() == 1, "Unexpected stack size: " + expressionStack.size());

      ParsingExpression expression = pop();
      b.rule(ruleKey).is(expression);

      this.buildingMethod = null;
      this.ruleKey = null;

      return null;
    }

    @Override
    public <T> T firstOf(T... methods) {
      ParsingExpression expression = new FirstOfExpression(pop(methods.length));
      expressionStack.push(expression);
      return null;
    }

    @Override
    public <T> Optional<T> optional(T method) {
      ParsingExpression expression = pop();
      GrammarRuleKey ruleKey = new DummyGrammarRuleKey("optional", expression);
      optionals.add(ruleKey);
      // FIXME Fix corner case where "expression" can already match the empty string...
      b.rule(ruleKey).is(b.optional(expression));
      invokeRule(ruleKey);
      return null;
    }

    @Override
    public <T> List<T> oneOrMore(T method) {
      ParsingExpression expression = pop();
      GrammarRuleKey ruleKey = new DummyGrammarRuleKey("oneOrMore", expression);
      repeated.add(ruleKey);
      b.rule(ruleKey).is(expression);
      invokeRule(ruleKey);
      expression = pop();
      push(new OneOrMoreExpression(expression));
      return null;
    }

    @Override
    public <T> Optional<List<T>> zeroOrMore(T method) {
      oneOrMore(method);
      optional(method);
      return null;
    }

    @Override
    public AstNode invokeRule(GrammarRuleKey ruleKey) {
      push(new RuleExpression(b, ruleKey));
      return null;
    }

    @Override
    public AstNode token(String value) {
      expressionStack.push(new StringExpression(value));
      return null;
    }

    public void replaceByRule(GrammarRuleKey ruleKey, int stackElements) {
      ParsingExpression expression = stackElements == 1 ? pop() : new SequenceExpression(pop(stackElements));
      b.rule(ruleKey).is(expression);

      invokeRule(ruleKey);
    }

    private ParsingExpression[] pop(int n) {
      ParsingExpression[] result = new ParsingExpression[n];
      for (int i = n - 1; i >= 0; i--) {
        result[i] = pop();
      }
      return result;
    }

    private ParsingExpression pop() {
      return expressionStack.pop();
    }

    private void push(ParsingExpression expression) {
      expressionStack.push(expression);
    }

    public GrammarRuleKey ruleKeyForAction(Method method) {
      GrammarRuleKey ruleKey = actions.get(method);
      if (ruleKey == null) {
        method.setAccessible(true);
        ruleKey = new DummyGrammarRuleKey(method);
        actions.put(method, ruleKey);
      }

      return ruleKey;
    }

    @Nullable
    public Method actionForRuleKey(Object ruleKey) {
      return actions.inverse().get(ruleKey);
    }

    @Nullable
    public GrammarRuleKey ruleKeyForMethod(Method method) {
      return mapping.get(method);
    }

    public boolean hasMethodForRuleKey(Object ruleKey) {
      return mapping.containsValue(ruleKey);
    }

    public boolean isOptionalRule(Object ruleKey) {
      return optionals.contains(ruleKey);
    }

    public boolean isRepeatedRule(Object ruleKey) {
      return repeated.contains(ruleKey);
    }

  }

  public static class ActionMethodInterceptor implements MethodInterceptor {

    private final GrammarBuilderInterceptor grammarBuilderInterceptor;

    public ActionMethodInterceptor(GrammarBuilderInterceptor grammarBuilderInterceptor) {
      this.grammarBuilderInterceptor = grammarBuilderInterceptor;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      if (method.getDeclaringClass().equals(Object.class)) {
        return proxy.invokeSuper(obj, args);
      }

      GrammarRuleKey ruleKey = grammarBuilderInterceptor.ruleKeyForAction(method);
      grammarBuilderInterceptor.replaceByRule(ruleKey, args.length);

      return null;
    }

  }

  private static class DummyGrammarRuleKey implements GrammarRuleKey {

    private final Method method;
    private final String operator;
    private final ParsingExpression expression;

    public DummyGrammarRuleKey(Method method) {
      this.method = method;
      this.operator = null;
      this.expression = null;
    }

    public DummyGrammarRuleKey(String operator, ParsingExpression expression) {
      this.method = null;
      this.operator = operator;
      this.expression = expression;
    }

    @Override
    public String toString() {
      if (operator != null) {
        return operator + "(" + expression + ")";
      }

      StringBuilder sb = new StringBuilder();
      sb.append("f.");
      sb.append(method.getName());
      sb.append('(');

      Class[] parameterTypes = method.getParameterTypes();
      for (int i = 0; i < parameterTypes.length - 1; i++) {
        sb.append(parameterTypes[i].getSimpleName());
        sb.append(", ");
      }
      if (parameterTypes.length > 0) {
        sb.append(parameterTypes[parameterTypes.length - 1].getSimpleName());
      }

      sb.append(')');

      return sb.toString();
    }

  }

  private static class RuleExpression implements ParsingExpression {

    private final LexerlessGrammarBuilder b;

    private GrammarRuleKey ruleKey;
    private final GrammarBuilderInterceptor grammarBuilderInterceptor;
    private final Method method;

    public RuleExpression(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey) {
      this.b = b;

      this.ruleKey = ruleKey;
      this.grammarBuilderInterceptor = null;
      this.method = null;
    }

    public RuleExpression(LexerlessGrammarBuilder b, GrammarBuilderInterceptor grammarBuilderInterceptor, Method method) {
      this.b = b;

      this.ruleKey = null;
      this.grammarBuilderInterceptor = grammarBuilderInterceptor;
      this.method = method;
    }

    @Override
    public Instruction[] compile(CompilationHandler compiler) {
      // TODO Horrible
      if (ruleKey == null) {
        ruleKey = grammarBuilderInterceptor.ruleKeyForMethod(method);
        Preconditions.checkState(ruleKey != null, "Cannot find rule key for method: " + method.getName());
      }

      return compiler.compile((ParsingExpression) b.sequence(b.nextNot(b.nothing()), ruleKey));
    }

    @Override
    public String toString() {
      if (ruleKey != null) {
        return ruleKey.toString();
      } else {
        return method.getName() + "()";
      }
    }

  }

}
