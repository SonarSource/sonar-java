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
package org.sonar.java.bytecode.se;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.se.testdata.BytecodeTestClass;
import org.sonar.java.bytecode.se.testdata.ExceptionEnqueue;
import org.sonar.java.bytecode.se.testdata.FinalBytecodeTestClass;
import org.sonar.java.bytecode.se.testdata.MaxRelationBytecode;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

public class BytecodeEGWalkerTest {

  private static SquidClassLoader squidClassLoader;
  @Rule
  public LogTester logTester = new LogTester();

  private static SemanticModel semanticModel;

  @BeforeClass
  public static void setUp() throws Exception {
    List<File> files = Lists.newArrayList(new File("target/test-classes"), new File("target/classes"));
    files.addAll(FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar"}, false));
    squidClassLoader = new SquidClassLoader(files);
    semanticModel = SemanticModel.createFor((CompilationUnitTree) JavaParser.createParser().parse("class A {}"), squidClassLoader);
  }

  @Test
  public void generateMethodBehavior() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior("fun(ZLjava/lang/Object;)Ljava/lang/Object;");
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svsecondArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svFirstArg, svsecondArg);
    List<ObjectConstraint> collect = methodBehavior.yields().stream().map(my -> {

      Collection<ProgramState> ps = my.statesAfterInvocation(invocationArguments, new ArrayList<>(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
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
    Collection<ProgramState> ps = nullConstraintResult.statesAfterInvocation(invocationArguments, new ArrayList<>(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(ps).hasSize(1);
    ObjectConstraint constraint = ps.iterator().next().getConstraint(svsecondArg, ObjectConstraint.class);
    assertThat(constraint).isSameAs(ObjectConstraint.NULL);
  }

  @Test
  public void verify_behavior_of_fun2_method() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior("fun2(Z)Ljava/lang/Object;");
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svFirstArg);
    List<HappyPathYield> oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    HappyPathYield yield = oneYield.get(0);
    Collection<ProgramState> pss = yield.statesAfterInvocation(invocationArguments, new ArrayList<>(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ProgramState ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.TRUE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isNull();

    oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NOT_NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    yield = oneYield.get(0);
    pss = yield.statesAfterInvocation(invocationArguments, new ArrayList<>(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.FALSE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isNull();

  }

  @Test
  public void test_int_comparator() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior("int_comparison(II)Ljava/lang/Object;");
    assertThat(methodBehavior.yields()).hasSize(1);
    HappyPathYield methodYield = ((HappyPathYield) methodBehavior.yields().get(0));
    assertThat(methodYield.resultConstraint().get(ObjectConstraint.class)).isSameAs(ObjectConstraint.NULL);
  }

  @Test
  public void goto_terminator() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior("gotoTerminator(Ljava/lang/Object;)Z");
    assertThat(methodBehavior.yields()).hasSize(2);
  }

  @Test
  public void test_method_throwing_exception() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior("throw_exception()V");
    assertThat(methodBehavior.yields()).hasSize(1);
    MethodYield methodYield = methodBehavior.yields().get(0);
    assertThat(methodYield).isInstanceOf(ExceptionalYield.class);
  }

  @Test
  public void test_method_is_complete() {
    String desc = "(Ljava/lang/String;)Z";
    MethodBehavior nativeMethod = getMethodBehavior("nativeMethod" + desc);
    assertThat(nativeMethod.isComplete()).isFalse();

    MethodBehavior abstractMethod = getMethodBehavior("abstractMethod" + desc);
    assertThat(abstractMethod.isComplete()).isFalse();

    MethodBehavior finalMethod = getMethodBehavior("finalMethod" + desc);
    assertThat(finalMethod.isComplete()).isFalse();

    MethodBehavior staticMethod = getMethodBehavior("staticMethod" + desc);
    assertThat(staticMethod.isComplete()).isTrue();

    MethodBehavior privateMethod = getMethodBehavior("privateMethod" + desc);
    assertThat(privateMethod.isComplete()).isFalse();

    MethodBehavior publicMethodInFinalClass = getMethodBehavior(FinalBytecodeTestClass.class, "publicMethod" + desc);
    assertThat(publicMethodInFinalClass.isComplete()).isFalse();
  }

  @Test
  public void method_array() throws Exception {
    BytecodeEGWalker walker = getBytecodeEGWalker(squidClassLoader);
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
    MethodBehavior methodBehavior = getMethodBehavior(BytecodeTestClass.class, "fun(ZLjava/lang/Object;)Ljava/lang/Object;", bytecodeEGWalker);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("Dataflow analysis is incomplete for method org.sonar.java.bytecode.se.testdata.BytecodeTestClass#fun(ZLjava/lang/Object;)Ljava/lang/Object;" +
          " : Too many steps resolving org.sonar.java.bytecode.se.testdata.BytecodeTestClass#fun(ZLjava/lang/Object;)Ljava/lang/Object;");
    assertThat(methodBehavior.isComplete()).isFalse();
    assertThat(methodBehavior.isVisited()).isTrue();
  }

  @Test
  public void unchecked_exceptions_should_be_enqueued() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "test(Lorg/sonar/java/bytecode/se/testdata/ExceptionEnqueue;)Ljava/lang/Object;");
    List<Constraint> resultConstraint = mb.happyPathYields().map(y -> y.resultConstraint().get(ObjectConstraint.class)).collect(Collectors.toList());
    assertThat(resultConstraint).contains(ObjectConstraint.NOT_NULL, ObjectConstraint.NULL);
    List<String> exceptions = mb.exceptionalPathYields().map(y -> y.exceptionType(semanticModel).fullyQualifiedName()).collect(Collectors.toList());
    assertThat(exceptions).contains("org.sonar.java.bytecode.se.testdata.ExceptionEnqueue$ExceptionCatch",
        "org.sonar.java.bytecode.se.testdata.ExceptionEnqueue$ThrowableCatch",
        "org.sonar.java.bytecode.se.testdata.ExceptionEnqueue$ErrorCatch");
  }

  @Test
  public void test_enqueueing_of_catch_blocks() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "testCatchBlockEnqueue(Lorg/sonar/java/bytecode/se/testdata/ExceptionEnqueue;)Z");
    List<HappyPathYield> happyPathYields = mb.happyPathYields().collect(Collectors.toList());
    assertThat(happyPathYields).hasSize(1);
    assertThat(happyPathYields.get(0).resultConstraint()).isNull();
    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(1);
    assertThat(exceptionalYields.get(0).exceptionType(semanticModel).is("java.lang.RuntimeException")).isTrue();
  }

  @Test
  public void test_enqueueing_of_catch_blocks2() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "testCatchBlockEnqueue2()Z");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(1);
    // result should have TRUE constraint, but wrong yield with FALSE constraint is also created
    // and two yields are reduced subsequently
    assertThat(mb.happyPathYields().findFirst().get().resultConstraint()).isNull();
    assertThat(mb.exceptionalPathYields().findFirst().isPresent()).isFalse();
  }

  @Test
  public void test_enqueueing_of_exit_block() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "enqueueExitBlock()Z");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(1);
    assertThat(mb.happyPathYields().findFirst().isPresent()).isFalse();
    ExceptionalYield exceptionalYield = mb.exceptionalPathYields().findFirst().get();
    Type exceptionType = exceptionalYield.exceptionType(semanticModel);
    assertThat(exceptionType.is("java.io.FileNotFoundException")).isTrue();
  }

  @Test
  public void test_enqueueing_of_exit_block2() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "enqueueExitBlock2(Lorg/sonar/java/bytecode/se/testdata/ExceptionEnqueue;)Z");
    List<HappyPathYield> happyPathYields = mb.happyPathYields().collect(Collectors.toList());
    assertThat(happyPathYields).hasSize(1);
    assertThat(happyPathYields.get(0).resultConstraint()).isNull();
    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(1);
    assertThat(exceptionalYields.get(0).exceptionType(semanticModel).is("java.io.IOException")).isTrue();
  }

  @Test
  public void test_enqueueing_of_exit_block3() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "enqueueExitBlock3()Z");
    assertThat(mb.happyPathYields().findFirst().isPresent()).isFalse();
    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(1);
    assertThat(exceptionalYields.get(0).exceptionType(semanticModel).is("java.io.FileNotFoundException")).isTrue();
  }

  @Test
  public void test_enqueueing_of_exit_block4() {
    MethodBehavior mb = getMethodBehavior(ExceptionEnqueue.class, "enqueueExitBlock4()Z");
    List<HappyPathYield> happyPathYields = mb.happyPathYields().collect(Collectors.toList());
    assertThat(happyPathYields.get(0).resultConstraint().hasConstraint(BooleanConstraint.TRUE)).isTrue();
    assertThat(mb.exceptionalPathYields().findFirst().isPresent()).isFalse();
  }

  @Test
  public void propagation_of_bytecode_analysis_exception() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(MaxRelationBytecode.class, "isXMLLetter(C)Z");
    assertThat(methodBehavior.isComplete()).isFalse();
  }

  @Test
  public void test_guava() throws Exception {
    MethodBehavior methodBehavior = getMethodBehavior(ByteStreams.class, "read(Ljava/io/InputStream;[BII)I");
    assertThat(methodBehavior.happyPathYields().anyMatch(y -> y.resultConstraint() == null)).isTrue();
  }

  private static MethodBehavior getMethodBehavior(String signature) {
    return getMethodBehavior(BytecodeTestClass.class, signature);
  }

  private static MethodBehavior getMethodBehavior(Class<?> clazz, String signature) {
    return getMethodBehavior(clazz, signature, getBytecodeEGWalker(squidClassLoader));
  }

  private static MethodBehavior getMethodBehavior(Class<?> clazz, String signature, BytecodeEGWalker walker) {
    return walker.getMethodBehavior(clazz.getCanonicalName() + "#" + signature, squidClassLoader);
  }

  private static BytecodeEGWalker getBytecodeEGWalker(SquidClassLoader squidClassLoader) {
    BehaviorCache behaviorCache = new BehaviorCache(squidClassLoader);
    behaviorCache.setFileContext(null, semanticModel);
    return new BytecodeEGWalker(behaviorCache, semanticModel);
  }

}
