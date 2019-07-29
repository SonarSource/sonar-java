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
import org.sonar.java.checks.security.EnvVariablesHotspotCheck;
import org.sonar.java.checks.security.HostnameVerifierImplementationCheck;
import org.sonar.java.checks.security.IntegerToHexStringCheck;
import org.sonar.java.checks.security.LDAPAuthenticatedConnectionCheck;
import org.sonar.java.checks.security.LDAPDeserializationCheck;
import org.sonar.java.checks.security.LogConfigurationCheck;
import org.sonar.java.checks.security.PasswordEncoderCheck;
import org.sonar.java.checks.security.ReceivingIntentsCheck;
import org.sonar.java.checks.security.RegexHotspotCheck;
import org.sonar.java.checks.security.SMTPSSLServerIdentityCheck;
import org.sonar.java.checks.security.SecureCookieCheck;
import org.sonar.java.checks.security.SecureXmlTransformerCheck;
import org.sonar.java.checks.security.SocketUsageCheck;
import org.sonar.java.checks.security.StandardInputReadCheck;
import org.sonar.java.checks.security.TrustManagerCertificateCheck;
import org.sonar.java.checks.security.XmlExternalEntityProcessingCheck;
import org.sonar.java.checks.security.ZipEntryCheck;
import org.sonar.java.checks.serialization.BlindSerialVersionUidCheck;
import org.sonar.java.checks.serialization.CustomSerializationMethodCheck;
import org.sonar.java.checks.serialization.ExternalizableClassConstructorCheck;
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
import org.sonar.java.se.checks.debug.DebugInterruptedExecutionCheck;
import org.sonar.java.se.checks.debug.DebugMethodYieldsCheck;
import org.sonar.java.se.checks.debug.DebugMethodYieldsOnInvocationsCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

public final class CheckList {

  public static final String REPOSITORY_KEY = "squid";

  private CheckList() {
  }

  public static List<Class> getChecks() {
    return ImmutableList.<Class>builder()
      .addAll(getJavaChecks())
      .addAll(getJavaTestChecks())
      .addAll(getXmlChecks())
      .build();
  }

  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return Arrays.asList(
      TabCharacterCheck.class,
      TooLongLineCheck.class,
      MissingNewLineAtEndOfFileCheck.class,
      VarArgCheck.class,
      ParsingErrorCheck.class,
      MethodComplexityCheck.class,
      DisallowedThreadGroupCheck.class,
      ClassComplexityCheck.class,
      UndocumentedApiCheck.class,
      NoSonarCheck.class,
      CommentedOutCodeLineCheck.class,
      InputStreamReadCheck.class,
      EmptyFileCheck.class,
      EmptyBlockCheck.class,
      TooManyLinesOfCodeInFileCheck.class,
      TooManyParametersCheck.class,
      RawExceptionCheck.class,
      BadMethodNameCheck.class,
      BadClassNameCheck.class,
      BadInterfaceNameCheck.class,
      BadConstantNameCheck.class,
      BadLocalConstantNameCheck.class,
      BadFieldNameCheck.class,
      BadFieldNameStaticNonFinalCheck.class,
      BadLocalVariableNameCheck.class,
      BadAbstractClassNameCheck.class,
      BadTypeParameterNameCheck.class,
      BadPackageNameCheck.class,
      MissingCurlyBracesCheck.class,
      Struts1EndpointCheck.class,
      TooManyStatementsPerLineCheck.class,
      LeftCurlyBraceStartLineCheck.class,
      RightCurlyBraceSameLineAsNextBlockCheck.class,
      RightCurlyBraceStartLineCheck.class,
      RightCurlyBraceDifferentLineAsNextBlockCheck.class,
      LeftCurlyBraceEndLineCheck.class,
      UselessParenthesesCheck.class,
      ObjectFinalizeCheck.class,
      ObjectFinalizeOverridenCheck.class,
      ObjectFinalizeOverridenCallsSuperFinalizeCheck.class,
      ClassVariableVisibilityCheck.class,
      ForLoopCounterChangedCheck.class,
      LabelsShouldNotBeUsedCheck.class,
      SwitchLastCaseIsDefaultCheck.class,
      EmptyStatementUsageCheck.class,
      ModifiersOrderCheck.class,
      AssignmentInSubExpressionCheck.class,
      TrailingCommentCheck.class,
      UselessImportCheck.class,
      MissingDeprecatedCheck.class,
      IndentationCheck.class,
      HiddenFieldCheck.class,
      DeprecatedTagPresenceCheck.class,
      FixmeTagPresenceCheck.class,
      TodoTagPresenceCheck.class,
      UtilityClassWithPublicConstructorCheck.class,
      StringLiteralInsideEqualsCheck.class,
      ReturnOfBooleanExpressionsCheck.class,
      ReuseRandomCheck.class,
      AccessibilityChangeCheck.class,
      BooleanLiteralCheck.class,
      ExpressionComplexityCheck.class,
      NestedTryCatchCheck.class,
      SystemExitCalledCheck.class,
      ReturnInFinallyCheck.class,
      IfConditionAlwaysTrueOrFalseCheck.class,
      CaseInsensitiveComparisonCheck.class,
      MethodWithExcessiveReturnsCheck.class,
      CollectionIsEmptyCheck.class,
      CollectionSizeAndArrayLengthCheck.class,
      SynchronizedClassUsageCheck.class,
      NonStaticClassInitializerCheck.class,
      ReturnEmptyArrayNotNullCheck.class,
      ConstantsShouldBeStaticFinalCheck.class,
      ThrowsFromFinallyCheck.class,
      SystemOutOrErrUsageCheck.class,
      ExceptionsShouldBeImmutableCheck.class,
      CollapsibleIfCandidateCheck.class,
      NestedIfStatementsCheck.class,
      CatchOfThrowableOrErrorCheck.class,
      ImplementsEnumerationCheck.class,
      CloneMethodCallsSuperCloneCheck.class,
      SwitchCaseTooBigCheck.class,
      Struts2EndpointCheck.class,
      SwitchCaseWithoutBreakCheck.class,
      CatchUsesExceptionWithContextCheck.class,
      MethodTooBigCheck.class,
      MethodIdenticalImplementationsCheck.class,
      KeywordAsIdentifierCheck.class,
      AnonymousClassesTooBigCheck.class,
      SunPackagesUsedCheck.class,
      SeveralBreakOrContinuePerLoopCheck.class,
      EmptyMethodsCheck.class,
      MethodOnlyCallsSuperCheck.class,
      ObjectFinalizeOverridenNotPublicCheck.class,
      ObjectFinalizeOverloadedCheck.class,
      ConcatenationWithStringValueOfCheck.class,
      PrintStackTraceCalledWithoutArgumentCheck.class,
      ArrayDesignatorAfterTypeCheck.class,
      ErrorClassExtendedCheck.class,
      InstanceofUsedOnExceptionCheck.class,
      StringLiteralDuplicatedCheck.class,
      ToStringUsingBoxingCheck.class,
      GarbageCollectorCalledCheck.class,
      ArrayDesignatorOnVariableCheck.class,
      DefaultPackageCheck.class,
      PopulateBeansCheck.class,
      MethodNamedHashcodeOrEqualCheck.class,
      NestedBlocksCheck.class,
      InterfaceAsConstantContainerCheck.class,
      MethodNamedEqualsCheck.class,
      EqualsNotOverridenWithCompareToCheck.class,
      EqualsOverridenWithHashCodeCheck.class,
      SwitchWithLabelsCheck.class,
      SwitchAtLeastThreeCasesCheck.class,
      ClassCouplingCheck.class,
      OctalValuesCheck.class,
      NoPmdTagPresenceCheck.class,
      NoCheckstyleTagPresenceCheck.class,
      ParameterReassignedToCheck.class,
      HardcodedIpCheck.class,
      HardcodedURICheck.class,
      LoggersDeclarationCheck.class,
      MethodNameSameAsClassCheck.class,
      CollectionImplementationReferencedCheck.class,
      IncorrectOrderOfMembersCheck.class,
      PublicStaticFieldShouldBeFinalCheck.class,
      WildcardReturnParameterTypeCheck.class,
      UnusedLocalVariableCheck.class,
      UnusedPrivateFieldCheck.class,
      StringBufferAndBuilderWithCharCheck.class,
      FileHeaderCheck.class,
      IncrementDecrementInSubExpressionCheck.class,
      CollectionsEmptyConstantsCheck.class,
      UselessExtendsCheck.class,
      DITCheck.class,
      CallToDeprecatedMethodCheck.class,
      CallToFileDeleteOnExitMethodCheck.class,
      NioFileDeleteCheck.class,
      UnusedPrivateMethodCheck.class,
      UnusedPrivateClassCheck.class,
      RedundantThrowsDeclarationCheck.class,
      RedundantCloseCheck.class,
      RedundantAssignmentsCheck.class,
      ThrowsSeveralCheckedExceptionCheck.class,
      ThreadRunCheck.class,
      DuplicateConditionIfElseIfCheck.class,
      DuplicateArgumentCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      LambdaSingleExpressionCheck.class,
      LambdaOptionalParenthesisCheck.class,
      LambdaTypeParameterCheck.class,
      AnonymousClassShouldBeLambdaCheck.class,
      AbstractClassNoFieldShouldBeInterfaceCheck.class,
      SAMAnnotatedCheck.class,
      CatchNPECheck.class,
      FieldNameMatchingTypeNameCheck.class,
      AbstractClassWithoutAbstractMethodCheck.class,
      UnusedMethodParameterCheck.class,
      MagicNumberCheck.class,
      StringConcatenationInLoopCheck.class,
      CompareObjectWithEqualsCheck.class,
      CompareStringsBoxedTypesWithEqualsCheck.class,
      RepeatAnnotationCheck.class,
      NPEThrowCheck.class,
      NullDereferenceInConditionalCheck.class,
      SelfAssignementCheck.class,
      MismatchPackageDirectoryCheck.class,
      ReplaceLambdaByMethodRefCheck.class,
      FieldModifierCheck.class,
      CookieDomainCheck.class,
      SerializableFieldInSerializableClassCheck.class,
      PackageInfoCheck.class,
      SwitchWithTooManyCasesCheck.class,
      IdenticalCasesInSwitchCheck.class,
      IdenticalOperandOnBinaryExpressionCheck.class,
      FloatEqualityCheck.class,
      SQLInjectionCheck.class,
      TernaryOperatorCheck.class,
      OverrideAnnotationCheck.class,
      ForLoopIncrementAndUpdateCheck.class,
      EmptyClassCheck.class,
      InstanceOfAlwaysTrueCheck.class,
      RedundantTypeCastCheck.class,
      CollectionCallingItselfCheck.class,
      UnusedLabelCheck.class,
      ThrowCheckedExceptionCheck.class,
      CastArithmeticOperandCheck.class,
      IgnoredReturnValueCheck.class,
      ToStringReturningNullCheck.class,
      TransactionalMethodVisibilityCheck.class,
      CompareToResultTestCheck.class,
      CookieHttpOnlyCheck.class,
      CookieShouldNotContainSensitiveDataCheck.class,
      SecureCookieCheck.class,
      CatchIllegalMonitorStateExceptionCheck.class,
      ForLoopTerminationConditionCheck.class,
      HttpRefererCheck.class,
      HardCodedCredentialsCheck.class,
      PseudoRandomCheck.class,
      MainMethodThrowsExceptionCheck.class,
      ResultSetIsLastCheck.class,
      HasNextCallingNextCheck.class,
      ThreadWaitCallCheck.class,
      WaitOnConditionCheck.class,
      DisallowedMethodCheck.class,
      DisallowedConstructorCheck.class,
      ForLoopIncrementSignCheck.class,
      ForLoopFalseConditionCheck.class,
      DeprecatedHashAlgorithmCheck.class,
      ControllingPermissionsCheck.class,
      NullCipherCheck.class,
      GetRequestedSessionIdCheck.class,
      CollectionMethodsWithLinearComplexityCheck.class,
      ServletInstanceFieldCheck.class,
      BigDecimalDoubleConstructorCheck.class,
      ReflectionOnNonRuntimeAnnotationCheck.class,
      WaitInSynchronizeCheck.class,
      ThreadSleepCheck.class,
      WaitInWhileLoopCheck.class,
      IteratorNextExceptionCheck.class,
      AvoidDESCheck.class,
      RSAUsesOAEPCheck.class,
      AESAlgorithmCheck.class,
      ConstructorCallingOverridableCheck.class,
      EqualsOnAtomicClassCheck.class,
      XmlExternalEntityProcessingCheck.class,
      LDAPAuthenticatedConnectionCheck.class,
      LDAPDeserializationCheck.class,
      SecureXmlTransformerCheck.class,
      NonShortCircuitLogicCheck.class,
      ArrayHashCodeAndToStringCheck.class,
      DefaultEncodingUsageCheck.class,
      CloneableImplementingCloneCheck.class,
      PrintfFailCheck.class,
      PrintfMisuseCheck.class,
      ModulusEqualityCheck.class,
      RunFinalizersCheck.class,
      LongBitsToDoubleOnIntCheck.class,
      SynchronizationOnStringOrBoxedCheck.class,
      SerializableSuperConstructorCheck.class,
      NonSerializableWriteCheck.class,
      InnerClassOfSerializableCheck.class,
      InnerClassOfNonSerializableCheck.class,
      SerialVersionUidCheck.class,
      SerializableComparatorCheck.class,
      TransientFieldInNonSerializableCheck.class,
      CustomSerializationMethodCheck.class,
      InterfaceOrSuperclassShadowingCheck.class,
      RedundantModifierCheck.class,
      MathOnFloatCheck.class,
      StringToPrimitiveConversionCheck.class,
      ClassNamedLikeExceptionCheck.class,
      ProtectedMemberInFinalClassCheck.class,
      SuppressWarningsCheck.class,
      ImmediateReverseBoxingCheck.class,
      CustomCryptographicAlgorithmCheck.class,
      UnusedTypeParameterCheck.class,
      ShiftOnIntOrLongCheck.class,
      CompareToReturnValueCheck.class,
      FinalizeFieldsSetCheck.class,
      UnnecessarySemicolonCheck.class,
      NotifyCheck.class,
      ScheduledThreadPoolExecutorZeroCheck.class,
      ThreadOverridesRunCheck.class,
      CollectionInappropriateCallsCheck.class,
      BooleanMethodReturnCheck.class,
      PrimitiveTypeBoxingWithToStringCheck.class,
      SillyBitOperationCheck.class,
      InvalidDateValuesCheck.class,
      EqualsNotOverriddenInSubclassCheck.class,
      ClassComparedByNameCheck.class,
      ClassWithOnlyStaticMethodsInstantiationCheck.class,
      SerializableObjectInSessionCheck.class,
      StaticFieldInitializationCheck.class,
      UselessIncrementCheck.class,
      ObjectCreatedOnlyToCallGetClassCheck.class,
      PrimitiveWrappersInTernaryOperatorCheck.class,
      SynchronizedLockCheck.class,
      SymmetricEqualsCheck.class,
      LoopExecutingAtMostOnceCheck.class,
      RedundantJumpCheck.class,
      CallSuperMethodFromInnerClassCheck.class,
      SelectorMethodArgumentCheck.class,
      ThreadAsRunnableArgumentCheck.class,
      SynchronizedFieldAssignmentCheck.class,
      NullDereferenceCheck.class,
      InvariantReturnCheck.class,
      MinMaxRangeCheck.class,
      ConditionalUnreachableCodeCheck.class,
      UnclosedResourcesCheck.class,
      CustomUnclosedResourcesCheck.class,
      StaticFieldUpateCheck.class,
      IgnoredStreamReturnValueCheck.class,
      DateUtilsTruncateCheck.class,
      DateAndTimesCheck.class,
      PreparedStatementAndResultSetCheck.class,
      URLHashCodeAndEqualsCheck.class,
      ChildClassShadowFieldCheck.class,
      OperatorPrecedenceCheck.class,
      NestedEnumStaticCheck.class,
      UnusedReturnedDataCheck.class,
      StringToStringCheck.class,
      ThreadStartedInConstructorCheck.class,
      ThreadLocalWithInitialCheck.class,
      KeySetInsteadOfEntrySetCheck.class,
      IndexOfWithPositiveNumberCheck.class,
      ReadObjectSynchronizedCheck.class,
      AbsOnNegativeCheck.class,
      StaticMultithreadedUnsafeFieldsCheck.class,
      LocksNotUnlockedCheck.class,
      EqualsArgumentTypeCheck.class,
      ConstantMathCheck.class,
      SillyEqualsCheck.class,
      IndexOfStartPositionCheck.class,
      StaticMembersAccessCheck.class,
      MutableMembersUsageCheck.class,
      StaticMethodCheck.class,
      ForLoopUsedAsWhileLoopCheck.class,
      MultilineBlocksCurlyBracesCheck.class,
      MapComputeIfAbsentOrPresentCheck.class,
      EnumMapCheck.class,
      FileCreateTempFileCheck.class,
      BooleanInversionCheck.class,
      InnerStaticClassesCheck.class,
      StandardFunctionalInterfaceCheck.class,
      WildcardImportsShouldNotBeUsedCheck.class,
      FinalClassCheck.class,
      OneDeclarationPerLineCheck.class,
      ServletMethodsExceptionsThrownCheck.class,
      DynamicClassLoadCheck.class,
      MembersDifferOnlyByCapitalizationCheck.class,
      LoopsOnSameSetCheck.class,
      PublicStaticMutableMembersCheck.class,
      OneClassInterfacePerFileCheck.class,
      CloneOverrideCheck.class,
      TooManyMethodsCheck.class,
      UppercaseSuffixesCheck.class,
      InnerClassTooManyLinesCheck.class,
      DefaultInitializedFieldCheck.class,
      EscapedUnicodeCharactersCheck.class,
      MainInServletCheck.class,
      AtLeastOneConstructorCheck.class,
      CatchExceptionCheck.class,
      VariableDeclarationScopeCheck.class,
      AnnotationArgumentOrderCheck.class,
      AnnotationDefaultArgumentCheck.class,
      DeadStoreCheck.class,
      DiamondOperatorCheck.class,
      CommentRegularExpressionCheck.class,
      AssertOnBooleanVariableCheck.class,
      CombineCatchCheck.class,
      TryWithResourcesCheck.class,
      ConstantMethodCheck.class,
      ChangeMethodContractCheck.class,
      CatchRethrowingCheck.class,
      InappropriateRegexpCheck.class,
      WeakSSLContextCheck.class,
      CallOuterPrivateMethodCheck.class,
      SubClassStaticReferenceCheck.class,
      InterruptedExceptionCheck.class,
      RawByteBitwiseOperationsCheck.class,
      EnumSetCheck.class,
      StringPrimitiveConstructorCheck.class,
      EnumMutableFieldCheck.class,
      StringMethodsWithLocaleCheck.class,
      StringMethodsOnSingleCharCheck.class,
      ConfusingOverloadCheck.class,
      RedundantAbstractMethodCheck.class,
      NonNullSetToNullCheck.class,
      ConstructorInjectionCheck.class,
      NoWayOutLoopCheck.class,
      ExternalizableClassConstructorCheck.class,
      PrivateReadResolveCheck.class,
      RandomFloatToIntCheck.class,
      CognitiveComplexityMethodCheck.class,
      SyncGetterAndSetterCheck.class,
      ToArrayCheck.class,
      ClassWithoutHashCodeInHashStructureCheck.class,
      CollectInsteadOfForeachCheck.class,
      IgnoredOperationStatusCheck.class,
      UnderscoreOnNumberCheck.class,
      UnderscoreMisplacedOnNumberCheck.class,
      OptionalAsParameterCheck.class,
      DoubleBraceInitializationCheck.class,
      ArraysAsListOfPrimitiveToStreamCheck.class,
      DivisionByZeroCheck.class,
      SimpleClassNameCheck.class,
      NullShouldNotBeUsedWithOptionalCheck.class,
      IntegerToHexStringCheck.class,
      PrivateFieldUsedLocallyCheck.class,
      OptionalGetBeforeIsPresentCheck.class,
      ValueBasedObjectsShouldNotBeSerializedCheck.class,
      ValueBasedObjectUsedForLockCheck.class,
      FilesExistsJDK8Check.class,
      StaticImportCountCheck.class,
      ClassFieldCountCheck.class,
      DoubleCheckedLockingCheck.class,
      WriteObjectTheOnlySynchronizedMethodCheck.class,
      TrustManagerCertificateCheck.class,
      TwoLocksWaitCheck.class,
      SynchronizationOnGetClassCheck.class,
      DisallowedClassCheck.class,
      LazyArgEvaluationCheck.class,
      BooleanMethodNameCheck.class,
      StaticFieldUpdateInConstructorCheck.class,
      NestedTernaryOperatorsCheck.class,
      ControllerWithSessionAttributesCheck.class,
      SpringAntMatcherOrderCheck.class,
      SpringAutoConfigurationCheck.class,
      SpringComponentScanCheck.class,
      SpringBeansShouldBeAccessibleCheck.class,
      SpringComponentWithNonAutowiredMembersCheck.class,
      SpringConfigurationWithAutowiredFieldsCheck.class,
      SpringIncompatibleTransactionalCheck.class,
      SpringComponentWithWrongScopeCheck.class,
      SpringComposedRequestMappingCheck.class,
      SpringRequestMappingMethodCheck.class,
      SpringScanDefaultPackageCheck.class,
      DebugFeatureEnabledCheck.class,
      SpringSecurityDisableCSRFCheck.class,
      PersistentEntityUsedAsRequestParameterCheck.class,
      RequestMappingMethodPublicCheck.class,
      BooleanGratuitousExpressionsCheck.class,
      AllBranchesAreIdenticalCheck.class,
      ArrayForVarArgCheck.class,
      WrongAssignmentOperatorCheck.class,
      DateFormatWeekYearCheck.class,
      SpringConstructorInjectionCheck.class,
      UnusedThrowableCheck.class,
      ConditionalOnNewLineCheck.class,
      UselessPackageInfoCheck.class,
      StreamConsumedCheck.class,
      StreamNotConsumedCheck.class,
      PreferStreamAnyMatchCheck.class,
      OverwrittenKeyCheck.class,
      LeastSpecificTypeCheck.class,
      SwitchInsteadOfIfSequenceCheck.class,
      IterableIteratorCheck.class,
      OutputStreamOverrideWriteCheck.class,
      InputStreamOverrideReadCheck.class,
      PredictableSeedCheck.class,
      RedundantStreamCollectCheck.class,
      GettersSettersOnRightFieldCheck.class,
      ParameterNullnessCheck.class,
      DoublePrefixOperatorCheck.class,
      CompareToNotOverloadedCheck.class,
      EqualsParametersMarkedNonNullCheck.class,
      NestedSwitchStatementCheck.class,
      ThisExposedFromConstructorCheck.class,
      IfElseIfStatementEndsWithElseCheck.class,
      MethodParametersOrderCheck.class,
      AssertsOnParametersOfPublicMethodCheck.class,
      NullCheckWithInstanceofCheck.class,
      HostnameVerifierImplementationCheck.class,
      SwitchDefaultLastCaseCheck.class,
      RegexPatternsNeedlesslyCheck.class,
      SpecializedFunctionalInterfacesCheck.class,
      ZipEntryCheck.class,
      IndentationAfterConditionalCheck.class,
      CipherBlockChainingCheck.class,
      CryptographicKeySizeCheck.class,
      SMTPSSLServerIdentityCheck.class,
      StringOffsetMethodsCheck.class,
      EnumEqualCheck.class,
      XmlDeserializationCheck.class,
      JacksonDeserializationCheck.class,
      ObjectDeserializationCheck.class,
      EmptyDatabasePasswordCheck.class,
      StreamPeekCheck.class,
      LogConfigurationCheck.class,
      VolatileNonPrimitiveFieldCheck.class,
      GetClassLoaderCheck.class,
      ObjectOutputStreamCheck.class,
      VolatileVariablesOperationsCheck.class,
      RegexHotspotCheck.class,
      DataEncryptionCheck.class,
      DataHashingCheck.class,
      CommandLineArgumentsCheck.class,
      StandardInputReadCheck.class,
      SocketUsageCheck.class,
      BlindSerialVersionUidCheck.class,
      JdbcDriverExplicitLoadingCheck.class,
      StandardCharsetsConstantsCheck.class,
      PrimitivesMarkedNullableCheck.class,
      ForLoopVariableTypeCheck.class,
      ReplaceGuavaWithJava8Check.class,
      LoggedRethrownExceptionsCheck.class,
      CORSCheck.class,
      BasicAuthCheck.class,
      SynchronizedOverrideCheck.class,
      MissingBeanValidationCheck.class,
      UseSwitchExpressionCheck.class,
      EnvVariablesHotspotCheck.class,
      PasswordEncoderCheck.class,
      AndroidExternalStorageCheck.class,
      ReceivingIntentsCheck.class,
      AndroidBroadcastingCheck.class,
      EmailHotspotCheck.class,
      AndroidSSLConnectionCheck.class,
      LoggerClassCheck.class,
      DanglingElseStatementsCheck.class,
      ArrayCopyLoopCheck.class,
      StringCallsBeyondBoundsCheck.class,
      SuspiciousListRemoveCheck.class,
      StaticMemberAccessCheck.class,
      ThreadLocalCleanupCheck.class,
      SillyStringOperationsCheck.class,
      StringReplaceCheck.class,
      BoxedBooleanExpressionsCheck.class,
      DoubleCheckedLockingAssignmentCheck.class
    );
  }

  public static List<Class<? extends DebugCheck>> getDebugChecks() {
    return Arrays.asList(
      DebugMethodYieldsCheck.class,
      DebugInterruptedExecutionCheck.class,
      DebugMethodYieldsOnInvocationsCheck.class);
  }

  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return Arrays.asList(
      IgnoredTestsCheck.class,
      BooleanLiteralInAssertionsCheck.class,
      AssertionsWithoutMessageCheck.class,
      CallSuperInTestCaseCheck.class,
      AssertionInThreadRunCheck.class,
      NoTestInTestClassCheck.class,
      AssertionsInTestsCheck.class,
      JunitMethodDeclarationCheck.class,
      AssertionsCompletenessCheck.class,
      ThreadSleepInTestsCheck.class,
      UnusedTestRuleCheck.class,
      BadTestClassNameCheck.class,
      BadTestMethodNameCheck.class,
      AssertionFailInCatchBlockCheck.class,
      AssertionArgumentOrderCheck.class);
  }

  public static List<Class<? extends SonarXmlCheck>> getXmlChecks() {
    return Arrays.asList(
      DependencyWithSystemScopeCheck.class,
      DefaultMessageListenerContainerCheck.class,
      DatabaseSchemaUpdateCheck.class,
      DisallowedDependenciesCheck.class,
      PomElementOrderCheck.class,
      DefaultInterceptorsLocationCheck.class,
      SecurityConstraintsInWebXmlCheck.class,
      InterceptorExclusionsCheck.class,
      SingleConnectionFactoryCheck.class,
      ArtifactIdNamingConventionCheck.class,
      GroupIdNamingConventionCheck.class,
      ActionNumberCheck.class,
      ValidationFiltersCheck.class,
      FormNameDuplicationCheck.class,
      DeprecatedPomPropertiesCheck.class);
  }
}
