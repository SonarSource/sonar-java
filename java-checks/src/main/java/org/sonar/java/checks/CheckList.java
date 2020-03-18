/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.sonar.java.DebugCheck;
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
import org.sonar.java.checks.security.AESAlgorithmCheck;
import org.sonar.java.checks.security.AndroidBroadcastingCheck;
import org.sonar.java.checks.security.AndroidExternalStorageCheck;
import org.sonar.java.checks.security.AndroidSSLConnectionCheck;
import org.sonar.java.checks.security.CipherBlockChainingCheck;
import org.sonar.java.checks.security.CommandLineArgumentsCheck;
import org.sonar.java.checks.security.ControllingPermissionsCheck;
import org.sonar.java.checks.security.CookieHttpOnlyCheck;
import org.sonar.java.checks.security.CookieShouldNotContainSensitiveDataCheck;
import org.sonar.java.checks.security.CryptographicKeySizeCheck;
import org.sonar.java.checks.security.DataEncryptionCheck;
import org.sonar.java.checks.security.DataHashingCheck;
import org.sonar.java.checks.security.DebugFeatureEnabledCheck;
import org.sonar.java.checks.security.EmailHotspotCheck;
import org.sonar.java.checks.security.EmptyDatabasePasswordCheck;
import org.sonar.java.checks.security.EncryptionAlgorithmCheck;
import org.sonar.java.checks.security.EnvVariablesHotspotCheck;
import org.sonar.java.checks.security.FilePermissionsCheck;
import org.sonar.java.checks.security.HostnameVerifierImplementationCheck;
import org.sonar.java.checks.security.IntegerToHexStringCheck;
import org.sonar.java.checks.security.LDAPAuthenticatedConnectionCheck;
import org.sonar.java.checks.security.LDAPDeserializationCheck;
import org.sonar.java.checks.security.LogConfigurationCheck;
import org.sonar.java.checks.security.OpenSAML2AuthenticationBypassCheck;
import org.sonar.java.checks.security.PasswordEncoderCheck;
import org.sonar.java.checks.security.ReceivingIntentsCheck;
import org.sonar.java.checks.security.RegexHotspotCheck;
import org.sonar.java.checks.security.SMTPSSLServerIdentityCheck;
import org.sonar.java.checks.security.SecureCookieCheck;
import org.sonar.java.checks.security.SecureXmlTransformerCheck;
import org.sonar.java.checks.security.ServerCertificatesCheck;
import org.sonar.java.checks.security.SocketUsageCheck;
import org.sonar.java.checks.security.StandardInputReadCheck;
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
import org.sonar.java.checks.spring.SpringComponentScanCheck;
import org.sonar.java.checks.spring.SpringComponentWithNonAutowiredMembersCheck;
import org.sonar.java.checks.spring.SpringComponentWithWrongScopeCheck;
import org.sonar.java.checks.spring.SpringComposedRequestMappingCheck;
import org.sonar.java.checks.spring.SpringConfigurationWithAutowiredFieldsCheck;
import org.sonar.java.checks.spring.SpringIncompatibleTransactionalCheck;
import org.sonar.java.checks.spring.SpringRequestMappingMethodCheck;
import org.sonar.java.checks.spring.SpringScanDefaultPackageCheck;
import org.sonar.java.checks.spring.SpringSecurityDisableCSRFCheck;
import org.sonar.java.checks.synchronization.DoubleCheckedLockingCheck;
import org.sonar.java.checks.synchronization.SynchronizationOnGetClassCheck;
import org.sonar.java.checks.synchronization.TwoLocksWaitCheck;
import org.sonar.java.checks.synchronization.ValueBasedObjectUsedForLockCheck;
import org.sonar.java.checks.synchronization.WriteObjectTheOnlySynchronizedMethodCheck;
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
import org.sonar.java.se.checks.debug.DebugInterruptedExecutionCheck;
import org.sonar.java.se.checks.debug.DebugMethodYieldsCheck;
import org.sonar.java.se.checks.debug.DebugMethodYieldsOnInvocationsCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

public final class CheckList {

  public static final String REPOSITORY_KEY = "java";

  private CheckList() {
  }

  public static List<Class> getChecks() {
    return ImmutableList.<Class>builder()
      .addAll(getJavaChecks())
      .addAll(getJavaTestChecks())
      .addAll(getXmlChecks())
      .build();
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
      AssertOnBooleanVariableCheck.class,
      AssertsOnParametersOfPublicMethodCheck.class,
      AssignmentInSubExpressionCheck.class,
      AtLeastOneConstructorCheck.class,
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
      CallToDeprecatedMethodCheck.class,
      CallToFileDeleteOnExitMethodCheck.class,
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
      ClassComplexityCheck.class,
      ClassCouplingCheck.class,
      ClassFieldCountCheck.class,
      ClassNamedLikeExceptionCheck.class,
      ClassVariableVisibilityCheck.class,
      ClassWithOnlyStaticMethodsInstantiationCheck.class,
      ClassWithoutHashCodeInHashStructureCheck.class,
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
      ConstantMathCheck.class,
      ConstantMethodCheck.class,
      ConstantsShouldBeStaticFinalCheck.class,
      ConstructorCallingOverridableCheck.class,
      ConstructorInjectionCheck.class,
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
      DateUtilsTruncateCheck.class,
      DeadStoreCheck.class,
      DebugFeatureEnabledCheck.class,
      DefaultEncodingUsageCheck.class,
      DefaultInitializedFieldCheck.class,
      DefaultPackageCheck.class,
      DeprecatedHashAlgorithmCheck.class,
      DeprecatedTagPresenceCheck.class,
      DiamondOperatorCheck.class,
      DisallowedClassCheck.class,
      DisallowedConstructorCheck.class,
      DisallowedMethodCheck.class,
      DisallowedThreadGroupCheck.class,
      DITCheck.class,
      DivisionByZeroCheck.class,
      DoubleBraceInitializationCheck.class,
      DoubleCheckedLockingAssignmentCheck.class,
      DoubleCheckedLockingCheck.class,
      DoublePrefixOperatorCheck.class,
      DuplicateArgumentCheck.class,
      DuplicateConditionIfElseIfCheck.class,
      DynamicClassLoadCheck.class,
      EmailHotspotCheck.class,
      EmptyBlockCheck.class,
      EmptyClassCheck.class,
      EmptyDatabasePasswordCheck.class,
      EmptyFileCheck.class,
      EmptyMethodsCheck.class,
      EmptyStatementUsageCheck.class,
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
      ExceptionsShouldBeImmutableCheck.class,
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
      HardCodedCredentialsCheck.class,
      HardcodedIpCheck.class,
      HardcodedURICheck.class,
      HasNextCallingNextCheck.class,
      HiddenFieldCheck.class,
      HostnameVerifierImplementationCheck.class,
      HttpRefererCheck.class,
      IdenticalCasesInSwitchCheck.class,
      IdenticalOperandOnBinaryExpressionCheck.class,
      IfConditionAlwaysTrueOrFalseCheck.class,
      IfElseIfStatementEndsWithElseCheck.class,
      IgnoredOperationStatusCheck.class,
      IgnoredReturnValueCheck.class,
      IgnoredStreamReturnValueCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      ImmediateReverseBoxingCheck.class,
      ImplementsEnumerationCheck.class,
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
      InstanceOfAlwaysTrueCheck.class,
      InstanceofUsedOnExceptionCheck.class,
      IntegerToHexStringCheck.class,
      InterfaceAsConstantContainerCheck.class,
      InterfaceOrSuperclassShadowingCheck.class,
      InterruptedExceptionCheck.class,
      InvalidDateValuesCheck.class,
      InvariantReturnCheck.class,
      IterableIteratorCheck.class,
      IteratorNextExceptionCheck.class,
      JacksonDeserializationCheck.class,
      JdbcDriverExplicitLoadingCheck.class,
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
      NestedSwitchStatementCheck.class,
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
      NullCipherCheck.class,
      NullDereferenceCheck.class,
      NullDereferenceInConditionalCheck.class,
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
      PackageInfoCheck.class,
      ParameterNullnessCheck.class,
      ParameterReassignedToCheck.class,
      ParsingErrorCheck.class,
      PasswordEncoderCheck.class,
      PersistentEntityUsedAsRequestParameterCheck.class,
      PopulateBeansCheck.class,
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
      PublicStaticFieldShouldBeFinalCheck.class,
      PublicStaticMutableMembersCheck.class,
      RandomFloatToIntCheck.class,
      RawByteBitwiseOperationsCheck.class,
      RawExceptionCheck.class,
      RawTypeCheck.class,
      ReadObjectSynchronizedCheck.class,
      ReceivingIntentsCheck.class,
      RedundantAbstractMethodCheck.class,
      RedundantAssignmentsCheck.class,
      RedundantCloseCheck.class,
      RedundantJumpCheck.class,
      RedundantModifierCheck.class,
      RedundantStreamCollectCheck.class,
      RedundantThrowsDeclarationCheck.class,
      RedundantTypeCastCheck.class,
      ReflectionOnNonRuntimeAnnotationCheck.class,
      RegexHotspotCheck.class,
      RegexPatternsNeedlesslyCheck.class,
      RepeatAnnotationCheck.class,
      ReplaceGuavaWithJava8Check.class,
      ReplaceLambdaByMethodRefCheck.class,
      RequestMappingMethodPublicCheck.class,
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
      SAMAnnotatedCheck.class,
      ScheduledThreadPoolExecutorZeroCheck.class,
      SecureCookieCheck.class,
      SecureXmlTransformerCheck.class,
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
      SMTPSSLServerIdentityCheck.class,
      SocketUsageCheck.class,
      SpecializedFunctionalInterfacesCheck.class,
      SpringAntMatcherOrderCheck.class,
      SpringAutoConfigurationCheck.class,
      SpringBeansShouldBeAccessibleCheck.class,
      SpringComponentScanCheck.class,
      SpringComponentWithNonAutowiredMembersCheck.class,
      SpringComponentWithWrongScopeCheck.class,
      SpringComposedRequestMappingCheck.class,
      SpringConfigurationWithAutowiredFieldsCheck.class,
      SpringConstructorInjectionCheck.class,
      SpringIncompatibleTransactionalCheck.class,
      SpringRequestMappingMethodCheck.class,
      SpringScanDefaultPackageCheck.class,
      SpringSecurityDisableCSRFCheck.class,
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
      SwitchCaseTooBigCheck.class,
      SwitchCaseWithoutBreakCheck.class,
      SwitchDefaultLastCaseCheck.class,
      SwitchInsteadOfIfSequenceCheck.class,
      SwitchLastCaseIsDefaultCheck.class,
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
      UnclosedResourcesCheck.class,
      UnderscoreMisplacedOnNumberCheck.class,
      UnderscoreOnNumberCheck.class,
      UndocumentedApiCheck.class,
      UnnecessarySemicolonCheck.class,
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
      UseSwitchExpressionCheck.class,
      UtilityClassWithPublicConstructorCheck.class,
      ValueBasedObjectsShouldNotBeSerializedCheck.class,
      ValueBasedObjectUsedForLockCheck.class,
      VarArgCheck.class,
      VariableDeclarationScopeCheck.class,
      VerifiedServerHostnamesCheck.class,
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
      ZipEntryCheck.class
    );
  }

  // Rule classes are listed alphabetically
  public static List<Class<? extends DebugCheck>> getDebugChecks() {
    return Arrays.asList(
      DebugInterruptedExecutionCheck.class,
      DebugMethodYieldsCheck.class,
      DebugMethodYieldsOnInvocationsCheck.class);
  }

  // Rule classes are listed alphabetically
  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return Arrays.asList(
      AssertionArgumentOrderCheck.class,
      AssertionFailInCatchBlockCheck.class,
      AssertionInThreadRunCheck.class,
      AssertionsCompletenessCheck.class,
      AssertionsInTestsCheck.class,
      AssertionsWithoutMessageCheck.class,
      BadTestClassNameCheck.class,
      BadTestMethodNameCheck.class,
      BooleanLiteralInAssertionsCheck.class,
      CallSuperInTestCaseCheck.class,
      IgnoredTestsCheck.class,
      JunitMethodDeclarationCheck.class,
      NoTestInTestClassCheck.class,
      ThreadSleepInTestsCheck.class,
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
