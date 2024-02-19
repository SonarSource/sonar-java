/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.java.checks.AbsOnNegativeCheck;
import org.sonar.java.checks.AbstractClassNoFieldShouldBeInterfaceCheck;
import org.sonar.java.checks.AbstractClassWithoutAbstractMethodCheck;
import org.sonar.java.checks.AccessibilityChangeCheck;
import org.sonar.java.checks.AccessibilityChangeOnRecordsCheck;
import org.sonar.java.checks.AllBranchesAreIdenticalCheck;
import org.sonar.java.checks.AnnotationDefaultArgumentCheck;
import org.sonar.java.checks.AnonymousClassShouldBeLambdaCheck;
import org.sonar.java.checks.AnonymousClassesTooBigCheck;
import org.sonar.java.checks.ArrayCopyLoopCheck;
import org.sonar.java.checks.ArrayDesignatorAfterTypeCheck;
import org.sonar.java.checks.ArrayDesignatorOnVariableCheck;
import org.sonar.java.checks.ArrayForVarArgCheck;
import org.sonar.java.checks.ArrayHashCodeAndToStringCheck;
import org.sonar.java.checks.ArraysAsListOfPrimitiveToStreamCheck;
import org.sonar.java.checks.AssertOnBooleanVariableCheck;
import org.sonar.java.checks.AssertionsInProductionCodeCheck;
import org.sonar.java.checks.AssertsOnParametersOfPublicMethodCheck;
import org.sonar.java.checks.AssignmentInSubExpressionCheck;
import org.sonar.java.checks.AtLeastOneConstructorCheck;
import org.sonar.java.checks.BasicAuthCheck;
import org.sonar.java.checks.BigDecimalDoubleConstructorCheck;
import org.sonar.java.checks.BluetoothLowPowerModeCheck;
import org.sonar.java.checks.BooleanInversionCheck;
import org.sonar.java.checks.BooleanLiteralCheck;
import org.sonar.java.checks.BooleanMethodReturnCheck;
import org.sonar.java.checks.BoxedBooleanExpressionsCheck;
import org.sonar.java.checks.CORSCheck;
import org.sonar.java.checks.CallOuterPrivateMethodCheck;
import org.sonar.java.checks.CallSuperMethodFromInnerClassCheck;
import org.sonar.java.checks.CallToDeprecatedCodeMarkedForRemovalCheck;
import org.sonar.java.checks.CallToDeprecatedMethodCheck;
import org.sonar.java.checks.CallToFileDeleteOnExitMethodCheck;
import org.sonar.java.checks.CaseInsensitiveComparisonCheck;
import org.sonar.java.checks.CastArithmeticOperandCheck;
import org.sonar.java.checks.CatchExceptionCheck;
import org.sonar.java.checks.CatchIllegalMonitorStateExceptionCheck;
import org.sonar.java.checks.CatchNPECheck;
import org.sonar.java.checks.CatchOfThrowableOrErrorCheck;
import org.sonar.java.checks.CatchRethrowingCheck;
import org.sonar.java.checks.CatchUsesExceptionWithContextCheck;
import org.sonar.java.checks.ChangeMethodContractCheck;
import org.sonar.java.checks.ChildClassShadowFieldCheck;
import org.sonar.java.checks.ClassComparedByNameCheck;
import org.sonar.java.checks.ClassFieldCountCheck;
import org.sonar.java.checks.ClassVariableVisibilityCheck;
import org.sonar.java.checks.ClassWithOnlyStaticMethodsInstantiationCheck;
import org.sonar.java.checks.ClassWithoutHashCodeInHashStructureCheck;
import org.sonar.java.checks.CloneMethodCallsSuperCloneCheck;
import org.sonar.java.checks.CloneOverrideCheck;
import org.sonar.java.checks.CloneableImplementingCloneCheck;
import org.sonar.java.checks.CognitiveComplexityMethodCheck;
import org.sonar.java.checks.CollapsibleIfCandidateCheck;
import org.sonar.java.checks.CollectInsteadOfForeachCheck;
import org.sonar.java.checks.CollectionCallingItselfCheck;
import org.sonar.java.checks.CollectionConstructorReferenceCheck;
import org.sonar.java.checks.CollectionImplementationReferencedCheck;
import org.sonar.java.checks.CollectionInappropriateCallsCheck;
import org.sonar.java.checks.CollectionIsEmptyCheck;
import org.sonar.java.checks.CollectionMethodsWithLinearComplexityCheck;
import org.sonar.java.checks.CollectionSizeAndArrayLengthCheck;
import org.sonar.java.checks.CollectionsEmptyConstantsCheck;
import org.sonar.java.checks.CollectorsToListCheck;
import org.sonar.java.checks.CombineCatchCheck;
import org.sonar.java.checks.CommentRegularExpressionCheck;
import org.sonar.java.checks.CommentedOutCodeLineCheck;
import org.sonar.java.checks.CompareObjectWithEqualsCheck;
import org.sonar.java.checks.CompareStringsBoxedTypesWithEqualsCheck;
import org.sonar.java.checks.CompareToNotOverloadedCheck;
import org.sonar.java.checks.CompareToResultTestCheck;
import org.sonar.java.checks.CompareToReturnValueCheck;
import org.sonar.java.checks.ConcatenationWithStringValueOfCheck;
import org.sonar.java.checks.ConditionalOnNewLineCheck;
import org.sonar.java.checks.ConfigurationBeanNamesCheck;
import org.sonar.java.checks.ConfusingOverloadCheck;
import org.sonar.java.checks.ConfusingVarargCheck;
import org.sonar.java.checks.ConstantMathCheck;
import org.sonar.java.checks.ConstantMethodCheck;
import org.sonar.java.checks.ConstantsShouldBeStaticFinalCheck;
import org.sonar.java.checks.ConstructorCallingOverridableCheck;
import org.sonar.java.checks.ConstructorInjectionCheck;
import org.sonar.java.checks.ControlCharacterInLiteralCheck;
import org.sonar.java.checks.CounterModeIVShouldNotBeReusedCheck;
import org.sonar.java.checks.CustomCryptographicAlgorithmCheck;
import org.sonar.java.checks.DanglingElseStatementsCheck;
import org.sonar.java.checks.DateAndTimesCheck;
import org.sonar.java.checks.DateFormatWeekYearCheck;
import org.sonar.java.checks.DateTimeFormatterMismatchCheck;
import org.sonar.java.checks.DateUtilsTruncateCheck;
import org.sonar.java.checks.DeadStoreCheck;
import org.sonar.java.checks.DefaultEncodingUsageCheck;
import org.sonar.java.checks.DefaultInitializedFieldCheck;
import org.sonar.java.checks.DefaultPackageCheck;
import org.sonar.java.checks.DeprecatedArgumentsCheck;
import org.sonar.java.checks.DeprecatedTagPresenceCheck;
import org.sonar.java.checks.DepthOfInheritanceTreeCheck;
import org.sonar.java.checks.DiamondOperatorCheck;
import org.sonar.java.checks.DisallowedClassCheck;
import org.sonar.java.checks.DisallowedConstructorCheck;
import org.sonar.java.checks.DisallowedMethodCheck;
import org.sonar.java.checks.DisallowedThreadGroupCheck;
import org.sonar.java.checks.DoubleBraceInitializationCheck;
import org.sonar.java.checks.DoubleCheckedLockingAssignmentCheck;
import org.sonar.java.checks.DoublePrefixOperatorCheck;
import org.sonar.java.checks.DuplicateConditionIfElseIfCheck;
import org.sonar.java.checks.DynamicClassLoadCheck;
import org.sonar.java.checks.EmptyBlockCheck;
import org.sonar.java.checks.EmptyClassCheck;
import org.sonar.java.checks.EmptyFileCheck;
import org.sonar.java.checks.EmptyMethodsCheck;
import org.sonar.java.checks.EmptyStatementUsageCheck;
import org.sonar.java.checks.EnumEqualCheck;
import org.sonar.java.checks.EnumMapCheck;
import org.sonar.java.checks.EnumMutableFieldCheck;
import org.sonar.java.checks.EnumSetCheck;
import org.sonar.java.checks.EqualsArgumentTypeCheck;
import org.sonar.java.checks.EqualsNotOverriddenInSubclassCheck;
import org.sonar.java.checks.EqualsNotOverridenWithCompareToCheck;
import org.sonar.java.checks.EqualsOnAtomicClassCheck;
import org.sonar.java.checks.EqualsOverridenWithHashCodeCheck;
import org.sonar.java.checks.EqualsParametersMarkedNonNullCheck;
import org.sonar.java.checks.ErrorClassExtendedCheck;
import org.sonar.java.checks.EscapedUnicodeCharactersCheck;
import org.sonar.java.checks.ExceptionsShouldBeImmutableCheck;
import org.sonar.java.checks.ExpressionComplexityCheck;
import org.sonar.java.checks.FieldModifierCheck;
import org.sonar.java.checks.FileHeaderCheck;
import org.sonar.java.checks.FilesExistsJDK8Check;
import org.sonar.java.checks.FinalClassCheck;
import org.sonar.java.checks.FinalizeFieldsSetCheck;
import org.sonar.java.checks.FixmeTagPresenceCheck;
import org.sonar.java.checks.FloatEqualityCheck;
import org.sonar.java.checks.ForLoopCounterChangedCheck;
import org.sonar.java.checks.ForLoopFalseConditionCheck;
import org.sonar.java.checks.ForLoopIncrementAndUpdateCheck;
import org.sonar.java.checks.ForLoopIncrementSignCheck;
import org.sonar.java.checks.ForLoopTerminationConditionCheck;
import org.sonar.java.checks.ForLoopUsedAsWhileLoopCheck;
import org.sonar.java.checks.ForLoopVariableTypeCheck;
import org.sonar.java.checks.GarbageCollectorCalledCheck;
import org.sonar.java.checks.GetClassLoaderCheck;
import org.sonar.java.checks.GetRequestedSessionIdCheck;
import org.sonar.java.checks.GettersSettersOnRightFieldCheck;
import org.sonar.java.checks.HardCodedPasswordCheck;
import org.sonar.java.checks.HardCodedSecretCheck;
import org.sonar.java.checks.HardcodedIpCheck;
import org.sonar.java.checks.HardcodedURICheck;
import org.sonar.java.checks.HasNextCallingNextCheck;
import org.sonar.java.checks.HiddenFieldCheck;
import org.sonar.java.checks.IdenticalCasesInSwitchCheck;
import org.sonar.java.checks.IdenticalOperandOnBinaryExpressionCheck;
import org.sonar.java.checks.IfElseIfStatementEndsWithElseCheck;
import org.sonar.java.checks.IgnoredOperationStatusCheck;
import org.sonar.java.checks.IgnoredReturnValueCheck;
import org.sonar.java.checks.IgnoredStreamReturnValueCheck;
import org.sonar.java.checks.ImmediateReverseBoxingCheck;
import org.sonar.java.checks.ImmediatelyReturnedVariableCheck;
import org.sonar.java.checks.ImplementsEnumerationCheck;
import org.sonar.java.checks.InappropriateRegexpCheck;
import org.sonar.java.checks.IncorrectOrderOfMembersCheck;
import org.sonar.java.checks.IncrementDecrementInSubExpressionCheck;
import org.sonar.java.checks.IndentationAfterConditionalCheck;
import org.sonar.java.checks.IndentationCheck;
import org.sonar.java.checks.IndexOfWithPositiveNumberCheck;
import org.sonar.java.checks.InnerClassOfNonSerializableCheck;
import org.sonar.java.checks.InnerClassOfSerializableCheck;
import org.sonar.java.checks.InnerClassTooManyLinesCheck;
import org.sonar.java.checks.InnerStaticClassesCheck;
import org.sonar.java.checks.InputStreamOverrideReadCheck;
import org.sonar.java.checks.InputStreamReadCheck;
import org.sonar.java.checks.InsecureCreateTempFileCheck;
import org.sonar.java.checks.InstanceOfPatternMatchingCheck;
import org.sonar.java.checks.InstanceofUsedOnExceptionCheck;
import org.sonar.java.checks.InterfaceAsConstantContainerCheck;
import org.sonar.java.checks.InterfaceOrSuperclassShadowingCheck;
import org.sonar.java.checks.InterruptedExceptionCheck;
import org.sonar.java.checks.InvalidDateValuesCheck;
import org.sonar.java.checks.IsInstanceMethodCheck;
import org.sonar.java.checks.IterableIteratorCheck;
import org.sonar.java.checks.IteratorNextExceptionCheck;
import org.sonar.java.checks.JacksonDeserializationCheck;
import org.sonar.java.checks.JdbcDriverExplicitLoadingCheck;
import org.sonar.java.checks.JpaEagerFetchTypeCheck;
import org.sonar.java.checks.KeySetInsteadOfEntrySetCheck;
import org.sonar.java.checks.KnownCapacityHashBasedCollectionCheck;
import org.sonar.java.checks.LabelsShouldNotBeUsedCheck;
import org.sonar.java.checks.LambdaOptionalParenthesisCheck;
import org.sonar.java.checks.LambdaSingleExpressionCheck;
import org.sonar.java.checks.LambdaTooBigCheck;
import org.sonar.java.checks.LambdaTypeParameterCheck;
import org.sonar.java.checks.LazyArgEvaluationCheck;
import org.sonar.java.checks.LeastSpecificTypeCheck;
import org.sonar.java.checks.LeftCurlyBraceEndLineCheck;
import org.sonar.java.checks.LeftCurlyBraceStartLineCheck;
import org.sonar.java.checks.LoggedRethrownExceptionsCheck;
import org.sonar.java.checks.LoggerClassCheck;
import org.sonar.java.checks.LoggersDeclarationCheck;
import org.sonar.java.checks.LongBitsToDoubleOnIntCheck;
import org.sonar.java.checks.LoopExecutingAtMostOnceCheck;
import org.sonar.java.checks.LoopsOnSameSetCheck;
import org.sonar.java.checks.MagicNumberCheck;
import org.sonar.java.checks.MainMethodThrowsExceptionCheck;
import org.sonar.java.checks.MapKeyNotComparableCheck;
import org.sonar.java.checks.MathOnFloatCheck;
import org.sonar.java.checks.MembersDifferOnlyByCapitalizationCheck;
import org.sonar.java.checks.MethodComplexityCheck;
import org.sonar.java.checks.MethodIdenticalImplementationsCheck;
import org.sonar.java.checks.MethodOnlyCallsSuperCheck;
import org.sonar.java.checks.MethodParametersOrderCheck;
import org.sonar.java.checks.MethodTooBigCheck;
import org.sonar.java.checks.MethodWithExcessiveReturnsCheck;
import org.sonar.java.checks.MismatchPackageDirectoryCheck;
import org.sonar.java.checks.MissingBeanValidationCheck;
import org.sonar.java.checks.MissingCurlyBracesCheck;
import org.sonar.java.checks.MissingDeprecatedCheck;
import org.sonar.java.checks.MissingNewLineAtEndOfFileCheck;
import org.sonar.java.checks.MissingOverridesInRecordWithArrayComponentCheck;
import org.sonar.java.checks.MissingPackageInfoCheck;
import org.sonar.java.checks.MissingPathVariableAnnotationCheck;
import org.sonar.java.checks.ModifiersOrderCheck;
import org.sonar.java.checks.ModulusEqualityCheck;
import org.sonar.java.checks.MultilineBlocksCurlyBracesCheck;
import org.sonar.java.checks.MutableMembersUsageCheck;
import org.sonar.java.checks.NPEThrowCheck;
import org.sonar.java.checks.NestedBlocksCheck;
import org.sonar.java.checks.NestedEnumStaticCheck;
import org.sonar.java.checks.NestedIfStatementsCheck;
import org.sonar.java.checks.NestedSwitchCheck;
import org.sonar.java.checks.NestedTernaryOperatorsCheck;
import org.sonar.java.checks.NestedTryCatchCheck;
import org.sonar.java.checks.NioFileDeleteCheck;
import org.sonar.java.checks.NoCheckstyleTagPresenceCheck;
import org.sonar.java.checks.NoPmdTagPresenceCheck;
import org.sonar.java.checks.NoSonarCheck;
import org.sonar.java.checks.NonShortCircuitLogicCheck;
import org.sonar.java.checks.NonStaticClassInitializerCheck;
import org.sonar.java.checks.NotifyCheck;
import org.sonar.java.checks.NullCheckWithInstanceofCheck;
import org.sonar.java.checks.NullReturnedOnComputeIfPresentOrAbsentCheck;
import org.sonar.java.checks.NullShouldNotBeUsedWithOptionalCheck;
import org.sonar.java.checks.OSCommandsPathCheck;
import org.sonar.java.checks.ObjectCreatedOnlyToCallGetClassCheck;
import org.sonar.java.checks.ObjectFinalizeCheck;
import org.sonar.java.checks.ObjectFinalizeOverloadedCheck;
import org.sonar.java.checks.ObjectFinalizeOverridenCallsSuperFinalizeCheck;
import org.sonar.java.checks.ObjectFinalizeOverridenCheck;
import org.sonar.java.checks.ObjectFinalizeOverridenNotPublicCheck;
import org.sonar.java.checks.OctalValuesCheck;
import org.sonar.java.checks.OmitPermittedTypesCheck;
import org.sonar.java.checks.OneClassInterfacePerFileCheck;
import org.sonar.java.checks.OneDeclarationPerLineCheck;
import org.sonar.java.checks.OperatorPrecedenceCheck;
import org.sonar.java.checks.OptionalAsParameterCheck;
import org.sonar.java.checks.OutputStreamOverrideWriteCheck;
import org.sonar.java.checks.OverrideAnnotationCheck;
import org.sonar.java.checks.OverwrittenKeyCheck;
import org.sonar.java.checks.ParameterReassignedToCheck;
import org.sonar.java.checks.ParsingErrorCheck;
import org.sonar.java.checks.PopulateBeansCheck;
import org.sonar.java.checks.PredictableSeedCheck;
import org.sonar.java.checks.PreferStreamAnyMatchCheck;
import org.sonar.java.checks.PreparedStatementAndResultSetCheck;
import org.sonar.java.checks.PrimitiveTypeBoxingWithToStringCheck;
import org.sonar.java.checks.PrimitiveWrappersInTernaryOperatorCheck;
import org.sonar.java.checks.PrimitivesMarkedNullableCheck;
import org.sonar.java.checks.PrintfFailCheck;
import org.sonar.java.checks.PrintfMisuseCheck;
import org.sonar.java.checks.PrivateFieldUsedLocallyCheck;
import org.sonar.java.checks.ProtectedMemberInFinalClassCheck;
import org.sonar.java.checks.PseudoRandomCheck;
import org.sonar.java.checks.PublicConstructorInAbstractClassCheck;
import org.sonar.java.checks.PublicStaticFieldShouldBeFinalCheck;
import org.sonar.java.checks.PublicStaticMutableMembersCheck;
import org.sonar.java.checks.QueryOnlyRequiredFieldsCheck;
import org.sonar.java.checks.RandomFloatToIntCheck;
import org.sonar.java.checks.RawByteBitwiseOperationsCheck;
import org.sonar.java.checks.RawExceptionCheck;
import org.sonar.java.checks.RawTypeCheck;
import org.sonar.java.checks.ReadObjectSynchronizedCheck;
import org.sonar.java.checks.RecordDuplicatedGetterCheck;
import org.sonar.java.checks.RecordInsteadOfClassCheck;
import org.sonar.java.checks.RedundantAbstractMethodCheck;
import org.sonar.java.checks.RedundantCloseCheck;
import org.sonar.java.checks.RedundantJumpCheck;
import org.sonar.java.checks.RedundantModifierCheck;
import org.sonar.java.checks.RedundantRecordMethodsCheck;
import org.sonar.java.checks.RedundantStreamCollectCheck;
import org.sonar.java.checks.RedundantThrowsDeclarationCheck;
import org.sonar.java.checks.RedundantTypeCastCheck;
import org.sonar.java.checks.ReflectionOnNonRuntimeAnnotationCheck;
import org.sonar.java.checks.RegexPatternsNeedlesslyCheck;
import org.sonar.java.checks.ReleaseSensorsCheck;
import org.sonar.java.checks.RepeatAnnotationCheck;
import org.sonar.java.checks.ReplaceGuavaWithJavaCheck;
import org.sonar.java.checks.ReplaceLambdaByMethodRefCheck;
import org.sonar.java.checks.RestrictedIdentifiersUsageCheck;
import org.sonar.java.checks.ResultSetIsLastCheck;
import org.sonar.java.checks.ReturnEmptyArrayNotNullCheck;
import org.sonar.java.checks.ReturnInFinallyCheck;
import org.sonar.java.checks.ReturnOfBooleanExpressionsCheck;
import org.sonar.java.checks.ReuseRandomCheck;
import org.sonar.java.checks.RightCurlyBraceDifferentLineAsNextBlockCheck;
import org.sonar.java.checks.RightCurlyBraceSameLineAsNextBlockCheck;
import org.sonar.java.checks.RightCurlyBraceStartLineCheck;
import org.sonar.java.checks.RunFinalizersCheck;
import org.sonar.java.checks.SQLInjectionCheck;
import org.sonar.java.checks.ScheduledThreadPoolExecutorZeroCheck;
import org.sonar.java.checks.SelectorMethodArgumentCheck;
import org.sonar.java.checks.SelfAssignementCheck;
import org.sonar.java.checks.ServletInstanceFieldCheck;
import org.sonar.java.checks.ServletMethodsExceptionsThrownCheck;
import org.sonar.java.checks.SeveralBreakOrContinuePerLoopCheck;
import org.sonar.java.checks.ShiftOnIntOrLongCheck;
import org.sonar.java.checks.SillyEqualsCheck;
import org.sonar.java.checks.SillyStringOperationsCheck;
import org.sonar.java.checks.SimpleClassNameCheck;
import org.sonar.java.checks.SimpleStringLiteralForSingleLineStringsCheck;
import org.sonar.java.checks.SpecializedFunctionalInterfacesCheck;
import org.sonar.java.checks.StandardCharsetsConstantsCheck;
import org.sonar.java.checks.StandardFunctionalInterfaceCheck;
import org.sonar.java.checks.StaticFieldInitializationCheck;
import org.sonar.java.checks.StaticFieldUpateCheck;
import org.sonar.java.checks.StaticFieldUpdateInConstructorCheck;
import org.sonar.java.checks.StaticImportCountCheck;
import org.sonar.java.checks.StaticMemberAccessCheck;
import org.sonar.java.checks.StaticMembersAccessCheck;
import org.sonar.java.checks.StaticMethodCheck;
import org.sonar.java.checks.StaticMultithreadedUnsafeFieldsCheck;
import org.sonar.java.checks.StreamPeekCheck;
import org.sonar.java.checks.StringBufferAndBuilderWithCharCheck;
import org.sonar.java.checks.StringCallsBeyondBoundsCheck;
import org.sonar.java.checks.StringConcatToTextBlockCheck;
import org.sonar.java.checks.StringConcatenationInLoopCheck;
import org.sonar.java.checks.StringLiteralDuplicatedCheck;
import org.sonar.java.checks.StringLiteralInsideEqualsCheck;
import org.sonar.java.checks.StringMethodsWithLocaleCheck;
import org.sonar.java.checks.StringOffsetMethodsCheck;
import org.sonar.java.checks.StringPrimitiveConstructorCheck;
import org.sonar.java.checks.StringToPrimitiveConversionCheck;
import org.sonar.java.checks.StringToStringCheck;
import org.sonar.java.checks.StrongCipherAlgorithmCheck;
import org.sonar.java.checks.SubClassStaticReferenceCheck;
import org.sonar.java.checks.SunPackagesUsedCheck;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.checks.SuspiciousListRemoveCheck;
import org.sonar.java.checks.SwitchAtLeastThreeCasesCheck;
import org.sonar.java.checks.SwitchCaseTooBigCheck;
import org.sonar.java.checks.SwitchCaseWithoutBreakCheck;
import org.sonar.java.checks.SwitchCasesShouldBeCommaSeparatedCheck;
import org.sonar.java.checks.SwitchDefaultLastCaseCheck;
import org.sonar.java.checks.SwitchInsteadOfIfSequenceCheck;
import org.sonar.java.checks.SwitchLastCaseIsDefaultCheck;
import org.sonar.java.checks.SwitchRedundantKeywordCheck;
import org.sonar.java.checks.SwitchWithLabelsCheck;
import org.sonar.java.checks.SwitchWithTooManyCasesCheck;
import org.sonar.java.checks.SymmetricEqualsCheck;
import org.sonar.java.checks.SyncGetterAndSetterCheck;
import org.sonar.java.checks.SynchronizationOnStringOrBoxedCheck;
import org.sonar.java.checks.SynchronizedClassUsageCheck;
import org.sonar.java.checks.SynchronizedFieldAssignmentCheck;
import org.sonar.java.checks.SynchronizedLockCheck;
import org.sonar.java.checks.SynchronizedOverrideCheck;
import org.sonar.java.checks.SystemExitCalledCheck;
import org.sonar.java.checks.SystemOutOrErrUsageCheck;
import org.sonar.java.checks.TabCharacterCheck;
import org.sonar.java.checks.TernaryOperatorCheck;
import org.sonar.java.checks.TestsInSeparateFolderCheck;
import org.sonar.java.checks.TextBlockTabsAndSpacesCheck;
import org.sonar.java.checks.TextBlocksInComplexExpressionsCheck;
import org.sonar.java.checks.ThisExposedFromConstructorCheck;
import org.sonar.java.checks.ThreadAsRunnableArgumentCheck;
import org.sonar.java.checks.ThreadLocalCleanupCheck;
import org.sonar.java.checks.ThreadLocalWithInitialCheck;
import org.sonar.java.checks.ThreadOverridesRunCheck;
import org.sonar.java.checks.ThreadRunCheck;
import org.sonar.java.checks.ThreadSleepCheck;
import org.sonar.java.checks.ThreadStartedInConstructorCheck;
import org.sonar.java.checks.ThreadWaitCallCheck;
import org.sonar.java.checks.ThrowCheckedExceptionCheck;
import org.sonar.java.checks.ThrowsFromFinallyCheck;
import org.sonar.java.checks.ThrowsSeveralCheckedExceptionCheck;
import org.sonar.java.checks.ToArrayCheck;
import org.sonar.java.checks.ToStringReturningNullCheck;
import org.sonar.java.checks.ToStringUsingBoxingCheck;
import org.sonar.java.checks.TodoTagPresenceCheck;
import org.sonar.java.checks.TooLongLineCheck;
import org.sonar.java.checks.TooManyLinesOfCodeInFileCheck;
import org.sonar.java.checks.TooManyMethodsCheck;
import org.sonar.java.checks.TooManyParametersCheck;
import org.sonar.java.checks.TooManyStatementsPerLineCheck;
import org.sonar.java.checks.TrailingCommentCheck;
import org.sonar.java.checks.TransientFieldInNonSerializableCheck;
import org.sonar.java.checks.TryWithResourcesCheck;
import org.sonar.java.checks.TypeParametersShadowingCheck;
import org.sonar.java.checks.TypeUpperBoundNotFinalCheck;
import org.sonar.java.checks.URLHashCodeAndEqualsCheck;
import org.sonar.java.checks.UnderscoreMisplacedOnNumberCheck;
import org.sonar.java.checks.UnderscoreOnNumberCheck;
import org.sonar.java.checks.UndocumentedApiCheck;
import org.sonar.java.checks.UnnecessaryBitOperationCheck;
import org.sonar.java.checks.UnnecessaryEscapeSequencesInTextBlockCheck;
import org.sonar.java.checks.UnnecessarySemicolonCheck;
import org.sonar.java.checks.UnreachableCatchCheck;
import org.sonar.java.checks.UppercaseSuffixesCheck;
import org.sonar.java.checks.UseSwitchExpressionCheck;
import org.sonar.java.checks.UselessExtendsCheck;
import org.sonar.java.checks.UselessImportCheck;
import org.sonar.java.checks.UselessIncrementCheck;
import org.sonar.java.checks.UselessPackageInfoCheck;
import org.sonar.java.checks.UselessParenthesesCheck;
import org.sonar.java.checks.UtilityClassWithPublicConstructorCheck;
import org.sonar.java.checks.ValueBasedObjectsShouldNotBeSerializedCheck;
import org.sonar.java.checks.VarArgCheck;
import org.sonar.java.checks.VarCanBeUsedCheck;
import org.sonar.java.checks.VariableDeclarationScopeCheck;
import org.sonar.java.checks.VisibleForTestingUsageCheck;
import org.sonar.java.checks.VolatileNonPrimitiveFieldCheck;
import org.sonar.java.checks.VolatileVariablesOperationsCheck;
import org.sonar.java.checks.WaitInSynchronizeCheck;
import org.sonar.java.checks.WaitInWhileLoopCheck;
import org.sonar.java.checks.WaitOnConditionCheck;
import org.sonar.java.checks.WeakSSLContextCheck;
import org.sonar.java.checks.WildcardImportsShouldNotBeUsedCheck;
import org.sonar.java.checks.WildcardReturnParameterTypeCheck;
import org.sonar.java.checks.WrongAssignmentOperatorCheck;
import org.sonar.java.checks.aws.AwsConsumerBuilderUsageCheck;
import org.sonar.java.checks.aws.AwsCredentialsShouldBeSetExplicitlyCheck;
import org.sonar.java.checks.aws.AwsLambdaSyncCallCheck;
import org.sonar.java.checks.aws.AwsLongTermAccessKeysCheck;
import org.sonar.java.checks.aws.AwsRegionSetterCheck;
import org.sonar.java.checks.aws.AwsRegionShouldBeSetExplicitlyCheck;
import org.sonar.java.checks.aws.AwsReusableResourcesInitializedOnceCheck;
import org.sonar.java.checks.design.BrainMethodCheck;
import org.sonar.java.checks.design.ClassCouplingCheck;
import org.sonar.java.checks.design.ClassImportCouplingCheck;
import org.sonar.java.checks.design.SingletonUsageCheck;
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
import org.sonar.java.checks.regex.EmptyRegexGroupCheck;
import org.sonar.java.checks.regex.EmptyStringRepetitionCheck;
import org.sonar.java.checks.regex.EscapeSequenceControlCharacterCheck;
import org.sonar.java.checks.regex.GraphemeClustersInClassesCheck;
import org.sonar.java.checks.regex.ImpossibleBackReferenceCheck;
import org.sonar.java.checks.regex.ImpossibleBoundariesCheck;
import org.sonar.java.checks.regex.InvalidRegexCheck;
import org.sonar.java.checks.regex.MultipleWhitespaceCheck;
import org.sonar.java.checks.regex.PossessiveQuantifierContinuationCheck;
import org.sonar.java.checks.regex.RedosCheck;
import org.sonar.java.checks.regex.RedundantRegexAlternativesCheck;
import org.sonar.java.checks.regex.RegexComplexityCheck;
import org.sonar.java.checks.regex.RegexLookaheadCheck;
import org.sonar.java.checks.regex.RegexStackOverflowCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierWithEmptyContinuationCheck;
import org.sonar.java.checks.regex.SingleCharCharacterClassCheck;
import org.sonar.java.checks.regex.SingleCharacterAlternationCheck;
import org.sonar.java.checks.regex.StringReplaceCheck;
import org.sonar.java.checks.regex.SuperfluousCurlyBraceCheck;
import org.sonar.java.checks.regex.UnicodeAwareCharClassesCheck;
import org.sonar.java.checks.regex.UnicodeCaseCheck;
import org.sonar.java.checks.regex.UnquantifiedNonCapturingGroupCheck;
import org.sonar.java.checks.regex.UnusedGroupNamesCheck;
import org.sonar.java.checks.regex.VerboseRegexCheck;
import org.sonar.java.checks.security.AndroidBiometricAuthWithoutCryptoCheck;
import org.sonar.java.checks.security.AndroidBroadcastingCheck;
import org.sonar.java.checks.security.AndroidExternalStorageCheck;
import org.sonar.java.checks.security.AndroidMobileDatabaseEncryptionKeysCheck;
import org.sonar.java.checks.security.AndroidNonAuthenticatedUsersCheck;
import org.sonar.java.checks.security.AndroidUnencryptedDatabaseCheck;
import org.sonar.java.checks.security.AndroidUnencryptedFilesCheck;
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
import org.sonar.java.checks.security.HardCodedCredentialsShouldNotBeUsedCheck;
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
import org.sonar.java.checks.security.WebViewJavaScriptSupportCheck;
import org.sonar.java.checks.security.WebViewsFileAccessCheck;
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
import org.sonar.java.checks.spring.AsyncMethodsCalledViaThisCheck;
import org.sonar.java.checks.spring.AsyncMethodsOnConfigurationClassCheck;
import org.sonar.java.checks.spring.AsyncMethodsReturnTypeCheck;
import org.sonar.java.checks.spring.AutowiredOnConstructorWhenMultipleConstructorsCheck;
import org.sonar.java.checks.spring.AutowiredOnMultipleConstructorsCheck;
import org.sonar.java.checks.spring.AvoidQualifierOnBeanMethodsCheck;
import org.sonar.java.checks.spring.ControllerWithRestControllerReplacementCheck;
import org.sonar.java.checks.spring.ControllerWithSessionAttributesCheck;
import org.sonar.java.checks.spring.DirectBeanMethodInvocationWithoutProxyCheck;
import org.sonar.java.checks.spring.FieldDependencyInjectionCheck;
import org.sonar.java.checks.spring.ModelAttributeNamingConventionForSpELCheck;
import org.sonar.java.checks.spring.NonSingletonAutowiredInSingletonCheck;
import org.sonar.java.checks.spring.NullableInjectedFieldsHaveDefaultValueCheck;
import org.sonar.java.checks.spring.OptionalRestParametersShouldBeObjectsCheck;
import org.sonar.java.checks.spring.PersistentEntityUsedAsRequestParameterCheck;
import org.sonar.java.checks.spring.RequestMappingMethodPublicCheck;
import org.sonar.java.checks.spring.SpelExpressionCheck;
import org.sonar.java.checks.spring.SpringAntMatcherOrderCheck;
import org.sonar.java.checks.spring.SpringAutoConfigurationCheck;
import org.sonar.java.checks.spring.SpringBeanNamingConventionCheck;
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
import org.sonar.java.checks.spring.StatusCodesOnResponseCheck;
import org.sonar.java.checks.spring.SuperfluousResponseBodyAnnotationCheck;
import org.sonar.java.checks.spring.TransactionalMethodVisibilityCheck;
import org.sonar.java.checks.spring.ValueAnnotationShouldInjectPropertyOrSpELCheck;
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
import org.sonar.java.se.checks.AllowXMLInclusionCheck;
import org.sonar.java.se.checks.BooleanGratuitousExpressionsCheck;
import org.sonar.java.se.checks.ConditionalUnreachableCodeCheck;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.DenialOfServiceXMLCheck;
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
import org.sonar.java.se.checks.XmlParserLoadsExternalSchemasCheck;
import org.sonar.java.se.checks.XmlValidatedSignatureCheck;
import org.sonar.java.se.checks.XxeProcessingCheck;
import org.sonar.plugins.java.api.JavaCheck;

public final class CheckList {

  public static final String REPOSITORY_KEY = "java";

  private static final List<Class<? extends JavaCheck>> JAVA_MAIN_CHECKS = Arrays.asList(

    // fast JavaFileScanner (not IssuableSubscriptionVisitor) ordered from the fastest to the slowest
    LeftCurlyBraceEndLineCheck.class,
    IndentationCheck.class,
    IncorrectOrderOfMembersCheck.class,
    MagicNumberCheck.class,
    NestedIfStatementsCheck.class,
    BadAbstractClassNameCheck.class,
    RawExceptionCheck.class,
    MissingPackageInfoCheck.class,
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
    AndroidUnencryptedFilesCheck.class,
    AndroidExternalStorageCheck.class,
    AndroidMobileDatabaseEncryptionKeysCheck.class,
    AndroidNonAuthenticatedUsersCheck.class,
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
    AsyncMethodsCalledViaThisCheck.class,
    AsyncMethodsOnConfigurationClassCheck.class,
    AsyncMethodsReturnTypeCheck.class,
    AtLeastOneConstructorCheck.class,
    AuthorizationsStrongDecisionsCheck.class,
    AutowiredOnConstructorWhenMultipleConstructorsCheck.class,
    AutowiredOnMultipleConstructorsCheck.class,
    AvoidQualifierOnBeanMethodsCheck.class,
    AwsConsumerBuilderUsageCheck.class,
    AwsCredentialsShouldBeSetExplicitlyCheck.class,
    AwsLambdaSyncCallCheck.class,
    AwsLongTermAccessKeysCheck.class,
    AwsRegionSetterCheck.class,
    AwsRegionShouldBeSetExplicitlyCheck.class,
    AwsReusableResourcesInitializedOnceCheck.class,
    BadConstantNameCheck.class,
    BadFieldNameCheck.class,
    BadFieldNameStaticNonFinalCheck.class,
    BadLocalConstantNameCheck.class,
    BadMethodNameCheck.class,
    BadTypeParameterNameCheck.class,
    BasicAuthCheck.class,
    BigDecimalDoubleConstructorCheck.class,
    AndroidBiometricAuthWithoutCryptoCheck.class,
    BlindSerialVersionUidCheck.class,
    BluetoothLowPowerModeCheck.class,
    BooleanInversionCheck.class,
    BooleanLiteralCheck.class,
    BooleanMethodNameCheck.class,
    BooleanMethodReturnCheck.class,
    BrainMethodCheck.class,
    ConfigurationBeanNamesCheck.class,
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
    CounterModeIVShouldNotBeReusedCheck.class,
    ChildClassShadowFieldCheck.class,
    CipherBlockChainingCheck.class,
    ClassComparedByNameCheck.class,
    ClassImportCouplingCheck.class,
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
    CollectionConstructorReferenceCheck.class,
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
    ControllerWithRestControllerReplacementCheck.class,
    ControllerWithSessionAttributesCheck.class,
    CookieHttpOnlyCheck.class,
    HardCodedCredentialsShouldNotBeUsedCheck.class,
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
    DeprecatedArgumentsCheck.class,
    DefaultEncodingUsageCheck.class,
    DefaultInitializedFieldCheck.class,
    DeprecatedTagPresenceCheck.class,
    DiamondOperatorCheck.class,
    DirectBeanMethodInvocationWithoutProxyCheck.class,
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
    EmptyRegexGroupCheck.class,
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
    FieldDependencyInjectionCheck.class,
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
    HardCodedPasswordCheck.class,
    HardCodedSecretCheck.class,
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
    JacksonDeserializationCheck.class,
    JdbcDriverExplicitLoadingCheck.class,
    JpaEagerFetchTypeCheck.class,
    JWTWithStrongCipherCheck.class,
    KeySetInsteadOfEntrySetCheck.class,
    KnownCapacityHashBasedCollectionCheck.class,
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
    MapKeyNotComparableCheck.class,
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
    ModelAttributeNamingConventionForSpELCheck.class,
    ModifiersOrderCheck.class,
    ModulusEqualityCheck.class,
    MultipleWhitespaceCheck.class,
    NPEThrowCheck.class,
    NestedEnumStaticCheck.class,
    NestedSwitchCheck.class,
    NestedTernaryOperatorsCheck.class,
    NioFileDeleteCheck.class,
    NoCheckstyleTagPresenceCheck.class,
    NonSingletonAutowiredInSingletonCheck.class,
    NoPmdTagPresenceCheck.class,
    NoSonarCheck.class,
    NonSerializableWriteCheck.class,
    NonShortCircuitLogicCheck.class,
    NonStaticClassInitializerCheck.class,
    NotifyCheck.class,
    NullableInjectedFieldsHaveDefaultValueCheck.class,
    NullCheckWithInstanceofCheck.class,
    NullReturnedOnComputeIfPresentOrAbsentCheck.class,
    OSCommandsPathCheck.class,
    ObjectCreatedOnlyToCallGetClassCheck.class,
    ObjectFinalizeCheck.class,
    ObjectFinalizeOverloadedCheck.class,
    ObjectFinalizeOverridenCallsSuperFinalizeCheck.class,
    ObjectFinalizeOverridenCheck.class,
    ObjectFinalizeOverridenNotPublicCheck.class,
    OmitPermittedTypesCheck.class,
    OneClassInterfacePerFileCheck.class,
    OneDeclarationPerLineCheck.class,
    OpenSAML2AuthenticationBypassCheck.class,
    OptionalAsParameterCheck.class,
    OptionalRestParametersShouldBeObjectsCheck.class,
    OutputStreamOverrideWriteCheck.class,
    OverrideAnnotationCheck.class,
    OverwrittenKeyCheck.class,
    PasswordEncoderCheck.class,
    MissingPathVariableAnnotationCheck.class,
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
    QueryOnlyRequiredFieldsCheck.class,
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
    ReleaseSensorsCheck.class,
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
    StatusCodesOnResponseCheck.class,
    UnnecessaryBitOperationCheck.class,
    SillyEqualsCheck.class,
    SillyStringOperationsCheck.class,
    SimpleClassNameCheck.class,
    SimpleStringLiteralForSingleLineStringsCheck.class,
    SingleCharacterAlternationCheck.class,
    SingleCharCharacterClassCheck.class,
    SingletonUsageCheck.class,
    SpecializedFunctionalInterfacesCheck.class,
    SpelExpressionCheck.class,
    SpringAntMatcherOrderCheck.class,
    SpringAutoConfigurationCheck.class,
    SpringBeanNamingConventionCheck.class,
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
    SuperfluousCurlyBraceCheck.class,
    SuperfluousResponseBodyAnnotationCheck.class,
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
    TooManyParametersCheck.class,
    TooManyStatementsPerLineCheck.class,
    TrailingCommentCheck.class,
    TransactionalMethodVisibilityCheck.class,
    TransientFieldInNonSerializableCheck.class,
    TryWithResourcesCheck.class,
    TwoLocksWaitCheck.class,
    TypeUpperBoundNotFinalCheck.class,
    URLHashCodeAndEqualsCheck.class,
    UnderscoreMisplacedOnNumberCheck.class,
    UnderscoreOnNumberCheck.class,
    AndroidUnencryptedDatabaseCheck.class,
    UnicodeAwareCharClassesCheck.class,
    UnicodeCaseCheck.class,
    UnnecessaryEscapeSequencesInTextBlockCheck.class,
    UnnecessarySemicolonCheck.class,
    UnpredictableSaltCheck.class,
    UnquantifiedNonCapturingGroupCheck.class,
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
    ValueAnnotationShouldInjectPropertyOrSpELCheck.class,
    ValueBasedObjectUsedForLockCheck.class,
    ValueBasedObjectsShouldNotBeSerializedCheck.class,
    VarArgCheck.class,
    VarCanBeUsedCheck.class,
    VariableDeclarationScopeCheck.class,
    VerboseRegexCheck.class,
    VerifiedServerHostnamesCheck.class,
    VisibleForTestingUsageCheck.class,
    VolatileNonPrimitiveFieldCheck.class,
    VolatileVariablesOperationsCheck.class,
    WaitInSynchronizeCheck.class,
    WaitInWhileLoopCheck.class,
    WaitOnConditionCheck.class,
    WeakSSLContextCheck.class,
    WebViewJavaScriptSupportCheck.class,
    WebViewsFileAccessCheck.class,
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
    XxeProcessingCheck.class,
    // SEChecks Depending on XxeProcessingCheck
    DenialOfServiceXMLCheck.class,
    AllowXMLInclusionCheck.class,
    XmlParserLoadsExternalSchemasCheck.class,

    // SEChecks not require by ExplodedGraphWalker, from the fastest to the slowest
    ParameterNullnessCheck.class,
    BooleanGratuitousExpressionsCheck.class,
    ConditionalUnreachableCodeCheck.class,
    XmlValidatedSignatureCheck.class,
    CustomUnclosedResourcesCheck.class,
    MapComputeIfAbsentOrPresentCheck.class,
    InvariantReturnCheck.class,
    StreamNotConsumedCheck.class,
    ObjectOutputStreamCheck.class,
    MinMaxRangeCheck.class);

  private static final List<Class<? extends JavaCheck>> JAVA_TEST_CHECKS = Arrays.asList(
    // Rule classes are listed alphabetically
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

  private static final List<Class<?>> ALL_CHECKS = Stream.of(JAVA_MAIN_CHECKS, JAVA_TEST_CHECKS)
    .flatMap(List::stream).collect(Collectors.toList());

  private static final Set<Class<? extends JavaCheck>> JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN = Set.of(
    // Symbolic executions rules are not in this list because they are dynamically excluded
    // Rules relying on correct setup of jdk.home
    CallToDeprecatedCodeMarkedForRemovalCheck.class,
    CallToDeprecatedMethodCheck.class,
    // Rules relying on correct setup of java version
    AbstractClassNoFieldShouldBeInterfaceCheck.class,
    AnonymousClassShouldBeLambdaCheck.class,
    CombineCatchCheck.class,
    DateAndTimesCheck.class,
    DateUtilsTruncateCheck.class,
    DiamondOperatorCheck.class,
    InsecureCreateTempFileCheck.class,
    JdbcDriverExplicitLoadingCheck.class,
    LambdaOptionalParenthesisCheck.class,
    LambdaSingleExpressionCheck.class,
    RepeatAnnotationCheck.class,
    ReplaceGuavaWithJavaCheck.class,
    ReplaceLambdaByMethodRefCheck.class,
    SwitchInsteadOfIfSequenceCheck.class,
    ThreadLocalWithInitialCheck.class,
    TryWithResourcesCheck.class,
    ValueBasedObjectUsedForLockCheck.class,
    // Rules with a high deviation (>3%)
    AccessibilityChangeCheck.class,
    CipherBlockChainingCheck.class,
    ClassNamedLikeExceptionCheck.class,
    ClassWithOnlyStaticMethodsInstantiationCheck.class,
    CollectionInappropriateCallsCheck.class,
    DeadStoreCheck.class,
    EqualsArgumentTypeCheck.class,
    EqualsNotOverridenWithCompareToCheck.class,
    EqualsOverridenWithHashCodeCheck.class,
    ForLoopVariableTypeCheck.class,
    JWTWithStrongCipherCheck.class,
    MethodNamedEqualsCheck.class,
    NioFileDeleteCheck.class,
    PrivateFieldUsedLocallyCheck.class,
    SillyEqualsCheck.class,
    StandardCharsetsConstantsCheck.class,
    ThreadLocalCleanupCheck.class,
    ThreadOverridesRunCheck.class,
    UnusedPrivateClassCheck.class,
    UnusedPrivateFieldCheck.class,
    VerifiedServerHostnamesCheck.class,
    VolatileNonPrimitiveFieldCheck.class,
    WeakSSLContextCheck.class);

  private CheckList() {
  }

  public static List<Class<?>> getChecks() {
    return ALL_CHECKS;
  }

  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return JAVA_MAIN_CHECKS;
  }

  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return JAVA_TEST_CHECKS;
  }

  public static Set<Class<? extends JavaCheck>> getJavaChecksNotWorkingForAutoScan() {
    return JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN;
  }

}
