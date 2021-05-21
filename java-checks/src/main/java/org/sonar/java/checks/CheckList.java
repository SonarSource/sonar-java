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
import org.sonar.java.checks.regex.ImpossibleRegexCheck;
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
import org.sonar.java.checks.security.AESAlgorithmCheck;
import org.sonar.java.checks.security.AndroidBroadcastingCheck;
import org.sonar.java.checks.security.AndroidExternalStorageCheck;
import org.sonar.java.checks.security.AndroidSSLConnectionCheck;
import org.sonar.java.checks.security.AuthorizationsStrongDecisionsCheck;
import org.sonar.java.checks.security.CipherBlockChainingCheck;
import org.sonar.java.checks.security.ClearTextProtocolCheck;
import org.sonar.java.checks.security.CommandLineArgumentsCheck;
import org.sonar.java.checks.security.ControllingPermissionsCheck;
import org.sonar.java.checks.security.CookieHttpOnlyCheck;
import org.sonar.java.checks.security.CookieShouldNotContainSensitiveDataCheck;
import org.sonar.java.checks.security.CryptographicKeySizeCheck;
import org.sonar.java.checks.security.DataEncryptionCheck;
import org.sonar.java.checks.security.DataHashingCheck;
import org.sonar.java.checks.security.DebugFeatureEnabledCheck;
import org.sonar.java.checks.security.DisableAutoEscapingCheck;
import org.sonar.java.checks.security.DisclosingTechnologyFingerprintsCheck;
import org.sonar.java.checks.security.EmailHotspotCheck;
import org.sonar.java.checks.security.EmptyDatabasePasswordCheck;
import org.sonar.java.checks.security.EncryptionAlgorithmCheck;
import org.sonar.java.checks.security.EnvVariablesHotspotCheck;
import org.sonar.java.checks.security.ExcessiveContentRequestCheck;
import org.sonar.java.checks.security.FilePermissionsCheck;
import org.sonar.java.checks.security.HostnameVerifierImplementationCheck;
import org.sonar.java.checks.security.IntegerToHexStringCheck;
import org.sonar.java.checks.security.JWTWithStrongCipherCheck;
import org.sonar.java.checks.security.LDAPAuthenticatedConnectionCheck;
import org.sonar.java.checks.security.LDAPDeserializationCheck;
import org.sonar.java.checks.security.LogConfigurationCheck;
import org.sonar.java.checks.security.OpenSAML2AuthenticationBypassCheck;
import org.sonar.java.checks.security.PasswordEncoderCheck;
import org.sonar.java.checks.security.PubliclyWritableDirectoriesCheck;
import org.sonar.java.checks.security.ReceivingIntentsCheck;
import org.sonar.java.checks.security.RegexHotspotCheck;
import org.sonar.java.checks.security.SMTPSSLServerIdentityCheck;
import org.sonar.java.checks.security.SecureCookieCheck;
import org.sonar.java.checks.security.ServerCertificatesCheck;
import org.sonar.java.checks.security.SocketUsageCheck;
import org.sonar.java.checks.security.StandardInputReadCheck;
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
import org.sonar.java.checks.tests.AssertJAssertionsInConsumerCheck;
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
import org.sonar.java.checks.tests.JunitMethodDeclarationCheck;
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
import org.sonar.java.checks.xml.web.SecurityConstraintsInWebXmlCheck;
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

  // Rule classes are listed alphabetically
  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return Arrays.asList(
      AbsOnNegativeCheck.class,
      AbstractClassNoFieldShouldBeInterfaceCheck.class,
      AbstractClassWithoutAbstractMethodCheck.class,
      AccessibilityChangeCheck.class,
      AESAlgorithmCheck.class,
      AllBranchesAreIdenticalCheck.class,
      AnchorPrecedenceCheck.class,
      AndroidBroadcastingCheck.class,
      AndroidExternalStorageCheck.class,
      AndroidSSLConnectionCheck.class,
      AnnotationDefaultArgumentCheck.class,
      AnonymousClassesTooBigCheck.class,
      AnonymousClassShouldBeLambdaCheck.class,
      ArrayCopyLoopCheck.class,
      ArrayDesignatorAfterTypeCheck.class,
      ArrayDesignatorOnVariableCheck.class,
      ArrayForVarArgCheck.class,
      ArrayHashCodeAndToStringCheck.class,
      ArraysAsListOfPrimitiveToStreamCheck.class,
      AssertionsInProductionCodeCheck.class,
      AssertOnBooleanVariableCheck.class,
      AssertsOnParametersOfPublicMethodCheck.class,
      AssignmentInSubExpressionCheck.class,
      AtLeastOneConstructorCheck.class,
      AuthorizationsStrongDecisionsCheck.class,
      AvoidDESCheck.class,
      BadAbstractClassNameCheck.class,
      BadClassNameCheck.class,
      BadConstantNameCheck.class,
      BadFieldNameCheck.class,
      BadFieldNameStaticNonFinalCheck.class,
      BadInterfaceNameCheck.class,
      BadLocalConstantNameCheck.class,
      BadLocalVariableNameCheck.class,
      BadMethodNameCheck.class,
      BadPackageNameCheck.class,
      BadTypeParameterNameCheck.class,
      BasicAuthCheck.class,
      BigDecimalDoubleConstructorCheck.class,
      BlindSerialVersionUidCheck.class,
      BooleanGratuitousExpressionsCheck.class,
      BooleanInversionCheck.class,
      BooleanLiteralCheck.class,
      BooleanMethodNameCheck.class,
      BooleanMethodReturnCheck.class,
      BoxedBooleanExpressionsCheck.class,
      CallOuterPrivateMethodCheck.class,
      CallSuperMethodFromInnerClassCheck.class,
      CallToDeprecatedCodeMarkedForRemovalCheck.class,
      CallToDeprecatedMethodCheck.class,
      CallToFileDeleteOnExitMethodCheck.class,
      CanonEqFlagInRegexCheck.class,
      CaseInsensitiveComparisonCheck.class,
      CastArithmeticOperandCheck.class,
      CatchExceptionCheck.class,
      CatchIllegalMonitorStateExceptionCheck.class,
      CatchNPECheck.class,
      CatchOfThrowableOrErrorCheck.class,
      CatchRethrowingCheck.class,
      CatchUsesExceptionWithContextCheck.class,
      ChangeMethodContractCheck.class,
      ChildClassShadowFieldCheck.class,
      CipherBlockChainingCheck.class,
      ClassComparedByNameCheck.class,
      ClassCouplingCheck.class,
      ClassFieldCountCheck.class,
      ClassNamedLikeExceptionCheck.class,
      ClassVariableVisibilityCheck.class,
      ClassWithOnlyStaticMethodsInstantiationCheck.class,
      ClassWithoutHashCodeInHashStructureCheck.class,
      ClearTextProtocolCheck.class,
      CloneableImplementingCloneCheck.class,
      CloneMethodCallsSuperCloneCheck.class,
      CloneOverrideCheck.class,
      CognitiveComplexityMethodCheck.class,
      CollapsibleIfCandidateCheck.class,
      CollectInsteadOfForeachCheck.class,
      CollectionCallingItselfCheck.class,
      CollectionImplementationReferencedCheck.class,
      CollectionInappropriateCallsCheck.class,
      CollectionIsEmptyCheck.class,
      CollectionMethodsWithLinearComplexityCheck.class,
      CollectionsEmptyConstantsCheck.class,
      CollectionSizeAndArrayLengthCheck.class,
      CombineCatchCheck.class,
      CommandLineArgumentsCheck.class,
      CommentedOutCodeLineCheck.class,
      CommentRegularExpressionCheck.class,
      CompareObjectWithEqualsCheck.class,
      CompareStringsBoxedTypesWithEqualsCheck.class,
      CompareToNotOverloadedCheck.class,
      CompareToResultTestCheck.class,
      CompareToReturnValueCheck.class,
      ConcatenationWithStringValueOfCheck.class,
      ConditionalOnNewLineCheck.class,
      ConditionalUnreachableCodeCheck.class,
      ConfusingOverloadCheck.class,
      ConfusingVarargCheck.class,
      ConstantMathCheck.class,
      ConstantMethodCheck.class,
      ConstantsShouldBeStaticFinalCheck.class,
      ConstructorCallingOverridableCheck.class,
      ConstructorInjectionCheck.class,
      ControlCharacterInLiteralCheck.class,
      ControllerWithSessionAttributesCheck.class,
      ControllingPermissionsCheck.class,
      CookieDomainCheck.class,
      CookieHttpOnlyCheck.class,
      CookieShouldNotContainSensitiveDataCheck.class,
      CORSCheck.class,
      CryptographicKeySizeCheck.class,
      CustomCryptographicAlgorithmCheck.class,
      CustomSerializationMethodCheck.class,
      CustomUnclosedResourcesCheck.class,
      DanglingElseStatementsCheck.class,
      DataEncryptionCheck.class,
      DataHashingCheck.class,
      DateAndTimesCheck.class,
      DateFormatWeekYearCheck.class,
      DateTimeFormatterMismatchCheck.class,
      DateUtilsTruncateCheck.class,
      DeadStoreCheck.class,
      DebugFeatureEnabledCheck.class,
      DefaultEncodingUsageCheck.class,
      DefaultInitializedFieldCheck.class,
      DefaultPackageCheck.class,
      DeprecatedTagPresenceCheck.class,
      DiamondOperatorCheck.class,
      DisableAutoEscapingCheck.class,
      DisallowedClassCheck.class,
      DisallowedConstructorCheck.class,
      DisallowedMethodCheck.class,
      DisallowedThreadGroupCheck.class,
      DisclosingTechnologyFingerprintsCheck.class,
      DepthOfInheritanceTreeCheck.class,
      DivisionByZeroCheck.class,
      DoubleBraceInitializationCheck.class,
      DoubleCheckedLockingAssignmentCheck.class,
      DoubleCheckedLockingCheck.class,
      DoublePrefixOperatorCheck.class,
      DuplicateConditionIfElseIfCheck.class,
      DuplicatesInCharacterClassCheck.class,
      DynamicClassLoadCheck.class,
      EmailHotspotCheck.class,
      EmptyBlockCheck.class,
      EmptyClassCheck.class,
      EmptyDatabasePasswordCheck.class,
      EmptyFileCheck.class,
      EmptyLineRegexCheck.class,
      EmptyMethodsCheck.class,
      EmptyStatementUsageCheck.class,
      EmptyStringRepetitionCheck.class,
      EncryptionAlgorithmCheck.class,
      EnumEqualCheck.class,
      EnumMapCheck.class,
      EnumMutableFieldCheck.class,
      EnumSetCheck.class,
      EnvVariablesHotspotCheck.class,
      EqualsArgumentTypeCheck.class,
      EqualsNotOverriddenInSubclassCheck.class,
      EqualsNotOverridenWithCompareToCheck.class,
      EqualsOnAtomicClassCheck.class,
      EqualsOverridenWithHashCodeCheck.class,
      EqualsParametersMarkedNonNullCheck.class,
      ErrorClassExtendedCheck.class,
      EscapedUnicodeCharactersCheck.class,
      EscapeSequenceControlCharacterCheck.class,
      ExceptionsShouldBeImmutableCheck.class,
      ExcessiveContentRequestCheck.class,
      ExpressionComplexityCheck.class,
      ExternalizableClassConstructorCheck.class,
      FieldModifierCheck.class,
      FieldNameMatchingTypeNameCheck.class,
      FileCreateTempFileCheck.class,
      FileHeaderCheck.class,
      FilePermissionsCheck.class,
      FilesExistsJDK8Check.class,
      FinalClassCheck.class,
      FinalizeFieldsSetCheck.class,
      FixmeTagPresenceCheck.class,
      FloatEqualityCheck.class,
      ForLoopCounterChangedCheck.class,
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
      HardcodedIpCheck.class,
      HardcodedURICheck.class,
      HasNextCallingNextCheck.class,
      HiddenFieldCheck.class,
      HostnameVerifierImplementationCheck.class,
      HttpRefererCheck.class,
      IdenticalCasesInSwitchCheck.class,
      IdenticalOperandOnBinaryExpressionCheck.class,
      IfElseIfStatementEndsWithElseCheck.class,
      IgnoredOperationStatusCheck.class,
      IgnoredReturnValueCheck.class,
      IgnoredStreamReturnValueCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      ImmediateReverseBoxingCheck.class,
      ImplementsEnumerationCheck.class,
      ImpossibleBackReferenceCheck.class,
      ImpossibleBoundariesCheck.class,
      ImpossibleRegexCheck.class,
      InappropriateRegexpCheck.class,
      IncorrectOrderOfMembersCheck.class,
      IncrementDecrementInSubExpressionCheck.class,
      IndentationAfterConditionalCheck.class,
      IndentationCheck.class,
      IndexOfStartPositionCheck.class,
      IndexOfWithPositiveNumberCheck.class,
      InnerClassOfNonSerializableCheck.class,
      InnerClassOfSerializableCheck.class,
      InnerClassTooManyLinesCheck.class,
      InnerStaticClassesCheck.class,
      InputStreamOverrideReadCheck.class,
      InputStreamReadCheck.class,
      InsecureCreateTempFileCheck.class,
      InstanceofUsedOnExceptionCheck.class,
      IntegerToHexStringCheck.class,
      InterfaceAsConstantContainerCheck.class,
      InterfaceOrSuperclassShadowingCheck.class,
      InterruptedExceptionCheck.class,
      InvalidDateValuesCheck.class,
      InvalidRegexCheck.class,
      InvariantReturnCheck.class,
      IsInstanceMethodCheck.class,
      IterableIteratorCheck.class,
      IteratorNextExceptionCheck.class,
      JacksonDeserializationCheck.class,
      JdbcDriverExplicitLoadingCheck.class,
      JWTWithStrongCipherCheck.class,
      KeySetInsteadOfEntrySetCheck.class,
      KeywordAsIdentifierCheck.class,
      LabelsShouldNotBeUsedCheck.class,
      LambdaOptionalParenthesisCheck.class,
      LambdaSingleExpressionCheck.class,
      LambdaTooBigCheck.class,
      LambdaTypeParameterCheck.class,
      LazyArgEvaluationCheck.class,
      LDAPAuthenticatedConnectionCheck.class,
      LDAPDeserializationCheck.class,
      LeastSpecificTypeCheck.class,
      LeftCurlyBraceEndLineCheck.class,
      LeftCurlyBraceStartLineCheck.class,
      LocksNotUnlockedCheck.class,
      LogConfigurationCheck.class,
      LoggedRethrownExceptionsCheck.class,
      LoggerClassCheck.class,
      LoggersDeclarationCheck.class,
      LongBitsToDoubleOnIntCheck.class,
      LoopExecutingAtMostOnceCheck.class,
      LoopsOnSameSetCheck.class,
      MagicNumberCheck.class,
      MainInServletCheck.class,
      MainMethodThrowsExceptionCheck.class,
      MapComputeIfAbsentOrPresentCheck.class,
      MathOnFloatCheck.class,
      MembersDifferOnlyByCapitalizationCheck.class,
      MethodComplexityCheck.class,
      MethodIdenticalImplementationsCheck.class,
      MethodNamedEqualsCheck.class,
      MethodNamedHashcodeOrEqualCheck.class,
      MethodNameSameAsClassCheck.class,
      MethodOnlyCallsSuperCheck.class,
      MethodParametersOrderCheck.class,
      MethodTooBigCheck.class,
      MethodWithExcessiveReturnsCheck.class,
      MinMaxRangeCheck.class,
      MismatchPackageDirectoryCheck.class,
      MissingBeanValidationCheck.class,
      MissingCurlyBracesCheck.class,
      MissingDeprecatedCheck.class,
      MissingNewLineAtEndOfFileCheck.class,
      ModifiersOrderCheck.class,
      ModulusEqualityCheck.class,
      MultilineBlocksCurlyBracesCheck.class,
      MutableMembersUsageCheck.class,
      NestedBlocksCheck.class,
      NestedEnumStaticCheck.class,
      NestedIfStatementsCheck.class,
      NestedSwitchCheck.class,
      NestedTernaryOperatorsCheck.class,
      NestedTryCatchCheck.class,
      NioFileDeleteCheck.class,
      NoCheckstyleTagPresenceCheck.class,
      NonNullSetToNullCheck.class,
      NonSerializableWriteCheck.class,
      NonShortCircuitLogicCheck.class,
      NonStaticClassInitializerCheck.class,
      NoPmdTagPresenceCheck.class,
      NoSonarCheck.class,
      NotifyCheck.class,
      NoWayOutLoopCheck.class,
      NPEThrowCheck.class,
      NullCheckWithInstanceofCheck.class,
      NullDereferenceCheck.class,
      NullReturnedOnComputeIfPresentOrAbsentCheck.class,
      NullShouldNotBeUsedWithOptionalCheck.class,
      ObjectCreatedOnlyToCallGetClassCheck.class,
      ObjectDeserializationCheck.class,
      ObjectFinalizeCheck.class,
      ObjectFinalizeOverloadedCheck.class,
      ObjectFinalizeOverridenCallsSuperFinalizeCheck.class,
      ObjectFinalizeOverridenCheck.class,
      ObjectFinalizeOverridenNotPublicCheck.class,
      ObjectOutputStreamCheck.class,
      OctalValuesCheck.class,
      OneClassInterfacePerFileCheck.class,
      OneDeclarationPerLineCheck.class,
      OpenSAML2AuthenticationBypassCheck.class,
      OperatorPrecedenceCheck.class,
      OptionalAsParameterCheck.class,
      OptionalGetBeforeIsPresentCheck.class,
      OutputStreamOverrideWriteCheck.class,
      OverrideAnnotationCheck.class,
      OverwrittenKeyCheck.class,
      OSCommandsPathCheck.class,
      PackageInfoCheck.class,
      ParameterNullnessCheck.class,
      ParameterReassignedToCheck.class,
      ParsingErrorCheck.class,
      PasswordEncoderCheck.class,
      PersistentEntityUsedAsRequestParameterCheck.class,
      PopulateBeansCheck.class,
      PossessiveQuantifierContinuationCheck.class,
      PredictableSeedCheck.class,
      PreferStreamAnyMatchCheck.class,
      PreparedStatementAndResultSetCheck.class,
      PrimitivesMarkedNullableCheck.class,
      PrimitiveTypeBoxingWithToStringCheck.class,
      PrimitiveWrappersInTernaryOperatorCheck.class,
      PrintfFailCheck.class,
      PrintfMisuseCheck.class,
      PrintStackTraceCalledWithoutArgumentCheck.class,
      PrivateFieldUsedLocallyCheck.class,
      PrivateReadResolveCheck.class,
      ProtectedMemberInFinalClassCheck.class,
      PseudoRandomCheck.class,
      PublicConstructorInAbstractClassCheck.class,
      PubliclyWritableDirectoriesCheck.class,
      PublicStaticFieldShouldBeFinalCheck.class,
      PublicStaticMutableMembersCheck.class,
      RandomFloatToIntCheck.class,
      RawByteBitwiseOperationsCheck.class,
      RawExceptionCheck.class,
      RawTypeCheck.class,
      ReadObjectSynchronizedCheck.class,
      ReceivingIntentsCheck.class,
      RedosCheck.class,
      RedundantAbstractMethodCheck.class,
      RedundantAssignmentsCheck.class,
      RedundantCloseCheck.class,
      RedundantJumpCheck.class,
      RedundantModifierCheck.class,
      RedundantRegexAlternativesCheck.class,
      RedundantStreamCollectCheck.class,
      RedundantThrowsDeclarationCheck.class,
      RedundantTypeCastCheck.class,
      ReflectionOnNonRuntimeAnnotationCheck.class,
      RegexComplexityCheck.class,
      RegexHotspotCheck.class,
      RegexLookaheadCheck.class,
      RegexPatternsNeedlesslyCheck.class,
      RegexStackOverflowCheck.class,
      ReluctantQuantifierCheck.class,
      ReluctantQuantifierWithEmptyContinuationCheck.class,
      RepeatAnnotationCheck.class,
      ReplaceGuavaWithJavaCheck.class,
      ReplaceLambdaByMethodRefCheck.class,
      RequestMappingMethodPublicCheck.class,
      RestrictedIdentifiersUsageCheck.class,
      ResultSetIsLastCheck.class,
      ReturnEmptyArrayNotNullCheck.class,
      ReturnInFinallyCheck.class,
      ReturnOfBooleanExpressionsCheck.class,
      ReuseRandomCheck.class,
      RightCurlyBraceDifferentLineAsNextBlockCheck.class,
      RightCurlyBraceSameLineAsNextBlockCheck.class,
      RightCurlyBraceStartLineCheck.class,
      RSAUsesOAEPCheck.class,
      RunFinalizersCheck.class,
      ScheduledThreadPoolExecutorZeroCheck.class,
      SecureCookieCheck.class,
      SelectorMethodArgumentCheck.class,
      SelfAssignementCheck.class,
      SerializableComparatorCheck.class,
      SerializableFieldInSerializableClassCheck.class,
      SerializableObjectInSessionCheck.class,
      SerializableSuperConstructorCheck.class,
      SerialVersionUidCheck.class,
      ServerCertificatesCheck.class,
      ServletInstanceFieldCheck.class,
      ServletMethodsExceptionsThrownCheck.class,
      SeveralBreakOrContinuePerLoopCheck.class,
      ShiftOnIntOrLongCheck.class,
      SillyBitOperationCheck.class,
      SillyEqualsCheck.class,
      SillyStringOperationsCheck.class,
      SimpleClassNameCheck.class,
      SimpleStringLiteralForSingleLineStringsCheck.class,
      SingleCharacterAlternationCheck.class,
      SMTPSSLServerIdentityCheck.class,
      SocketUsageCheck.class,
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
      SQLInjectionCheck.class,
      StandardCharsetsConstantsCheck.class,
      StandardFunctionalInterfaceCheck.class,
      StandardInputReadCheck.class,
      StaticFieldInitializationCheck.class,
      StaticFieldUpateCheck.class,
      StaticFieldUpdateInConstructorCheck.class,
      StaticImportCountCheck.class,
      StaticMemberAccessCheck.class,
      StaticMembersAccessCheck.class,
      StaticMethodCheck.class,
      StaticMultithreadedUnsafeFieldsCheck.class,
      StreamConsumedCheck.class,
      StreamNotConsumedCheck.class,
      StreamPeekCheck.class,
      StringBufferAndBuilderWithCharCheck.class,
      StringCallsBeyondBoundsCheck.class,
      StringConcatenationInLoopCheck.class,
      StringConcatToTextBlockCheck.class,
      StringLiteralDuplicatedCheck.class,
      StringLiteralInsideEqualsCheck.class,
      StringMethodsOnSingleCharCheck.class,
      StringMethodsWithLocaleCheck.class,
      StringOffsetMethodsCheck.class,
      StringPrimitiveConstructorCheck.class,
      StringReplaceCheck.class,
      StringToPrimitiveConversionCheck.class,
      StringToStringCheck.class,
      StrongCipherAlgorithmCheck.class,
      Struts1EndpointCheck.class,
      Struts2EndpointCheck.class,
      SubClassStaticReferenceCheck.class,
      SunPackagesUsedCheck.class,
      SuppressWarningsCheck.class,
      SuspiciousListRemoveCheck.class,
      SwitchAtLeastThreeCasesCheck.class,
      SwitchCasesShouldBeCommaSeparatedCheck.class,
      SwitchCaseTooBigCheck.class,
      SwitchCaseWithoutBreakCheck.class,
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
      TextBlocksInComplexExpressionsCheck.class,
      TextBlockTabsAndSpacesCheck.class,
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
      ThrowsFromFinallyCheck.class,
      ThrowsSeveralCheckedExceptionCheck.class,
      ToArrayCheck.class,
      TodoTagPresenceCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeInFileCheck.class,
      TooManyMethodsCheck.class,
      TooManyParametersCheck.class,
      TooManyStatementsPerLineCheck.class,
      ToStringReturningNullCheck.class,
      ToStringUsingBoxingCheck.class,
      TrailingCommentCheck.class,
      TransactionalMethodVisibilityCheck.class,
      TransientFieldInNonSerializableCheck.class,
      TryWithResourcesCheck.class,
      TwoLocksWaitCheck.class,
      TypeParametersShadowingCheck.class,
      UnclosedResourcesCheck.class,
      UnderscoreMisplacedOnNumberCheck.class,
      UnderscoreOnNumberCheck.class,
      UndocumentedApiCheck.class,
      UnicodeCaseCheck.class,
      UnicodeAwareCharClassesCheck.class,
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
      URLHashCodeAndEqualsCheck.class,
      UselessExtendsCheck.class,
      UselessImportCheck.class,
      UselessIncrementCheck.class,
      UselessPackageInfoCheck.class,
      UselessParenthesesCheck.class,
      UserEnumerationCheck.class,
      UseSwitchExpressionCheck.class,
      UtilityClassWithPublicConstructorCheck.class,
      ValueBasedObjectsShouldNotBeSerializedCheck.class,
      ValueBasedObjectUsedForLockCheck.class,
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
      XmlDeserializationCheck.class,
      XxeActiveMQCheck.class,
      XxeProcessingCheck.class,
      ZipEntryCheck.class);
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
      JunitMethodDeclarationCheck.class,
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
      SecurityConstraintsInWebXmlCheck.class,
      SingleConnectionFactoryCheck.class,
      ValidationFiltersCheck.class);
  }
}
