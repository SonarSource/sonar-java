/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.java.checks.naming.BadAbstractClassNameCheck;
import org.sonar.java.checks.naming.BadClassNameCheck;
import org.sonar.java.checks.naming.BadConstantNameCheck;
import org.sonar.java.checks.naming.BadFieldNameCheck;
import org.sonar.java.checks.naming.BadFieldNameStaticNonFinalCheck;
import org.sonar.java.checks.naming.BadInterfaceNameCheck;
import org.sonar.java.checks.naming.BadLocalConstantNameCheck;
import org.sonar.java.checks.naming.BadLocalVariableNameCheck;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.checks.naming.BadPackageNameCheck;
import org.sonar.java.checks.naming.BadTestClassNameCheck;
import org.sonar.java.checks.naming.BadTestMethodNameCheck;
import org.sonar.java.checks.naming.BadTypeParameterNameCheck;
import org.sonar.java.checks.naming.BooleanMethodNameCheck;
import org.sonar.java.checks.naming.ClassNamedLikeExceptionCheck;
import org.sonar.java.checks.naming.FieldNameMatchingTypeNameCheck;
import org.sonar.java.checks.naming.KeywordAsIdentifierCheck;
import org.sonar.java.checks.naming.MethodNameSameAsClassCheck;
import org.sonar.java.checks.naming.MethodNamedEqualsCheck;
import org.sonar.java.checks.naming.MethodNamedHashcodeOrEqualCheck;
import org.sonar.java.checks.regex.AnchorPrecedenceCheck;
import org.sonar.java.checks.regex.CanonEqFlagInRegexCheck;
import org.sonar.java.checks.regex.DuplicatesInCharacterClassCheck;
import org.sonar.java.checks.regex.EmptyLineRegexCheck;
import org.sonar.java.checks.regex.EmptyStringRepetitionCheck;
import org.sonar.java.checks.regex.EscapeSequenceControlCharacterCheck;
import org.sonar.java.checks.regex.GraphemeClustersInClassesCheck;
import org.sonar.java.checks.regex.ImpossibleBackReferenceCheck;
import org.sonar.java.checks.regex.ImpossibleBoundariesCheck;
import org.sonar.java.checks.regex.InvalidRegexCheck;
import org.sonar.java.checks.regex.PossessiveQuantifierContinuationCheck;
import org.sonar.java.checks.regex.RedosCheck;
import org.sonar.java.checks.regex.RedundantRegexAlternativesCheck;
import org.sonar.java.checks.regex.RegexComplexityCheck;
import org.sonar.java.checks.regex.RegexLookaheadCheck;
import org.sonar.java.checks.regex.RegexStackOverflowCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierWithEmptyContinuationCheck;
import org.sonar.java.checks.regex.SingleCharacterAlternationCheck;
import org.sonar.java.checks.regex.StringReplaceCheck;
import org.sonar.java.checks.regex.UnicodeAwareCharClassesCheck;
import org.sonar.java.checks.regex.UnicodeCaseCheck;
import org.sonar.java.checks.regex.UnusedGroupNamesCheck;
import org.sonar.java.checks.security.AndroidBroadcastingCheck;
import org.sonar.java.checks.security.AndroidExternalStorageCheck;
import org.sonar.java.checks.security.AuthorizationsStrongDecisionsCheck;
import org.sonar.java.checks.security.CipherBlockChainingCheck;
import org.sonar.java.checks.security.ClearTextProtocolCheck;
import org.sonar.java.checks.security.CookieHttpOnlyCheck;
import org.sonar.java.checks.security.CryptographicKeySizeCheck;
import org.sonar.java.checks.security.DataHashingCheck;
import org.sonar.java.checks.security.DebugFeatureEnabledCheck;
import org.sonar.java.checks.security.DisableAutoEscapingCheck;
import org.sonar.java.checks.security.DisclosingTechnologyFingerprintsCheck;
import org.sonar.java.checks.security.EmptyDatabasePasswordCheck;
import org.sonar.java.checks.security.EncryptionAlgorithmCheck;
import org.sonar.java.checks.security.ExcessiveContentRequestCheck;
import org.sonar.java.checks.security.FilePermissionsCheck;
import org.sonar.java.checks.security.IntegerToHexStringCheck;
import org.sonar.java.checks.security.JWTWithStrongCipherCheck;
import org.sonar.java.checks.security.LDAPAuthenticatedConnectionCheck;
import org.sonar.java.checks.security.LDAPDeserializationCheck;
import org.sonar.java.checks.security.LogConfigurationCheck;
import org.sonar.java.checks.security.OpenSAML2AuthenticationBypassCheck;
import org.sonar.java.checks.security.PasswordEncoderCheck;
import org.sonar.java.checks.security.PubliclyWritableDirectoriesCheck;
import org.sonar.java.checks.security.ReceivingIntentsCheck;
import org.sonar.java.checks.security.SecureCookieCheck;
import org.sonar.java.checks.security.ServerCertificatesCheck;
import org.sonar.java.checks.security.UnpredictableSaltCheck;
import org.sonar.java.checks.security.UserEnumerationCheck;
import org.sonar.java.checks.security.VerifiedServerHostnamesCheck;
import org.sonar.java.checks.security.XxeActiveMQCheck;
import org.sonar.java.checks.security.ZipEntryCheck;
import org.sonar.java.checks.serialization.BlindSerialVersionUidCheck;
import org.sonar.java.checks.serialization.CustomSerializationMethodCheck;
import org.sonar.java.checks.serialization.ExternalizableClassConstructorCheck;
import org.sonar.java.checks.serialization.NonSerializableWriteCheck;
import org.sonar.java.checks.serialization.PrivateReadResolveCheck;
import org.sonar.java.checks.serialization.SerialVersionUidCheck;
import org.sonar.java.checks.serialization.SerializableComparatorCheck;
import org.sonar.java.checks.serialization.SerializableFieldInSerializableClassCheck;
import org.sonar.java.checks.serialization.SerializableObjectInSessionCheck;
import org.sonar.java.checks.serialization.SerializableSuperConstructorCheck;
import org.sonar.java.checks.spring.ControllerWithSessionAttributesCheck;
import org.sonar.java.checks.spring.PersistentEntityUsedAsRequestParameterCheck;
import org.sonar.java.checks.spring.RequestMappingMethodPublicCheck;
import org.sonar.java.checks.spring.SpringAntMatcherOrderCheck;
import org.sonar.java.checks.spring.SpringAutoConfigurationCheck;
import org.sonar.java.checks.spring.SpringBeansShouldBeAccessibleCheck;
import org.sonar.java.checks.spring.SpringComponentWithNonAutowiredMembersCheck;
import org.sonar.java.checks.spring.SpringComponentWithWrongScopeCheck;
import org.sonar.java.checks.spring.SpringComposedRequestMappingCheck;
import org.sonar.java.checks.spring.SpringConfigurationWithAutowiredFieldsCheck;
import org.sonar.java.checks.spring.SpringConstructorInjectionCheck;
import org.sonar.java.checks.spring.SpringIncompatibleTransactionalCheck;
import org.sonar.java.checks.spring.SpringRequestMappingMethodCheck;
import org.sonar.java.checks.spring.SpringScanDefaultPackageCheck;
import org.sonar.java.checks.spring.SpringSecurityDisableCSRFCheck;
import org.sonar.java.checks.spring.SpringSessionFixationCheck;
import org.sonar.java.checks.synchronization.DoubleCheckedLockingCheck;
import org.sonar.java.checks.synchronization.SynchronizationOnGetClassCheck;
import org.sonar.java.checks.synchronization.TwoLocksWaitCheck;
import org.sonar.java.checks.synchronization.ValueBasedObjectUsedForLockCheck;
import org.sonar.java.checks.synchronization.WriteObjectTheOnlySynchronizedMethodCheck;
import org.sonar.java.checks.tests.AssertJApplyConfigurationCheck;
import org.sonar.java.checks.tests.AssertJAssertionsInConsumerCheck;
import org.sonar.java.checks.tests.AssertJChainSimplificationCheck;
import org.sonar.java.checks.tests.AssertJConsecutiveAssertionCheck;
import org.sonar.java.checks.tests.AssertJContextBeforeAssertionCheck;
import org.sonar.java.checks.tests.AssertJTestForEmptinessCheck;
import org.sonar.java.checks.tests.AssertThatThrownByAloneCheck;
import org.sonar.java.checks.tests.AssertTrueInsteadOfDedicatedAssertCheck;
import org.sonar.java.checks.tests.AssertionArgumentOrderCheck;
import org.sonar.java.checks.tests.AssertionCompareToSelfCheck;
import org.sonar.java.checks.tests.AssertionFailInCatchBlockCheck;
import org.sonar.java.checks.tests.AssertionInThreadRunCheck;
import org.sonar.java.checks.tests.AssertionInTryCatchCheck;
import org.sonar.java.checks.tests.AssertionTypesCheck;
import org.sonar.java.checks.tests.AssertionsCompletenessCheck;
import org.sonar.java.checks.tests.AssertionsInTestsCheck;
import org.sonar.java.checks.tests.AssertionsWithoutMessageCheck;
import org.sonar.java.checks.tests.BooleanOrNullLiteralInAssertionsCheck;
import org.sonar.java.checks.tests.CallSuperInTestCaseCheck;
import org.sonar.java.checks.tests.ExpectedExceptionCheck;
import org.sonar.java.checks.tests.IgnoredTestsCheck;
import org.sonar.java.checks.tests.JUnit45MethodAnnotationCheck;
import org.sonar.java.checks.tests.JUnit4AnnotationsCheck;
import org.sonar.java.checks.tests.JUnit5DefaultPackageClassAndMethodCheck;
import org.sonar.java.checks.tests.JUnit5SilentlyIgnoreClassAndMethodCheck;
import org.sonar.java.checks.tests.JUnitCompatibleAnnotationsCheck;
import org.sonar.java.checks.tests.JunitNestedAnnotationCheck;
import org.sonar.java.checks.tests.MockingAllMethodsCheck;
import org.sonar.java.checks.tests.MockitoAnnotatedObjectsShouldBeInitializedCheck;
import org.sonar.java.checks.tests.MockitoArgumentMatchersUsedOnAllParametersCheck;
import org.sonar.java.checks.tests.MockitoEqSimplificationCheck;
import org.sonar.java.checks.tests.NoTestInTestClassCheck;
import org.sonar.java.checks.tests.OneExpectedCheckedExceptionCheck;
import org.sonar.java.checks.tests.OneExpectedRuntimeExceptionCheck;
import org.sonar.java.checks.tests.ParameterizedTestCheck;
import org.sonar.java.checks.tests.RandomizedTestDataCheck;
import org.sonar.java.checks.tests.SpringAssertionsSimplificationCheck;
import org.sonar.java.checks.tests.TestAnnotationWithExpectedExceptionCheck;
import org.sonar.java.checks.tests.TestsStabilityCheck;
import org.sonar.java.checks.tests.ThreadSleepInTestsCheck;
import org.sonar.java.checks.tests.TooManyAssertionsCheck;
import org.sonar.java.checks.unused.UnusedLabelCheck;
import org.sonar.java.checks.unused.UnusedLocalVariableCheck;
import org.sonar.java.checks.unused.UnusedMethodParameterCheck;
import org.sonar.java.checks.unused.UnusedPrivateClassCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.java.checks.unused.UnusedPrivateMethodCheck;
import org.sonar.java.checks.unused.UnusedReturnedDataCheck;
import org.sonar.java.checks.unused.UnusedTestRuleCheck;
import org.sonar.java.checks.unused.UnusedThrowableCheck;
import org.sonar.java.checks.unused.UnusedTypeParameterCheck;
import org.sonar.java.checks.xml.ejb.DefaultInterceptorsLocationCheck;
import org.sonar.java.checks.xml.ejb.InterceptorExclusionsCheck;
import org.sonar.java.checks.xml.hibernate.DatabaseSchemaUpdateCheck;
import org.sonar.java.checks.xml.maven.ArtifactIdNamingConventionCheck;
import org.sonar.java.checks.xml.maven.DependencyWithSystemScopeCheck;
import org.sonar.java.checks.xml.maven.DeprecatedPomPropertiesCheck;
import org.sonar.java.checks.xml.maven.DisallowedDependenciesCheck;
import org.sonar.java.checks.xml.maven.GroupIdNamingConventionCheck;
import org.sonar.java.checks.xml.maven.PomElementOrderCheck;
import org.sonar.java.checks.xml.spring.DefaultMessageListenerContainerCheck;
import org.sonar.java.checks.xml.spring.SingleConnectionFactoryCheck;
import org.sonar.java.checks.xml.struts.ActionNumberCheck;
import org.sonar.java.checks.xml.struts.FormNameDuplicationCheck;
import org.sonar.java.checks.xml.web.ValidationFiltersCheck;
import org.sonar.java.se.checks.BooleanGratuitousExpressionsCheck;
import org.sonar.java.se.checks.ConditionalUnreachableCodeCheck;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.InvariantReturnCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.MapComputeIfAbsentOrPresentCheck;
import org.sonar.java.se.checks.MinMaxRangeCheck;
import org.sonar.java.se.checks.NoWayOutLoopCheck;
import org.sonar.java.se.checks.NonNullSetToNullCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.ObjectOutputStreamCheck;
import org.sonar.java.se.checks.OptionalGetBeforeIsPresentCheck;
import org.sonar.java.se.checks.ParameterNullnessCheck;
import org.sonar.java.se.checks.RedundantAssignmentsCheck;
import org.sonar.java.se.checks.StreamConsumedCheck;
import org.sonar.java.se.checks.StreamNotConsumedCheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.checks.XxeProcessingCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

public final class CheckList {

  public static final String REPOSITORY_KEY = "java";

  private CheckList() {
  }

  public static List<Class<?>> getChecks() {
    return Stream.of(getJavaChecks(), getJavaTestChecks(), getXmlChecks())
      .flatMap(List::stream).collect(Collectors.toList());
  }

  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return Arrays.asList(
      // JavaFileScanner (not IssuableSubscriptionVisitor) ordered from the fastest to the slowest
      IndentationCheck.class,
      LeftCurlyBraceStartLineCheck.class,
      MagicNumberCheck.class,
      LeftCurlyBraceEndLineCheck.class,
      IncorrectOrderOfMembersCheck.class,
      NestedIfStatementsCheck.class,
      SunPackagesUsedCheck.class,
      BadLocalVariableNameCheck.class,
      PackageInfoCheck.class,
      IncrementDecrementInSubExpressionCheck.class,
      RawTypeCheck.class,
      StringLiteralDuplicatedCheck.class,
      RawExceptionCheck.class,
      ClassVariableVisibilityCheck.class,
      BadAbstractClassNameCheck.class,
      AnonymousClassShouldBeLambdaCheck.class,
      CatchUsesExceptionWithContextCheck.class,
      CollapsibleIfCandidateCheck.class,
      MathOnFloatCheck.class,
      OperatorPrecedenceCheck.class,
      UndocumentedApiCheck.class,
      LoggersDeclarationCheck.class,
      ClassCouplingCheck.class,
      SeveralBreakOrContinuePerLoopCheck.class,
      ForLoopCounterChangedCheck.class,
      BadClassNameCheck.class,
      BadPackageNameCheck.class,
      AssignmentInSubExpressionCheck.class,
      StaticMethodCheck.class,
      TooManyParametersCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      SwitchAtLeastThreeCasesCheck.class,
      AnonymousClassesTooBigCheck.class,
      NestedTryCatchCheck.class,
      CollectionIsEmptyCheck.class,
      CompareStringsBoxedTypesWithEqualsCheck.class,
      LambdaTooBigCheck.class,
      MutableMembersUsageCheck.class,
      NestedBlocksCheck.class,
      PublicStaticFieldShouldBeFinalCheck.class,
      DepthOfInheritanceTreeCheck.class,
      OctalValuesCheck.class,
      MissingNewLineAtEndOfFileCheck.class,
      CompareObjectWithEqualsCheck.class,
      CastArithmeticOperandCheck.class,
      LazyArgEvaluationCheck.class,
      InnerStaticClassesCheck.class,

      // IssuableSubscriptionVisitor, ordered from the fastest to the slowest even if the order is not so important
      RedundantModifierCheck.class,
      EmptyStatementUsageCheck.class,
      UppercaseSuffixesCheck.class,
      RightCurlyBraceDifferentLineAsNextBlockCheck.class,
      DiamondOperatorCheck.class,
      BadTypeParameterNameCheck.class,
      LabelsShouldNotBeUsedCheck.class,
      IfElseIfStatementEndsWithElseCheck.class,
      WildcardImportsShouldNotBeUsedCheck.class,
      MissingCurlyBracesCheck.class,
      EscapedUnicodeCharactersCheck.class,
      FieldModifierCheck.class,
      TernaryOperatorCheck.class,
      RightCurlyBraceSameLineAsNextBlockCheck.class,
      CatchExceptionCheck.class,
      BadConstantNameCheck.class,
      SwitchCaseTooBigCheck.class,
      SwitchLastCaseIsDefaultCheck.class,
      DefaultInitializedFieldCheck.class,
      OverrideAnnotationCheck.class,
      ArrayDesignatorOnVariableCheck.class,
      ConstructorCallingOverridableCheck.class,
      NonStaticClassInitializerCheck.class,
      ThrowCheckedExceptionCheck.class,
      BadFieldNameCheck.class,
      StringLiteralInsideEqualsCheck.class,
      LambdaOptionalParenthesisCheck.class,
      SystemOutOrErrUsageCheck.class,
      NestedEnumStaticCheck.class,
      CombineCatchCheck.class,
      SuppressWarningsCheck.class,
      LambdaSingleExpressionCheck.class,
      LambdaTypeParameterCheck.class,
      UnderscoreOnNumberCheck.class,
      BlindSerialVersionUidCheck.class,
      BadMethodNameCheck.class,
      BadFieldNameStaticNonFinalCheck.class,
      VarCanBeUsedCheck.class,
      VarArgCheck.class,
      OneClassInterfacePerFileCheck.class,
      InterfaceAsConstantContainerCheck.class,
      OneDeclarationPerLineCheck.class,
      TrailingCommentCheck.class,
      NPEThrowCheck.class,
      TooManyStatementsPerLineCheck.class,
      BooleanLiteralCheck.class,
      RedundantTypeCastCheck.class,
      NestedSwitchCheck.class,
      UselessParenthesesCheck.class,
      EmptyMethodsCheck.class,
      MethodWithExcessiveReturnsCheck.class,
      VariableDeclarationScopeCheck.class,
      PublicConstructorInAbstractClassCheck.class,
      CognitiveComplexityMethodCheck.class,
      AbstractClassWithoutAbstractMethodCheck.class,
      FinalClassCheck.class,
      MethodComplexityCheck.class,
      FileHeaderCheck.class,
      EmptyBlockCheck.class,
      InnerClassTooManyLinesCheck.class,
      AbstractClassNoFieldShouldBeInterfaceCheck.class,
      ThisExposedFromConstructorCheck.class,
      TooManyMethodsCheck.class,
      AtLeastOneConstructorCheck.class,
      TooLongLineCheck.class,
      KeySetInsteadOfEntrySetCheck.class,
      SynchronizedFieldAssignmentCheck.class,
      NestedTernaryOperatorsCheck.class,
      SerialVersionUidCheck.class,
      CloneOverrideCheck.class,
      SymmetricEqualsCheck.class,
      UtilityClassWithPublicConstructorCheck.class,
      DeprecatedTagPresenceCheck.class,
      VolatileNonPrimitiveFieldCheck.class,
      BooleanMethodNameCheck.class,
      SerializableFieldInSerializableClassCheck.class,
      LoggedRethrownExceptionsCheck.class,
      GetClassLoaderCheck.class,
      NullCheckWithInstanceofCheck.class,
      ModifiersOrderCheck.class,
      ProtectedMemberInFinalClassCheck.class,
      WildcardReturnParameterTypeCheck.class,
      RestrictedIdentifiersUsageCheck.class,
      InterruptedExceptionCheck.class,
      NonShortCircuitLogicCheck.class,
      MissingDeprecatedCheck.class,
      StaticImportCountCheck.class,
      CallToDeprecatedMethodCheck.class,
      TodoTagPresenceCheck.class,
      ReturnOfBooleanExpressionsCheck.class,
      SwitchInsteadOfIfSequenceCheck.class,
      StaticMemberAccessCheck.class,
      StringMethodsWithLocaleCheck.class,
      EnumEqualCheck.class,
      FloatEqualityCheck.class,
      ArrayForVarArgCheck.class,
      DynamicClassLoadCheck.class,
      ExceptionsShouldBeImmutableCheck.class,
      ThrowsSeveralCheckedExceptionCheck.class,
      EmptyClassCheck.class,
      CommentedOutCodeLineCheck.class,
      AssertOnBooleanVariableCheck.class,
      ClassFieldCountCheck.class,
      SelectorMethodArgumentCheck.class,
      BadLocalConstantNameCheck.class,
      DateAndTimesCheck.class,
      HiddenFieldCheck.class,
      ReplaceLambdaByMethodRefCheck.class,
      ShiftOnIntOrLongCheck.class,
      RegexPatternsNeedlesslyCheck.class,
      ConstantsShouldBeStaticFinalCheck.class,
      SimpleClassNameCheck.class,
      ArrayCopyLoopCheck.class,
      VolatileVariablesOperationsCheck.class,
      SwitchDefaultLastCaseCheck.class,
      DanglingElseStatementsCheck.class,
      NioFileDeleteCheck.class,
      OptionalAsParameterCheck.class,
      IndexOfWithPositiveNumberCheck.class,
      CatchOfThrowableOrErrorCheck.class,
      RedundantAbstractMethodCheck.class,
      ConstantMethodCheck.class,
      ObjectFinalizeOverridenCheck.class,
      AssertsOnParametersOfPublicMethodCheck.class,
      MethodNamedHashcodeOrEqualCheck.class,
      MethodTooBigCheck.class,
      RedundantThrowsDeclarationCheck.class,
      SynchronizedOverrideCheck.class,
      UselessExtendsCheck.class,
      CallOuterPrivateMethodCheck.class,
      LoopExecutingAtMostOnceCheck.class,
      DoubleBraceInitializationCheck.class,
      RightCurlyBraceStartLineCheck.class,
      MainMethodThrowsExceptionCheck.class,
      StringPrimitiveConstructorCheck.class,
      InterfaceOrSuperclassShadowingCheck.class,
      UnusedLocalVariableCheck.class,
      TryWithResourcesCheck.class,
      IdenticalCasesInSwitchCheck.class,
      PrintfMisuseCheck.class,
      EqualsNotOverridenWithCompareToCheck.class,
      IteratorNextExceptionCheck.class,
      UnderscoreMisplacedOnNumberCheck.class,
      ServletMethodsExceptionsThrownCheck.class,
      EqualsNotOverriddenInSubclassCheck.class,
      StaticFieldUpateCheck.class,
      ChildClassShadowFieldCheck.class,
      RedundantJumpCheck.class,
      TransientFieldInNonSerializableCheck.class,
      ExpressionComplexityCheck.class,
      ClassComparedByNameCheck.class,
      NotifyCheck.class,
      EqualsOverridenWithHashCodeCheck.class,
      MethodOnlyCallsSuperCheck.class,
      IgnoredReturnValueCheck.class,
      StringToStringCheck.class,
      PublicStaticMutableMembersCheck.class,
      CloneMethodCallsSuperCloneCheck.class,
      ConstructorInjectionCheck.class,
      FixmeTagPresenceCheck.class,
      BigDecimalDoubleConstructorCheck.class,
      UnusedMethodParameterCheck.class,
      BooleanMethodReturnCheck.class,
      UnusedPrivateFieldCheck.class,
      ReturnEmptyArrayNotNullCheck.class,
      SynchronizationOnStringOrBoxedCheck.class,
      LeastSpecificTypeCheck.class,
      SwitchCaseWithoutBreakCheck.class,
      UselessImportCheck.class,
      HardcodedURICheck.class,
      UnusedPrivateMethodCheck.class,
      CallSuperMethodFromInnerClassCheck.class,
      CollectionMethodsWithLinearComplexityCheck.class,
      TabCharacterCheck.class,
      CloneableImplementingCloneCheck.class,
      SynchronizedClassUsageCheck.class,
      AllBranchesAreIdenticalCheck.class,
      EnumMutableFieldCheck.class,
      DeadStoreCheck.class,
      EqualsArgumentTypeCheck.class,
      CipherBlockChainingCheck.class,
      ValueBasedObjectUsedForLockCheck.class,
      TooManyLinesOfCodeInFileCheck.class,
      UnicodeAwareCharClassesCheck.class,
      MethodNamedEqualsCheck.class,
      SyncGetterAndSetterCheck.class,
      ReplaceGuavaWithJavaCheck.class,
      InnerClassOfNonSerializableCheck.class,
      DefaultEncodingUsageCheck.class,
      PrivateFieldUsedLocallyCheck.class,
      ToStringReturningNullCheck.class,
      SubClassStaticReferenceCheck.class,
      CallToDeprecatedCodeMarkedForRemovalCheck.class,
      IgnoredStreamReturnValueCheck.class,
      MethodIdenticalImplementationsCheck.class,
      SpringBeansShouldBeAccessibleCheck.class,
      StrongCipherAlgorithmCheck.class,
      StandardCharsetsConstantsCheck.class,
      ReuseRandomCheck.class,
      ImpossibleBoundariesCheck.class,
      SerializableComparatorCheck.class,
      MembersDifferOnlyByCapitalizationCheck.class,
      PrivateReadResolveCheck.class,
      SystemExitCalledCheck.class,
      ThreadLocalCleanupCheck.class,
      AccessibilityChangeCheck.class,
      StaticFieldUpdateInConstructorCheck.class,
      IgnoredOperationStatusCheck.class,
      StringToPrimitiveConversionCheck.class,
      ConditionalOnNewLineCheck.class,
      StaticFieldInitializationCheck.class,
      CompareToResultTestCheck.class,
      ObjectFinalizeOverridenNotPublicCheck.class,
      IdenticalOperandOnBinaryExpressionCheck.class,
      UnnecessaryEscapeSequencesInTextBlockCheck.class,
      SimpleStringLiteralForSingleLineStringsCheck.class,
      SerializableSuperConstructorCheck.class,
      TextBlockTabsAndSpacesCheck.class,
      CollectionSizeAndArrayLengthCheck.class,
      SynchronizationOnGetClassCheck.class,
      RegexStackOverflowCheck.class,
      SpecializedFunctionalInterfacesCheck.class,
      UnusedLabelCheck.class,
      LoopsOnSameSetCheck.class,
      SpringConfigurationWithAutowiredFieldsCheck.class,
      ConfusingOverloadCheck.class,
      StringReplaceCheck.class,
      LongBitsToDoubleOnIntCheck.class,
      GetRequestedSessionIdCheck.class,
      DoublePrefixOperatorCheck.class,
      MethodParametersOrderCheck.class,
      ServerCertificatesCheck.class,
      SillyEqualsCheck.class,
      ObjectFinalizeOverloadedCheck.class,
      CatchIllegalMonitorStateExceptionCheck.class,
      WeakSSLContextCheck.class,
      ThreadStartedInConstructorCheck.class,
      NoSonarCheck.class,
      PrimitiveWrappersInTernaryOperatorCheck.class,
      CompareToNotOverloadedCheck.class,
      DoubleCheckedLockingAssignmentCheck.class,
      SpringIncompatibleTransactionalCheck.class,
      ThreadOverridesRunCheck.class,
      ArrayHashCodeAndToStringCheck.class,
      StringOffsetMethodsCheck.class,
      SelfAssignementCheck.class,
      GettersSettersOnRightFieldCheck.class,
      UnusedThrowableCheck.class,
      DoubleCheckedLockingCheck.class,
      ForLoopUsedAsWhileLoopCheck.class,
      EmptyStringRepetitionCheck.class,
      SerializableObjectInSessionCheck.class,
      UnreachableCatchCheck.class,
      SynchronizedLockCheck.class,
      PersistentEntityUsedAsRequestParameterCheck.class,
      ControlCharacterInLiteralCheck.class,
      InputStreamReadCheck.class,
      InvalidRegexCheck.class,
      ObjectFinalizeOverridenCallsSuperFinalizeCheck.class,
      ThreadWaitCallCheck.class,
      WaitInSynchronizeCheck.class,
      AbsOnNegativeCheck.class,
      SwitchWithTooManyCasesCheck.class,
      StreamPeekCheck.class,
      NonSerializableWriteCheck.class,
      ServletInstanceFieldCheck.class,
      InappropriateRegexpCheck.class,
      SpringComponentWithNonAutowiredMembersCheck.class,
      ThreadSleepCheck.class,
      UnpredictableSaltCheck.class,
      ThreadRunCheck.class,
      ScheduledThreadPoolExecutorZeroCheck.class,
      AuthorizationsStrongDecisionsCheck.class,
      PrintfFailCheck.class,
      XxeActiveMQCheck.class,
      ImmediateReverseBoxingCheck.class,
      SillyStringOperationsCheck.class,
      UnnecessarySemicolonCheck.class,
      DateFormatWeekYearCheck.class,
      ForLoopFalseConditionCheck.class,
      WrongAssignmentOperatorCheck.class,
      CollectionInappropriateCallsCheck.class,
      NullReturnedOnComputeIfPresentOrAbsentCheck.class,
      JWTWithStrongCipherCheck.class,
      SillyBitOperationCheck.class,
      BooleanInversionCheck.class,
      ImpossibleBackReferenceCheck.class,
      AssertionsInProductionCodeCheck.class,
      UselessIncrementCheck.class,
      ForLoopTerminationConditionCheck.class,
      EmptyLineRegexCheck.class,
      RandomFloatToIntCheck.class,
      ForLoopIncrementSignCheck.class,
      PossessiveQuantifierContinuationCheck.class,
      InnerClassOfSerializableCheck.class,
      CatchRethrowingCheck.class,
      VerifiedServerHostnamesCheck.class,
      EqualsParametersMarkedNonNullCheck.class,
      UnicodeCaseCheck.class,
      OverwrittenKeyCheck.class,
      DateTimeFormatterMismatchCheck.class,
      OpenSAML2AuthenticationBypassCheck.class,
      ForLoopIncrementAndUpdateCheck.class,
      SpringComponentWithWrongScopeCheck.class,
      AnchorPrecedenceCheck.class,
      ExternalizableClassConstructorCheck.class,
      LDAPAuthenticatedConnectionCheck.class,
      ImplementsEnumerationCheck.class,
      TextBlocksInComplexExpressionsCheck.class,
      StaticMultithreadedUnsafeFieldsCheck.class,
      UnusedPrivateClassCheck.class,
      ChangeMethodContractCheck.class,
      UnusedReturnedDataCheck.class,
      EqualsOnAtomicClassCheck.class,
      ClassWithoutHashCodeInHashStructureCheck.class,
      SuspiciousListRemoveCheck.class,
      GraphemeClustersInClassesCheck.class,
      CustomSerializationMethodCheck.class,
      WaitInWhileLoopCheck.class,
      IterableIteratorCheck.class,
      InputStreamOverrideReadCheck.class,
      SpringComposedRequestMappingCheck.class,
      ReadObjectSynchronizedCheck.class,
      StandardFunctionalInterfaceCheck.class,
      ObjectFinalizeCheck.class,
      ControllerWithSessionAttributesCheck.class,
      SpringScanDefaultPackageCheck.class,
      VisibleForTestingUsageCheck.class,
      EscapeSequenceControlCharacterCheck.class,
      ClassNamedLikeExceptionCheck.class,
      SwitchWithLabelsCheck.class,
      IndexOfStartPositionCheck.class,
      ToArrayCheck.class,
      FinalizeFieldsSetCheck.class,
      RunFinalizersCheck.class,
      CompareToReturnValueCheck.class,
      PasswordEncoderCheck.class,
      FilesExistsJDK8Check.class,
      BasicAuthCheck.class,
      ThreadLocalWithInitialCheck.class,
      SpringAutoConfigurationCheck.class,
      HasNextCallingNextCheck.class,
      ArrayDesignatorAfterTypeCheck.class,
      GarbageCollectorCalledCheck.class,
      RedundantCloseCheck.class,
      CallToFileDeleteOnExitMethodCheck.class,
      DateUtilsTruncateCheck.class,
      SpringAntMatcherOrderCheck.class,
      OutputStreamOverrideWriteCheck.class,
      TransactionalMethodVisibilityCheck.class,
      SpringConstructorInjectionCheck.class,
      EmptyDatabasePasswordCheck.class,
      InstanceofUsedOnExceptionCheck.class,
      RegexLookaheadCheck.class,
      IntegerToHexStringCheck.class,
      JdbcDriverExplicitLoadingCheck.class,
      WaitOnConditionCheck.class,
      StringCallsBeyondBoundsCheck.class,
      EncryptionAlgorithmCheck.class,
      ReceivingIntentsCheck.class,
      ObjectCreatedOnlyToCallGetClassCheck.class,
      RedundantRegexAlternativesCheck.class,
      ResultSetIsLastCheck.class,
      ReflectionOnNonRuntimeAnnotationCheck.class,
      DisallowedMethodCheck.class,
      CollectInsteadOfForeachCheck.class,
      ArraysAsListOfPrimitiveToStreamCheck.class,
      CaseInsensitiveComparisonCheck.class,
      ForLoopVariableTypeCheck.class,
      SpringSessionFixationCheck.class,
      TwoLocksWaitCheck.class,
      PrimitivesMarkedNullableCheck.class,
      LoggerClassCheck.class,
      FilePermissionsCheck.class,
      DisallowedConstructorCheck.class,
      MissingBeanValidationCheck.class,
      ToStringUsingBoxingCheck.class,
      ValueBasedObjectsShouldNotBeSerializedCheck.class,
      SpringRequestMappingMethodCheck.class,
      PredictableSeedCheck.class,
      CryptographicKeySizeCheck.class,
      UnusedTypeParameterCheck.class,
      CollectionCallingItselfCheck.class,
      StaticMembersAccessCheck.class,
      JacksonDeserializationCheck.class,
      SpringSecurityDisableCSRFCheck.class,
      ErrorClassExtendedCheck.class,
      PopulateBeansCheck.class,
      ClearTextProtocolCheck.class,
      RedundantStreamCollectCheck.class,
      AndroidExternalStorageCheck.class,
      IsInstanceMethodCheck.class,
      TestsInSeparateFolderCheck.class,
      SwitchCasesShouldBeCommaSeparatedCheck.class,
      OSCommandsPathCheck.class,
      DisableAutoEscapingCheck.class,
      RequestMappingMethodPublicCheck.class,
      PreferStreamAnyMatchCheck.class,
      PseudoRandomCheck.class,
      DisclosingTechnologyFingerprintsCheck.class,
      PreparedStatementAndResultSetCheck.class,
      UserEnumerationCheck.class,
      InvalidDateValuesCheck.class,
      ModulusEqualityCheck.class,
      ReluctantQuantifierCheck.class,
      AnnotationDefaultArgumentCheck.class,
      ThreadAsRunnableArgumentCheck.class,
      AndroidBroadcastingCheck.class,
      CanonEqFlagInRegexCheck.class,
      ZipEntryCheck.class,
      DebugFeatureEnabledCheck.class,
      WriteObjectTheOnlySynchronizedMethodCheck.class,
      ExcessiveContentRequestCheck.class,
      PubliclyWritableDirectoriesCheck.class,
      EnumSetCheck.class,
      LDAPDeserializationCheck.class,
      CustomCryptographicAlgorithmCheck.class,
      ConfusingVarargCheck.class,
      CORSCheck.class,
      DuplicatesInCharacterClassCheck.class,
      ConstantMathCheck.class,
      LogConfigurationCheck.class,
      NoCheckstyleTagPresenceCheck.class,
      DataHashingCheck.class,
      NoPmdTagPresenceCheck.class,
      URLHashCodeAndEqualsCheck.class,
      RegexComplexityCheck.class,
      SQLInjectionCheck.class,
      CommentRegularExpressionCheck.class,
      UnusedGroupNamesCheck.class,
      CookieHttpOnlyCheck.class,
      ReluctantQuantifierWithEmptyContinuationCheck.class,
      SingleCharacterAlternationCheck.class,
      HardCodedCredentialsCheck.class,
      RedosCheck.class,
      SecureCookieCheck.class,
      ClassWithOnlyStaticMethodsInstantiationCheck.class,
      StringConcatToTextBlockCheck.class,
      SwitchRedundantKeywordCheck.class,
      UseSwitchExpressionCheck.class,

      // SECheck, ordered from the fastest to the slowest even if the order of some of them will be forced at runtime
      BooleanGratuitousExpressionsCheck.class,
      ConditionalUnreachableCodeCheck.class,
      NullDereferenceCheck.class,
      MapComputeIfAbsentOrPresentCheck.class,
      InvariantReturnCheck.class,
      OptionalGetBeforeIsPresentCheck.class,
      UnclosedResourcesCheck.class,
      ParameterNullnessCheck.class,
      RedundantAssignmentsCheck.class,
      StreamNotConsumedCheck.class,
      NoWayOutLoopCheck.class,
      DivisionByZeroCheck.class,
      LocksNotUnlockedCheck.class,
      NonNullSetToNullCheck.class,
      MinMaxRangeCheck.class,
      StreamConsumedCheck.class,
      ObjectOutputStreamCheck.class,
      CustomUnclosedResourcesCheck.class,
      XxeProcessingCheck.class,

      // JavaFileScanner (not IssuableSubscriptionVisitor) that rarely raise issues
      StringConcatenationInLoopCheck.class,
      CollectionImplementationReferencedCheck.class,
      CatchNPECheck.class,
      BoxedBooleanExpressionsCheck.class,
      FieldNameMatchingTypeNameCheck.class,
      IndentationAfterConditionalCheck.class,
      DisallowedThreadGroupCheck.class,
      RawByteBitwiseOperationsCheck.class,
      ReturnInFinallyCheck.class,
      DuplicateConditionIfElseIfCheck.class,
      MismatchPackageDirectoryCheck.class,
      MultilineBlocksCurlyBracesCheck.class,
      NullShouldNotBeUsedWithOptionalCheck.class,
      ParsingErrorCheck.class,
      DefaultPackageCheck.class,
      EmptyFileCheck.class,
      KeywordAsIdentifierCheck.class,
      ParameterReassignedToCheck.class,
      StringBufferAndBuilderWithCharCheck.class,
      ThrowsFromFinallyCheck.class,
      UselessPackageInfoCheck.class,
      RepeatAnnotationCheck.class,
      ConcatenationWithStringValueOfCheck.class,
      MethodNameSameAsClassCheck.class,
      InsecureCreateTempFileCheck.class,
      BadInterfaceNameCheck.class,
      CollectionsEmptyConstantsCheck.class,
      TypeParametersShadowingCheck.class,
      HardcodedIpCheck.class,
      PrimitiveTypeBoxingWithToStringCheck.class,
      EnumMapCheck.class,
      DisallowedClassCheck.class);
  }

  // Rule classes are listed alphabetically
  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return Arrays.asList(
      AssertionArgumentOrderCheck.class,
      AssertionCompareToSelfCheck.class,
      AssertionFailInCatchBlockCheck.class,
      AssertionInThreadRunCheck.class,
      AssertionInTryCatchCheck.class,
      AssertionsCompletenessCheck.class,
      AssertionsInTestsCheck.class,
      AssertionTypesCheck.class,
      AssertionsWithoutMessageCheck.class,
      AssertJApplyConfigurationCheck.class,
      AssertJAssertionsInConsumerCheck.class,
      AssertJChainSimplificationCheck.class,
      AssertJConsecutiveAssertionCheck.class,
      AssertJContextBeforeAssertionCheck.class,
      AssertJTestForEmptinessCheck.class,
      AssertThatThrownByAloneCheck.class,
      AssertTrueInsteadOfDedicatedAssertCheck.class,
      BadTestClassNameCheck.class,
      BadTestMethodNameCheck.class,
      BooleanOrNullLiteralInAssertionsCheck.class,
      CallSuperInTestCaseCheck.class,
      ExpectedExceptionCheck.class,
      IgnoredTestsCheck.class,
      JUnitCompatibleAnnotationsCheck.class,
      JUnit4AnnotationsCheck.class,
      JUnit45MethodAnnotationCheck.class,
      JUnit5DefaultPackageClassAndMethodCheck.class,
      JUnit5SilentlyIgnoreClassAndMethodCheck.class,
      JunitNestedAnnotationCheck.class,
      MockingAllMethodsCheck.class,
      MockitoAnnotatedObjectsShouldBeInitializedCheck.class,
      MockitoArgumentMatchersUsedOnAllParametersCheck.class,
      MockitoEqSimplificationCheck.class,
      NoTestInTestClassCheck.class,
      OneExpectedCheckedExceptionCheck.class,
      OneExpectedRuntimeExceptionCheck.class,
      RandomizedTestDataCheck.class,
      SpringAssertionsSimplificationCheck.class,
      ParameterizedTestCheck.class,
      TestAnnotationWithExpectedExceptionCheck.class,
      TestsStabilityCheck.class,
      ThreadSleepInTestsCheck.class,
      TooManyAssertionsCheck.class,
      UnusedTestRuleCheck.class);
  }

  // Rule classes are listed alphabetically
  public static List<Class<? extends SonarXmlCheck>> getXmlChecks() {
    return Arrays.asList(
      ActionNumberCheck.class,
      ArtifactIdNamingConventionCheck.class,
      DatabaseSchemaUpdateCheck.class,
      DefaultInterceptorsLocationCheck.class,
      DefaultMessageListenerContainerCheck.class,
      DependencyWithSystemScopeCheck.class,
      DeprecatedPomPropertiesCheck.class,
      DisallowedDependenciesCheck.class,
      FormNameDuplicationCheck.class,
      GroupIdNamingConventionCheck.class,
      InterceptorExclusionsCheck.class,
      PomElementOrderCheck.class,
      SingleConnectionFactoryCheck.class,
      ValidationFiltersCheck.class);
  }
}
