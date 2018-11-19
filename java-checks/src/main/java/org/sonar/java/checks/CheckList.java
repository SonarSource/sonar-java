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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
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
import org.sonar.java.checks.security.CipherBlockChainingCheck;
import org.sonar.java.checks.security.CommandLineArgumentsCheck;
import org.sonar.java.checks.security.ControllingPermissionsCheck;
import org.sonar.java.checks.security.CookieHttpOnlyCheck;
import org.sonar.java.checks.security.CookieShouldNotContainSensitiveDataCheck;
import org.sonar.java.checks.security.CryptographicKeySizeCheck;
import org.sonar.java.checks.security.DataEncryptionCheck;
import org.sonar.java.checks.security.DataHashingCheck;
import org.sonar.java.checks.security.DebugFeatureEnabledCheck;
import org.sonar.java.checks.security.DynamicCodeCheck;
import org.sonar.java.checks.security.EmptyDatabasePasswordCheck;
import org.sonar.java.checks.security.ExecCallCheck;
import org.sonar.java.checks.security.FileHandlingCheck;
import org.sonar.java.checks.security.HostnameVerifierImplementationCheck;
import org.sonar.java.checks.security.HttpRequestsHotspotCheck;
import org.sonar.java.checks.security.IntegerToHexStringCheck;
import org.sonar.java.checks.security.LDAPAuthenticatedConnectionCheck;
import org.sonar.java.checks.security.LDAPDeserializationCheck;
import org.sonar.java.checks.security.RegexHotspotCheck;
import org.sonar.java.checks.security.LogConfigurationCheck;
import org.sonar.java.checks.security.SMTPSSLServerIdentityCheck;
import org.sonar.java.checks.security.SecureCookieCheck;
import org.sonar.java.checks.security.SecureXmlTransformerCheck;
import org.sonar.java.checks.security.StandardInputReadCheck;
import org.sonar.java.checks.security.SocketUsageCheck;
import org.sonar.java.checks.security.TrustManagerCertificateCheck;
import org.sonar.java.checks.security.XPathUsageCheck;
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
import org.sonar.java.checks.spring.SpringEndpointsCheck;
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

public final class CheckList {

  public static final String REPOSITORY_KEY = "squid";

  private CheckList() {
  }

  public static List<Class> getChecks() {
    return ImmutableList.<Class>builder().addAll(getJavaChecks()).addAll(getJavaTestChecks()).addAll(getXmlChecks()).build();
  }

  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      .add(TabCharacterCheck.class)
      .add(TooLongLineCheck.class)
      .add(MissingNewLineAtEndOfFileCheck.class)
      .add(VarArgCheck.class)
      .add(ParsingErrorCheck.class)
      .add(MethodComplexityCheck.class)
      .add(DisallowedThreadGroupCheck.class)
      .add(ClassComplexityCheck.class)
      .add(UndocumentedApiCheck.class)
      .add(NoSonarCheck.class)
      .add(CommentedOutCodeLineCheck.class)
      .add(InputStreamReadCheck.class)
      .add(EmptyFileCheck.class)
      .add(EmptyBlockCheck.class)
      .add(TooManyLinesOfCodeInFileCheck.class)
      .add(TooManyParametersCheck.class)
      .add(RawExceptionCheck.class)
      .add(BadMethodNameCheck.class)
      .add(BadClassNameCheck.class)
      .add(BadInterfaceNameCheck.class)
      .add(BadConstantNameCheck.class)
      .add(BadLocalConstantNameCheck.class)
      .add(BadFieldNameCheck.class)
      .add(BadFieldNameStaticNonFinalCheck.class)
      .add(BadLocalVariableNameCheck.class)
      .add(BadAbstractClassNameCheck.class)
      .add(BadTypeParameterNameCheck.class)
      .add(BadPackageNameCheck.class)
      .add(MissingCurlyBracesCheck.class)
      .add(Struts1EndpointCheck.class)
      .add(TooManyStatementsPerLineCheck.class)
      .add(LeftCurlyBraceStartLineCheck.class)
      .add(RightCurlyBraceSameLineAsNextBlockCheck.class)
      .add(RightCurlyBraceStartLineCheck.class)
      .add(RightCurlyBraceDifferentLineAsNextBlockCheck.class)
      .add(LeftCurlyBraceEndLineCheck.class)
      .add(UselessParenthesesCheck.class)
      .add(ObjectFinalizeCheck.class)
      .add(ObjectFinalizeOverridenCheck.class)
      .add(ObjectFinalizeOverridenCallsSuperFinalizeCheck.class)
      .add(ClassVariableVisibilityCheck.class)
      .add(ForLoopCounterChangedCheck.class)
      .add(LabelsShouldNotBeUsedCheck.class)
      .add(SwitchLastCaseIsDefaultCheck.class)
      .add(EmptyStatementUsageCheck.class)
      .add(ModifiersOrderCheck.class)
      .add(AssignmentInSubExpressionCheck.class)
      .add(TrailingCommentCheck.class)
      .add(UselessImportCheck.class)
      .add(MissingDeprecatedCheck.class)
      .add(IndentationCheck.class)
      .add(HiddenFieldCheck.class)
      .add(DeprecatedTagPresenceCheck.class)
      .add(FixmeTagPresenceCheck.class)
      .add(TodoTagPresenceCheck.class)
      .add(UtilityClassWithPublicConstructorCheck.class)
      .add(StringLiteralInsideEqualsCheck.class)
      .add(ReturnOfBooleanExpressionsCheck.class)
      .add(ReuseRandomCheck.class)
      .add(AccessibilityChangeCheck.class)
      .add(BooleanLiteralCheck.class)
      .add(ExpressionComplexityCheck.class)
      .add(NestedTryCatchCheck.class)
      .add(SystemExitCalledCheck.class)
      .add(ReturnInFinallyCheck.class)
      .add(IfConditionAlwaysTrueOrFalseCheck.class)
      .add(CaseInsensitiveComparisonCheck.class)
      .add(MethodWithExcessiveReturnsCheck.class)
      .add(CollectionIsEmptyCheck.class)
      .add(CollectionSizeAndArrayLengthCheck.class)
      .add(SynchronizedClassUsageCheck.class)
      .add(NonStaticClassInitializerCheck.class)
      .add(ReturnEmptyArrayNotNullCheck.class)
      .add(ConstantsShouldBeStaticFinalCheck.class)
      .add(ThrowsFromFinallyCheck.class)
      .add(SystemOutOrErrUsageCheck.class)
      .add(ExceptionsShouldBeImmutableCheck.class)
      .add(CollapsibleIfCandidateCheck.class)
      .add(NestedIfStatementsCheck.class)
      .add(CatchOfThrowableOrErrorCheck.class)
      .add(ImplementsEnumerationCheck.class)
      .add(CloneMethodCallsSuperCloneCheck.class)
      .add(SwitchCaseTooBigCheck.class)
      .add(Struts2EndpointCheck.class)
      .add(SwitchCaseWithoutBreakCheck.class)
      .add(CatchUsesExceptionWithContextCheck.class)
      .add(MethodTooBigCheck.class)
      .add(MethodIdenticalImplementationsCheck.class)
      .add(KeywordAsIdentifierCheck.class)
      .add(AnonymousClassesTooBigCheck.class)
      .add(SunPackagesUsedCheck.class)
      .add(SeveralBreakOrContinuePerLoopCheck.class)
      .add(EmptyMethodsCheck.class)
      .add(MethodOnlyCallsSuperCheck.class)
      .add(ObjectFinalizeOverridenNotPublicCheck.class)
      .add(ObjectFinalizeOverloadedCheck.class)
      .add(ConcatenationWithStringValueOfCheck.class)
      .add(PrintStackTraceCalledWithoutArgumentCheck.class)
      .add(ArrayDesignatorAfterTypeCheck.class)
      .add(ErrorClassExtendedCheck.class)
      .add(InstanceofUsedOnExceptionCheck.class)
      .add(StringLiteralDuplicatedCheck.class)
      .add(ToStringUsingBoxingCheck.class)
      .add(GarbageCollectorCalledCheck.class)
      .add(ArrayDesignatorOnVariableCheck.class)
      .add(DefaultPackageCheck.class)
      .add(PopulateBeansCheck.class)
      .add(MethodNamedHashcodeOrEqualCheck.class)
      .add(NestedBlocksCheck.class)
      .add(InterfaceAsConstantContainerCheck.class)
      .add(MethodNamedEqualsCheck.class)
      .add(EqualsNotOverridenWithCompareToCheck.class)
      .add(EqualsOverridenWithHashCodeCheck.class)
      .add(SwitchWithLabelsCheck.class)
      .add(SwitchAtLeastThreeCasesCheck.class)
      .add(ClassCouplingCheck.class)
      .add(OctalValuesCheck.class)
      .add(NoPmdTagPresenceCheck.class)
      .add(NoCheckstyleTagPresenceCheck.class)
      .add(ParameterReassignedToCheck.class)
      .add(HardcodedIpCheck.class)
      .add(HardcodedURICheck.class)
      .add(LoggersDeclarationCheck.class)
      .add(MethodNameSameAsClassCheck.class)
      .add(CollectionImplementationReferencedCheck.class)
      .add(IncorrectOrderOfMembersCheck.class)
      .add(PublicStaticFieldShouldBeFinalCheck.class)
      .add(WildcardReturnParameterTypeCheck.class)
      .add(UnusedLocalVariableCheck.class)
      .add(UnusedPrivateFieldCheck.class)
      .add(StringBufferAndBuilderWithCharCheck.class)
      .add(FileHeaderCheck.class)
      .add(IncrementDecrementInSubExpressionCheck.class)
      .add(CollectionsEmptyConstantsCheck.class)
      .add(UselessExtendsCheck.class)
      .add(DITCheck.class)
      .add(CallToDeprecatedMethodCheck.class)
      .add(CallToFileDeleteOnExitMethodCheck.class)
      .add(NioFileDeleteCheck.class)
      .add(UnusedPrivateMethodCheck.class)
      .add(UnusedPrivateClassCheck.class)
      .add(RedundantThrowsDeclarationCheck.class)
      .add(RedundantCloseCheck.class)
      .add(RedundantAssignmentsCheck.class)
      .add(ThrowsSeveralCheckedExceptionCheck.class)
      .add(ThreadRunCheck.class)
      .add(DuplicateConditionIfElseIfCheck.class)
      .add(DuplicateArgumentCheck.class)
      .add(ImmediatelyReturnedVariableCheck.class)
      .add(LambdaSingleExpressionCheck.class)
      .add(LambdaOptionalParenthesisCheck.class)
      .add(LambdaTypeParameterCheck.class)
      .add(AnonymousClassShouldBeLambdaCheck.class)
      .add(AbstractClassNoFieldShouldBeInterfaceCheck.class)
      .add(SAMAnnotatedCheck.class)
      .add(CatchNPECheck.class)
      .add(FieldNameMatchingTypeNameCheck.class)
      .add(AbstractClassWithoutAbstractMethodCheck.class)
      .add(UnusedMethodParameterCheck.class)
      .add(MagicNumberCheck.class)
      .add(StringConcatenationInLoopCheck.class)
      .add(CompareObjectWithEqualsCheck.class)
      .add(RepeatAnnotationCheck.class)
      .add(NPEThrowCheck.class)
      .add(NullDereferenceInConditionalCheck.class)
      .add(SelfAssignementCheck.class)
      .add(MismatchPackageDirectoryCheck.class)
      .add(ReplaceLambdaByMethodRefCheck.class)
      .add(FieldModifierCheck.class)
      .add(SerializableFieldInSerializableClassCheck.class)
      .add(PackageInfoCheck.class)
      .add(SwitchWithTooManyCasesCheck.class)
      .add(IdenticalCasesInSwitchCheck.class)
      .add(IdenticalOperandOnBinaryExpressionCheck.class)
      .add(FloatEqualityCheck.class)
      .add(SQLInjectionCheck.class)
      .add(TernaryOperatorCheck.class)
      .add(OverrideAnnotationCheck.class)
      .add(ForLoopIncrementAndUpdateCheck.class)
      .add(EmptyClassCheck.class)
      .add(InstanceOfAlwaysTrueCheck.class)
      .add(RedundantTypeCastCheck.class)
      .add(CollectionCallingItselfCheck.class)
      .add(UnusedLabelCheck.class)
      .add(ThrowCheckedExceptionCheck.class)
      .add(CastArithmeticOperandCheck.class)
      .add(IgnoredReturnValueCheck.class)
      .add(ToStringReturningNullCheck.class)
      .add(TransactionalMethodVisibilityCheck.class)
      .add(CompareToResultTestCheck.class)
      .add(CookieHttpOnlyCheck.class)
      .add(CookieShouldNotContainSensitiveDataCheck.class)
      .add(SecureCookieCheck.class)
      .add(CatchIllegalMonitorStateExceptionCheck.class)
      .add(ForLoopTerminationConditionCheck.class)
      .add(HttpRefererCheck.class)
      .add(HardCodedCredentialsCheck.class)
      .add(PseudoRandomCheck.class)
      .add(MainMethodThrowsExceptionCheck.class)
      .add(ResultSetIsLastCheck.class)
      .add(HasNextCallingNextCheck.class)
      .add(ThreadWaitCallCheck.class)
      .add(WaitOnConditionCheck.class)
      .add(DisallowedMethodCheck.class)
      .add(DisallowedConstructorCheck.class)
      .add(ForLoopIncrementSignCheck.class)
      .add(ForLoopFalseConditionCheck.class)
      .add(DeprecatedHashAlgorithmCheck.class)
      .add(ControllingPermissionsCheck.class)
      .add(NullCipherCheck.class)
      .add(GetRequestedSessionIdCheck.class)
      .add(CollectionMethodsWithLinearComplexityCheck.class)
      .add(ServletInstanceFieldCheck.class)
      .add(BigDecimalDoubleConstructorCheck.class)
      .add(ReflectionOnNonRuntimeAnnotationCheck.class)
      .add(WaitInSynchronizeCheck.class)
      .add(ThreadSleepCheck.class)
      .add(WaitInWhileLoopCheck.class)
      .add(IteratorNextExceptionCheck.class)
      .add(AvoidDESCheck.class)
      .add(RSAUsesOAEPCheck.class)
      .add(AESAlgorithmCheck.class)
      .add(ConstructorCallingOverridableCheck.class)
      .add(EqualsOnAtomicClassCheck.class)
      .add(XmlExternalEntityProcessingCheck.class)
      .add(LDAPAuthenticatedConnectionCheck.class)
      .add(LDAPDeserializationCheck.class)
      .add(SecureXmlTransformerCheck.class)
      .add(NonShortCircuitLogicCheck.class)
      .add(ArrayHashCodeAndToStringCheck.class)
      .add(DefaultEncodingUsageCheck.class)
      .add(CloneableImplementingCloneCheck.class)
      .add(PrintfFailCheck.class)
      .add(PrintfMisuseCheck.class)
      .add(ModulusEqualityCheck.class)
      .add(RunFinalizersCheck.class)
      .add(LongBitsToDoubleOnIntCheck.class)
      .add(SynchronizationOnStringOrBoxedCheck.class)
      .add(SerializableSuperConstructorCheck.class)
      .add(NonSerializableWriteCheck.class)
      .add(InnerClassOfSerializableCheck.class)
      .add(InnerClassOfNonSerializableCheck.class)
      .add(SerialVersionUidCheck.class)
      .add(SerializableComparatorCheck.class)
      .add(TransientFieldInNonSerializableCheck.class)
      .add(CustomSerializationMethodCheck.class)
      .add(InterfaceOrSuperclassShadowingCheck.class)
      .add(RedundantModifierCheck.class)
      .add(MathOnFloatCheck.class)
      .add(StringToPrimitiveConversionCheck.class)
      .add(ClassNamedLikeExceptionCheck.class)
      .add(ProtectedMemberInFinalClassCheck.class)
      .add(SuppressWarningsCheck.class)
      .add(ImmediateReverseBoxingCheck.class)
      .add(CustomCryptographicAlgorithmCheck.class)
      .add(UnusedTypeParameterCheck.class)
      .add(ShiftOnIntOrLongCheck.class)
      .add(CompareToReturnValueCheck.class)
      .add(FinalizeFieldsSetCheck.class)
      .add(UnnecessarySemicolonCheck.class)
      .add(NotifyCheck.class)
      .add(ScheduledThreadPoolExecutorZeroCheck.class)
      .add(ThreadOverridesRunCheck.class)
      .add(CollectionInappropriateCallsCheck.class)
      .add(BooleanMethodReturnCheck.class)
      .add(PrimitiveTypeBoxingWithToStringCheck.class)
      .add(SillyBitOperationCheck.class)
      .add(InvalidDateValuesCheck.class)
      .add(EqualsNotOverriddenInSubclassCheck.class)
      .add(ClassComparedByNameCheck.class)
      .add(ClassWithOnlyStaticMethodsInstantiationCheck.class)
      .add(SerializableObjectInSessionCheck.class)
      .add(StaticFieldInitializationCheck.class)
      .add(UselessIncrementCheck.class)
      .add(ObjectCreatedOnlyToCallGetClassCheck.class)
      .add(PrimitiveWrappersInTernaryOperatorCheck.class)
      .add(SynchronizedLockCheck.class)
      .add(SymmetricEqualsCheck.class)
      .add(LoopExecutingAtMostOnceCheck.class)
      .add(RedundantJumpCheck.class)
      .add(CallSuperMethodFromInnerClassCheck.class)
      .add(SelectorMethodArgumentCheck.class)
      .add(ThreadAsRunnableArgumentCheck.class)
      .add(SynchronizedFieldAssignmentCheck.class)
      .add(NullDereferenceCheck.class)
      .add(InvariantReturnCheck.class)
      .add(MinMaxRangeCheck.class)
      .add(ConditionalUnreachableCodeCheck.class)
      .add(UnclosedResourcesCheck.class)
      .add(CustomUnclosedResourcesCheck.class)
      .add(StaticFieldUpateCheck.class)
      .add(IgnoredStreamReturnValueCheck.class)
      .add(DateUtilsTruncateCheck.class)
      .add(DateAndTimesCheck.class)
      .add(PreparedStatementAndResultSetCheck.class)
      .add(URLHashCodeAndEqualsCheck.class)
      .add(ChildClassShadowFieldCheck.class)
      .add(OperatorPrecedenceCheck.class)
      .add(NestedEnumStaticCheck.class)
      .add(UnusedReturnedDataCheck.class)
      .add(StringToStringCheck.class)
      .add(ThreadStartedInConstructorCheck.class)
      .add(ThreadLocalWithInitialCheck.class)
      .add(KeySetInsteadOfEntrySetCheck.class)
      .add(IndexOfWithPositiveNumberCheck.class)
      .add(ReadObjectSynchronizedCheck.class)
      .add(AbsOnNegativeCheck.class)
      .add(StaticMultithreadedUnsafeFieldsCheck.class)
      .add(LocksNotUnlockedCheck.class)
      .add(EqualsArgumentTypeCheck.class)
      .add(ConstantMathCheck.class)
      .add(SillyEqualsCheck.class)
      .add(IndexOfStartPositionCheck.class)
      .add(StaticMembersAccessCheck.class)
      .add(MutableMembersUsageCheck.class)
      .add(StaticMethodCheck.class)
      .add(ForLoopUsedAsWhileLoopCheck.class)
      .add(MultilineBlocksCurlyBracesCheck.class)
      .add(MapComputeIfAbsentOrPresentCheck.class)
      .add(EnumMapCheck.class)
      .add(FileCreateTempFileCheck.class)
      .add(BooleanInversionCheck.class)
      .add(InnerStaticClassesCheck.class)
      .add(StandardFunctionalInterfaceCheck.class)
      .add(WildcardImportsShouldNotBeUsedCheck.class)
      .add(FinalClassCheck.class)
      .add(OneDeclarationPerLineCheck.class)
      .add(ServletMethodsExceptionsThrownCheck.class)
      .add(DynamicClassLoadCheck.class)
      .add(MembersDifferOnlyByCapitalizationCheck.class)
      .add(LoopsOnSameSetCheck.class)
      .add(PublicStaticMutableMembersCheck.class)
      .add(OneClassInterfacePerFileCheck.class)
      .add(CloneOverrideCheck.class)
      .add(TooManyMethodsCheck.class)
      .add(UppercaseSuffixesCheck.class)
      .add(InnerClassTooManyLinesCheck.class)
      .add(DefaultInitializedFieldCheck.class)
      .add(EscapedUnicodeCharactersCheck.class)
      .add(MainInServletCheck.class)
      .add(AtLeastOneConstructorCheck.class)
      .add(CatchExceptionCheck.class)
      .add(VariableDeclarationScopeCheck.class)
      .add(AnnotationArgumentOrderCheck.class)
      .add(AnnotationDefaultArgumentCheck.class)
      .add(DeadStoreCheck.class)
      .add(DiamondOperatorCheck.class)
      .add(CommentRegularExpressionCheck.class)
      .add(AssertOnBooleanVariableCheck.class)
      .add(CombineCatchCheck.class)
      .add(TryWithResourcesCheck.class)
      .add(ConstantMethodCheck.class)
      .add(ChangeMethodContractCheck.class)
      .add(CatchRethrowingCheck.class)
      .add(InappropriateRegexpCheck.class)
      .add(WeakSSLContextCheck.class)
      .add(CallOuterPrivateMethodCheck.class)
      .add(SubClassStaticReferenceCheck.class)
      .add(InterruptedExceptionCheck.class)
      .add(RawByteBitwiseOperationsCheck.class)
      .add(EnumSetCheck.class)
      .add(StringPrimitiveConstructorCheck.class)
      .add(EnumMutableFieldCheck.class)
      .add(StringMethodsWithLocaleCheck.class)
      .add(StringMethodsOnSingleCharCheck.class)
      .add(ConfusingOverloadCheck.class)
      .add(RedundantAbstractMethodCheck.class)
      .add(NonNullSetToNullCheck.class)
      .add(ConstructorInjectionCheck.class)
      .add(NoWayOutLoopCheck.class)
      .add(ExternalizableClassConstructorCheck.class)
      .add(PrivateReadResolveCheck.class)
      .add(RandomFloatToIntCheck.class)
      .add(CognitiveComplexityMethodCheck.class)
      .add(SyncGetterAndSetterCheck.class)
      .add(ToArrayCheck.class)
      .add(ClassWithoutHashCodeInHashStructureCheck.class)
      .add(CollectInsteadOfForeachCheck.class)
      .add(IgnoredOperationStatusCheck.class)
      .add(UnderscoreOnNumberCheck.class)
      .add(UnderscoreMisplacedOnNumberCheck.class)
      .add(OptionalAsParameterCheck.class)
      .add(DoubleBraceInitializationCheck.class)
      .add(ArraysAsListOfPrimitiveToStreamCheck.class)
      .add(DivisionByZeroCheck.class)
      .add(SimpleClassNameCheck.class)
      .add(NullShouldNotBeUsedWithOptionalCheck.class)
      .add(IntegerToHexStringCheck.class)
      .add(PrivateFieldUsedLocallyCheck.class)
      .add(OptionalGetBeforeIsPresentCheck.class)
      .add(ValueBasedObjectsShouldNotBeSerializedCheck.class)
      .add(ValueBasedObjectUsedForLockCheck.class)
      .add(FilesExistsJDK8Check.class)
      .add(StaticImportCountCheck.class)
      .add(ClassFieldCountCheck.class)
      .add(DoubleCheckedLockingCheck.class)
      .add(WriteObjectTheOnlySynchronizedMethodCheck.class)
      .add(TrustManagerCertificateCheck.class)
      .add(TwoLocksWaitCheck.class)
      .add(SynchronizationOnGetClassCheck.class)
      .add(DisallowedClassCheck.class)
      .add(LazyArgEvaluationCheck.class)
      .add(BooleanMethodNameCheck.class)
      .add(StaticFieldUpdateInConstructorCheck.class)
      .add(NestedTernaryOperatorsCheck.class)
      .add(ControllerWithSessionAttributesCheck.class)
      .add(SpringAntMatcherOrderCheck.class)
      .add(SpringAutoConfigurationCheck.class)
      .add(SpringComponentScanCheck.class)
      .add(SpringBeansShouldBeAccessibleCheck.class)
      .add(SpringComponentWithNonAutowiredMembersCheck.class)
      .add(SpringConfigurationWithAutowiredFieldsCheck.class)
      .add(SpringIncompatibleTransactionalCheck.class)
      .add(SpringComponentWithWrongScopeCheck.class)
      .add(SpringComposedRequestMappingCheck.class)
      .add(SpringRequestMappingMethodCheck.class)
      .add(SpringScanDefaultPackageCheck.class)
      .add(DebugFeatureEnabledCheck.class)
      .add(SpringSecurityDisableCSRFCheck.class)
      .add(PersistentEntityUsedAsRequestParameterCheck.class)
      .add(RequestMappingMethodPublicCheck.class)
      .add(BooleanGratuitousExpressionsCheck.class)
      .add(AllBranchesAreIdenticalCheck.class)
      .add(ArrayForVarArgCheck.class)
      .add(WrongAssignmentOperatorCheck.class)
      .add(DateFormatWeekYearCheck.class)
      .add(SpringConstructorInjectionCheck.class)
      .add(UnusedThrowableCheck.class)
      .add(ConditionalOnNewLineCheck.class)
      .add(UselessPackageInfoCheck.class)
      .add(StreamConsumedCheck.class)
      .add(StreamNotConsumedCheck.class)
      .add(PreferStreamAnyMatchCheck.class)
      .add(OverwrittenKeyCheck.class)
      .add(LeastSpecificTypeCheck.class)
      .add(SwitchInsteadOfIfSequenceCheck.class)
      .add(IterableIteratorCheck.class)
      .add(OutputStreamOverrideWriteCheck.class)
      .add(InputStreamOverrideReadCheck.class)
      .add(PredictableSeedCheck.class)
      .add(RedundantStreamCollectCheck.class)
      .add(GettersSettersOnRightFieldCheck.class)
      .add(ParameterNullnessCheck.class)
      .add(DoublePrefixOperatorCheck.class)
      .add(CompareToNotOverloadedCheck.class)
      .add(EqualsParametersMarkedNonNullCheck.class)
      .add(NestedSwitchStatementCheck.class)
      .add(ThisExposedFromConstructorCheck.class)
      .add(IfElseIfStatementEndsWithElseCheck.class)
      .add(MethodParametersOrderCheck.class)
      .add(AssertsOnParametersOfPublicMethodCheck.class)
      .add(NullCheckWithInstanceofCheck.class)
      .add(HostnameVerifierImplementationCheck.class)
      .add(SwitchDefaultLastCaseCheck.class)
      .add(RegexPatternsNeedlesslyCheck.class)
      .add(SpecializedFunctionalInterfacesCheck.class)
      .add(ZipEntryCheck.class)
      .add(IndentationAfterConditionalCheck.class)
      .add(CipherBlockChainingCheck.class)
      .add(CryptographicKeySizeCheck.class)
      .add(SMTPSSLServerIdentityCheck.class)
      .add(StringOffsetMethodsCheck.class)
      .add(EnumEqualCheck.class)
      .add(XmlDeserializationCheck.class)
      .add(JacksonDeserializationCheck.class)
      .add(ObjectDeserializationCheck.class)
      .add(SpringEndpointsCheck.class)
      .add(ExecCallCheck.class)
      .add(EmptyDatabasePasswordCheck.class)
      .add(StreamPeekCheck.class)
      .add(LogConfigurationCheck.class)
      .add(VolatileNonPrimitiveFieldCheck.class)
      .add(DynamicCodeCheck.class)
      .add(GetClassLoaderCheck.class)
      .add(ObjectOutputStreamCheck.class)
      .add(VolatileVariablesOperationsCheck.class)
      .add(RegexHotspotCheck.class)
      .add(DataEncryptionCheck.class)
      .add(DataHashingCheck.class)
      .add(CommandLineArgumentsCheck.class)
      .add(StandardInputReadCheck.class)
      .add(FileHandlingCheck.class)
      .add(SocketUsageCheck.class)
      .add(XPathUsageCheck.class)
      .add(HttpRequestsHotspotCheck.class)
      .add(BlindSerialVersionUidCheck.class)
      .add(JdbcDriverExplicitLoadingCheck.class)
      .add(StandardCharsetsConstantsCheck.class)
      .add(PrimitivesMarkedNullableCheck.class)
      .add(ForLoopVariableTypeCheck.class)
      .add(ReplaceGuavaWithJava8Check.class)
      .add(LoggedRethrownExceptionsCheck.class)
      .build();
  }

  public static List<Class<? extends DebugCheck>> getDebugChecks() {
    return ImmutableList.<Class<? extends DebugCheck>>builder()
      .add(DebugMethodYieldsCheck.class)
      .add(DebugInterruptedExecutionCheck.class)
      .add(DebugMethodYieldsOnInvocationsCheck.class)
      .build();
  }

  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      .add(IgnoredTestsCheck.class)
      .add(BooleanLiteralInAssertionsCheck.class)
      .add(AssertionsWithoutMessageCheck.class)
      .add(CallSuperInTestCaseCheck.class)
      .add(AssertionInThreadRunCheck.class)
      .add(NoTestInTestClassCheck.class)
      .add(AssertionsInTestsCheck.class)
      .add(JunitMethodDeclarationCheck.class)
      .add(AssertionsCompletenessCheck.class)
      .add(ThreadSleepInTestsCheck.class)
      .add(UnusedTestRuleCheck.class)
      .add(BadTestClassNameCheck.class)
      .add(BadTestMethodNameCheck.class)
      .add(AssertionFailInCatchBlockCheck.class)
      .add(AssertionArgumentOrderCheck.class)
      .build();
  }

  public static List<Class<? extends JavaCheck>> getXmlChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      .addAll(getMavenChecks())
      .add(DefaultInterceptorsLocationCheck.class)
      .add(InterceptorExclusionsCheck.class)
      .add(SingleConnectionFactoryCheck.class)
      .add(DefaultMessageListenerContainerCheck.class)
      .add(SecurityConstraintsInWebXmlCheck.class)
      .add(ValidationFiltersCheck.class)
      .add(ActionNumberCheck.class)
      .add(FormNameDuplicationCheck.class)
      .add(DatabaseSchemaUpdateCheck.class)
      .build();
  }

  private static List<Class<? extends JavaCheck>> getMavenChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      .add(PomElementOrderCheck.class)
      .add(DependencyWithSystemScopeCheck.class)
      .add(GroupIdNamingConventionCheck.class)
      .add(ArtifactIdNamingConventionCheck.class)
      .add(DisallowedDependenciesCheck.class)
      .add(DeprecatedPomPropertiesCheck.class)
      .build();
  }
}
