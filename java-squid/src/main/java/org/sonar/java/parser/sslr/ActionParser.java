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
package org.sonar.java.parser.sslr;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.sonar.sslr.api.RecognitionException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.internal.matchers.InputBuffer;
import org.sonar.sslr.internal.vm.FirstOfExpression;
import org.sonar.sslr.internal.vm.ParsingExpression;
import org.sonar.sslr.internal.vm.SequenceExpression;
import org.sonar.sslr.parser.ParseError;
import org.sonar.sslr.parser.ParseErrorFormatter;
import org.sonar.sslr.parser.ParseRunner;
import org.sonar.sslr.parser.ParsingResult;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public class ActionParser {

  private final Charset charset;

  private final SyntaxTreeCreator<Tree> syntaxTreeCreator;
  private final GrammarRuleKey rootRule;
  private final ParseRunner parseRunner;

  public ActionParser(Charset charset, LexerlessGrammarBuilder b, Class grammarClass, Object treeFactory, GrammarRuleKey rootRule) {
    this.charset = charset;

    GrammarBuilderInterceptor grammarBuilderInterceptor = new GrammarBuilderInterceptor(b);
    Enhancer grammarEnhancer = new Enhancer();
    grammarEnhancer.setSuperclass(grammarClass);
    grammarEnhancer.setCallback(grammarBuilderInterceptor);

    ActionMethodInterceptor actionMethodInterceptor = new ActionMethodInterceptor(grammarBuilderInterceptor);
    Enhancer actionEnhancer = new Enhancer();
    actionEnhancer.setSuperclass(treeFactory.getClass());
    actionEnhancer.setCallback(actionMethodInterceptor);

    Object grammar = grammarEnhancer.create(
      new Class[] {GrammarBuilder.class, treeFactory.getClass()},
      new Object[] {grammarBuilderInterceptor, actionEnhancer.create()});

    for (Method method : grammarClass.getMethods()) {
      if (method.getDeclaringClass().equals(Object.class)) {
        continue;
      }

      try {
        method.invoke(grammar);
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw Throwables.propagate(e);
      }
    }

    this.syntaxTreeCreator = new SyntaxTreeCreator<>(treeFactory, grammarBuilderInterceptor);

    b.setRootRule(rootRule);
    this.rootRule = rootRule;
    this.parseRunner = new ParseRunner(b.build().getRootRule());
  }

  public Tree parse(File file) {
    try {
      return parse(new Input(Files.toString(file, charset).toCharArray(), file.toURI()));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public Tree parse(String source) {
    return parse(new Input(source.toCharArray()));
  }

  private Tree parse(Input input) {
    ParsingResult result = parseRunner.parse(input.input());

    if (!result.isMatched()) {
      ParseError parseError = result.getParseError();
      InputBuffer inputBuffer = parseError.getInputBuffer();
      int line = inputBuffer.getPosition(parseError.getErrorIndex()).getLine();
      String message = new ParseErrorFormatter().format(parseError);
      throw new RecognitionException(line, message);
    }
    return syntaxTreeCreator.create(result.getParseTreeRoot(), input);
  }

  public GrammarRuleKey rootRule() {
    return rootRule;
  }

  public static class GrammarBuilderInterceptor implements MethodInterceptor, GrammarBuilder, NonterminalBuilder {

    private final LexerlessGrammarBuilder b;
    private final BiMap<Method, GrammarRuleKey> mapping = HashBiMap.create();
    private final BiMap<Method, GrammarRuleKey> actions = HashBiMap.create();
    private final Set<GrammarRuleKey> optionals = Sets.newHashSet();
    private final Set<GrammarRuleKey> oneOrMores = Sets.newHashSet();
    private final Set<GrammarRuleKey> zeroOrMores = Sets.newHashSet();

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
        push(new DelayedRuleInvocationExpression(b, this, method));
        return null;
      }

      buildingMethod = method;

      return proxy.invokeSuper(obj, args);
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
      GrammarRuleKey grammarRuleKey = new DummyGrammarRuleKey("optional", expression);
      optionals.add(grammarRuleKey);
      b.rule(grammarRuleKey).is(b.optional(expression));
      token(grammarRuleKey);
      return null;
    }

    @Override
    public <T> List<T> oneOrMore(T method) {
      ParsingExpression expression = pop();
      GrammarRuleKey grammarRuleKey = new DummyGrammarRuleKey("oneOrMore", expression);
      oneOrMores.add(grammarRuleKey);
      b.rule(grammarRuleKey).is(b.oneOrMore(expression));
      token(grammarRuleKey);
      return null;
    }

    @Override
    public <T> Optional<List<T>> zeroOrMore(T method) {
      ParsingExpression expression = pop();
      GrammarRuleKey grammarRuleKey = new DummyGrammarRuleKey("zeroOrMore", expression);
      zeroOrMores.add(grammarRuleKey);
      b.rule(grammarRuleKey).is(b.zeroOrMore(expression));
      token(grammarRuleKey);
      return null;
    }

    @Override
    public InternalSyntaxToken token(GrammarRuleKey grammarRuleKey) {
      push(new DelayedRuleInvocationExpression(b, grammarRuleKey));
      return null;
    }

    public void replaceByRule(GrammarRuleKey grammarRuleKey, int stackElements) {
      ParsingExpression expression = stackElements == 1 ? pop() : new SequenceExpression(pop(stackElements));
      b.rule(grammarRuleKey).is(expression);
      token(grammarRuleKey);
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
      GrammarRuleKey grammarRuleKey = actions.get(method);
      if (grammarRuleKey == null) {
        method.setAccessible(true);
        grammarRuleKey = new DummyGrammarRuleKey(method);
        actions.put(method, grammarRuleKey);
      }
      return grammarRuleKey;
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

    public boolean isOneOrMoreRule(Object ruleKey) {
      return oneOrMores.contains(ruleKey);
    }

    public boolean isZeroOrMoreRule(Object ruleKey) {
      return zeroOrMores.contains(ruleKey);
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

}
