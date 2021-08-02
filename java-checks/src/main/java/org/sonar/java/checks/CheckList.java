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
import org.sonar.java.checks.serialization.RecordSerializationIgnoredMembersCheck;
import org.sonar.java.checks.serialization.SerialVersionUidCheck;
import org.sonar.java.checks.serialization.SerialVersionUidInRecordCheck;
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
import org.sonar.java.checks.xml.ejb.InterceptorExclusionsCheck;
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
      // fast JavaFileScanner (not IssuableSubscriptionVisitor) ordered from the fastest to the slowest
      LeftCurlyBraceEndLineCheck.class,
      IndentationCheck.class,
      IncorrectOrderOfMembersCheck.class,
      MagicNumberCheck.class,
      NestedIfStatementsCheck.class,
      BadAbstractClassNameCheck.class,
      RawExceptionCheck.class,
      PackageInfoCheck.class,
      OperatorPrecedenceCheck.class,
      RawTypeCheck.class,

      // IssuableSubscriptionVisitor rules ordered alphabetically
      AbsOnNegativeCheck.class,
      AbstractClassNoFieldShouldBeInterfaceCheck.class,
      AbstractClassWithoutAbstractMethodCheck.class,
      AccessibilityChangeCheck.class,
      AccessibilityChangeOnRecordsCheck.class,
      AllBranchesAreIdenticalCheck.class,
      AnchorPrecedenceCheck.class,
      AndroidBroadcastingCheck.class,
      AndroidExternalStorageCheck.class,
      AnnotationDefaultArgumentCheck.class,
      ArrayCopyLoopCheck.class,
      ArrayDesignatorAfterTypeCheck.class,
      ArrayDesignatorOnVariableCheck.class,
      ArrayForVarArgCheck.class,
      ArrayHashCodeAndToStringCheck.class,
      ArraysAsListOfPrimitiveToStreamCheck.class,
      AssertOnBooleanVariableCheck.class,
      AssertionsInProductionCodeCheck.class,
      AssertsOnParametersOfPublicMethodCheck.class,
      AtLeastOneConstructorCheck.class,
      AuthorizationsStrongDecisionsCheck.class,
      BadConstantNameCheck.class,
      BadFieldNameCheck.class,
      BadFieldNameStaticNonFinalCheck.class,
      BadLocalConstantNameCheck.class,
      BadMethodNameCheck.class,
      BadTypeParameterNameCheck.class,
      BasicAuthCheck.class,
      BigDecimalDoubleConstructorCheck.class,
      BlindSerialVersionUidCheck.class,
      BooleanInversionCheck.class,
      BooleanLiteralCheck.class,
      BooleanMethodNameCheck.class,
      BooleanMethodReturnCheck.class,
      CORSCheck.class,
      CallOuterPrivateMethodCheck.class,
      CallSuperMethodFromInnerClassCheck.class,
      CallToDeprecatedCodeMarkedForRemovalCheck.class,
      CallToDeprecatedMethodCheck.class,
      CallToFileDeleteOnExitMethodCheck.class,
      CanonEqFlagInRegexCheck.class,
      CaseInsensitiveComparisonCheck.class,
      CatchExceptionCheck.class,
      CatchIllegalMonitorStateExceptionCheck.class,
      CatchOfThrowableOrErrorCheck.class,
      CatchRethrowingCheck.class,
      ChangeMethodContractCheck.class,
      ChildClassShadowFieldCheck.class,
      CipherBlockChainingCheck.class,
      ClassComparedByNameCheck.class,
      ClassFieldCountCheck.class,
      ClassNamedLikeExceptionCheck.class,
      ClassWithOnlyStaticMethodsInstantiationCheck.class,
      ClassWithoutHashCodeInHashStructureCheck.class,
      ClearTextProtocolCheck.class,
      CloneMethodCallsSuperCloneCheck.class,
      CloneOverrideCheck.class,
      CloneableImplementingCloneCheck.class,
      CognitiveComplexityMethodCheck.class,
      CollectInsteadOfForeachCheck.class,
      CollectionCallingItselfCheck.class,
      CollectionInappropriateCallsCheck.class,
      CollectionMethodsWithLinearComplexityCheck.class,
      CollectionSizeAndArrayLengthCheck.class,
      CollectorsToListCheck.class,
      CombineCatchCheck.class,
      CommentRegularExpressionCheck.class,
      CommentedOutCodeLineCheck.class,
      CompareToNotOverloadedCheck.class,
      CompareToResultTestCheck.class,
      CompareToReturnValueCheck.class,
      ConditionalOnNewLineCheck.class,
      ConfusingOverloadCheck.class,
      ConfusingVarargCheck.class,
      ConstantMathCheck.class,
      ConstantMethodCheck.class,
      ConstantsShouldBeStaticFinalCheck.class,
      ConstructorCallingOverridableCheck.class,
      ConstructorInjectionCheck.class,
      ControlCharacterInLiteralCheck.class,
      ControllerWithSessionAttributesCheck.class,
      CookieHttpOnlyCheck.class,
      CryptographicKeySizeCheck.class,
      CustomCryptographicAlgorithmCheck.class,
      CustomSerializationMethodCheck.class,
      DanglingElseStatementsCheck.class,
      DataHashingCheck.class,
      DateAndTimesCheck.class,
      DateFormatWeekYearCheck.class,
      DateTimeFormatterMismatchCheck.class,
      DateUtilsTruncateCheck.class,
      DeadStoreCheck.class,
      DebugFeatureEnabledCheck.class,
      DefaultEncodingUsageCheck.class,
      DefaultInitializedFieldCheck.class,
      DeprecatedTagPresenceCheck.class,
      DiamondOperatorCheck.class,
      DisableAutoEscapingCheck.class,
      DisallowedConstructorCheck.class,
      DisallowedMethodCheck.class,
      DisclosingTechnologyFingerprintsCheck.class,
      DoubleBraceInitializationCheck.class,
      DoubleCheckedLockingAssignmentCheck.class,
      DoubleCheckedLockingCheck.class,
      DoublePrefixOperatorCheck.class,
      DuplicatesInCharacterClassCheck.class,
      DynamicClassLoadCheck.class,
      EmptyBlockCheck.class,
      EmptyClassCheck.class,
      EmptyDatabasePasswordCheck.class,
      EmptyLineRegexCheck.class,
      EmptyMethodsCheck.class,
      EmptyStatementUsageCheck.class,
      EmptyStringRepetitionCheck.class,
      EncryptionAlgorithmCheck.class,
      EnumEqualCheck.class,
      EnumMutableFieldCheck.class,
      EnumSetCheck.class,
      EqualsArgumentTypeCheck.class,
      EqualsNotOverriddenInSubclassCheck.class,
      EqualsNotOverridenWithCompareToCheck.class,
      EqualsOnAtomicClassCheck.class,
      EqualsOverridenWithHashCodeCheck.class,
      EqualsParametersMarkedNonNullCheck.class,
      ErrorClassExtendedCheck.class,
      EscapeSequenceControlCharacterCheck.class,
      EscapedUnicodeCharactersCheck.class,
      ExceptionsShouldBeImmutableCheck.class,
      ExcessiveContentRequestCheck.class,
      ExpressionComplexityCheck.class,
      ExternalizableClassConstructorCheck.class,
      FieldModifierCheck.class,
      FileHeaderCheck.class,
      FilePermissionsCheck.class,
      FilesExistsJDK8Check.class,
      FinalClassCheck.class,
      FinalizeFieldsSetCheck.class,
      FixmeTagPresenceCheck.class,
      FloatEqualityCheck.class,
      ForLoopFalseConditionCheck.class,
      ForLoopIncrementAndUpdateCheck.class,
      ForLoopIncrementSignCheck.class,
      ForLoopTerminationConditionCheck.class,
      ForLoopUsedAsWhileLoopCheck.class,
      ForLoopVariableTypeCheck.class,
      GarbageCollectorCalledCheck.class,
      GetClassLoaderCheck.class,
      GetRequestedSessionIdCheck.class,
      GettersSettersOnRightFieldCheck.class,
      GraphemeClustersInClassesCheck.class,
      HardCodedCredentialsCheck.class,
      HardcodedURICheck.class,
      HasNextCallingNextCheck.class,
      HiddenFieldCheck.class,
      IdenticalCasesInSwitchCheck.class,
      IdenticalOperandOnBinaryExpressionCheck.class,
      IfElseIfStatementEndsWithElseCheck.class,
      IgnoredOperationStatusCheck.class,
      IgnoredReturnValueCheck.class,
      IgnoredStreamReturnValueCheck.class,
      ImmediateReverseBoxingCheck.class,
      ImplementsEnumerationCheck.class,
      ImpossibleBackReferenceCheck.class,
      ImpossibleBoundariesCheck.class,
      InappropriateRegexpCheck.class,
      IndexOfStartPositionCheck.class,
      IndexOfWithPositiveNumberCheck.class,
      InnerClassOfNonSerializableCheck.class,
      InnerClassOfSerializableCheck.class,
      InnerClassTooManyLinesCheck.class,
      InputStreamOverrideReadCheck.class,
      InputStreamReadCheck.class,
      InstanceOfPatternMatchingCheck.class,
      InstanceofUsedOnExceptionCheck.class,
      IntegerToHexStringCheck.class,
      InterfaceAsConstantContainerCheck.class,
      InterfaceOrSuperclassShadowingCheck.class,
      InterruptedExceptionCheck.class,
      InvalidDateValuesCheck.class,
      InvalidRegexCheck.class,
      IsInstanceMethodCheck.class,
      IterableIteratorCheck.class,
      IteratorNextExceptionCheck.class,
      JWTWithStrongCipherCheck.class,
      JacksonDeserializationCheck.class,
      JdbcDriverExplicitLoadingCheck.class,
      KeySetInsteadOfEntrySetCheck.class,
      LDAPAuthenticatedConnectionCheck.class,
      LDAPDeserializationCheck.class,
      LabelsShouldNotBeUsedCheck.class,
      LambdaOptionalParenthesisCheck.class,
      LambdaSingleExpressionCheck.class,
      LambdaTypeParameterCheck.class,
      LeastSpecificTypeCheck.class,
      LogConfigurationCheck.class,
      LoggedRethrownExceptionsCheck.class,
      LoggerClassCheck.class,
      LongBitsToDoubleOnIntCheck.class,
      LoopExecutingAtMostOnceCheck.class,
      LoopsOnSameSetCheck.class,
      MainMethodThrowsExceptionCheck.class,
      MembersDifferOnlyByCapitalizationCheck.class,
      MethodComplexityCheck.class,
      MethodIdenticalImplementationsCheck.class,
      MethodNamedEqualsCheck.class,
      MethodNamedHashcodeOrEqualCheck.class,
      MethodOnlyCallsSuperCheck.class,
      MethodParametersOrderCheck.class,
      MethodTooBigCheck.class,
      MethodWithExcessiveReturnsCheck.class,
      MissingBeanValidationCheck.class,
      MissingCurlyBracesCheck.class,
      MissingDeprecatedCheck.class,
      MissingOverridesInRecordWithArrayComponentCheck.class,
      ModifiersOrderCheck.class,
      ModulusEqualityCheck.class,
      NPEThrowCheck.class,
      NestedEnumStaticCheck.class,
      NestedSwitchCheck.class,
      NestedTernaryOperatorsCheck.class,
      NioFileDeleteCheck.class,
      NoCheckstyleTagPresenceCheck.class,
      NoPmdTagPresenceCheck.class,
      NoSonarCheck.class,
      NonSerializableWriteCheck.class,
      NonShortCircuitLogicCheck.class,
      NonStaticClassInitializerCheck.class,
      NotifyCheck.class,
      NullCheckWithInstanceofCheck.class,
      NullReturnedOnComputeIfPresentOrAbsentCheck.class,
      OSCommandsPathCheck.class,
      ObjectCreatedOnlyToCallGetClassCheck.class,
      ObjectFinalizeCheck.class,
      ObjectFinalizeOverloadedCheck.class,
      ObjectFinalizeOverridenCallsSuperFinalizeCheck.class,
      ObjectFinalizeOverridenCheck.class,
      ObjectFinalizeOverridenNotPublicCheck.class,
      OneClassInterfacePerFileCheck.class,
      OneDeclarationPerLineCheck.class,
      OpenSAML2AuthenticationBypassCheck.class,
      OptionalAsParameterCheck.class,
      OutputStreamOverrideWriteCheck.class,
      OverrideAnnotationCheck.class,
      OverwrittenKeyCheck.class,
      PasswordEncoderCheck.class,
      PersistentEntityUsedAsRequestParameterCheck.class,
      PopulateBeansCheck.class,
      PossessiveQuantifierContinuationCheck.class,
      PredictableSeedCheck.class,
      PreferStreamAnyMatchCheck.class,
      PreparedStatementAndResultSetCheck.class,
      PrimitiveWrappersInTernaryOperatorCheck.class,
      PrimitivesMarkedNullableCheck.class,
      PrintfFailCheck.class,
      PrintfMisuseCheck.class,
      PrivateFieldUsedLocallyCheck.class,
      PrivateReadResolveCheck.class,
      ProtectedMemberInFinalClassCheck.class,
      PseudoRandomCheck.class,
      PublicConstructorInAbstractClassCheck.class,
      PublicStaticMutableMembersCheck.class,
      PubliclyWritableDirectoriesCheck.class,
      RandomFloatToIntCheck.class,
      ReadObjectSynchronizedCheck.class,
      ReceivingIntentsCheck.class,
      RecordDuplicatedGetterCheck.class,
      RecordInsteadOfClassCheck.class,
      RecordSerializationIgnoredMembersCheck.class,
      RedosCheck.class,
      RedundantAbstractMethodCheck.class,
      RedundantCloseCheck.class,
      RedundantJumpCheck.class,
      RedundantModifierCheck.class,
      RedundantRecordMethodsCheck.class,
      RedundantRegexAlternativesCheck.class,
      RedundantStreamCollectCheck.class,
      RedundantThrowsDeclarationCheck.class,
      RedundantTypeCastCheck.class,
      ReflectionOnNonRuntimeAnnotationCheck.class,
      RegexComplexityCheck.class,
      RegexLookaheadCheck.class,
      RegexPatternsNeedlesslyCheck.class,
      RegexStackOverflowCheck.class,
      ReluctantQuantifierCheck.class,
      ReluctantQuantifierWithEmptyContinuationCheck.class,
      ReplaceGuavaWithJavaCheck.class,
      ReplaceLambdaByMethodRefCheck.class,
      RequestMappingMethodPublicCheck.class,
      RestrictedIdentifiersUsageCheck.class,
      ResultSetIsLastCheck.class,
      ReturnEmptyArrayNotNullCheck.class,
      ReturnOfBooleanExpressionsCheck.class,
      ReuseRandomCheck.class,
      RightCurlyBraceDifferentLineAsNextBlockCheck.class,
      RightCurlyBraceSameLineAsNextBlockCheck.class,
      RightCurlyBraceStartLineCheck.class,
      RunFinalizersCheck.class,
      SQLInjectionCheck.class,
      ScheduledThreadPoolExecutorZeroCheck.class,
      SecureCookieCheck.class,
      SelectorMethodArgumentCheck.class,
      SelfAssignementCheck.class,
      SerialVersionUidCheck.class,
      SerializableComparatorCheck.class,
      SerializableFieldInSerializableClassCheck.class,
      SerializableObjectInSessionCheck.class,
      SerializableSuperConstructorCheck.class,
      SerialVersionUidInRecordCheck.class,
      ServerCertificatesCheck.class,
      ServletInstanceFieldCheck.class,
      ServletMethodsExceptionsThrownCheck.class,
      ShiftOnIntOrLongCheck.class,
      SillyBitOperationCheck.class,
      SillyEqualsCheck.class,
      SillyStringOperationsCheck.class,
      SimpleClassNameCheck.class,
      SimpleStringLiteralForSingleLineStringsCheck.class,
      SingleCharacterAlternationCheck.class,
      SpecializedFunctionalInterfacesCheck.class,
      SpringAntMatcherOrderCheck.class,
      SpringAutoConfigurationCheck.class,
      SpringBeansShouldBeAccessibleCheck.class,
      SpringComponentWithNonAutowiredMembersCheck.class,
      SpringComponentWithWrongScopeCheck.class,
      SpringComposedRequestMappingCheck.class,
      SpringConfigurationWithAutowiredFieldsCheck.class,
      SpringConstructorInjectionCheck.class,
      SpringIncompatibleTransactionalCheck.class,
      SpringRequestMappingMethodCheck.class,
      SpringScanDefaultPackageCheck.class,
      SpringSecurityDisableCSRFCheck.class,
      SpringSessionFixationCheck.class,
      StandardCharsetsConstantsCheck.class,
      StandardFunctionalInterfaceCheck.class,
      StaticFieldInitializationCheck.class,
      StaticFieldUpateCheck.class,
      StaticFieldUpdateInConstructorCheck.class,
      StaticImportCountCheck.class,
      StaticMemberAccessCheck.class,
      StaticMembersAccessCheck.class,
      StaticMultithreadedUnsafeFieldsCheck.class,
      StreamPeekCheck.class,
      StringCallsBeyondBoundsCheck.class,
      StringConcatToTextBlockCheck.class,
      StringLiteralInsideEqualsCheck.class,
      StringMethodsWithLocaleCheck.class,
      StringOffsetMethodsCheck.class,
      StringPrimitiveConstructorCheck.class,
      StringReplaceCheck.class,
      StringToPrimitiveConversionCheck.class,
      StringToStringCheck.class,
      StrongCipherAlgorithmCheck.class,
      SubClassStaticReferenceCheck.class,
      SuppressWarningsCheck.class,
      SuspiciousListRemoveCheck.class,
      SwitchCaseTooBigCheck.class,
      SwitchCaseWithoutBreakCheck.class,
      SwitchCasesShouldBeCommaSeparatedCheck.class,
      SwitchDefaultLastCaseCheck.class,
      SwitchInsteadOfIfSequenceCheck.class,
      SwitchLastCaseIsDefaultCheck.class,
      SwitchRedundantKeywordCheck.class,
      SwitchWithLabelsCheck.class,
      SwitchWithTooManyCasesCheck.class,
      SymmetricEqualsCheck.class,
      SyncGetterAndSetterCheck.class,
      SynchronizationOnGetClassCheck.class,
      SynchronizationOnStringOrBoxedCheck.class,
      SynchronizedClassUsageCheck.class,
      SynchronizedFieldAssignmentCheck.class,
      SynchronizedLockCheck.class,
      SynchronizedOverrideCheck.class,
      SystemExitCalledCheck.class,
      SystemOutOrErrUsageCheck.class,
      TabCharacterCheck.class,
      TernaryOperatorCheck.class,
      TestsInSeparateFolderCheck.class,
      TextBlockTabsAndSpacesCheck.class,
      TextBlocksInComplexExpressionsCheck.class,
      ThisExposedFromConstructorCheck.class,
      ThreadAsRunnableArgumentCheck.class,
      ThreadLocalCleanupCheck.class,
      ThreadLocalWithInitialCheck.class,
      ThreadOverridesRunCheck.class,
      ThreadRunCheck.class,
      ThreadSleepCheck.class,
      ThreadStartedInConstructorCheck.class,
      ThreadWaitCallCheck.class,
      ThrowCheckedExceptionCheck.class,
      ThrowsSeveralCheckedExceptionCheck.class,
      ToArrayCheck.class,
      ToStringReturningNullCheck.class,
      ToStringUsingBoxingCheck.class,
      TodoTagPresenceCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeInFileCheck.class,
      TooManyMethodsCheck.class,
      TooManyStatementsPerLineCheck.class,
      TrailingCommentCheck.class,
      TransactionalMethodVisibilityCheck.class,
      TransientFieldInNonSerializableCheck.class,
      TryWithResourcesCheck.class,
      TwoLocksWaitCheck.class,
      URLHashCodeAndEqualsCheck.class,
      UnderscoreMisplacedOnNumberCheck.class,
      UnderscoreOnNumberCheck.class,
      UnicodeAwareCharClassesCheck.class,
      UnicodeCaseCheck.class,
      UnnecessaryEscapeSequencesInTextBlockCheck.class,
      UnnecessarySemicolonCheck.class,
      UnpredictableSaltCheck.class,
      UnreachableCatchCheck.class,
      UnusedGroupNamesCheck.class,
      UnusedLabelCheck.class,
      UnusedLocalVariableCheck.class,
      UnusedMethodParameterCheck.class,
      UnusedPrivateClassCheck.class,
      UnusedPrivateFieldCheck.class,
      UnusedPrivateMethodCheck.class,
      UnusedReturnedDataCheck.class,
      UnusedThrowableCheck.class,
      UnusedTypeParameterCheck.class,
      UppercaseSuffixesCheck.class,
      UseSwitchExpressionCheck.class,
      UselessExtendsCheck.class,
      UselessImportCheck.class,
      UselessIncrementCheck.class,
      UselessParenthesesCheck.class,
      UserEnumerationCheck.class,
      UtilityClassWithPublicConstructorCheck.class,
      ValueBasedObjectUsedForLockCheck.class,
      ValueBasedObjectsShouldNotBeSerializedCheck.class,
      VarArgCheck.class,
      VarCanBeUsedCheck.class,
      VariableDeclarationScopeCheck.class,
      VerifiedServerHostnamesCheck.class,
      VisibleForTestingUsageCheck.class,
      VolatileNonPrimitiveFieldCheck.class,
      VolatileVariablesOperationsCheck.class,
      WaitInSynchronizeCheck.class,
      WaitInWhileLoopCheck.class,
      WaitOnConditionCheck.class,
      WeakSSLContextCheck.class,
      WildcardImportsShouldNotBeUsedCheck.class,
      WildcardReturnParameterTypeCheck.class,
      WriteObjectTheOnlySynchronizedMethodCheck.class,
      WrongAssignmentOperatorCheck.class,
      XxeActiveMQCheck.class,
      ZipEntryCheck.class,

      // slow JavaFileScanner (not IssuableSubscriptionVisitor) ordered from the fastest to the slowest
      IncrementDecrementInSubExpressionCheck.class,
      StringLiteralDuplicatedCheck.class,
      LoggersDeclarationCheck.class,
      AssignmentInSubExpressionCheck.class,
      SeveralBreakOrContinuePerLoopCheck.class,
      ClassCouplingCheck.class,
      AnonymousClassesTooBigCheck.class,
      CatchUsesExceptionWithContextCheck.class,
      ForLoopCounterChangedCheck.class,
      CollapsibleIfCandidateCheck.class,
      MutableMembersUsageCheck.class,
      LambdaTooBigCheck.class,
      CollectionImplementationReferencedCheck.class,
      NestedTryCatchCheck.class,
      TooManyParametersCheck.class,
      BadLocalVariableNameCheck.class,
      StaticMethodCheck.class,
      AnonymousClassShouldBeLambdaCheck.class,
      SwitchAtLeastThreeCasesCheck.class,
      DepthOfInheritanceTreeCheck.class,
      CatchNPECheck.class,
      CollectionIsEmptyCheck.class,
      CompareObjectWithEqualsCheck.class,
      StringConcatenationInLoopCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      DisallowedThreadGroupCheck.class,
      ClassVariableVisibilityCheck.class,
      PublicStaticFieldShouldBeFinalCheck.class,
      UndocumentedApiCheck.class,
      BadClassNameCheck.class,
      LazyArgEvaluationCheck.class,
      BoxedBooleanExpressionsCheck.class,
      ThrowsFromFinallyCheck.class,
      TypeParametersShadowingCheck.class,
      ParsingErrorCheck.class,
      NullShouldNotBeUsedWithOptionalCheck.class,
      ReturnInFinallyCheck.class,
      CollectionsEmptyConstantsCheck.class,
      CompareStringsBoxedTypesWithEqualsCheck.class,
      FieldNameMatchingTypeNameCheck.class,
      DefaultPackageCheck.class,
      SunPackagesUsedCheck.class,
      ConcatenationWithStringValueOfCheck.class,
      EmptyFileCheck.class,
      MissingNewLineAtEndOfFileCheck.class,
      InnerStaticClassesCheck.class,
      MathOnFloatCheck.class,
      CastArithmeticOperandCheck.class,
      NestedBlocksCheck.class,
      HardcodedIpCheck.class,
      MismatchPackageDirectoryCheck.class,
      UselessPackageInfoCheck.class,
      BadPackageNameCheck.class,
      RepeatAnnotationCheck.class,
      OctalValuesCheck.class,
      DuplicateConditionIfElseIfCheck.class,
      MethodNameSameAsClassCheck.class,
      IndentationAfterConditionalCheck.class,
      BadInterfaceNameCheck.class,
      RawByteBitwiseOperationsCheck.class,
      StringBufferAndBuilderWithCharCheck.class,
      InsecureCreateTempFileCheck.class,
      KeywordAsIdentifierCheck.class,
      MultilineBlocksCurlyBracesCheck.class,
      PrimitiveTypeBoxingWithToStringCheck.class,
      EnumMapCheck.class,
      LeftCurlyBraceStartLineCheck.class,
      DisallowedClassCheck.class,
      ParameterReassignedToCheck.class,

      // SEChecks ordered by ExplodedGraphWalker need
      NullDereferenceCheck.class,
      DivisionByZeroCheck.class,
      UnclosedResourcesCheck.class,
      LocksNotUnlockedCheck.class,
      NonNullSetToNullCheck.class,
      NoWayOutLoopCheck.class,
      OptionalGetBeforeIsPresentCheck.class,
      StreamConsumedCheck.class,
      RedundantAssignmentsCheck.class,

      // SEChecks not require by ExplodedGraphWalker, from the fastest to the slowest
      ParameterNullnessCheck.class,
      BooleanGratuitousExpressionsCheck.class,
      ConditionalUnreachableCodeCheck.class,
      CustomUnclosedResourcesCheck.class,
      MapComputeIfAbsentOrPresentCheck.class,
      InvariantReturnCheck.class,
      XxeProcessingCheck.class,
      StreamNotConsumedCheck.class,
      ObjectOutputStreamCheck.class,
      MinMaxRangeCheck.class);
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
