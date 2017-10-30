/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.bytecode.se;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;

public class BytecodeEGWalkerTest {


  private static SquidClassLoader squidClassLoader;
  @Rule
  public LogTester logTester  = new LogTester();

  private static SemanticModel semanticModel;

  @BeforeClass
  public static void setUp() throws Exception {
    squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    semanticModel = SemanticModel.createFor((CompilationUnitTree) JavaParser.createParser().parse("class A {}"), squidClassLoader);
  }

  @Test
  public void generateMethodBehavior() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(0);
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svsecondArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svFirstArg, svsecondArg);
    List<ObjectConstraint> collect = methodBehavior.yields().stream().map(my -> {

      Collection<ProgramState> ps = my.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
      assertThat(ps).hasSize(1);
      ProgramState next = ps.iterator().next();
      return next.getConstraint(svResult, ObjectConstraint.class);
    })
      .collect(Collectors.toList());
    assertThat(collect).hasSize(2).containsOnly(ObjectConstraint.NOT_NULL, ObjectConstraint.NULL);

    List<HappyPathYield> nullConstraintOnResult =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());
    assertThat(nullConstraintOnResult).hasSize(1);
    HappyPathYield nullConstraintResult = nullConstraintOnResult.get(0);
    Collection<ProgramState> ps = nullConstraintResult.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(ps).hasSize(1);
    ObjectConstraint constraint = ps.iterator().next().getConstraint(svsecondArg, ObjectConstraint.class);
    assertThat(constraint).isSameAs(ObjectConstraint.NULL);
  }

  @Test
  public void verify_behavior_of_fun2_method() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(1);
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svFirstArg);
    List<HappyPathYield> oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    HappyPathYield yield = oneYield.get(0);
    Collection<ProgramState> pss = yield.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ProgramState ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.TRUE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isSameAs(DivisionByZeroCheck.ZeroConstraint.NON_ZERO);

    oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NOT_NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    yield = oneYield.get(0);
    pss = yield.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.FALSE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isSameAs(DivisionByZeroCheck.ZeroConstraint.ZERO);

  }

  @Test
  public void test_int_comparator() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(2);
    assertThat(methodBehavior.yields()).hasSize(1);
    HappyPathYield methodYield = ((HappyPathYield) methodBehavior.yields().get(0));
    assertThat(methodYield.resultConstraint().get(ObjectConstraint.class)).isSameAs(ObjectConstraint.NULL);
  }

  @Test
  public void goto_terminator() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(3);
    assertThat(methodBehavior.yields()).hasSize(2);
  }

  @Test
  public void test_method_throwing_exception() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(4);
    assertThat(methodBehavior.yields()).hasSize(1);
    MethodYield methodYield = methodBehavior.yields().get(0);
    assertThat(methodYield).isInstanceOf(ExceptionalYield.class);
  }

  @Test
  public void test_method_is_complete() {
    MethodBehavior nativeMethod = getMethodBehavior("nativeMethod");
    assertThat(nativeMethod.isComplete()).isFalse();

    MethodBehavior abstractMethod = getMethodBehavior("abstractMethod");
    assertThat(abstractMethod.isComplete()).isFalse();

    MethodBehavior finalMethod = getMethodBehavior("finalMethod");
    assertThat(finalMethod.isComplete()).isTrue();

    MethodBehavior staticMethod = getMethodBehavior("staticMethod");
    assertThat(staticMethod.isComplete()).isTrue();

    MethodBehavior privateMethod = getMethodBehavior("privateMethod");
    assertThat(privateMethod.isComplete()).isTrue();

    MethodBehavior publicMethodInFinalClass = getMethodBehavior("FinalInnerClass", finalInnerClass -> ((MethodTree) finalInnerClass.members().get(0)).symbol());
    assertThat(publicMethodInFinalClass.isComplete()).isTrue();
  }

  @Test
  public void method_array() throws Exception {
    BytecodeEGWalker walker = getBytecodeEGWalker(squidClassLoader, null);

    MethodBehavior behavior = walker.getMethodBehavior("java.lang.Class[]#clone()Ljava/lang/Object;", squidClassLoader);

    assertThat(behavior).isNull();
  }

  @Test
  public void test_starting_states() throws Exception {
    BytecodeEGWalker walker = new BytecodeEGWalker(null, semanticModel);

    String signature = "type#foo()V";
    walker.methodBehavior = new MethodBehavior(signature);
    ProgramState startingState = Iterables.getOnlyElement(walker.startingStates(signature, ProgramState.EMPTY_STATE, false));
    SymbolicValue thisSv = startingState.getValue(0);
    assertThat(thisSv).isNotNull();
    assertThat(startingState.getConstraints(thisSv).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    startingState = Iterables.getOnlyElement(walker.startingStates(signature, ProgramState.EMPTY_STATE, true));
    assertThat(startingState).isEqualTo(ProgramState.EMPTY_STATE);

    signature = "type#foo(DIJ)V";
    walker.methodBehavior = new MethodBehavior(signature);
    startingState = Iterables.getOnlyElement(walker.startingStates(signature, ProgramState.EMPTY_STATE, true));
    assertThat(startingState.getValue(0)).isNotNull();
    SymbolicValue doubleArg = startingState.getValue(0);
    assertThat(startingState.getConstraint(doubleArg, BytecodeEGWalker.StackValueCategoryConstraint.class)).isEqualTo(BytecodeEGWalker.StackValueCategoryConstraint.LONG_OR_DOUBLE);
    assertThat(startingState.getValue(1)).isNull();
    assertThat(startingState.getValue(2)).isNotNull();
    SymbolicValue longArg = startingState.getValue(3);
    assertThat(longArg).isNotNull();
    assertThat(startingState.getConstraint(longArg, BytecodeEGWalker.StackValueCategoryConstraint.class)).isEqualTo(BytecodeEGWalker.StackValueCategoryConstraint.LONG_OR_DOUBLE);
  }

  @Test
  public void max_step_exception_should_log_warning_and_generate_behavior() {
    BytecodeEGWalker bytecodeEGWalker = new BytecodeEGWalker(new BehaviorCache(squidClassLoader), semanticModel) {
      @Override
      int maxSteps() {
        return 2;
      }
    };
    File file = new File("src/test/java/org/sonar/java/bytecode/se/BytecodeEGWalkerTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    ClassTree innerClass = getClass(tree, "InnerClass");
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) innerClass.symbol().lookupSymbols("fun").stream().findFirst().get();
    MethodBehavior methodBehavior = bytecodeEGWalker.getMethodBehavior(methodSymbol.completeSignature(), squidClassLoader);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("Dataflow analysis is incomplete for method org.sonar.java.bytecode.se.BytecodeEGWalkerTest$InnerClass#fun(ZLjava/lang/Object;)Ljava/lang/Object; : Too many steps resolving org.sonar.java.bytecode.se.BytecodeEGWalkerTest$InnerClass#fun(ZLjava/lang/Object;)Ljava/lang/Object;");
    assertThat(methodBehavior.isComplete()).isFalse();
    assertThat(methodBehavior.isVisited()).isTrue();
  }

  private MethodBehavior getMethodBehavior(int index) {
    return getMethodBehavior("InnerClass", innerClass -> ((MethodTree) innerClass.members().get(index)).symbol());
  }

  private static MethodBehavior getMethodBehavior(String methodName) {
    return getMethodBehavior("InnerClass", innerClass -> (Symbol.MethodSymbol) innerClass.symbol().lookupSymbols(methodName).stream().findFirst().get());
  }

  private static MethodBehavior getMethodBehavior(String targetClass, Function<ClassTree, Symbol.MethodSymbol> methodFinder) {
    File file = new File("src/test/java/org/sonar/java/bytecode/se/BytecodeEGWalkerTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel semanticModel = SemanticModel.createFor(tree, squidClassLoader);
    BytecodeEGWalker bytecodeEGWalker = getBytecodeEGWalker(squidClassLoader, semanticModel);
    ClassTree innerClass = getClass(tree, targetClass);
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) methodFinder.apply(innerClass);
    return bytecodeEGWalker.getMethodBehavior(methodSymbol.completeSignature(), squidClassLoader);
  }

  @Test
  public void name() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(new ArrayList<>());
    MethodBehavior methodBehavior = getBytecodeEGWalker(classLoader, null).getMethodBehavior("java.lang.Package#getPackages()L$Array", classLoader);
    System.out.println(methodBehavior.yields());

  }

  private static BytecodeEGWalker getBytecodeEGWalker(SquidClassLoader squidClassLoader, SemanticModel semanticModel) {
    BehaviorCache behaviorCache = new BehaviorCache(squidClassLoader);
    behaviorCache.setFileContext(null, semanticModel);
    return new BytecodeEGWalker(behaviorCache, semanticModel);
  }

  private static ClassTree getClass(CompilationUnitTree cut, String className) {
    ClassTree testClass = (ClassTree) cut.types().get(0);
    return testClass.members().stream()
      .filter(m -> m.is(Tree.Kind.CLASS))
      .map(ClassTree.class::cast)
      .filter(ct -> className.equals(ct.simpleName().name()))
      .findFirst()
      .get();
  }

  /**
   * --------------------- following code is used for byte code ----------------------------------------------
   */
  abstract static class InnerClass {
    private Object fun(boolean a, Object b) {
      if (b == null) {
        return null;
      }
      return "";
    }

    private Object fun2(boolean a) {
      if (a) {
        return null;
      }
      return "";
    }

    private Object int_comparison(int a, int b) {
      if (a < b) {
        if (a < b) {
          return null;
        }
        return "";
      }
      return null;
    }

    private boolean gotoTerminator(Object o) {
      return o == null;
    }

    private void throw_exception() {
      throw new RuntimeException();
    }

    abstract boolean abstractMethod(String s);
    static native boolean nativeMethod(String s);
    final boolean finalMethod(String s) { return true; }
    static boolean staticMethod(String s) { return true; }
    private boolean privateMethod(String s) { return true; }
  }

  final static class FinalInnerClass {
    public boolean publicMethod(String s) { return true; }
  }
}
