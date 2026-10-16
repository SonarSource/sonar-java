package org.sonar.java.javac.resources;

import org.sonar.java.javac.code.Attribute.Compound;
import org.sonar.java.javac.code.FlagsEnum;
import org.sonar.java.javac.code.Kinds.Kind;
import org.sonar.java.javac.code.Kinds.KindName;
import org.sonar.java.javac.code.Source;
import org.sonar.java.javac.code.Symbol;
import org.sonar.java.javac.code.Type;
import org.sonar.java.javac.jvm.Profile;
import org.sonar.java.javac.jvm.Target;
import org.sonar.java.javac.main.Option;
import org.sonar.java.javac.parser.Tokens.TokenKind;
import org.sonar.java.javac.util.JCDiagnostic;
import org.sonar.java.javac.util.JCDiagnostic.AnnotatedType;
import org.sonar.java.javac.util.Name;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import org.sonar.java.javac.util.JCDiagnostic.DiagnosticFlag;
import org.sonar.java.javac.util.JCDiagnostic.Error;
import org.sonar.java.javac.util.JCDiagnostic.Warning;
import org.sonar.java.javac.util.JCDiagnostic.LintWarning;
import org.sonar.java.javac.util.JCDiagnostic.Note;
import org.sonar.java.javac.util.JCDiagnostic.Fragment;
import org.sonar.java.javac.code.Lint.LintCategory;

import java.util.EnumSet;

import static org.sonar.java.javac.util.JCDiagnostic.DiagnosticFlag.*;

public class CompilerProperties {
    public static class Errors {
        /**
         * compiler.err.abstract.cant.be.accessed.directly=\
         *    abstract {0} {1} in {2} cannot be accessed directly
         */
        public static Error AbstractCantBeAccessedDirectly(KindName arg0, Symbol arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "abstract.cant.be.accessed.directly", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.abstract.cant.be.instantiated=\
         *    {0} is abstract; cannot be instantiated
         */
        public static Error AbstractCantBeInstantiated(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "abstract.cant.be.instantiated", arg0);
        }
        
        /**
         * "abstract.meth.cant.have.body"
         */
        public static final Error AbstractMethCantHaveBody = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "abstract.meth.cant.have.body");
        
        /**
         * compiler.err.add.exports.with.release=\
         *    exporting a package from system module {0} is not allowed with --release
         */
        public static Error AddExportsWithRelease(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "add.exports.with.release", arg0);
        }
        
        /**
         * compiler.err.add.reads.with.release=\
         *    adding read edges for system module {0} is not allowed with --release
         */
        public static Error AddReadsWithRelease(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "add.reads.with.release", arg0);
        }
        
        /**
         * "addmods.all.module.path.invalid"
         */
        public static final Error AddmodsAllModulePathInvalid = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "addmods.all.module.path.invalid");
        
        /**
         * compiler.err.already.annotated=\
         *    {0} {1} has already been annotated
         */
        public static Error AlreadyAnnotated(KindName arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.annotated", arg0, arg1);
        }
        
        /**
         * compiler.err.already.defined=\
         *    {0} {1} is already defined in {2} {3}
         */
        public static Error AlreadyDefined(KindName arg0, Symbol arg1, KindName arg2, Symbol arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.defined", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.already.defined.in.clinit=\
         *    {0} {1} is already defined in {2} of {3} {4}
         */
        public static Error AlreadyDefinedInClinit(KindName arg0, Symbol arg1, KindName arg2, KindName arg3, Symbol arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.defined.in.clinit", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.already.defined.single.import=\
         *    a type with the same simple name is already defined by the single-type-import of {0}
         */
        public static Error AlreadyDefinedSingleImport(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.defined.single.import", arg0);
        }
        
        /**
         * compiler.err.already.defined.static.single.import=\
         *    a type with the same simple name is already defined by the static single-type-import of {0}
         */
        public static Error AlreadyDefinedStaticSingleImport(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.defined.static.single.import", arg0);
        }
        
        /**
         * compiler.err.already.defined.this.unit=\
         *    {0} is already defined in this compilation unit
         */
        public static Error AlreadyDefinedThisUnit(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "already.defined.this.unit", arg0);
        }
        
        /**
         * compiler.err.annotation.array.too.large=\
         *    Annotation array element too large in \"{0}\"
         */
        public static Error AnnotationArrayTooLarge(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.array.too.large", arg0);
        }
        
        /**
         * "annotation.decl.not.allowed.here"
         */
        public static final Error AnnotationDeclNotAllowedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.decl.not.allowed.here");
        
        /**
         * compiler.err.annotation.missing.default.value=\
         *    annotation @{0} is missing a default value for the element ''{1}''
         */
        public static Error AnnotationMissingDefaultValue(Type arg0, List<? extends Name> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.missing.default.value", arg0, arg1);
        }
        
        /**
         * compiler.err.annotation.missing.default.value.1=\
         *    annotation @{0} is missing default values for elements {1}
         */
        public static Error AnnotationMissingDefaultValue1(Type arg0, List<? extends Name> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.missing.default.value.1", arg0, arg1);
        }
        
        /**
         * "annotation.missing.element.value"
         */
        public static final Error AnnotationMissingElementValue = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.missing.element.value");
        
        /**
         * compiler.err.annotation.not.valid.for.type=\
         *    annotation not valid for an element of type {0}
         */
        public static Error AnnotationNotValidForType(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.not.valid.for.type", arg0);
        }
        
        /**
         * "annotation.type.not.applicable"
         */
        public static final Error AnnotationTypeNotApplicable = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.type.not.applicable");
        
        /**
         * compiler.err.annotation.type.not.applicable.to.type=\
         *    annotation @{0} not applicable in this type context
         */
        public static Error AnnotationTypeNotApplicableToType(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.type.not.applicable.to.type", arg0);
        }
        
        /**
         * compiler.err.annotation.unrecognized.attribute.name=\
         *    annotation @{0} has an unknown attribute named ''{1}''
         */
        public static Error AnnotationUnrecognizedAttributeName(Type arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.unrecognized.attribute.name", arg0, arg1);
        }
        
        /**
         * "annotation.value.must.be.annotation"
         */
        public static final Error AnnotationValueMustBeAnnotation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.value.must.be.annotation");
        
        /**
         * "annotation.value.must.be.class.literal"
         */
        public static final Error AnnotationValueMustBeClassLiteral = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.value.must.be.class.literal");
        
        /**
         * "annotation.value.must.be.name.value"
         */
        public static final Error AnnotationValueMustBeNameValue = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.value.must.be.name.value");
        
        /**
         * "annotation.value.not.allowable.type"
         */
        public static final Error AnnotationValueNotAllowableType = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "annotation.value.not.allowable.type");
        
        /**
         * "anon.class.impl.intf.no.args"
         */
        public static final Error AnonClassImplIntfNoArgs = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anon.class.impl.intf.no.args");
        
        /**
         * "anon.class.impl.intf.no.qual.for.new"
         */
        public static final Error AnonClassImplIntfNoQualForNew = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anon.class.impl.intf.no.qual.for.new");
        
        /**
         * "anon.class.impl.intf.no.typeargs"
         */
        public static final Error AnonClassImplIntfNoTypeargs = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anon.class.impl.intf.no.typeargs");
        
        /**
         * compiler.err.anonymous.diamond.method.does.not.override.superclass=\
         *    method does not override or implement a method from a supertype\n\
         *    {0}
         */
        public static Error AnonymousDiamondMethodDoesNotOverrideSuperclass(JCDiagnostic arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anonymous.diamond.method.does.not.override.superclass", arg0);
        }
        
        /**
         * compiler.err.anonymous.diamond.method.does.not.override.superclass=\
         *    method does not override or implement a method from a supertype\n\
         *    {0}
         */
        public static Error AnonymousDiamondMethodDoesNotOverrideSuperclass(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anonymous.diamond.method.does.not.override.superclass", arg0);
        }
        
        /**
         * "array.and.receiver"
         */
        public static final Error ArrayAndReceiver = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "array.and.receiver");
        
        /**
         * compiler.err.array.and.varargs=\
         *    cannot declare both {0} and {1} in {2}
         */
        public static Error ArrayAndVarargs(Symbol arg0, Symbol arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "array.and.varargs", arg0, arg1, arg2);
        }
        
        /**
         * "array.dimension.missing"
         */
        public static final Error ArrayDimensionMissing = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "array.dimension.missing");
        
        /**
         * compiler.err.array.req.but.found=\
         *    array required, but {0} found
         */
        public static Error ArrayReqButFound(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "array.req.but.found", arg0);
        }
        
        /**
         * "assert.as.identifier"
         */
        public static final Error AssertAsIdentifier = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "assert.as.identifier");
        
        /**
         * "attribute.value.must.be.constant"
         */
        public static final Error AttributeValueMustBeConstant = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "attribute.value.must.be.constant");
        
        /**
         * compiler.err.bad.file.name=\
         *    bad file name: {0}
         */
        public static Error BadFileName(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.file.name", arg0);
        }
        
        /**
         * "bad.functional.intf.anno"
         */
        public static final Error BadFunctionalIntfAnno = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.functional.intf.anno");
        
        /**
         * compiler.err.bad.functional.intf.anno.1=\
         *    Unexpected @FunctionalInterface annotation\n\
         *    {0}
         */
        public static Error BadFunctionalIntfAnno1(JCDiagnostic arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.functional.intf.anno.1", arg0);
        }
        
        /**
         * compiler.err.bad.functional.intf.anno.1=\
         *    Unexpected @FunctionalInterface annotation\n\
         *    {0}
         */
        public static Error BadFunctionalIntfAnno1(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.functional.intf.anno.1", arg0);
        }
        
        /**
         * compiler.err.bad.initializer=\
         *    bad initializer for {0}
         */
        public static Error BadInitializer(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.initializer", arg0);
        }
        
        /**
         * compiler.err.bad.name.for.option=\
         *    bad name in value for {0} option: ''{1}''
         */
        public static Error BadNameForOption(Option arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.name.for.option", arg0, arg1);
        }
        
        /**
         * compiler.err.bad.value.for.option=\
         *    bad value for {0} option: ''{1}''
         */
        public static Error BadValueForOption(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.value.for.option", arg0, arg1);
        }
        
        /**
         * "break.outside.switch.expression"
         */
        public static final Error BreakOutsideSwitchExpression = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "break.outside.switch.expression");
        
        /**
         * "break.outside.switch.loop"
         */
        public static final Error BreakOutsideSwitchLoop = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "break.outside.switch.loop");
        
        /**
         * "call.must.only.appear.in.ctor"
         */
        public static final Error CallMustOnlyAppearInCtor = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "call.must.only.appear.in.ctor");
        
        /**
         * compiler.err.call.to.super.not.allowed.in.enum.ctor=\
         *    call to super not allowed in enum constructor
         */
        public static Error CallToSuperNotAllowedInEnumCtor(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "call.to.super.not.allowed.in.enum.ctor", arg0);
        }
        
        /**
         * compiler.err.cannot.assign.not.declared.guard=\
         *    cannot assign to {0}, as it was not declared inside the guard
         */
        public static Error CannotAssignNotDeclaredGuard(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cannot.assign.not.declared.guard", arg0);
        }
        
        /**
         * "cannot.create.array.with.diamond"
         */
        public static final Error CannotCreateArrayWithDiamond = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cannot.create.array.with.diamond");
        
        /**
         * "cannot.create.array.with.type.arguments"
         */
        public static final Error CannotCreateArrayWithTypeArguments = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cannot.create.array.with.type.arguments");
        
        /**
         * compiler.err.cannot.generate.class=\
         *    error while generating class {0}\n\
         *    ({1})
         */
        public static Error CannotGenerateClass(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cannot.generate.class", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.access=\
         *    cannot access {0}\n\
         *    {1}
         */
        public static Error CantAccess(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.access", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.access=\
         *    cannot access {0}\n\
         *    {1}
         */
        public static Error CantAccess(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.access", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond=\
         *    cannot infer type arguments for {0}
         */
        public static Error CantApplyDiamond(JCDiagnostic arg0, Void arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond=\
         *    cannot infer type arguments for {0}
         */
        public static Error CantApplyDiamond(Fragment arg0, Void arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(JCDiagnostic arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(JCDiagnostic arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(Fragment arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(Fragment arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(Type arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Error CantApplyDiamond1(Type arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types;\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Error CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.cant.apply.symbol.noargs=\
         *    {0} {1} in {2} {3} cannot be applied to given types;\n\
         *    reason: {4}
         */
        public static Error CantApplySymbolNoargs(Kind arg0, Name arg1, Kind arg2, Type arg3, JCDiagnostic arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol.noargs", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.apply.symbol.noargs=\
         *    {0} {1} in {2} {3} cannot be applied to given types;\n\
         *    reason: {4}
         */
        public static Error CantApplySymbolNoargs(Kind arg0, Name arg1, Kind arg2, Type arg3, Fragment arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol.noargs", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.apply.symbols=\
         *    no suitable {0} found for {1}({2})
         */
        public static Error CantApplySymbols(Kind arg0, Name arg1, List<? extends Type> arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbols", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.cant.assign.initialized.before.ctor.called=\
         *    assignment to initialized field ''{0}'' may only appear after an explicit constructor invocation
         */
        public static Error CantAssignInitializedBeforeCtorCalled(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.assign.initialized.before.ctor.called", arg0);
        }
        
        /**
         * compiler.err.cant.assign.initialized.before.ctor.called=\
         *    assignment to initialized field ''{0}'' may only appear after an explicit constructor invocation
         */
        public static Error CantAssignInitializedBeforeCtorCalled(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.assign.initialized.before.ctor.called", arg0);
        }
        
        /**
         * "cant.assign.val.to.this"
         */
        public static final Error CantAssignValToThis = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.assign.val.to.this");
        
        /**
         * compiler.err.cant.assign.val.to.var=\
         *    cannot assign a value to {0} variable {1}
         */
        public static Error CantAssignValToVar(Set<? extends FlagsEnum> arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.assign.val.to.var", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.assign.val.to.var=\
         *    cannot assign a value to {0} variable {1}
         */
        public static Error CantAssignValToVar(String arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.assign.val.to.var", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.attach.type.annotations=\
         *    Cannot attach type annotations {0} to {1}.{2}:\n\
         *    {3}
         */
        public static Error CantAttachTypeAnnotations(List<? extends Compound> arg0, Symbol arg1, Name arg2, JCDiagnostic arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.attach.type.annotations", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.cant.attach.type.annotations=\
         *    Cannot attach type annotations {0} to {1}.{2}:\n\
         *    {3}
         */
        public static Error CantAttachTypeAnnotations(List<? extends Compound> arg0, Symbol arg1, Name arg2, Fragment arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.attach.type.annotations", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.cant.deref=\
         *    {0} cannot be dereferenced
         */
        public static Error CantDeref(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.deref", arg0);
        }
        
        /**
         * "cant.extend.intf.annotation"
         */
        public static final Error CantExtendIntfAnnotation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.extend.intf.annotation");
        
        /**
         * compiler.err.cant.infer.local.var.type=\
         *    cannot infer type for local variable {0}\n\
         *    ({1})
         */
        public static Error CantInferLocalVarType(Name arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.infer.local.var.type", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.infer.local.var.type=\
         *    cannot infer type for local variable {0}\n\
         *    ({1})
         */
        public static Error CantInferLocalVarType(Name arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.infer.local.var.type", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.inherit.diff.arg=\
         *    {0} cannot be inherited with different arguments: <{1}> and <{2}>
         */
        public static Error CantInheritDiffArg(Symbol arg0, String arg1, String arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.inherit.diff.arg", arg0, arg1, arg2);
        }
        
        /**
         * "cant.inherit.from.anon"
         */
        public static final Error CantInheritFromAnon = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.inherit.from.anon");
        
        /**
         * compiler.err.cant.inherit.from.final=\
         *    cannot inherit from final {0}
         */
        public static Error CantInheritFromFinal(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.inherit.from.final", arg0);
        }
        
        /**
         * compiler.err.cant.inherit.from.sealed=\
         *    class is not allowed to extend sealed class: {0} \
         *    (as it is not listed in its ''permits'' clause)
         */
        public static Error CantInheritFromSealed(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.inherit.from.sealed", arg0);
        }
        
        /**
         * "cant.read.file"
         */
        public static final Error CantReadFile = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.read.file");
        
        /**
         * compiler.err.cant.ref.before.ctor.called=\
         *    reference to {0} may only appear after an explicit constructor invocation
         */
        public static Error CantRefBeforeCtorCalled(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.ref.before.ctor.called", arg0);
        }
        
        /**
         * compiler.err.cant.ref.before.ctor.called=\
         *    reference to {0} may only appear after an explicit constructor invocation
         */
        public static Error CantRefBeforeCtorCalled(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.ref.before.ctor.called", arg0);
        }
        
        /**
         * compiler.err.cant.ref.non.effectively.final.var=\
         *    local variables referenced from {1} must be final or effectively final
         */
        public static Error CantRefNonEffectivelyFinalVar(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.ref.non.effectively.final.var", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.ref.non.effectively.final.var=\
         *    local variables referenced from {1} must be final or effectively final
         */
        public static Error CantRefNonEffectivelyFinalVar(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.ref.non.effectively.final.var", arg0, arg1);
        }
        
        /**
         * compiler.err.cant.resolve=\
         *    cannot find symbol\n\
         *    symbol: {0} {1}
         */
        public static Error CantResolve(KindName arg0, Name arg1, Void arg2, Void arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.cant.resolve.args=\
         *    cannot find symbol\n\
         *    symbol: {0} {1}({3})
         */
        public static Error CantResolveArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.args", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.cant.resolve.args.params=\
         *    cannot find symbol\n\
         *    symbol: {0} <{2}>{1}({3})
         */
        public static Error CantResolveArgsParams(KindName arg0, Name arg1, List<? extends Type> arg2, List<? extends Type> arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.args.params", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.cant.resolve.location=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}\n\
         *    location: {4}
         */
        public static Error CantResolveLocation(KindName arg0, Name arg1, Void arg2, Void arg3, JCDiagnostic arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.resolve.location=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}\n\
         *    location: {4}
         */
        public static Error CantResolveLocation(KindName arg0, Name arg1, Void arg2, Void arg3, Fragment arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.resolve.location.args=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}({3})\n\
         *    location: {4}
         */
        public static Error CantResolveLocationArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3, JCDiagnostic arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.resolve.location.args=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}({3})\n\
         *    location: {4}
         */
        public static Error CantResolveLocationArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3, Fragment arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.resolve.location.args.params=\
         *    cannot find symbol\n\
         *    symbol:   {0} <{2}>{1}({3})\n\
         *    location: {4}
         */
        @SuppressWarnings("rawtypes")
        public static Error CantResolveLocationArgsParams(KindName arg0, Name arg1, List<? extends Type> arg2, List arg3, JCDiagnostic arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args.params", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.cant.resolve.location.args.params=\
         *    cannot find symbol\n\
         *    symbol:   {0} <{2}>{1}({3})\n\
         *    location: {4}
         */
        @SuppressWarnings("rawtypes")
        public static Error CantResolveLocationArgsParams(KindName arg0, Name arg1, List<? extends Type> arg2, List arg3, Fragment arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args.params", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * "cant.select.static.class.from.param.type"
         */
        public static final Error CantSelectStaticClassFromParamType = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.select.static.class.from.param.type");
        
        /**
         * "catch.without.try"
         */
        public static final Error CatchWithoutTry = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "catch.without.try");
        
        /**
         * compiler.err.clash.with.pkg.of.same.name=\
         *    {0} {1} clashes with package of same name
         */
        public static Error ClashWithPkgOfSameName(KindName arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "clash.with.pkg.of.same.name", arg0, arg1);
        }
        
        /**
         * compiler.err.class.cant.write=\
         *    error while writing {0}: {1}
         */
        public static Error ClassCantWrite(Symbol arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.cant.write", arg0, arg1);
        }
        
        /**
         * compiler.err.class.in.module.cant.extend.sealed.in.diff.module=\
         *    class {0} in module {1} cannot extend a sealed class in a different module
         */
        public static Error ClassInModuleCantExtendSealedInDiffModule(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.in.module.cant.extend.sealed.in.diff.module", arg0, arg1);
        }
        
        /**
         * compiler.err.class.in.unnamed.module.cant.extend.sealed.in.diff.package=\
         *    class {0} in unnamed module cannot extend a sealed class in a different package
         */
        public static Error ClassInUnnamedModuleCantExtendSealedInDiffPackage(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.in.unnamed.module.cant.extend.sealed.in.diff.package", arg0);
        }
        
        /**
         * "class.method.or.field.expected"
         */
        public static final Error ClassMethodOrFieldExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.method.or.field.expected");
        
        /**
         * "class.not.allowed"
         */
        public static final Error ClassNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.not.allowed");
        
        /**
         * compiler.err.class.public.should.be.in.file=\
         *    {0} {1} is public, should be declared in a file named {1}.java
         */
        public static Error ClassPublicShouldBeInFile(KindName arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.public.should.be.in.file", arg0, arg1);
        }
        
        /**
         * compiler.err.concrete.inheritance.conflict=\
         *    methods {0} from {1} and {2} from {3} are inherited with the same signature
         */
        public static Error ConcreteInheritanceConflict(Symbol arg0, Type arg1, Symbol arg2, Type arg3, Type arg4) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "concrete.inheritance.conflict", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.err.conflicting.exports=\
         *    duplicate or conflicting exports: {0}
         */
        public static Error ConflictingExports(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "conflicting.exports", arg0);
        }
        
        /**
         * compiler.err.conflicting.exports.to.module=\
         *    duplicate or conflicting exports to module: {0}
         */
        public static Error ConflictingExportsToModule(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "conflicting.exports.to.module", arg0);
        }
        
        /**
         * compiler.err.conflicting.opens=\
         *    duplicate or conflicting opens: {0}
         */
        public static Error ConflictingOpens(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "conflicting.opens", arg0);
        }
        
        /**
         * compiler.err.conflicting.opens.to.module=\
         *    duplicate or conflicting opens to module: {0}
         */
        public static Error ConflictingOpensToModule(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "conflicting.opens.to.module", arg0);
        }
        
        /**
         * "const.expr.req"
         */
        public static final Error ConstExprReq = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "const.expr.req");
        
        /**
         * compiler.err.constant.label.not.compatible=\
         *    constant label of type {0} is not compatible with switch selector type {1}
         */
        public static Error ConstantLabelNotCompatible(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "constant.label.not.compatible", arg0, arg1);
        }
        
        /**
         * "cont.outside.loop"
         */
        public static final Error ContOutsideLoop = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cont.outside.loop");
        
        /**
         * "continue.outside.switch.expression"
         */
        public static final Error ContinueOutsideSwitchExpression = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "continue.outside.switch.expression");
        
        /**
         * "ctor.calls.not.allowed.here"
         */
        public static final Error CtorCallsNotAllowedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "ctor.calls.not.allowed.here");
        
        /**
         * compiler.err.cyclic.annotation.element=\
         *    type of element {0} is cyclic
         */
        public static Error CyclicAnnotationElement(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cyclic.annotation.element", arg0);
        }
        
        /**
         * compiler.err.cyclic.inheritance=\
         *    cyclic inheritance involving {0}
         */
        public static Error CyclicInheritance(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cyclic.inheritance", arg0);
        }
        
        /**
         * compiler.err.cyclic.inheritance=\
         *    cyclic inheritance involving {0}
         */
        public static Error CyclicInheritance(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cyclic.inheritance", arg0);
        }
        
        /**
         * compiler.err.cyclic.requires=\
         *    cyclic dependence involving {0}
         */
        public static Error CyclicRequires(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cyclic.requires", arg0);
        }
        
        /**
         * "dc.bad.entity"
         */
        public static final Error DcBadEntity = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.bad.entity");
        
        /**
         * "dc.bad.inline.tag"
         */
        public static final Error DcBadInlineTag = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.bad.inline.tag");
        
        /**
         * "dc.gt.expected"
         */
        public static final Error DcGtExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.gt.expected");
        
        /**
         * "dc.identifier.expected"
         */
        public static final Error DcIdentifierExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.identifier.expected");
        
        /**
         * "dc.invalid.html"
         */
        public static final Error DcInvalidHtml = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.invalid.html");
        
        /**
         * "dc.malformed.html"
         */
        public static final Error DcMalformedHtml = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.malformed.html");
        
        /**
         * "dc.missing.semicolon"
         */
        public static final Error DcMissingSemicolon = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.missing.semicolon");
        
        /**
         * "dc.no.content"
         */
        public static final Error DcNoContent = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.no.content");
        
        /**
         * "dc.no.tag.name"
         */
        public static final Error DcNoTagName = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.no.tag.name");
        
        /**
         * "dc.no.title"
         */
        public static final Error DcNoTitle = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.no.title");
        
        /**
         * "dc.no.url"
         */
        public static final Error DcNoUrl = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.no.url");
        
        /**
         * "dc.ref.annotations.not.allowed"
         */
        public static final Error DcRefAnnotationsNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.ref.annotations.not.allowed");
        
        /**
         * "dc.ref.bad.parens"
         */
        public static final Error DcRefBadParens = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.ref.bad.parens");
        
        /**
         * "dc.ref.syntax.error"
         */
        public static final Error DcRefSyntaxError = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.ref.syntax.error");
        
        /**
         * "dc.ref.unexpected.input"
         */
        public static final Error DcRefUnexpectedInput = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.ref.unexpected.input");
        
        /**
         * "dc.unexpected.content"
         */
        public static final Error DcUnexpectedContent = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.unexpected.content");
        
        /**
         * "dc.unterminated.inline.tag"
         */
        public static final Error DcUnterminatedInlineTag = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.unterminated.inline.tag");
        
        /**
         * "dc.unterminated.signature"
         */
        public static final Error DcUnterminatedSignature = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.unterminated.signature");
        
        /**
         * "dc.unterminated.string"
         */
        public static final Error DcUnterminatedString = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dc.unterminated.string");
        
        /**
         * compiler.err.deconstruction.pattern.only.records=\
         *    deconstruction patterns can only be applied to records, {0} is not a record
         */
        public static Error DeconstructionPatternOnlyRecords(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deconstruction.pattern.only.records", arg0);
        }
        
        /**
         * "deconstruction.pattern.var.not.allowed"
         */
        public static final Error DeconstructionPatternVarNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deconstruction.pattern.var.not.allowed");
        
        /**
         * "default.allowed.in.intf.annotation.member"
         */
        public static final Error DefaultAllowedInIntfAnnotationMember = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "default.allowed.in.intf.annotation.member");
        
        /**
         * "default.and.both.boolean.values"
         */
        public static final Error DefaultAndBothBooleanValues = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "default.and.both.boolean.values");
        
        /**
         * "default.label.not.allowed"
         */
        public static final Error DefaultLabelNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "default.label.not.allowed");
        
        /**
         * compiler.err.default.overrides.object.member=\
         *    default method {0} in {1} {2} overrides a member of java.lang.Object
         */
        public static Error DefaultOverridesObjectMember(Name arg0, KindName arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "default.overrides.object.member", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.does.not.override.abstract=\
         *    {0} is not abstract and does not override abstract method {1} in {2}
         */
        public static Error DoesNotOverrideAbstract(Symbol arg0, Symbol arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "does.not.override.abstract", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.doesnt.exist=\
         *    package {0} does not exist
         */
        public static Error DoesntExist(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "doesnt.exist", arg0);
        }
        
        /**
         * "dot.class.expected"
         */
        public static final Error DotClassExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "dot.class.expected");
        
        /**
         * compiler.err.duplicate.annotation.invalid.repeated=\
         *    annotation {0} is not a valid repeatable annotation
         */
        public static Error DuplicateAnnotationInvalidRepeated(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.annotation.invalid.repeated", arg0);
        }
        
        /**
         * compiler.err.duplicate.annotation.member.value=\
         *    duplicate element ''{0}'' in annotation @{1}.
         */
        public static Error DuplicateAnnotationMemberValue(Name arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.annotation.member.value", arg0, arg1);
        }
        
        /**
         * compiler.err.duplicate.annotation.missing.container=\
         *    {0} is not a repeatable annotation interface
         */
        public static Error DuplicateAnnotationMissingContainer(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.annotation.missing.container", arg0);
        }
        
        /**
         * "duplicate.case.label"
         */
        public static final Error DuplicateCaseLabel = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.case.label");
        
        /**
         * compiler.err.duplicate.class=\
         *    duplicate class: {0}
         */
        public static Error DuplicateClass(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.class", arg0);
        }
        
        /**
         * "duplicate.default.label"
         */
        public static final Error DuplicateDefaultLabel = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.default.label");
        
        /**
         * compiler.err.duplicate.module=\
         *    duplicate module: {0}
         */
        public static Error DuplicateModule(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.module", arg0);
        }
        
        /**
         * compiler.err.duplicate.module.on.path=\
         *    duplicate module on {0}\nmodule in {1}
         */
        public static Error DuplicateModuleOnPath(Fragment arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.module.on.path", arg0, arg1);
        }
        
        /**
         * compiler.err.duplicate.provides=\
         *    duplicate provides: service {0}, implementation {1}
         */
        public static Error DuplicateProvides(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.provides", arg0, arg1);
        }
        
        /**
         * compiler.err.duplicate.requires=\
         *    duplicate requires: {0}
         */
        public static Error DuplicateRequires(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.requires", arg0);
        }
        
        /**
         * "duplicate.unconditional.pattern"
         */
        public static final Error DuplicateUnconditionalPattern = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.unconditional.pattern");
        
        /**
         * compiler.err.duplicate.uses=\
         *    duplicate uses: {0}
         */
        public static Error DuplicateUses(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "duplicate.uses", arg0);
        }
        
        /**
         * "else.without.if"
         */
        public static final Error ElseWithoutIf = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "else.without.if");
        
        /**
         * "empty.A.argument"
         */
        public static final Error EmptyAArgument = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "empty.A.argument");
        
        /**
         * "empty.char.lit"
         */
        public static final Error EmptyCharLit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "empty.char.lit");
        
        /**
         * compiler.err.encl.class.required=\
         *    an enclosing instance that contains {0} is required
         */
        public static Error EnclClassRequired(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "encl.class.required", arg0);
        }
        
        /**
         * compiler.err.enclosing.class.type.non.denotable=\
         *    enclosing class type: {0}\n\
         *    is non-denotable, try casting to a denotable type
         */
        public static Error EnclosingClassTypeNonDenotable(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enclosing.class.type.non.denotable", arg0);
        }
        
        /**
         * "enum.annotation.must.be.enum.constant"
         */
        public static final Error EnumAnnotationMustBeEnumConstant = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.annotation.must.be.enum.constant");
        
        /**
         * "enum.as.identifier"
         */
        public static final Error EnumAsIdentifier = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.as.identifier");
        
        /**
         * "enum.cant.be.generic"
         */
        public static final Error EnumCantBeGeneric = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.cant.be.generic");
        
        /**
         * "enum.cant.be.instantiated"
         */
        public static final Error EnumCantBeInstantiated = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.cant.be.instantiated");
        
        /**
         * "enum.constant.expected"
         */
        public static final Error EnumConstantExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.constant.expected");
        
        /**
         * "enum.constant.not.expected"
         */
        public static final Error EnumConstantNotExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.constant.not.expected");
        
        /**
         * "enum.label.must.be.enum.constant"
         */
        public static final Error EnumLabelMustBeEnumConstant = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.label.must.be.enum.constant");
        
        /**
         * "enum.label.must.be.unqualified.enum"
         */
        public static final Error EnumLabelMustBeUnqualifiedEnum = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.label.must.be.unqualified.enum");
        
        /**
         * "enum.no.finalize"
         */
        public static final Error EnumNoFinalize = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.no.finalize");
        
        /**
         * "enum.no.subclassing"
         */
        public static final Error EnumNoSubclassing = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.no.subclassing");
        
        /**
         * "enum.types.not.extensible"
         */
        public static final Error EnumTypesNotExtensible = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.types.not.extensible");
        
        /**
         * "error"
         */
        public static final Error Error = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "error");
        
        /**
         * compiler.err.error.reading.file=\
         *    error reading {0}; {1}
         */
        public static Error ErrorReadingFile(File arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "error.reading.file", arg0, arg1);
        }
        
        /**
         * compiler.err.error.reading.file=\
         *    error reading {0}; {1}
         */
        public static Error ErrorReadingFile(JavaFileObject arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "error.reading.file", arg0, arg1);
        }
        
        /**
         * compiler.err.error.reading.file=\
         *    error reading {0}; {1}
         */
        public static Error ErrorReadingFile(Path arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "error.reading.file", arg0, arg1);
        }
        
        /**
         * compiler.err.error.writing.file=\
         *    error writing {0}; {1}
         */
        public static Error ErrorWritingFile(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "error.writing.file", arg0, arg1);
        }
        
        /**
         * compiler.err.except.already.caught=\
         *    exception {0} has already been caught
         */
        public static Error ExceptAlreadyCaught(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "except.already.caught", arg0);
        }
        
        /**
         * compiler.err.except.never.thrown.in.try=\
         *    exception {0} is never thrown in body of corresponding try statement
         */
        public static Error ExceptNeverThrownInTry(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "except.never.thrown.in.try", arg0);
        }
        
        /**
         * compiler.err.expected=\
         *    {0} expected
         */
        public static Error Expected(TokenKind arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected", arg0);
        }
        
        /**
         * "expected.module"
         */
        public static final Error ExpectedModule = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected.module");
        
        /**
         * "expected.module.or.open"
         */
        public static final Error ExpectedModuleOrOpen = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected.module.or.open");
        
        /**
         * compiler.err.expected.str=\
         *    {0} expected
         */
        public static Error ExpectedStr(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected.str", arg0);
        }
        
        /**
         * compiler.err.expected2=\
         *    {0} or {1} expected
         */
        public static Error Expected2(TokenKind arg0, TokenKind arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected2", arg0, arg1);
        }
        
        /**
         * compiler.err.expected3=\
         *    {0}, {1}, or {2} expected
         */
        public static Error Expected3(TokenKind arg0, TokenKind arg1, TokenKind arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected3", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.expected4=\
         *    {0}, {1}, {2}, or {3} expected
         */
        public static Error Expected4(TokenKind arg0, TokenKind arg1, TokenKind arg2, String arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expected4", arg0, arg1, arg2, arg3);
        }
        
        /**
         * "expression.not.allowable.as.annotation.value"
         */
        public static final Error ExpressionNotAllowableAsAnnotationValue = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "expression.not.allowable.as.annotation.value");
        
        /**
         * "extraneous.semicolon"
         */
        public static final Error ExtraneousSemicolon = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "extraneous.semicolon");
        
        /**
         * compiler.err.feature.not.supported.in.source=\
         *   {0} is not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Error FeatureNotSupportedInSource(JCDiagnostic arg0, String arg1, String arg2) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "feature.not.supported.in.source", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.feature.not.supported.in.source=\
         *   {0} is not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Error FeatureNotSupportedInSource(Fragment arg0, String arg1, String arg2) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "feature.not.supported.in.source", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.feature.not.supported.in.source.plural=\
         *   {0} are not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Error FeatureNotSupportedInSourcePlural(JCDiagnostic arg0, String arg1, String arg2) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "feature.not.supported.in.source.plural", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.feature.not.supported.in.source.plural=\
         *   {0} are not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Error FeatureNotSupportedInSourcePlural(Fragment arg0, String arg1, String arg2) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "feature.not.supported.in.source.plural", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.file.not.directory=\
         *    not a directory: {0}
         */
        public static Error FileNotDirectory(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.not.directory", arg0);
        }
        
        /**
         * compiler.err.file.not.file=\
         *    not a file: {0}
         */
        public static Error FileNotFile(Object arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.not.file", arg0);
        }
        
        /**
         * compiler.err.file.not.found=\
         *    file not found: {0}
         */
        public static Error FileNotFound(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.not.found", arg0);
        }
        
        /**
         * compiler.err.file.patched.and.msp=\
         *    file accessible from both --patch-module and --module-source-path, \
         *    but belongs to a different module on each path: {0}, {1}
         */
        public static Error FilePatchedAndMsp(Name arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.patched.and.msp", arg0, arg1);
        }
        
        /**
         * "file.sb.on.source.or.patch.path.for.module"
         */
        public static final Error FileSbOnSourceOrPatchPathForModule = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.sb.on.source.or.patch.path.for.module");
        
        /**
         * compiler.err.final.parameter.may.not.be.assigned=\
         *    final parameter {0} may not be assigned
         */
        public static Error FinalParameterMayNotBeAssigned(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "final.parameter.may.not.be.assigned", arg0);
        }
        
        /**
         * "finally.without.try"
         */
        public static final Error FinallyWithoutTry = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "finally.without.try");
        
        /**
         * "flows.through.from.pattern"
         */
        public static final Error FlowsThroughFromPattern = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "flows.through.from.pattern");
        
        /**
         * "flows.through.to.pattern"
         */
        public static final Error FlowsThroughToPattern = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "flows.through.to.pattern");
        
        /**
         * compiler.err.foreach.not.applicable.to.type=\
         *    for-each not applicable to expression type\n\
         *    required: {1}\n\
         *    found:    {0}
         */
        public static Error ForeachNotApplicableToType(Type arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "foreach.not.applicable.to.type", arg0, arg1);
        }
        
        /**
         * compiler.err.foreach.not.applicable.to.type=\
         *    for-each not applicable to expression type\n\
         *    required: {1}\n\
         *    found:    {0}
         */
        public static Error ForeachNotApplicableToType(Type arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "foreach.not.applicable.to.type", arg0, arg1);
        }
        
        /**
         * "fp.number.too.large"
         */
        public static final Error FpNumberTooLarge = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fp.number.too.large");
        
        /**
         * "fp.number.too.small"
         */
        public static final Error FpNumberTooSmall = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fp.number.too.small");
        
        /**
         * "generic.array.creation"
         */
        public static final Error GenericArrayCreation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "generic.array.creation");
        
        /**
         * "generic.throwable"
         */
        public static final Error GenericThrowable = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "generic.throwable");
        
        /**
         * "guard.has.constant.expression.false"
         */
        public static final Error GuardHasConstantExpressionFalse = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "guard.has.constant.expression.false");
        
        /**
         * "guard.not.allowed"
         */
        public static final Error GuardNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "guard.not.allowed");
        
        /**
         * compiler.err.icls.cant.have.static.decl=\
         *    Illegal static declaration in inner class {0}\n\
         *    modifier \''static\'' is only allowed in constant variable declarations
         */
        public static Error IclsCantHaveStaticDecl(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "icls.cant.have.static.decl", arg0);
        }
        
        /**
         * compiler.err.illegal.argument.for.option=\
         *    illegal argument for {0}: {1}
         */
        public static Error IllegalArgumentForOption(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.argument.for.option", arg0, arg1);
        }
        
        /**
         * "illegal.array.creation.both.dimension.and.initialization"
         */
        public static final Error IllegalArrayCreationBothDimensionAndInitialization = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.array.creation.both.dimension.and.initialization");
        
        /**
         * compiler.err.illegal.char=\
         *    illegal character: ''{0}''
         */
        public static Error IllegalChar(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.char", arg0);
        }
        
        /**
         * compiler.err.illegal.char.for.encoding=\
         *    unmappable character (0x{0}) for encoding {1}
         */
        public static Error IllegalCharForEncoding(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.char.for.encoding", arg0, arg1);
        }
        
        /**
         * "illegal.char.literal.multiple.surrogates"
         */
        public static final Error IllegalCharLiteralMultipleSurrogates = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.char.literal.multiple.surrogates");
        
        /**
         * compiler.err.illegal.combination.of.modifiers=\
         *    illegal combination of modifiers: {0} and {1}
         */
        public static Error IllegalCombinationOfModifiers(Set<? extends FlagsEnum> arg0, Set<? extends FlagsEnum> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.combination.of.modifiers", arg0, arg1);
        }
        
        /**
         * compiler.err.illegal.default.super.call=\
         *    bad type qualifier {0} in default super call\n\
         *    {1}
         */
        public static Error IllegalDefaultSuperCall(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.default.super.call", arg0, arg1);
        }
        
        /**
         * compiler.err.illegal.default.super.call=\
         *    bad type qualifier {0} in default super call\n\
         *    {1}
         */
        public static Error IllegalDefaultSuperCall(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.default.super.call", arg0, arg1);
        }
        
        /**
         * compiler.err.illegal.default.super.call=\
         *    bad type qualifier {0} in default super call\n\
         *    {1}
         */
        public static Error IllegalDefaultSuperCall(Type arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.default.super.call", arg0, arg1);
        }
        
        /**
         * compiler.err.illegal.default.super.call=\
         *    bad type qualifier {0} in default super call\n\
         *    {1}
         */
        public static Error IllegalDefaultSuperCall(Type arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.default.super.call", arg0, arg1);
        }
        
        /**
         * "illegal.digit.in.binary.literal"
         */
        public static final Error IllegalDigitInBinaryLiteral = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.digit.in.binary.literal");
        
        /**
         * "illegal.digit.in.octal.literal"
         */
        public static final Error IllegalDigitInOctalLiteral = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.digit.in.octal.literal");
        
        /**
         * "illegal.dot"
         */
        public static final Error IllegalDot = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.dot");
        
        /**
         * "illegal.enum.static.ref"
         */
        public static final Error IllegalEnumStaticRef = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.enum.static.ref");
        
        /**
         * "illegal.esc.char"
         */
        public static final Error IllegalEscChar = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.esc.char");
        
        /**
         * "illegal.forward.ref"
         */
        public static final Error IllegalForwardRef = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.forward.ref");
        
        /**
         * compiler.err.illegal.initializer.for.type=\
         *    illegal initializer for {0}
         */
        public static Error IllegalInitializerForType(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.initializer.for.type", arg0);
        }
        
        /**
         * "illegal.line.end.in.char.lit"
         */
        public static final Error IllegalLineEndInCharLit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.line.end.in.char.lit");
        
        /**
         * "illegal.nonascii.digit"
         */
        public static final Error IllegalNonasciiDigit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.nonascii.digit");
        
        /**
         * "illegal.parenthesized.expression"
         */
        public static final Error IllegalParenthesizedExpression = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.parenthesized.expression");
        
        /**
         * compiler.err.illegal.qual.not.icls=\
         *    illegal qualifier; {0} is not an inner class
         */
        public static Error IllegalQualNotIcls(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.qual.not.icls", arg0);
        }
        
        /**
         * compiler.err.illegal.record.component.name=\
         *    illegal record component name {0}
         */
        public static Error IllegalRecordComponentName(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.record.component.name", arg0);
        }
        
        /**
         * compiler.err.illegal.ref.to.restricted.type=\
         *    illegal reference to restricted type ''{0}''
         */
        public static Error IllegalRefToRestrictedType(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.ref.to.restricted.type", arg0);
        }
        
        /**
         * "illegal.self.ref"
         */
        public static final Error IllegalSelfRef = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.self.ref");
        
        /**
         * "illegal.start.of.expr"
         */
        public static final Error IllegalStartOfExpr = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.start.of.expr");
        
        /**
         * "illegal.start.of.stmt"
         */
        public static final Error IllegalStartOfStmt = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.start.of.stmt");
        
        /**
         * "illegal.start.of.type"
         */
        public static final Error IllegalStartOfType = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.start.of.type");
        
        /**
         * compiler.err.illegal.static.intf.meth.call=\
         *    illegal static interface method call\n\
         *    the receiver expression should be replaced with the type qualifier ''{0}''
         */
        public static Error IllegalStaticIntfMethCall(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.static.intf.meth.call", arg0);
        }
        
        /**
         * "illegal.text.block.open"
         */
        public static final Error IllegalTextBlockOpen = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.text.block.open");
        
        /**
         * "illegal.underscore"
         */
        public static final Error IllegalUnderscore = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.underscore");
        
        /**
         * "illegal.unicode.esc"
         */
        public static final Error IllegalUnicodeEsc = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.unicode.esc");
        
        /**
         * "implicit.class.does.not.have.main.method"
         */
        public static final Error ImplicitClassDoesNotHaveMainMethod = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "implicit.class.does.not.have.main.method");
        
        /**
         * "implicit.class.should.not.have.package.declaration"
         */
        public static final Error ImplicitClassShouldNotHavePackageDeclaration = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "implicit.class.should.not.have.package.declaration");
        
        /**
         * compiler.err.import.module.does.not.read=\
         *    module {0} does not read: {1}
         */
        public static Error ImportModuleDoesNotRead(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "import.module.does.not.read", arg0, arg1);
        }
        
        /**
         * compiler.err.import.module.does.not.read.unnamed=\
         *    unnamed module does not read: {0}
         */
        public static Error ImportModuleDoesNotReadUnnamed(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "import.module.does.not.read.unnamed", arg0);
        }
        
        /**
         * compiler.err.import.module.not.found=\
         *    imported module not found: {0}
         */
        public static Error ImportModuleNotFound(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "import.module.not.found", arg0);
        }
        
        /**
         * compiler.err.import.requires.canonical=\
         *    import requires canonical name for {0}
         */
        public static Error ImportRequiresCanonical(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "import.requires.canonical", arg0);
        }
        
        /**
         * "improperly.formed.type.inner.raw.param"
         */
        public static final Error ImproperlyFormedTypeInnerRawParam = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "improperly.formed.type.inner.raw.param");
        
        /**
         * "improperly.formed.type.param.missing"
         */
        public static final Error ImproperlyFormedTypeParamMissing = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "improperly.formed.type.param.missing");
        
        /**
         * compiler.err.incomparable.types=\
         *    incomparable types: {0} and {1}
         */
        public static Error IncomparableTypes(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incomparable.types", arg0, arg1);
        }
        
        /**
         * compiler.err.incompatible.diff.ret.same.type=\
         *    type {0} defines {1}({2}) more than once with unrelated return types
         */
        public static Error IncompatibleDiffRetSameType(Type arg0, Name arg1, List<? extends Type> arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.diff.ret.same.type", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.incompatible.thrown.types.in.mref=\
         *    incompatible thrown types {0} in functional expression
         */
        public static Error IncompatibleThrownTypesInMref(List<? extends Type> arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.thrown.types.in.mref", arg0);
        }
        
        /**
         * compiler.err.incorrect.constructor.receiver.name=\
         *    the receiver name does not match the enclosing outer class type\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Error IncorrectConstructorReceiverName(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incorrect.constructor.receiver.name", arg0, arg1);
        }
        
        /**
         * compiler.err.incorrect.constructor.receiver.type=\
         *    the receiver type does not match the enclosing outer class type\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Error IncorrectConstructorReceiverType(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incorrect.constructor.receiver.type", arg0, arg1);
        }
        
        /**
         * compiler.err.incorrect.number.of.nested.patterns=\
         *    incorrect number of nested patterns\n\
         *    required: {0}\n\
         *    found: {1}
         */
        public static Error IncorrectNumberOfNestedPatterns(List<? extends Type> arg0, List<? extends Type> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incorrect.number.of.nested.patterns", arg0, arg1);
        }
        
        /**
         * compiler.err.incorrect.receiver.name=\
         *    the receiver name does not match the enclosing class type\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Error IncorrectReceiverName(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incorrect.receiver.name", arg0, arg1);
        }
        
        /**
         * compiler.err.incorrect.receiver.type=\
         *    the receiver type does not match the enclosing class type\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Error IncorrectReceiverType(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incorrect.receiver.type", arg0, arg1);
        }
        
        /**
         * "initializer.must.be.able.to.complete.normally"
         */
        public static final Error InitializerMustBeAbleToCompleteNormally = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "initializer.must.be.able.to.complete.normally");
        
        /**
         * "initializer.not.allowed"
         */
        public static final Error InitializerNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "initializer.not.allowed");
        
        /**
         * "instance.initializer.not.allowed.in.records"
         */
        public static final Error InstanceInitializerNotAllowedInRecords = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "instance.initializer.not.allowed.in.records");
        
        /**
         * compiler.err.instanceof.reifiable.not.safe=\
         *    {0} cannot be safely cast to {1}
         */
        public static Error InstanceofReifiableNotSafe(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "instanceof.reifiable.not.safe", arg0, arg1);
        }
        
        /**
         * compiler.err.int.number.too.large=\
         *    integer number too large
         */
        public static Error IntNumberTooLarge(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "int.number.too.large", arg0);
        }
        
        /**
         * compiler.err.intf.annotation.cant.have.type.params=\
         *    annotation interface {0} cannot be generic
         */
        public static Error IntfAnnotationCantHaveTypeParams(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.annotation.cant.have.type.params", arg0);
        }
        
        /**
         * compiler.err.intf.annotation.member.clash=\
         *    annotation interface {1} declares an element with the same name as method {0}
         */
        public static Error IntfAnnotationMemberClash(Symbol arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.annotation.member.clash", arg0, arg1);
        }
        
        /**
         * "intf.annotation.members.cant.have.params"
         */
        public static final Error IntfAnnotationMembersCantHaveParams = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.annotation.members.cant.have.params");
        
        /**
         * "intf.annotation.members.cant.have.type.params"
         */
        public static final Error IntfAnnotationMembersCantHaveTypeParams = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.annotation.members.cant.have.type.params");
        
        /**
         * "intf.expected.here"
         */
        public static final Error IntfExpectedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.expected.here");
        
        /**
         * "intf.meth.cant.have.body"
         */
        public static final Error IntfMethCantHaveBody = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.meth.cant.have.body");
        
        /**
         * "intf.not.allowed.here"
         */
        public static final Error IntfNotAllowedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intf.not.allowed.here");
        
        /**
         * compiler.err.invalid.A.key=\
         *    key in annotation processor option ''{0}'' is not a dot-separated sequence of identifiers
         */
        public static Error InvalidAKey(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.A.key", arg0);
        }
        
        /**
         * compiler.err.invalid.accessor.method.in.record=\
         *    invalid accessor method in record {0}\n\
         *    ({1})
         */
        public static Error InvalidAccessorMethodInRecord(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.accessor.method.in.record", arg0, arg1);
        }
        
        /**
         * "invalid.annotation.member.type"
         */
        public static final Error InvalidAnnotationMemberType = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.annotation.member.type");
        
        /**
         * "invalid.binary.number"
         */
        public static final Error InvalidBinaryNumber = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.binary.number");
        
        /**
         * compiler.err.invalid.canonical.constructor.in.record=\
         *    invalid {0} constructor in record {1}\n\
         *    ({2})
         */
        public static Error InvalidCanonicalConstructorInRecord(Fragment arg0, Name arg1, Fragment arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.canonical.constructor.in.record", arg0, arg1, arg2);
        }
        
        /**
         * "invalid.case.label.combination"
         */
        public static final Error InvalidCaseLabelCombination = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.case.label.combination");
        
        /**
         * compiler.err.invalid.flag=\
         *    invalid flag: {0}
         */
        public static Error InvalidFlag(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.flag", arg0);
        }
        
        /**
         * "invalid.hex.number"
         */
        public static final Error InvalidHexNumber = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.hex.number");
        
        /**
         * compiler.err.invalid.lambda.parameter.declaration=\
         *    invalid lambda parameter declaration\n\
         *    ({0})
         */
        public static Error InvalidLambdaParameterDeclaration(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.lambda.parameter.declaration", arg0);
        }
        
        /**
         * "invalid.meth.decl.ret.type.req"
         */
        public static final Error InvalidMethDeclRetTypeReq = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.meth.decl.ret.type.req");
        
        /**
         * "invalid.module.directive"
         */
        public static final Error InvalidModuleDirective = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.module.directive");
        
        /**
         * compiler.err.invalid.module.specifier=\
         *    module specifier not allowed: {0}
         */
        public static Error InvalidModuleSpecifier(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.module.specifier", arg0);
        }
        
        /**
         * compiler.err.invalid.mref=\
         *    invalid {0} reference\n\
         *    {1}
         */
        public static Error InvalidMref(KindName arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.mref", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.mref=\
         *    invalid {0} reference\n\
         *    {1}
         */
        public static Error InvalidMref(KindName arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.mref", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.path=\
         *    Invalid filename: {0}
         */
        public static Error InvalidPath(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.path", arg0);
        }
        
        /**
         * compiler.err.invalid.permits.clause=\
         *    invalid permits clause\n\
         *    ({0})
         */
        public static Error InvalidPermitsClause(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.permits.clause", arg0);
        }
        
        /**
         * compiler.err.invalid.profile=\
         *    invalid profile: {0}
         */
        public static Error InvalidProfile(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.profile", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation=\
         *    duplicate annotation: {0} is annotated with an invalid @Repeatable annotation
         */
        public static Error InvalidRepeatableAnnotation(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.elem.nondefault=\
         *    containing annotation interface ({0}) does not have a default value for element {1}
         */
        public static Error InvalidRepeatableAnnotationElemNondefault(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.elem.nondefault", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.elem.nondefault=\
         *    containing annotation interface ({0}) does not have a default value for element {1}
         */
        public static Error InvalidRepeatableAnnotationElemNondefault(Type arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.elem.nondefault", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.incompatible.target=\
         *    containing annotation interface ({0}) is applicable to more targets than repeatable annotation interface ({1})
         */
        public static Error InvalidRepeatableAnnotationIncompatibleTarget(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.incompatible.target", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.invalid.value=\
         *    {0} is not a valid @Repeatable: invalid value element
         */
        public static Error InvalidRepeatableAnnotationInvalidValue(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.invalid.value", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.multiple.values=\
         *    {0} is not a valid @Repeatable, {1} element methods named ''value'' declared
         */
        public static Error InvalidRepeatableAnnotationMultipleValues(Type arg0, int arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.multiple.values", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.no.value=\
         *    {0} is not a valid @Repeatable, no value element method declared
         */
        public static Error InvalidRepeatableAnnotationNoValue(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.no.value", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.no.value=\
         *    {0} is not a valid @Repeatable, no value element method declared
         */
        public static Error InvalidRepeatableAnnotationNoValue(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.no.value", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.not.applicable=\
         *    container {0} is not applicable to element {1}
         */
        public static Error InvalidRepeatableAnnotationNotApplicable(Type arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.not.applicable", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.not.applicable.in.context=\
         *    container {0} is not applicable in this type context
         */
        public static Error InvalidRepeatableAnnotationNotApplicableInContext(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.not.applicable.in.context", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.not.documented=\
         *    repeatable annotation interface ({1}) is @Documented while containing annotation interface ({0}) is not
         */
        public static Error InvalidRepeatableAnnotationNotDocumented(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.not.documented", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.not.inherited=\
         *    repeatable annotation interface ({1}) is @Inherited while containing annotation interface ({0}) is not
         */
        public static Error InvalidRepeatableAnnotationNotInherited(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.not.inherited", arg0, arg1);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.repeated.and.container.present=\
         *    container {0} must not be present at the same time as the element it contains
         */
        public static Error InvalidRepeatableAnnotationRepeatedAndContainerPresent(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.repeated.and.container.present", arg0);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.retention=\
         *    retention of containing annotation interface ({0}) is shorter than the retention of repeatable annotation interface ({2})
         */
        public static Error InvalidRepeatableAnnotationRetention(Symbol arg0, String arg1, Symbol arg2, String arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.retention", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.value.return=\
         *    containing annotation interface ({0}) must declare an element named ''value'' of type {2}
         */
        public static Error InvalidRepeatableAnnotationValueReturn(Symbol arg0, Type arg1, Type arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.value.return", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.invalid.repeatable.annotation.value.return=\
         *    containing annotation interface ({0}) must declare an element named ''value'' of type {2}
         */
        public static Error InvalidRepeatableAnnotationValueReturn(Type arg0, Type arg1, Type arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.repeatable.annotation.value.return", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.invalid.source=\
         *    invalid source release: {0}
         */
        public static Error InvalidSource(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.source", arg0);
        }
        
        /**
         * compiler.err.invalid.supertype.record=\
         *    classes cannot directly extend {0}
         */
        public static Error InvalidSupertypeRecord(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.supertype.record", arg0);
        }
        
        /**
         * compiler.err.invalid.target=\
         *    invalid target release: {0}
         */
        public static Error InvalidTarget(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.target", arg0);
        }
        
        /**
         * "invalid.yield"
         */
        public static final Error InvalidYield = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.yield");
        
        /**
         * "io.exception"
         */
        public static final Error IoException = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "io.exception");
        
        /**
         * compiler.err.is.preview=\
         *    {0} is a preview API and is disabled by default.\n\
         *    (use --enable-preview to enable preview APIs)
         */
        public static Error IsPreview(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "is.preview", arg0);
        }
        
        /**
         * compiler.err.label.already.in.use=\
         *    label {0} already in use
         */
        public static Error LabelAlreadyInUse(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "label.already.in.use", arg0);
        }
        
        /**
         * "lambda.body.neither.value.nor.void.compatible"
         */
        public static final Error LambdaBodyNeitherValueNorVoidCompatible = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lambda.body.neither.value.nor.void.compatible");
        
        /**
         * "limit.code"
         */
        public static final Error LimitCode = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.code");
        
        /**
         * "limit.code.too.large.for.try.stmt"
         */
        public static final Error LimitCodeTooLargeForTryStmt = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.code.too.large.for.try.stmt");
        
        /**
         * "limit.dimensions"
         */
        public static final Error LimitDimensions = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.dimensions");
        
        /**
         * "limit.locals"
         */
        public static final Error LimitLocals = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.locals");
        
        /**
         * "limit.parameters"
         */
        public static final Error LimitParameters = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.parameters");
        
        /**
         * "limit.pool"
         */
        public static final Error LimitPool = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.pool");
        
        /**
         * "limit.pool.in.class"
         */
        public static final Error LimitPoolInClass = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.pool.in.class");
        
        /**
         * "limit.stack"
         */
        public static final Error LimitStack = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.stack");
        
        /**
         * "limit.string"
         */
        public static final Error LimitString = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.string");
        
        /**
         * compiler.err.limit.string.overflow=\
         *    UTF8 representation for string \"{0}...\" is too long for the constant pool
         */
        public static Error LimitStringOverflow(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "limit.string.overflow", arg0);
        }
        
        /**
         * compiler.err.local.cant.be.inst.static=\
         *    local {0} {1} cannot be instantiated from a static context
         */
        public static Error LocalCantBeInstStatic(Kind arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.cant.be.inst.static", arg0, arg1);
        }
        
        /**
         * compiler.err.local.classes.cant.extend.sealed=\
         *    {0} classes must not extend sealed classes\
         */
        public static Error LocalClassesCantExtendSealed(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.classes.cant.extend.sealed", arg0);
        }
        
        /**
         * "local.enum"
         */
        public static final Error LocalEnum = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.enum");
        
        /**
         * compiler.err.locn.bad.module-info=\
         *    problem reading module-info.class in {0}
         */
        public static Error LocnBadModuleInfo(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.bad.module-info", arg0);
        }
        
        /**
         * compiler.err.locn.cant.get.module.name.for.jar=\
         *    cannot determine module name for {0}
         */
        public static Error LocnCantGetModuleNameForJar(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.cant.get.module.name.for.jar", arg0);
        }
        
        /**
         * compiler.err.locn.cant.read.directory=\
         *    cannot read directory {0}
         */
        public static Error LocnCantReadDirectory(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.cant.read.directory", arg0);
        }
        
        /**
         * compiler.err.locn.cant.read.file=\
         *    cannot read file {0}
         */
        public static Error LocnCantReadFile(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.cant.read.file", arg0);
        }
        
        /**
         * compiler.err.locn.invalid.arg.for.xpatch=\
         *    invalid argument for --patch-module option: {0}
         */
        public static Error LocnInvalidArgForXpatch(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.invalid.arg.for.xpatch", arg0);
        }
        
        /**
         * compiler.err.locn.module-info.not.allowed.on.patch.path=\
         *    module-info.class not allowed on patch path: {0}
         */
        public static Error LocnModuleInfoNotAllowedOnPatchPath(JavaFileObject arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.module-info.not.allowed.on.patch.path", arg0);
        }
        
        /**
         * "malformed.fp.lit"
         */
        public static final Error MalformedFpLit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "malformed.fp.lit");
        
        /**
         * "match.binding.exists"
         */
        public static final Error MatchBindingExists = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "match.binding.exists");
        
        /**
         * compiler.err.method.does.not.override.superclass=\
         *    {0} in {1} does not override or implement a method from a supertype
         */
        public static Error MethodDoesNotOverrideSuperclass(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "method.does.not.override.superclass", arg0, arg1);
        }
        
        /**
         * compiler.err.missing.meth.body.or.decl.abstract=\
         *    method {0} in {1} is missing a method body, or should be declared abstract
         */
        public static Error MissingMethBodyOrDeclAbstract(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "missing.meth.body.or.decl.abstract", arg0, arg1);
        }
        
        /**
         * "missing.ret.stmt"
         */
        public static final Error MissingRetStmt = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "missing.ret.stmt");
        
        /**
         * compiler.err.mod.not.allowed.here=\
         *    modifier {0} not allowed here
         */
        public static Error ModNotAllowedHere(Set<? extends FlagsEnum> arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "mod.not.allowed.here", arg0);
        }
        
        /**
         * "module.decl.sb.in.module-info.java"
         */
        public static final Error ModuleDeclSbInModuleInfoJava = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.decl.sb.in.module-info.java");
        
        /**
         * compiler.err.module.name.mismatch=\
         *    module name {0} does not match expected name {1}
         */
        public static Error ModuleNameMismatch(Name arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.name.mismatch", arg0, arg1);
        }
        
        /**
         * compiler.err.module.non.zero.opens=\
         *    open module {0} has non-zero opens_count
         */
        public static Error ModuleNonZeroOpens(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.non.zero.opens", arg0);
        }
        
        /**
         * compiler.err.module.not.found=\
         *    module not found: {0}
         */
        public static Error ModuleNotFound(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.not.found", arg0);
        }
        
        /**
         * compiler.err.module.not.found.in.module.source.path=\
         *    module {0} not found in module source path
         */
        public static Error ModuleNotFoundInModuleSourcePath(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.not.found.in.module.source.path", arg0);
        }
        
        /**
         * "module.not.found.on.module.source.path"
         */
        public static final Error ModuleNotFoundOnModuleSourcePath = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.not.found.on.module.source.path");
        
        /**
         * "modulesourcepath.must.be.specified.with.dash.m.option"
         */
        public static final Error ModulesourcepathMustBeSpecifiedWithDashMOption = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "modulesourcepath.must.be.specified.with.dash.m.option");
        
        /**
         * compiler.err.multi-module.outdir.cannot.be.exploded.module=\
         *    in multi-module mode, the output directory cannot be an exploded module: {0}
         */
        public static Error MultiModuleOutdirCannotBeExplodedModule(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "multi-module.outdir.cannot.be.exploded.module", arg0);
        }
        
        /**
         * compiler.err.multicatch.parameter.may.not.be.assigned=\
         *    multi-catch parameter {0} may not be assigned
         */
        public static Error MulticatchParameterMayNotBeAssigned(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "multicatch.parameter.may.not.be.assigned", arg0);
        }
        
        /**
         * compiler.err.multicatch.types.must.be.disjoint=\
         *    Alternatives in a multi-catch statement cannot be related by subclassing\n\
         *    Alternative {0} is a subclass of alternative {1}
         */
        public static Error MulticatchTypesMustBeDisjoint(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "multicatch.types.must.be.disjoint", arg0, arg1);
        }
        
        /**
         * "multiple.values.for.module.source.path"
         */
        public static final Error MultipleValuesForModuleSourcePath = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "multiple.values.for.module.source.path");
        
        /**
         * compiler.err.name.clash.same.erasure=\
         *    name clash: {0} and {1} have the same erasure
         */
        public static Error NameClashSameErasure(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "name.clash.same.erasure", arg0, arg1);
        }
        
        /**
         * compiler.err.name.clash.same.erasure.no.hide=\
         *    name clash: {0} in {1} and {2} in {3} have the same erasure, yet neither hides the other
         */
        public static Error NameClashSameErasureNoHide(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "name.clash.same.erasure.no.hide", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.name.clash.same.erasure.no.override=\
         *    name clash: {0}({1}) in {2} and {3}({4}) in {5} have the same erasure, yet neither overrides the other
         */
        public static Error NameClashSameErasureNoOverride(Name arg0, List<? extends Type> arg1, Symbol arg2, Name arg3, List<? extends Type> arg4, Symbol arg5) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "name.clash.same.erasure.no.override", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.err.name.clash.same.erasure.no.override.1=\
         *    name clash: {0} {1} has two methods with the same erasure, yet neither overrides the other\n\
         *    first method:  {2}({3}) in {4}\n\
         *    second method: {5}({6}) in {7}
         */
        public static Error NameClashSameErasureNoOverride1(String arg0, Name arg1, Name arg2, List<? extends Type> arg3, Symbol arg4, Name arg5, List<? extends Type> arg6, Symbol arg7) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "name.clash.same.erasure.no.override.1", arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
        
        /**
         * "name.reserved.for.internal.use"
         */
        public static final Error NameReservedForInternalUse = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "name.reserved.for.internal.use");
        
        /**
         * "native.meth.cant.have.body"
         */
        public static final Error NativeMethCantHaveBody = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "native.meth.cant.have.body");
        
        /**
         * "new.not.allowed.in.annotation"
         */
        public static final Error NewNotAllowedInAnnotation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "new.not.allowed.in.annotation");
        
        /**
         * compiler.err.no.annotation.member=\
         *    no annotation member {0} in {1}
         */
        public static Error NoAnnotationMember(Name arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.annotation.member", arg0, arg1);
        }
        
        /**
         * "no.annotations.on.dot.class"
         */
        public static final Error NoAnnotationsOnDotClass = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.annotations.on.dot.class");
        
        /**
         * compiler.err.no.encl.instance.of.type.in.scope=\
         *    no enclosing instance of type {0} is in scope
         */
        public static Error NoEnclInstanceOfTypeInScope(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.encl.instance.of.type.in.scope", arg0);
        }
        
        /**
         * "no.intf.expected.here"
         */
        public static final Error NoIntfExpectedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.intf.expected.here");
        
        /**
         * "no.java.lang"
         */
        public static final Error NoJavaLang = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.java.lang");
        
        /**
         * "no.match.entry"
         */
        public static final Error NoMatchEntry = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.match.entry");
        
        /**
         * "no.opens.unless.strong"
         */
        public static final Error NoOpensUnlessStrong = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.opens.unless.strong");
        
        /**
         * "no.output.dir"
         */
        public static final Error NoOutputDir = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.output.dir");
        
        /**
         * "no.pkg.in.module-info.java"
         */
        public static final Error NoPkgInModuleInfoJava = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.pkg.in.module-info.java");
        
        /**
         * "no.source.files"
         */
        public static final Error NoSourceFiles = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.source.files");
        
        /**
         * "no.source.files.classes"
         */
        public static final Error NoSourceFilesClasses = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.source.files.classes");
        
        /**
         * compiler.err.no.superclass=\
         *    {0} has no superclass.
         */
        public static Error NoSuperclass(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.superclass", arg0);
        }
        
        /**
         * "no.switch.expression"
         */
        public static final Error NoSwitchExpression = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.switch.expression");
        
        /**
         * "no.switch.expression.qualify"
         */
        public static final Error NoSwitchExpressionQualify = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.switch.expression.qualify");
        
        /**
         * compiler.err.no.value.for.option=\
         *    no value for {0} option
         */
        public static Error NoValueForOption(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.value.for.option", arg0);
        }
        
        /**
         * compiler.err.no.zipfs.for.archive=\
         *    No file system provider is available to handle this file: {0}
         */
        public static Error NoZipfsForArchive(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.zipfs.for.archive", arg0);
        }
        
        /**
         * compiler.err.non-static.cant.be.ref=\
         *    non-static {0} {1} cannot be referenced from a static context
         */
        public static Error NonStaticCantBeRef(Kind arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non-static.cant.be.ref", arg0, arg1);
        }
        
        /**
         * compiler.err.non.canonical.constructor.invoke.another.constructor=\
         *    constructor is not canonical, so it must invoke another constructor of class {0}
         */
        public static Error NonCanonicalConstructorInvokeAnotherConstructor(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non.canonical.constructor.invoke.another.constructor", arg0);
        }
        
        /**
         * "non.sealed.or.sealed.expected"
         */
        public static final Error NonSealedOrSealedExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non.sealed.or.sealed.expected");
        
        /**
         * "non.sealed.sealed.or.final.expected"
         */
        public static final Error NonSealedSealedOrFinalExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non.sealed.sealed.or.final.expected");
        
        /**
         * compiler.err.non.sealed.with.no.sealed.supertype=\
         *    non-sealed modifier not allowed here\n\
         *    (class {0} does not have any sealed supertypes)
         */
        public static Error NonSealedWithNoSealedSupertype(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non.sealed.with.no.sealed.supertype", arg0);
        }
        
        /**
         * compiler.err.not.annotation.type=\
         *    {0} is not an annotation interface
         */
        public static Error NotAnnotationType(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.annotation.type", arg0);
        }
        
        /**
         * compiler.err.not.def.access.class.intf.cant.access=\
         *    {1}.{0} is defined in an inaccessible class or interface
         */
        public static Error NotDefAccessClassIntfCantAccess(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access", arg0, arg1);
        }
        
        /**
         * compiler.err.not.def.access.class.intf.cant.access.reason=\
         *    {1}.{0} in package {2} is not accessible\n\
         *    ({3})
         */
        public static Error NotDefAccessClassIntfCantAccessReason(Symbol arg0, Symbol arg1, Symbol arg2, JCDiagnostic arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.not.def.access.class.intf.cant.access.reason=\
         *    {1}.{0} in package {2} is not accessible\n\
         *    ({3})
         */
        public static Error NotDefAccessClassIntfCantAccessReason(Symbol arg0, Symbol arg1, Symbol arg2, Fragment arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.not.def.access.package.cant.access=\
         *    {0} is not visible\n\
         *    ({2})
         */
        public static Error NotDefAccessPackageCantAccess(Symbol arg0, Symbol arg1, JCDiagnostic arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.package.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.not.def.access.package.cant.access=\
         *    {0} is not visible\n\
         *    ({2})
         */
        public static Error NotDefAccessPackageCantAccess(Symbol arg0, Symbol arg1, Fragment arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.package.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.not.def.public=\
         *    {0} is not public in {1}
         */
        public static Error NotDefPublic(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.public", arg0, arg1);
        }
        
        /**
         * compiler.err.not.def.public.cant.access=\
         *    {0} is not public in {1}; cannot be accessed from outside package
         */
        public static Error NotDefPublicCantAccess(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.public.cant.access", arg0, arg1);
        }
        
        /**
         * compiler.err.not.encl.class=\
         *    not an enclosing class: {0}
         */
        public static Error NotEnclClass(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.encl.class", arg0);
        }
        
        /**
         * "not.exhaustive"
         */
        public static final Error NotExhaustive = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.exhaustive");
        
        /**
         * "not.exhaustive.details"
         */
        public static final Error NotExhaustiveDetails = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.exhaustive.details");
        
        /**
         * "not.exhaustive.statement"
         */
        public static final Error NotExhaustiveStatement = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.exhaustive.statement");
        
        /**
         * "not.exhaustive.statement.details"
         */
        public static final Error NotExhaustiveStatementDetails = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.exhaustive.statement.details");
        
        /**
         * "not.in.module.on.module.source.path"
         */
        public static final Error NotInModuleOnModuleSourcePath = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.in.module.on.module.source.path");
        
        /**
         * compiler.err.not.in.profile=\
         *    {0} is not available in profile ''{1}''
         */
        public static Error NotInProfile(Symbol arg0, Object arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.in.profile", arg0, arg1);
        }
        
        /**
         * compiler.err.not.loop.label=\
         *    not a loop label: {0}
         */
        public static Error NotLoopLabel(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.loop.label", arg0);
        }
        
        /**
         * "not.stmt"
         */
        public static final Error NotStmt = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.stmt");
        
        /**
         * compiler.err.not.within.bounds=\
         *    type argument {0} is not within bounds of type-variable {1}
         */
        public static Error NotWithinBounds(Type arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.within.bounds", arg0, arg1);
        }
        
        /**
         * compiler.err.not.within.bounds=\
         *    type argument {0} is not within bounds of type-variable {1}
         */
        public static Error NotWithinBounds(Type arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.within.bounds", arg0, arg1);
        }
        
        /**
         * compiler.err.operator.cant.be.applied=\
         *    bad operand type {1} for unary operator ''{0}''
         */
        public static Error OperatorCantBeApplied(Name arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "operator.cant.be.applied", arg0, arg1);
        }
        
        /**
         * compiler.err.operator.cant.be.applied.1=\
         *    bad operand types for binary operator ''{0}''\n\
         *    first type:  {1}\n\
         *    second type: {2}
         */
        public static Error OperatorCantBeApplied1(Name arg0, Type arg1, Type arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "operator.cant.be.applied.1", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.option.not.allowed.with.target=\
         *    option {0} not allowed with target {1}
         */
        public static Error OptionNotAllowedWithTarget(Option arg0, Target arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "option.not.allowed.with.target", arg0, arg1);
        }
        
        /**
         * compiler.err.option.removed.source=\
         *    Source option {0} is no longer supported. Use {1} or later.
         */
        public static Error OptionRemovedSource(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "option.removed.source", arg0, arg1);
        }
        
        /**
         * compiler.err.option.removed.target=\
         *    Target option {0} is no longer supported. Use {1} or later.
         */
        public static Error OptionRemovedTarget(Target arg0, Target arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "option.removed.target", arg0, arg1);
        }
        
        /**
         * compiler.err.option.too.many=\
         *    option {0} can only be specified once
         */
        public static Error OptionTooMany(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "option.too.many", arg0);
        }
        
        /**
         * compiler.err.orphaned=\
         *    orphaned {0}
         */
        public static Error Orphaned(TokenKind arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "orphaned", arg0);
        }
        
        /**
         * "output.dir.must.be.specified.with.dash.m.option"
         */
        public static final Error OutputDirMustBeSpecifiedWithDashMOption = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "output.dir.must.be.specified.with.dash.m.option");
        
        /**
         * compiler.err.override.incompatible.ret=\
         *    {0}\n\
         *    return type {1} is not compatible with {2}
         */
        public static Error OverrideIncompatibleRet(JCDiagnostic arg0, Type arg1, Type arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.incompatible.ret", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.override.incompatible.ret=\
         *    {0}\n\
         *    return type {1} is not compatible with {2}
         */
        public static Error OverrideIncompatibleRet(Fragment arg0, Type arg1, Type arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.incompatible.ret", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.override.meth=\
         *    {0}\n\
         *    overridden method is {1}
         */
        public static Error OverrideMeth(JCDiagnostic arg0, Set<? extends FlagsEnum> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.meth", arg0, arg1);
        }
        
        /**
         * compiler.err.override.meth=\
         *    {0}\n\
         *    overridden method is {1}
         */
        public static Error OverrideMeth(Fragment arg0, Set<? extends FlagsEnum> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.meth", arg0, arg1);
        }
        
        /**
         * compiler.err.override.meth.doesnt.throw=\
         *    {0}\n\
         *    overridden method does not throw {1}
         */
        public static Error OverrideMethDoesntThrow(JCDiagnostic arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.meth.doesnt.throw", arg0, arg1);
        }
        
        /**
         * compiler.err.override.meth.doesnt.throw=\
         *    {0}\n\
         *    overridden method does not throw {1}
         */
        public static Error OverrideMethDoesntThrow(Fragment arg0, Type arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.meth.doesnt.throw", arg0, arg1);
        }
        
        /**
         * compiler.err.override.static=\
         *    {0}\n\
         *    overriding method is static
         */
        public static Error OverrideStatic(JCDiagnostic arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.static", arg0);
        }
        
        /**
         * compiler.err.override.static=\
         *    {0}\n\
         *    overriding method is static
         */
        public static Error OverrideStatic(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.static", arg0);
        }
        
        /**
         * compiler.err.override.weaker.access=\
         *    {0}\n\
         *    attempting to assign weaker access privileges; was {1}
         */
        public static Error OverrideWeakerAccess(JCDiagnostic arg0, Set<? extends FlagsEnum> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.weaker.access", arg0, arg1);
        }
        
        /**
         * compiler.err.override.weaker.access=\
         *    {0}\n\
         *    attempting to assign weaker access privileges; was {1}
         */
        public static Error OverrideWeakerAccess(JCDiagnostic arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.weaker.access", arg0, arg1);
        }
        
        /**
         * compiler.err.override.weaker.access=\
         *    {0}\n\
         *    attempting to assign weaker access privileges; was {1}
         */
        public static Error OverrideWeakerAccess(Fragment arg0, Set<? extends FlagsEnum> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.weaker.access", arg0, arg1);
        }
        
        /**
         * compiler.err.override.weaker.access=\
         *    {0}\n\
         *    attempting to assign weaker access privileges; was {1}
         */
        public static Error OverrideWeakerAccess(Fragment arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.weaker.access", arg0, arg1);
        }
        
        /**
         * compiler.err.package.clash.from.requires=\
         *    module {0} reads package {1} from both {2} and {3}
         */
        public static Error PackageClashFromRequires(Symbol arg0, Name arg1, Symbol arg2, Symbol arg3) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.clash.from.requires", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.err.package.clash.from.requires.in.unnamed=\
         *    the unnamed module reads package {0} from both {1} and {2}
         */
        public static Error PackageClashFromRequiresInUnnamed(Name arg0, Symbol arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.clash.from.requires.in.unnamed", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.package.empty.or.not.found=\
         *    package is empty or does not exist: {0}
         */
        public static Error PackageEmptyOrNotFound(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.empty.or.not.found", arg0);
        }
        
        /**
         * compiler.err.package.in.other.module=\
         *    package exists in another module: {0}
         */
        public static Error PackageInOtherModule(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.in.other.module", arg0);
        }
        
        /**
         * compiler.err.package.not.visible=\
         *    package {0} is not visible\n\
         *    ({1})
         */
        public static Error PackageNotVisible(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.not.visible", arg0, arg1);
        }
        
        /**
         * compiler.err.package.not.visible=\
         *    package {0} is not visible\n\
         *    ({1})
         */
        public static Error PackageNotVisible(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.not.visible", arg0, arg1);
        }
        
        /**
         * "pattern.dominated"
         */
        public static final Error PatternDominated = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pattern.dominated");
        
        /**
         * "pattern.expected"
         */
        public static final Error PatternExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pattern.expected");
        
        /**
         * "pattern.or.enum.req"
         */
        public static final Error PatternOrEnumReq = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pattern.or.enum.req");
        
        /**
         * "pattern.type.cannot.infer"
         */
        public static final Error PatternTypeCannotInfer = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pattern.type.cannot.infer");
        
        /**
         * "pkg.annotations.sb.in.package-info.java"
         */
        public static final Error PkgAnnotationsSbInPackageInfoJava = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pkg.annotations.sb.in.package-info.java");
        
        /**
         * compiler.err.pkg.clashes.with.class.of.same.name=\
         *    package {0} clashes with class of same name
         */
        public static Error PkgClashesWithClassOfSameName(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pkg.clashes.with.class.of.same.name", arg0);
        }
        
        /**
         * compiler.err.plugin.not.found=\
         *    plug-in not found: {0}
         */
        public static Error PluginNotFound(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "plugin.not.found", arg0);
        }
        
        /**
         * "premature.eof"
         */
        public static final Error PrematureEof = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "premature.eof");
        
        /**
         * compiler.err.preview.feature.disabled=\
         *   {0} is a preview feature and is disabled by default.\n\
         *   (use --enable-preview to enable {0})
         */
        public static Error PreviewFeatureDisabled(JCDiagnostic arg0) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "preview.feature.disabled", arg0);
        }
        
        /**
         * compiler.err.preview.feature.disabled=\
         *   {0} is a preview feature and is disabled by default.\n\
         *   (use --enable-preview to enable {0})
         */
        public static Error PreviewFeatureDisabled(Fragment arg0) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "preview.feature.disabled", arg0);
        }
        
        /**
         * compiler.err.preview.feature.disabled.classfile=\
         *   class file for {0} uses preview features of Java SE {1}.\n\
         *   (use --enable-preview to allow loading of class files which contain preview features)
         */
        public static Error PreviewFeatureDisabledClassfile(JavaFileObject arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.feature.disabled.classfile", arg0, arg1);
        }
        
        /**
         * compiler.err.preview.feature.disabled.plural=\
         *   {0} are a preview feature and are disabled by default.\n\
         *   (use --enable-preview to enable {0})
         */
        public static Error PreviewFeatureDisabledPlural(JCDiagnostic arg0) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "preview.feature.disabled.plural", arg0);
        }
        
        /**
         * compiler.err.preview.feature.disabled.plural=\
         *   {0} are a preview feature and are disabled by default.\n\
         *   (use --enable-preview to enable {0})
         */
        public static Error PreviewFeatureDisabledPlural(Fragment arg0) {
            return new Error(EnumSet.of(SOURCE_LEVEL), "compiler", "preview.feature.disabled.plural", arg0);
        }
        
        /**
         * compiler.err.preview.not.latest=\
         *    invalid source release {0} with --enable-preview\n\
         *    (preview language features are only supported for release {1})
         */
        public static Error PreviewNotLatest(String arg0, Source arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.not.latest", arg0, arg1);
        }
        
        /**
         * "preview.without.source.or.release"
         */
        public static final Error PreviewWithoutSourceOrRelease = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.without.source.or.release");
        
        /**
         * compiler.err.prob.found.req=\
         *    incompatible types: {0}
         */
        public static Error ProbFoundReq(JCDiagnostic arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "prob.found.req", arg0);
        }
        
        /**
         * compiler.err.prob.found.req=\
         *    incompatible types: {0}
         */
        public static Error ProbFoundReq(Fragment arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "prob.found.req", arg0);
        }
        
        /**
         * compiler.err.proc.bad.config.file=\
         *    Bad service configuration file, or exception thrown while constructing Processor object: {0}
         */
        public static Error ProcBadConfigFile(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.bad.config.file", arg0);
        }
        
        /**
         * compiler.err.proc.cant.access=\
         *    cannot access {0}\n\
         *    {1}\n\
         *    Consult the following stack trace for details.\n\
         *    {2}
         */
        public static Error ProcCantAccess(Symbol arg0, JCDiagnostic arg1, String arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.proc.cant.access=\
         *    cannot access {0}\n\
         *    {1}\n\
         *    Consult the following stack trace for details.\n\
         *    {2}
         */
        public static Error ProcCantAccess(Symbol arg0, Fragment arg1, String arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.proc.cant.access.1=\
         *    cannot access {0}\n\
         *    {1}
         */
        public static Error ProcCantAccess1(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.access.1", arg0, arg1);
        }
        
        /**
         * compiler.err.proc.cant.access.1=\
         *    cannot access {0}\n\
         *    {1}
         */
        public static Error ProcCantAccess1(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.access.1", arg0, arg1);
        }
        
        /**
         * "proc.cant.create.loader"
         */
        public static final Error ProcCantCreateLoader = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.create.loader");
        
        /**
         * compiler.err.proc.cant.find.class=\
         *    Could not find class file for ''{0}''.
         */
        public static Error ProcCantFindClass(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.find.class", arg0);
        }
        
        /**
         * compiler.err.proc.cant.load.class=\
         *    Could not load processor class file due to ''{0}''.
         */
        public static Error ProcCantLoadClass(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.cant.load.class", arg0);
        }
        
        /**
         * compiler.err.proc.messager=\
         *    {0}
         */
        public static Error ProcMessager(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.messager", arg0);
        }
        
        /**
         * compiler.err.proc.no.explicit.annotation.processing.requested=\
         *    Class names, ''{0}'', are only accepted if annotation processing is explicitly requested
         */
        public static Error ProcNoExplicitAnnotationProcessingRequested(Collection<? extends String> arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.no.explicit.annotation.processing.requested", arg0);
        }
        
        /**
         * compiler.err.proc.processor.bad.option.name=\
         *    Bad option name ''{0}'' provided by processor ''{1}''
         */
        public static Error ProcProcessorBadOptionName(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.processor.bad.option.name", arg0, arg1);
        }
        
        /**
         * compiler.err.proc.processor.cant.instantiate=\
         *    Could not instantiate an instance of processor ''{0}''
         */
        public static Error ProcProcessorCantInstantiate(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.processor.cant.instantiate", arg0);
        }
        
        /**
         * compiler.err.proc.processor.not.found=\
         *    Annotation processor ''{0}'' not found
         */
        public static Error ProcProcessorNotFound(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.processor.not.found", arg0);
        }
        
        /**
         * compiler.err.proc.processor.wrong.type=\
         *    Annotation processor ''{0}'' does not implement javax.annotation.processing.Processor
         */
        public static Error ProcProcessorWrongType(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.processor.wrong.type", arg0);
        }
        
        /**
         * "proc.service.problem"
         */
        public static final Error ProcServiceProblem = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.service.problem");
        
        /**
         * "processorpath.no.processormodulepath"
         */
        public static final Error ProcessorpathNoProcessormodulepath = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "processorpath.no.processormodulepath");
        
        /**
         * "profile.bootclasspath.conflict"
         */
        public static final Error ProfileBootclasspathConflict = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "profile.bootclasspath.conflict");
        
        /**
         * compiler.err.qualified.new.of.static.class=\
         *    qualified new of static class
         */
        public static Error QualifiedNewOfStaticClass(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "qualified.new.of.static.class", arg0);
        }
        
        /**
         * "receiver.parameter.not.applicable.constructor.toplevel.class"
         */
        public static final Error ReceiverParameterNotApplicableConstructorToplevelClass = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "receiver.parameter.not.applicable.constructor.toplevel.class");
        
        /**
         * "record.cannot.declare.instance.fields"
         */
        public static final Error RecordCannotDeclareInstanceFields = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "record.cannot.declare.instance.fields");
        
        /**
         * "record.cant.declare.field.modifiers"
         */
        public static final Error RecordCantDeclareFieldModifiers = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "record.cant.declare.field.modifiers");
        
        /**
         * "record.component.and.old.array.syntax"
         */
        public static final Error RecordComponentAndOldArraySyntax = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "record.component.and.old.array.syntax");
        
        /**
         * "record.patterns.annotations.not.allowed"
         */
        public static final Error RecordPatternsAnnotationsNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "record.patterns.annotations.not.allowed");
        
        /**
         * "recursive.ctor.invocation"
         */
        public static final Error RecursiveCtorInvocation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "recursive.ctor.invocation");
        
        /**
         * "redundant.superclass.init"
         */
        public static final Error RedundantSuperclassInit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "redundant.superclass.init");
        
        /**
         * compiler.err.ref.ambiguous=\
         *    reference to {0} is ambiguous\n\
         *    both {1} {2} in {3} and {4} {5} in {6} match
         */
        public static Error RefAmbiguous(Name arg0, Kind arg1, Symbol arg2, Symbol arg3, Kind arg4, Symbol arg5, Symbol arg6) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "ref.ambiguous", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.err.release.bootclasspath.conflict=\
         *    option {0} cannot be used together with --release
         */
        public static Error ReleaseBootclasspathConflict(Option arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "release.bootclasspath.conflict", arg0);
        }
        
        /**
         * "repeated.annotation.target"
         */
        public static final Error RepeatedAnnotationTarget = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.annotation.target");
        
        /**
         * "repeated.interface"
         */
        public static final Error RepeatedInterface = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.interface");
        
        /**
         * "repeated.modifier"
         */
        public static final Error RepeatedModifier = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.modifier");
        
        /**
         * compiler.err.repeated.provides.for.service=\
         *    multiple ''provides'' for service {0}
         */
        public static Error RepeatedProvidesForService(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.provides.for.service", arg0);
        }
        
        /**
         * compiler.err.repeated.value.for.module.source.path=\
         *    --module-source-path specified more than once for module {0}
         */
        public static Error RepeatedValueForModuleSourcePath(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.value.for.module.source.path", arg0);
        }
        
        /**
         * compiler.err.repeated.value.for.patch.module=\
         *    --patch-module specified more than once for module {0}
         */
        public static Error RepeatedValueForPatchModule(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "repeated.value.for.patch.module", arg0);
        }
        
        /**
         * compiler.err.report.access=\
         *    {0} has {1} access in {2}
         */
        public static Error ReportAccess(Symbol arg0, Set<? extends Modifier> arg1, Symbol arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "report.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.req.arg=\
         *    {0} requires an argument
         */
        public static Error ReqArg(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "req.arg", arg0);
        }
        
        /**
         * compiler.err.restricted.type.not.allowed=\
         *    ''{0}'' not allowed here\n\
         *    as of release {1}, ''{0}'' is a restricted type name and cannot be used for type declarations
         */
        public static Error RestrictedTypeNotAllowed(Name arg0, Source arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed", arg0, arg1);
        }
        
        /**
         * compiler.err.restricted.type.not.allowed.array=\
         *    ''{0}'' is not allowed as an element type of an array
         */
        public static Error RestrictedTypeNotAllowedArray(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed.array", arg0);
        }
        
        /**
         * compiler.err.restricted.type.not.allowed.compound=\
         *    ''{0}'' is not allowed in a compound declaration
         */
        public static Error RestrictedTypeNotAllowedCompound(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed.compound", arg0);
        }
        
        /**
         * compiler.err.restricted.type.not.allowed.here=\
         *    ''{0}'' is not allowed here
         */
        public static Error RestrictedTypeNotAllowedHere(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed.here", arg0);
        }
        
        /**
         * "ret.outside.meth"
         */
        public static final Error RetOutsideMeth = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "ret.outside.meth");
        
        /**
         * "return.before.superclass.initialized"
         */
        public static final Error ReturnBeforeSuperclassInitialized = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "return.before.superclass.initialized");
        
        /**
         * "return.outside.switch.expression"
         */
        public static final Error ReturnOutsideSwitchExpression = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "return.outside.switch.expression");
        
        /**
         * "rule.completes.normally"
         */
        public static final Error RuleCompletesNormally = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "rule.completes.normally");
        
        /**
         * compiler.err.same.binary.name=\
         *    classes: {0} and {1} have the same binary name
         */
        public static Error SameBinaryName(Name arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "same.binary.name", arg0, arg1);
        }
        
        /**
         * "sealed.class.must.have.subclasses"
         */
        public static final Error SealedClassMustHaveSubclasses = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "sealed.class.must.have.subclasses");
        
        /**
         * "sealed.or.non.sealed.local.classes.not.allowed"
         */
        public static final Error SealedOrNonSealedLocalClassesNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "sealed.or.non.sealed.local.classes.not.allowed");
        
        /**
         * compiler.err.service.definition.is.enum=\
         *    the service definition is an enum: {0}
         */
        public static Error ServiceDefinitionIsEnum(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.definition.is.enum", arg0);
        }
        
        /**
         * compiler.err.service.implementation.doesnt.have.a.no.args.constructor=\
         *    the service implementation does not have a default constructor: {0}
         */
        public static Error ServiceImplementationDoesntHaveANoArgsConstructor(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.doesnt.have.a.no.args.constructor", arg0);
        }
        
        /**
         * compiler.err.service.implementation.is.abstract=\
         *    the service implementation is an abstract class: {0}
         */
        public static Error ServiceImplementationIsAbstract(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.is.abstract", arg0);
        }
        
        /**
         * compiler.err.service.implementation.is.inner=\
         *    the service implementation is an inner class: {0}
         */
        public static Error ServiceImplementationIsInner(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.is.inner", arg0);
        }
        
        /**
         * "service.implementation.must.be.subtype.of.service.interface"
         */
        public static final Error ServiceImplementationMustBeSubtypeOfServiceInterface = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.must.be.subtype.of.service.interface");
        
        /**
         * compiler.err.service.implementation.no.args.constructor.not.public=\
         *    the no arguments constructor of the service implementation is not public: {0}
         */
        public static Error ServiceImplementationNoArgsConstructorNotPublic(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.no.args.constructor.not.public", arg0);
        }
        
        /**
         * compiler.err.service.implementation.not.in.right.module=\
         *    service implementation must be defined in the same module as the provides directive
         */
        public static Error ServiceImplementationNotInRightModule(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.not.in.right.module", arg0);
        }
        
        /**
         * "service.implementation.provider.return.must.be.subtype.of.service.interface"
         */
        public static final Error ServiceImplementationProviderReturnMustBeSubtypeOfServiceInterface = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.implementation.provider.return.must.be.subtype.of.service.interface");
        
        /**
         * "signature.doesnt.match.intf"
         */
        public static final Error SignatureDoesntMatchIntf = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "signature.doesnt.match.intf");
        
        /**
         * "signature.doesnt.match.supertype"
         */
        public static final Error SignatureDoesntMatchSupertype = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "signature.doesnt.match.supertype");
        
        /**
         * compiler.err.source.cant.overwrite.input.file=\
         *    error writing source; cannot overwrite input file {0}
         */
        public static Error SourceCantOverwriteInputFile(JavaFileObject arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.cant.overwrite.input.file", arg0);
        }
        
        /**
         * compiler.err.source.target.conflict=\
         *    specified target release {1} is too old for the specified source release {0}\n\
         *    --release {1} is recommended when compiling code to run on JDK {1}
         */
        public static Error SourceTargetConflict(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.target.conflict", arg0, arg1);
        }
        
        /**
         * "sourcepath.modulesourcepath.conflict"
         */
        public static final Error SourcepathModulesourcepathConflict = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "sourcepath.modulesourcepath.conflict");
        
        /**
         * compiler.err.stack.sim.error=\
         *    Internal error: stack sim error on {0}
         */
        public static Error StackSimError(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "stack.sim.error", arg0);
        }
        
        /**
         * "statement.not.expected"
         */
        public static final Error StatementNotExpected = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "statement.not.expected");
        
        /**
         * "static.declaration.not.allowed.in.inner.classes"
         */
        public static final Error StaticDeclarationNotAllowedInInnerClasses = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "static.declaration.not.allowed.in.inner.classes");
        
        /**
         * "static.imp.only.classes.and.interfaces"
         */
        public static final Error StaticImpOnlyClassesAndInterfaces = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "static.imp.only.classes.and.interfaces");
        
        /**
         * compiler.err.static.methods.cannot.be.annotated.with.override=\
         *    static method {0} in {1} cannot be annotated with @Override
         */
        public static Error StaticMethodsCannotBeAnnotatedWithOverride(Symbol arg0, Symbol arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "static.methods.cannot.be.annotated.with.override", arg0, arg1);
        }
        
        /**
         * "string.const.req"
         */
        public static final Error StringConstReq = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "string.const.req");
        
        /**
         * "switch.case.unexpected.statement"
         */
        public static final Error SwitchCaseUnexpectedStatement = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.case.unexpected.statement");
        
        /**
         * "switch.expression.completes.normally"
         */
        public static final Error SwitchExpressionCompletesNormally = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.expression.completes.normally");
        
        /**
         * "switch.expression.empty"
         */
        public static final Error SwitchExpressionEmpty = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.expression.empty");
        
        /**
         * "switch.expression.no.result.expressions"
         */
        public static final Error SwitchExpressionNoResultExpressions = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.expression.no.result.expressions");
        
        /**
         * "switch.mixing.case.types"
         */
        public static final Error SwitchMixingCaseTypes = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.mixing.case.types");
        
        /**
         * compiler.err.target.default.source.conflict=\
         *    specified target release {1} is too old for the default source release {0}\n\
         *    --release {1} is recommended when compiling code to run on JDK {1}
         */
        public static Error TargetDefaultSourceConflict(String arg0, String arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "target.default.source.conflict", arg0, arg1);
        }
        
        /**
         * "this.as.identifier"
         */
        public static final Error ThisAsIdentifier = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "this.as.identifier");
        
        /**
         * "throws.not.allowed.in.intf.annotation"
         */
        public static final Error ThrowsNotAllowedInIntfAnnotation = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "throws.not.allowed.in.intf.annotation");
        
        /**
         * "too.many.modules"
         */
        public static final Error TooManyModules = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "too.many.modules");
        
        /**
         * compiler.err.too.many.patched.modules=\
         *    too many patched modules ({0}), use --module-source-path
         */
        public static Error TooManyPatchedModules(Set<? extends String> arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "too.many.patched.modules", arg0);
        }
        
        /**
         * compiler.err.try.resource.may.not.be.assigned=\
         *    auto-closeable resource {0} may not be assigned
         */
        public static Error TryResourceMayNotBeAssigned(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.resource.may.not.be.assigned", arg0);
        }
        
        /**
         * compiler.err.try.with.resources.expr.effectively.final.var=\
         *    variable {0} used as a try-with-resources resource neither final nor effectively final
         */
        public static Error TryWithResourcesExprEffectivelyFinalVar(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.with.resources.expr.effectively.final.var", arg0);
        }
        
        /**
         * "try.with.resources.expr.needs.var"
         */
        public static final Error TryWithResourcesExprNeedsVar = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.with.resources.expr.needs.var");
        
        /**
         * "try.without.catch.finally.or.resource.decls"
         */
        public static final Error TryWithoutCatchFinallyOrResourceDecls = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.without.catch.finally.or.resource.decls");
        
        /**
         * "two.class.loaders.1"
         */
        public static final Error TwoClassLoaders1 = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "two.class.loaders.1");
        
        /**
         * compiler.err.two.class.loaders.2=\
         *    javac is split between multiple class loaders:\n\
         *    one class comes from file: {0}\n\
         *    while javac comes from {1}
         */
        public static Error TwoClassLoaders2(URL arg0, URL arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "two.class.loaders.2", arg0, arg1);
        }
        
        /**
         * compiler.err.type.annotation.inadmissible=\
         *    {0} not expected here\n\
         *    (to annotate a qualified type, write {1}.{2})
         */
        public static Error TypeAnnotationInadmissible(Fragment arg0, Symbol arg1, AnnotatedType arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.annotation.inadmissible", arg0, arg1, arg2);
        }
        
        /**
         * compiler.err.type.doesnt.take.params=\
         *    type {0} does not take parameters
         */
        public static Error TypeDoesntTakeParams(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.doesnt.take.params", arg0);
        }
        
        /**
         * compiler.err.type.found.req=\
         *    unexpected type\n\
         *    required: {1}\n\
         *    found:    {0}
         */
        public static Error TypeFoundReq(Object arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.found.req", arg0, arg1);
        }
        
        /**
         * compiler.err.type.found.req=\
         *    unexpected type\n\
         *    required: {1}\n\
         *    found:    {0}
         */
        public static Error TypeFoundReq(Object arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.found.req", arg0, arg1);
        }
        
        /**
         * "type.var.cant.be.deref"
         */
        public static final Error TypeVarCantBeDeref = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.var.cant.be.deref");
        
        /**
         * "type.var.may.not.be.followed.by.other.bounds"
         */
        public static final Error TypeVarMayNotBeFollowedByOtherBounds = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.var.may.not.be.followed.by.other.bounds");
        
        /**
         * "type.var.more.than.once"
         */
        public static final Error TypeVarMoreThanOnce = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.var.more.than.once");
        
        /**
         * "type.var.more.than.once.in.result"
         */
        public static final Error TypeVarMoreThanOnceInResult = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.var.more.than.once.in.result");
        
        /**
         * compiler.err.types.incompatible=\
         *    types {0} and {1} are incompatible;\n\
         *    {2}
         */
        public static Error TypesIncompatible(Type arg0, Type arg1, Fragment arg2) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "types.incompatible", arg0, arg1, arg2);
        }
        
        /**
         * "unclosed.char.lit"
         */
        public static final Error UnclosedCharLit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unclosed.char.lit");
        
        /**
         * "unclosed.comment"
         */
        public static final Error UnclosedComment = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unclosed.comment");
        
        /**
         * "unclosed.str.lit"
         */
        public static final Error UnclosedStrLit = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unclosed.str.lit");
        
        /**
         * "unclosed.text.block"
         */
        public static final Error UnclosedTextBlock = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unclosed.text.block");
        
        /**
         * "unconditional.pattern.and.both.boolean.values"
         */
        public static final Error UnconditionalPatternAndBothBooleanValues = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unconditional.pattern.and.both.boolean.values");
        
        /**
         * "unconditional.pattern.and.default"
         */
        public static final Error UnconditionalPatternAndDefault = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unconditional.pattern.and.default");
        
        /**
         * compiler.err.undef.label=\
         *    undefined label: {0}
         */
        public static Error UndefLabel(Name arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "undef.label", arg0);
        }
        
        /**
         * "underscore.as.identifier"
         */
        public static final Error UnderscoreAsIdentifier = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "underscore.as.identifier");
        
        /**
         * "unexpected.lambda"
         */
        public static final Error UnexpectedLambda = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unexpected.lambda");
        
        /**
         * "unexpected.mref"
         */
        public static final Error UnexpectedMref = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unexpected.mref");
        
        /**
         * compiler.err.unexpected.type=\
         *    unexpected type\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Error UnexpectedType(Set<? extends KindName> arg0, Set<? extends KindName> arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unexpected.type", arg0, arg1);
        }
        
        /**
         * compiler.err.unmatched.quote=\
         *    unmatched quote in environment variable {0}
         */
        public static Error UnmatchedQuote(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unmatched.quote", arg0);
        }
        
        /**
         * "unnamed.pkg.not.allowed.named.modules"
         */
        public static final Error UnnamedPkgNotAllowedNamedModules = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unnamed.pkg.not.allowed.named.modules");
        
        /**
         * "unreachable.stmt"
         */
        public static final Error UnreachableStmt = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreachable.stmt");
        
        /**
         * compiler.err.unreported.exception.default.constructor=\
         *    unreported exception {0} in default constructor
         */
        public static Error UnreportedExceptionDefaultConstructor(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreported.exception.default.constructor", arg0);
        }
        
        /**
         * compiler.err.unreported.exception.implicit.close=\
         *    unreported exception {0}; must be caught or declared to be thrown\n\
         *    exception thrown from implicit call to close() on resource variable ''{1}''
         */
        public static Error UnreportedExceptionImplicitClose(Type arg0, Name arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreported.exception.implicit.close", arg0, arg1);
        }
        
        /**
         * compiler.err.unreported.exception.need.to.catch.or.throw=\
         *    unreported exception {0}; must be caught or declared to be thrown
         */
        public static Error UnreportedExceptionNeedToCatchOrThrow(Type arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreported.exception.need.to.catch.or.throw", arg0);
        }
        
        /**
         * compiler.err.unsupported.encoding=\
         *    unsupported encoding: {0}
         */
        public static Error UnsupportedEncoding(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unsupported.encoding", arg0);
        }
        
        /**
         * compiler.err.unsupported.release.version=\
         *    release version {0} not supported
         */
        public static Error UnsupportedReleaseVersion(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unsupported.release.version", arg0);
        }
        
        /**
         * "use.of.underscore.not.allowed"
         */
        public static final Error UseOfUnderscoreNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "use.of.underscore.not.allowed");
        
        /**
         * "use.of.underscore.not.allowed.non.variable"
         */
        public static final Error UseOfUnderscoreNotAllowedNonVariable = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "use.of.underscore.not.allowed.non.variable");
        
        /**
         * "use.of.underscore.not.allowed.with.brackets"
         */
        public static final Error UseOfUnderscoreNotAllowedWithBrackets = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "use.of.underscore.not.allowed.with.brackets");
        
        /**
         * compiler.err.var.might.already.be.assigned=\
         *    variable {0} might already have been assigned
         */
        public static Error VarMightAlreadyBeAssigned(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.might.already.be.assigned", arg0);
        }
        
        /**
         * compiler.err.var.might.be.assigned.in.loop=\
         *    variable {0} might be assigned in loop
         */
        public static Error VarMightBeAssignedInLoop(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.might.be.assigned.in.loop", arg0);
        }
        
        /**
         * compiler.err.var.might.not.have.been.initialized=\
         *    variable {0} might not have been initialized
         */
        public static Error VarMightNotHaveBeenInitialized(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.might.not.have.been.initialized", arg0);
        }
        
        /**
         * compiler.err.var.not.initialized.in.default.constructor=\
         *    variable {0} not initialized in the default constructor
         */
        public static Error VarNotInitializedInDefaultConstructor(Symbol arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.not.initialized.in.default.constructor", arg0);
        }
        
        /**
         * "varargs.and.old.array.syntax"
         */
        public static final Error VarargsAndOldArraySyntax = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.and.old.array.syntax");
        
        /**
         * "varargs.and.receiver"
         */
        public static final Error VarargsAndReceiver = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.and.receiver");
        
        /**
         * compiler.err.varargs.invalid.trustme.anno=\
         *    Invalid {0} annotation. {1}
         */
        public static Error VarargsInvalidTrustmeAnno(Symbol arg0, JCDiagnostic arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.invalid.trustme.anno", arg0, arg1);
        }
        
        /**
         * compiler.err.varargs.invalid.trustme.anno=\
         *    Invalid {0} annotation. {1}
         */
        public static Error VarargsInvalidTrustmeAnno(Symbol arg0, Fragment arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.invalid.trustme.anno", arg0, arg1);
        }
        
        /**
         * "varargs.must.be.last"
         */
        public static final Error VarargsMustBeLast = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.must.be.last");
        
        /**
         * "variable.not.allowed"
         */
        public static final Error VariableNotAllowed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "variable.not.allowed");
        
        /**
         * "void.not.allowed.here"
         */
        public static final Error VoidNotAllowedHere = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "void.not.allowed.here");
        
        /**
         * "warnings.and.werror"
         */
        public static final Error WarningsAndWerror = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "warnings.and.werror");
        
        /**
         * compiler.err.wrong.number.type.args=\
         *    wrong number of type arguments; required {0}
         */
        public static Error WrongNumberTypeArgs(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "wrong.number.type.args", arg0);
        }
        
        /**
         * "wrong.receiver"
         */
        public static final Error WrongReceiver = new Error(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "wrong.receiver");
    }
    public static class Warnings {
        /**
         * compiler.warn.bad.name.for.option=\
         *    bad name in value for {0} option: ''{1}''
         */
        public static Warning BadNameForOption(Option arg0, String arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.name.for.option", arg0, arg1);
        }
        
        /**
         * compiler.warn.big.major.version=\
         *    {0}: major version {1} is newer than {2}, the highest major version supported by this compiler.\n\
         *    It is recommended that the compiler be upgraded.
         */
        public static Warning BigMajorVersion(File arg0, int arg1, int arg2) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "big.major.version", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.big.major.version=\
         *    {0}: major version {1} is newer than {2}, the highest major version supported by this compiler.\n\
         *    It is recommended that the compiler be upgraded.
         */
        public static Warning BigMajorVersion(JavaFileObject arg0, int arg1, int arg2) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "big.major.version", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.big.major.version=\
         *    {0}: major version {1} is newer than {2}, the highest major version supported by this compiler.\n\
         *    It is recommended that the compiler be upgraded.
         */
        public static Warning BigMajorVersion(Path arg0, int arg1, int arg2) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "big.major.version", arg0, arg1, arg2);
        }
        
        /**
         * "diamond.redundant.args"
         */
        public static final Warning DiamondRedundantArgs = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.redundant.args");
        
        /**
         * "doclint.not.available"
         */
        public static final Warning DoclintNotAvailable = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "doclint.not.available");
        
        /**
         * "extraneous.semicolon"
         */
        public static final Warning ExtraneousSemicolon = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "extraneous.semicolon");
        
        /**
         * compiler.warn.file.from.future=\
         *    Modification date is in the future for file {0}
         */
        public static Warning FileFromFuture(File arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.from.future", arg0);
        }
        
        /**
         * compiler.warn.file.from.future=\
         *    Modification date is in the future for file {0}
         */
        public static Warning FileFromFuture(JavaFileObject arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.from.future", arg0);
        }
        
        /**
         * compiler.warn.file.from.future=\
         *    Modification date is in the future for file {0}
         */
        public static Warning FileFromFuture(Path arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.from.future", arg0);
        }
        
        /**
         * compiler.warn.forward.ref=\
         *    reference to variable ''{0}'' before it has been initialized
         */
        public static Warning ForwardRef(Symbol arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "forward.ref", arg0);
        }
        
        /**
         * "illegal.char.for.encoding"
         */
        public static final Warning IllegalCharForEncoding = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.char.for.encoding");
        
        /**
         * compiler.warn.illegal.ref.to.restricted.type=\
         *    illegal reference to restricted type ''{0}''
         */
        public static Warning IllegalRefToRestrictedType(Name arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.ref.to.restricted.type", arg0);
        }
        
        /**
         * compiler.warn.inexact.non-varargs.call=\
         *    non-varargs call of varargs method with inexact argument type for last parameter;\n\
         *    cast to {0} for a varargs call\n\
         *    cast to {1} for a non-varargs call and to suppress this warning
         */
        public static Warning InexactNonVarargsCall(Type arg0, Type arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inexact.non-varargs.call", arg0, arg1);
        }
        
        /**
         * compiler.warn.invalid.utf8.in.classfile=\
         *    {0}: classfile contains invalid UTF-8: {1}
         */
        public static Warning InvalidUtf8InClassfile(File arg0, Fragment arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.utf8.in.classfile", arg0, arg1);
        }
        
        /**
         * compiler.warn.invalid.utf8.in.classfile=\
         *    {0}: classfile contains invalid UTF-8: {1}
         */
        public static Warning InvalidUtf8InClassfile(JavaFileObject arg0, Fragment arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.utf8.in.classfile", arg0, arg1);
        }
        
        /**
         * compiler.warn.invalid.utf8.in.classfile=\
         *    {0}: classfile contains invalid UTF-8: {1}
         */
        public static Warning InvalidUtf8InClassfile(Path arg0, Fragment arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.utf8.in.classfile", arg0, arg1);
        }
        
        /**
         * "invalid.yield"
         */
        public static final Warning InvalidYield = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.yield");
        
        /**
         * compiler.warn.lintOption=\
         *    [{0}]\u0020
         */
        public static Warning LintOption(Option arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lintOption", arg0);
        }
        
        /**
         * "local.redundant.type"
         */
        public static final Warning LocalRedundantType = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.redundant.type");
        
        /**
         * "method.redundant.typeargs"
         */
        public static final Warning MethodRedundantTypeargs = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "method.redundant.typeargs");
        
        /**
         * compiler.warn.override.bridge=\
         *    {0}; overridden method is a bridge method
         */
        public static Warning OverrideBridge(JCDiagnostic arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.bridge", arg0);
        }
        
        /**
         * compiler.warn.override.bridge=\
         *    {0}; overridden method is a bridge method
         */
        public static Warning OverrideBridge(Fragment arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "override.bridge", arg0);
        }
        
        /**
         * compiler.warn.pkg-info.already.seen=\
         *    a package-info.java file has already been seen for package {0}
         */
        public static Warning PkgInfoAlreadySeen(Symbol arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "pkg-info.already.seen", arg0);
        }
        
        /**
         * compiler.warn.position.overflow=\
         *    Position encoding overflows at line {0}
         */
        public static Warning PositionOverflow(int arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "position.overflow", arg0);
        }
        
        /**
         * "potential.lambda.found"
         */
        public static final Warning PotentialLambdaFound = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "potential.lambda.found");
        
        /**
         * compiler.warn.proc.file.create.last.round=\
         *    File for type ''{0}'' created in the last round will not be subject to annotation processing.
         */
        public static Warning ProcFileCreateLastRound(String arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.file.create.last.round", arg0);
        }
        
        /**
         * compiler.warn.proc.messager=\
         *    {0}
         */
        public static Warning ProcMessager(String arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.messager", arg0);
        }
        
        /**
         * compiler.warn.proc.package.does.not.exist=\
         *    package {0} does not exist
         */
        public static Warning ProcPackageDoesNotExist(String arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.package.does.not.exist", arg0);
        }
        
        /**
         * "proc.proc-only.requested.no.procs"
         */
        public static final Warning ProcProcOnlyRequestedNoProcs = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.proc-only.requested.no.procs");
        
        /**
         * compiler.warn.proc.processor.incompatible.source.version=\
         *    Supported source version ''{0}'' from annotation processor ''{1}'' less than -source ''{2}''
         */
        public static Warning ProcProcessorIncompatibleSourceVersion(SourceVersion arg0, String arg1, String arg2) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.processor.incompatible.source.version", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.proc.unclosed.type.files=\
         *    Unclosed files for the types ''{0}''; these types will not undergo annotation processing
         */
        public static Warning ProcUnclosedTypeFiles(Set<? extends String> arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.unclosed.type.files", arg0);
        }
        
        /**
         * compiler.warn.proc.unmatched.processor.options=\
         *    The following options were not recognized by any processor: ''{0}''
         */
        public static Warning ProcUnmatchedProcessorOptions(String arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.unmatched.processor.options", arg0);
        }
        
        /**
         * "proc.use.implicit"
         */
        public static final Warning ProcUseImplicit = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.use.implicit");
        
        /**
         * "proc.use.proc.or.implicit"
         */
        public static final Warning ProcUseProcOrImplicit = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.use.proc.or.implicit");
        
        /**
         * compiler.warn.profile.target.conflict=\
         *    profile {0} is not valid for target release {1}
         */
        public static Warning ProfileTargetConflict(Profile arg0, Target arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "profile.target.conflict", arg0, arg1);
        }
        
        /**
         * compiler.warn.restricted.type.not.allowed=\
         *    as of release {1}, ''{0}'' is a restricted type name and cannot be used for type declarations or as the element type of an array
         */
        public static Warning RestrictedTypeNotAllowed(Name arg0, Source arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed", arg0, arg1);
        }
        
        /**
         * compiler.warn.restricted.type.not.allowed.preview=\
         *    ''{0}'' may become a restricted type name in a future release and may be unusable for type declarations or as the element type of an array
         */
        public static Warning RestrictedTypeNotAllowedPreview(Name arg0, Source arg1) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "restricted.type.not.allowed.preview", arg0, arg1);
        }
        
        /**
         * compiler.warn.self.ref=\
         *    self-reference in initializer of variable ''{0}''
         */
        public static Warning SelfRef(Symbol arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "self.ref", arg0);
        }
        
        /**
         * compiler.warn.service.provided.but.not.exported.or.used=\
         *    service interface provided but not exported or used
         */
        public static Warning ServiceProvidedButNotExportedOrUsed(Symbol arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "service.provided.but.not.exported.or.used", arg0);
        }
        
        /**
         * compiler.warn.sun.proprietary=\
         *    {0} is internal proprietary API and may be removed in a future release
         */
        public static Warning SunProprietary(Symbol arg0) {
            return new Warning(EnumSet.of(STRICT), "compiler", "sun.proprietary", arg0);
        }
        
        /**
         * "underscore.as.identifier"
         */
        public static final Warning UnderscoreAsIdentifier = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "underscore.as.identifier");
        
        /**
         * compiler.warn.unreachable.catch=\
         *    unreachable catch clause\n\
         *    thrown type {0} has already been caught
         */
        public static Warning UnreachableCatch(List<? extends Type> arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreachable.catch", arg0);
        }
        
        /**
         * compiler.warn.unreachable.catch.1=\
         *    unreachable catch clause\n\
         *    thrown types {0} have already been caught
         */
        public static Warning UnreachableCatch1(List<? extends Type> arg0) {
            return new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unreachable.catch.1", arg0);
        }
        
        /**
         * "warning"
         */
        public static final Warning Warning = new Warning(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "warning");
    }
    public static class LintWarnings {
        /**
         * "OSF.array.SPF"
         */
        public static final LintWarning OSFArraySPF = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "OSF.array.SPF");
        
        /**
         * "SPF.null.init"
         */
        public static final LintWarning SPFNullInit = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "SPF.null.init");
        
        /**
         * compiler.warn.access.to.member.from.serializable.element=\
         *    access to member {0} from serializable element can be publicly accessible to untrusted code
         */
        public static LintWarning AccessToMemberFromSerializableElement(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "access.to.member.from.serializable.element", arg0);
        }
        
        /**
         * compiler.warn.access.to.member.from.serializable.lambda=\
         *    access to member {0} from serializable lambda can be publicly accessible to untrusted code
         */
        public static LintWarning AccessToMemberFromSerializableLambda(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "access.to.member.from.serializable.lambda", arg0);
        }
        
        /**
         * "addopens.ignored"
         */
        public static final LintWarning AddopensIgnored = new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "addopens.ignored");
        
        /**
         * compiler.warn.annotation.method.not.found=\
         *    Cannot find annotation method ''{1}()'' in type ''{0}''
         */
        public static LintWarning AnnotationMethodNotFound(Type arg0, Name arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "annotation.method.not.found", arg0, arg1);
        }
        
        /**
         * compiler.warn.annotation.method.not.found.reason=\
         *    Cannot find annotation method ''{1}()'' in type ''{0}'': {2}
         */
        public static LintWarning AnnotationMethodNotFoundReason(Type arg0, Name arg1, JCDiagnostic arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "annotation.method.not.found.reason", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.annotation.method.not.found.reason=\
         *    Cannot find annotation method ''{1}()'' in type ''{0}'': {2}
         */
        public static LintWarning AnnotationMethodNotFoundReason(Type arg0, Name arg1, Fragment arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "annotation.method.not.found.reason", arg0, arg1, arg2);
        }
        
        /**
         * "attempt.to.synchronize.on.instance.of.value.based.class"
         */
        public static final LintWarning AttemptToSynchronizeOnInstanceOfValueBasedClass = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.IDENTITY, "compiler", "attempt.to.synchronize.on.instance.of.value.based.class");
        
        /**
         * "attempt.to.use.value.based.where.identity.expected"
         */
        public static final LintWarning AttemptToUseValueBasedWhereIdentityExpected = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.IDENTITY, "compiler", "attempt.to.use.value.based.where.identity.expected");
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Symbol arg0, File arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Symbol arg0, JavaFileObject arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Symbol arg0, Path arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Type arg0, File arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Type arg0, JavaFileObject arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.auxiliary.class.accessed.from.outside.of.its.source.file=\
         *    auxiliary class {0} in {1} should not be accessed from outside its own source file
         */
        public static LintWarning AuxiliaryClassAccessedFromOutsideOfItsSourceFile(Type arg0, Path arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.AUXILIARYCLASS, "compiler", "auxiliary.class.accessed.from.outside.of.its.source.file", arg0, arg1);
        }
        
        /**
         * compiler.warn.bit.shift.out.of.range=\
         *    shifting {0} by {1} bits is equivalent to shifting by {2} bit(s)
         */
        public static LintWarning BitShiftOutOfRange(Type arg0, long arg1, int arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.LOSSY_CONVERSIONS, "compiler", "bit.shift.out.of.range", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.constant.SVUID=\
         *    serialVersionUID must be constant in class {0}
         */
        public static LintWarning ConstantSVUID(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "constant.SVUID", arg0);
        }
        
        /**
         * "dangling.doc.comment"
         */
        public static final LintWarning DanglingDocComment = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.DANGLING_DOC_COMMENTS, "compiler", "dangling.doc.comment");
        
        /**
         * compiler.warn.declared.using.preview=\
         *    {0} {1} is declared using a preview feature, which may be removed in a future release.
         */
        public static LintWarning DeclaredUsingPreview(KindName arg0, Symbol arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "declared.using.preview", arg0, arg1);
        }
        
        /**
         * "default.ineffective"
         */
        public static final LintWarning DefaultIneffective = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "default.ineffective");
        
        /**
         * compiler.warn.deprecated.annotation.has.no.effect=\
         *    @Deprecated annotation has no effect on this {0} declaration
         */
        public static LintWarning DeprecatedAnnotationHasNoEffect(KindName arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.DEPRECATION, "compiler", "deprecated.annotation.has.no.effect", arg0);
        }
        
        /**
         * compiler.warn.dir.path.element.not.directory=\
         *    bad path element "{0}": not a directory
         */
        public static LintWarning DirPathElementNotDirectory(File arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "dir.path.element.not.directory", arg0);
        }
        
        /**
         * compiler.warn.dir.path.element.not.directory=\
         *    bad path element "{0}": not a directory
         */
        public static LintWarning DirPathElementNotDirectory(JavaFileObject arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "dir.path.element.not.directory", arg0);
        }
        
        /**
         * compiler.warn.dir.path.element.not.directory=\
         *    bad path element "{0}": not a directory
         */
        public static LintWarning DirPathElementNotDirectory(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "dir.path.element.not.directory", arg0);
        }
        
        /**
         * compiler.warn.dir.path.element.not.found=\
         *    bad path element "{0}": no such directory
         */
        public static LintWarning DirPathElementNotFound(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "dir.path.element.not.found", arg0);
        }
        
        /**
         * "div.zero"
         */
        public static final LintWarning DivZero = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.DIVZERO, "compiler", "div.zero");
        
        /**
         * "empty.if"
         */
        public static final LintWarning EmptyIf = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.EMPTY, "compiler", "empty.if");
        
        /**
         * "externalizable.missing.public.no.arg.ctor"
         */
        public static final LintWarning ExternalizableMissingPublicNoArgCtor = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "externalizable.missing.public.no.arg.ctor");
        
        /**
         * "finally.cannot.complete"
         */
        public static final LintWarning FinallyCannotComplete = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.FINALLY, "compiler", "finally.cannot.complete");
        
        /**
         * compiler.warn.future.attr=\
         *    {0} attribute introduced in version {1}.{2} class files is ignored in version {3}.{4} class files
         */
        public static LintWarning FutureAttr(Name arg0, int arg1, int arg2, int arg3, int arg4) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "future.attr", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.warn.has.been.deprecated=\
         *    {0} in {1} has been deprecated
         */
        public static LintWarning HasBeenDeprecated(Symbol arg0, Symbol arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED, DEPRECATION_SENSITIVE), LintCategory.DEPRECATION, "compiler", "has.been.deprecated", arg0, arg1);
        }
        
        /**
         * compiler.warn.has.been.deprecated.for.removal=\
         *    {0} in {1} has been deprecated and marked for removal
         */
        public static LintWarning HasBeenDeprecatedForRemoval(Symbol arg0, Symbol arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.REMOVAL, "compiler", "has.been.deprecated.for.removal", arg0, arg1);
        }
        
        /**
         * compiler.warn.has.been.deprecated.for.removal.module=\
         *    module {0} has been deprecated and marked for removal
         */
        public static LintWarning HasBeenDeprecatedForRemovalModule(Symbol arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.REMOVAL, "compiler", "has.been.deprecated.for.removal.module", arg0);
        }
        
        /**
         * compiler.warn.has.been.deprecated.module=\
         *    module {0} has been deprecated
         */
        public static LintWarning HasBeenDeprecatedModule(Symbol arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.DEPRECATION, "compiler", "has.been.deprecated.module", arg0);
        }
        
        /**
         * "improper.SPF"
         */
        public static final LintWarning ImproperSPF = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "improper.SPF");
        
        /**
         * compiler.warn.improper.SVUID=\
         *    serialVersionUID must be declared static final in class {0}
         */
        public static LintWarning ImproperSVUID(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "improper.SVUID", arg0);
        }
        
        /**
         * compiler.warn.inconsistent.inner.classes=\
         *    InnerClasses attribute for {0} in {1} inconsistent with source code\n\
         *    ({1} may need to be recompiled with {0})
         */
        public static LintWarning InconsistentInnerClasses(Symbol arg0, JavaFileObject arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "inconsistent.inner.classes", arg0, arg1);
        }
        
        /**
         * "inconsistent.white.space.indentation"
         */
        public static final LintWarning InconsistentWhiteSpaceIndentation = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TEXT_BLOCKS, "compiler", "inconsistent.white.space.indentation");
        
        /**
         * compiler.warn.incubating.modules=\
         *    using incubating module(s): {0}
         */
        public static LintWarning IncubatingModules(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.INCUBATING, "compiler", "incubating.modules", arg0);
        }
        
        /**
         * compiler.warn.ineffectual.extern.method.enum=\
         *    externalization-related method {0} is not effective in an enum class
         */
        public static LintWarning IneffectualExternMethodEnum(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.extern.method.enum", arg0);
        }
        
        /**
         * compiler.warn.ineffectual.externalizable.method.record=\
         *    externalization-related method {0} is not effective in a record class
         */
        public static LintWarning IneffectualExternalizableMethodRecord(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.externalizable.method.record", arg0);
        }
        
        /**
         * compiler.warn.ineffectual.serial.field.enum=\
         *     serialization-related field {0} is not effective in an enum class
         */
        public static LintWarning IneffectualSerialFieldEnum(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.field.enum", arg0);
        }
        
        /**
         * "ineffectual.serial.field.externalizable"
         */
        public static final LintWarning IneffectualSerialFieldExternalizable = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.field.externalizable");
        
        /**
         * "ineffectual.serial.field.interface"
         */
        public static final LintWarning IneffectualSerialFieldInterface = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.field.interface");
        
        /**
         * "ineffectual.serial.field.record"
         */
        public static final LintWarning IneffectualSerialFieldRecord = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.field.record");
        
        /**
         * compiler.warn.ineffectual.serial.method.enum=\
         *    serialization-related method {0} is not effective in an enum class
         */
        public static LintWarning IneffectualSerialMethodEnum(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.method.enum", arg0);
        }
        
        /**
         * compiler.warn.ineffectual.serial.method.externalizable=\
         *    serialization-related method {0} is not effective in an Externalizable class
         */
        public static LintWarning IneffectualSerialMethodExternalizable(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.method.externalizable", arg0);
        }
        
        /**
         * compiler.warn.ineffectual.serial.method.record=\
         *    serialization-related method {0} is not effective in a record class
         */
        public static LintWarning IneffectualSerialMethodRecord(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "ineffectual.serial.method.record", arg0);
        }
        
        /**
         * compiler.warn.invalid.archive.file=\
         *    Unexpected file on path: {0}
         */
        public static LintWarning InvalidArchiveFile(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "invalid.archive.file", arg0);
        }
        
        /**
         * compiler.warn.invalid.path=\
         *    Invalid filename: {0}
         */
        public static LintWarning InvalidPath(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "invalid.path", arg0);
        }
        
        /**
         * compiler.warn.is.preview=\
         *    {0} is a preview API and may be removed in a future release.
         */
        public static LintWarning IsPreview(Symbol arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "is.preview", arg0);
        }
        
        /**
         * compiler.warn.is.preview.reflective=\
         *    {0} is a reflective preview API and may be removed in a future release.
         */
        public static LintWarning IsPreviewReflective(Symbol arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "is.preview.reflective", arg0);
        }
        
        /**
         * compiler.warn.leaks.not.accessible=\
         *    {0} {1} in module {2} is not accessible to clients that require this module
         */
        public static LintWarning LeaksNotAccessible(KindName arg0, Symbol arg1, Symbol arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.EXPORTS, "compiler", "leaks.not.accessible", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.leaks.not.accessible.not.required.transitive=\
         *    {0} {1} in module {2} is not indirectly exported using ''requires transitive''
         */
        public static LintWarning LeaksNotAccessibleNotRequiredTransitive(KindName arg0, Symbol arg1, Symbol arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.EXPORTS, "compiler", "leaks.not.accessible.not.required.transitive", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.leaks.not.accessible.unexported=\
         *    {0} {1} in module {2} is not exported
         */
        public static LintWarning LeaksNotAccessibleUnexported(KindName arg0, Symbol arg1, Symbol arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.EXPORTS, "compiler", "leaks.not.accessible.unexported", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.leaks.not.accessible.unexported.qualified=\
         *    {0} {1} in module {2} may not be visible to all clients that require this module
         */
        public static LintWarning LeaksNotAccessibleUnexportedQualified(KindName arg0, Symbol arg1, Symbol arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.EXPORTS, "compiler", "leaks.not.accessible.unexported.qualified", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.locn.unknown.file.on.module.path=\
         *    unknown file on module path: {0}
         */
        public static LintWarning LocnUnknownFileOnModulePath(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "locn.unknown.file.on.module.path", arg0);
        }
        
        /**
         * compiler.warn.long.SVUID=\
         *    serialVersionUID must be of type long in class {0}
         */
        public static LintWarning LongSVUID(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "long.SVUID", arg0);
        }
        
        /**
         * compiler.warn.missing-explicit-ctor=\
         *    class {0} in exported package {1} declares no explicit constructors, thereby exposing a default constructor to clients of module {2}
         */
        public static LintWarning MissingExplicitCtor(Symbol arg0, Symbol arg1, Symbol arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.MISSING_EXPLICIT_CTOR, "compiler", "missing-explicit-ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.missing.SVUID=\
         *    serializable class {0} has no definition of serialVersionUID
         */
        public static LintWarning MissingSVUID(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "missing.SVUID", arg0);
        }
        
        /**
         * "missing.deprecated.annotation"
         */
        public static final LintWarning MissingDeprecatedAnnotation = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.DEP_ANN, "compiler", "missing.deprecated.annotation");
        
        /**
         * compiler.warn.module.for.option.not.found=\
         *    module name in {0} option not found: {1}
         */
        public static LintWarning ModuleForOptionNotFound(Option arg0, Symbol arg1) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "module.for.option.not.found", arg0, arg1);
        }
        
        /**
         * compiler.warn.module.not.found=\
         *    module not found: {0}
         */
        public static LintWarning ModuleNotFound(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.MODULE, "compiler", "module.not.found", arg0);
        }
        
        /**
         * "non.private.method.weaker.access"
         */
        public static final LintWarning NonPrivateMethodWeakerAccess = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "non.private.method.weaker.access");
        
        /**
         * "non.serializable.instance.field"
         */
        public static final LintWarning NonSerializableInstanceField = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "non.serializable.instance.field");
        
        /**
         * compiler.warn.non.serializable.instance.field.array=\
         *    non-transient instance field of a serializable class declared with an array having a non-serializable base component type {0}
         */
        public static LintWarning NonSerializableInstanceFieldArray(Type arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "non.serializable.instance.field.array", arg0);
        }
        
        /**
         * compiler.warn.option.obsolete.source=\
         *    source value {0} is obsolete and will be removed in a future release
         */
        public static LintWarning OptionObsoleteSource(String arg0) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "option.obsolete.source", arg0);
        }
        
        /**
         * "option.obsolete.suppression"
         */
        public static final LintWarning OptionObsoleteSuppression = new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "option.obsolete.suppression");
        
        /**
         * compiler.warn.option.obsolete.target=\
         *    target value {0} is obsolete and will be removed in a future release
         */
        public static LintWarning OptionObsoleteTarget(Target arg0) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "option.obsolete.target", arg0);
        }
        
        /**
         * compiler.warn.outdir.is.in.exploded.module=\
         *    the output directory is within an exploded module: {0}
         */
        public static LintWarning OutdirIsInExplodedModule(Path arg0) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.PATH, "compiler", "outdir.is.in.exploded.module", arg0);
        }
        
        /**
         * compiler.warn.output.file.clash=\
         *    output file written more than once: {0}
         */
        public static LintWarning OutputFileClash(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OUTPUT_FILE_CLASH, "compiler", "output.file.clash", arg0);
        }
        
        /**
         * compiler.warn.override.equals.but.not.hashcode=\
         *    Class {0} overrides equals, but neither it nor any superclass overrides hashCode method
         */
        public static LintWarning OverrideEqualsButNotHashcode(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERRIDES, "compiler", "override.equals.but.not.hashcode", arg0);
        }
        
        /**
         * compiler.warn.override.unchecked.ret=\
         *    {0}\n\
         *    return type requires unchecked conversion from {1} to {2}
         */
        public static LintWarning OverrideUncheckedRet(JCDiagnostic arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "override.unchecked.ret", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.override.unchecked.ret=\
         *    {0}\n\
         *    return type requires unchecked conversion from {1} to {2}
         */
        public static LintWarning OverrideUncheckedRet(Fragment arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "override.unchecked.ret", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.override.unchecked.thrown=\
         *    {0}\n\
         *    overridden method does not throw {1}
         */
        public static LintWarning OverrideUncheckedThrown(JCDiagnostic arg0, Type arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "override.unchecked.thrown", arg0, arg1);
        }
        
        /**
         * compiler.warn.override.unchecked.thrown=\
         *    {0}\n\
         *    overridden method does not throw {1}
         */
        public static LintWarning OverrideUncheckedThrown(Fragment arg0, Type arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "override.unchecked.thrown", arg0, arg1);
        }
        
        /**
         * compiler.warn.override.varargs.extra=\
         *    {0}; overriding method is missing ''...''
         */
        public static LintWarning OverrideVarargsExtra(JCDiagnostic arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERRIDES, "compiler", "override.varargs.extra", arg0);
        }
        
        /**
         * compiler.warn.override.varargs.extra=\
         *    {0}; overriding method is missing ''...''
         */
        public static LintWarning OverrideVarargsExtra(Fragment arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERRIDES, "compiler", "override.varargs.extra", arg0);
        }
        
        /**
         * compiler.warn.override.varargs.missing=\
         *    {0}; overridden method has no ''...''
         */
        public static LintWarning OverrideVarargsMissing(JCDiagnostic arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERRIDES, "compiler", "override.varargs.missing", arg0);
        }
        
        /**
         * compiler.warn.override.varargs.missing=\
         *    {0}; overridden method has no ''...''
         */
        public static LintWarning OverrideVarargsMissing(Fragment arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERRIDES, "compiler", "override.varargs.missing", arg0);
        }
        
        /**
         * compiler.warn.package.empty.or.not.found=\
         *    package is empty or does not exist: {0}
         */
        public static LintWarning PackageEmptyOrNotFound(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OPENS, "compiler", "package.empty.or.not.found", arg0);
        }
        
        /**
         * compiler.warn.path.element.not.found=\
         *    bad path element "{0}": no such file or directory
         */
        public static LintWarning PathElementNotFound(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "path.element.not.found", arg0);
        }
        
        /**
         * compiler.warn.poor.choice.for.module.name=\
         *    module name component {0} should avoid terminal digits
         */
        public static LintWarning PoorChoiceForModuleName(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.MODULE, "compiler", "poor.choice.for.module.name", arg0);
        }
        
        /**
         * "possible.fall-through.into.case"
         */
        public static final LintWarning PossibleFallThroughIntoCase = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.FALLTHROUGH, "compiler", "possible.fall-through.into.case");
        
        /**
         * compiler.warn.possible.loss.of.precision=\
         *    implicit cast from {0} to {1} in compound assignment is possibly lossy
         */
        public static LintWarning PossibleLossOfPrecision(Type arg0, Type arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.LOSSY_CONVERSIONS, "compiler", "possible.loss.of.precision", arg0, arg1);
        }
        
        /**
         * "possible.this.escape"
         */
        public static final LintWarning PossibleThisEscape = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.THIS_ESCAPE, "compiler", "possible.this.escape");
        
        /**
         * "possible.this.escape.location"
         */
        public static final LintWarning PossibleThisEscapeLocation = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.THIS_ESCAPE, "compiler", "possible.this.escape.location");
        
        /**
         * compiler.warn.potentially.ambiguous.overload=\
         *    {0} in {1} is potentially ambiguous with {2} in {3}
         */
        public static LintWarning PotentiallyAmbiguousOverload(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.OVERLOADS, "compiler", "potentially.ambiguous.overload", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.warn.preview.feature.use=\
         *   {0} is a preview feature and may be removed in a future release.
         */
        public static LintWarning PreviewFeatureUse(JCDiagnostic arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "preview.feature.use", arg0);
        }
        
        /**
         * compiler.warn.preview.feature.use=\
         *   {0} is a preview feature and may be removed in a future release.
         */
        public static LintWarning PreviewFeatureUse(Fragment arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "preview.feature.use", arg0);
        }
        
        /**
         * compiler.warn.preview.feature.use.classfile=\
         *   class file for {0} uses preview features of Java SE {1}.
         */
        public static LintWarning PreviewFeatureUseClassfile(JavaFileObject arg0, String arg1) {
            return new LintWarning(EnumSet.of(MANDATORY), LintCategory.PREVIEW, "compiler", "preview.feature.use.classfile", arg0, arg1);
        }
        
        /**
         * compiler.warn.preview.feature.use.plural=\
         *   {0} are a preview feature and may be removed in a future release.
         */
        public static LintWarning PreviewFeatureUsePlural(JCDiagnostic arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "preview.feature.use.plural", arg0);
        }
        
        /**
         * compiler.warn.preview.feature.use.plural=\
         *   {0} are a preview feature and may be removed in a future release.
         */
        public static LintWarning PreviewFeatureUsePlural(Fragment arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.PREVIEW, "compiler", "preview.feature.use.plural", arg0);
        }
        
        /**
         * compiler.warn.prob.found.req=\
         *    {0}\n\
         *    required: {2}\n\
         *    found:    {1}
         */
        public static LintWarning ProbFoundReq(JCDiagnostic arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "prob.found.req", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.prob.found.req=\
         *    {0}\n\
         *    required: {2}\n\
         *    found:    {1}
         */
        public static LintWarning ProbFoundReq(Fragment arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "prob.found.req", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.proc.annotations.without.processors=\
         *    No processor claimed any of these annotations: {0}
         */
        public static LintWarning ProcAnnotationsWithoutProcessors(Set<? extends String> arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.annotations.without.processors", arg0);
        }
        
        /**
         * compiler.warn.proc.duplicate.option.name=\
         *    Duplicate supported option ''{0}'' returned by annotation processor ''{1}''
         */
        public static LintWarning ProcDuplicateOptionName(String arg0, String arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.duplicate.option.name", arg0, arg1);
        }
        
        /**
         * compiler.warn.proc.duplicate.supported.annotation=\
         *    Duplicate supported annotation interface ''{0}'' returned by annotation processor ''{1}''
         */
        public static LintWarning ProcDuplicateSupportedAnnotation(String arg0, String arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.duplicate.supported.annotation", arg0, arg1);
        }
        
        /**
         * compiler.warn.proc.file.reopening=\
         *    Attempt to create a file for ''{0}'' multiple times
         */
        public static LintWarning ProcFileReopening(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.file.reopening", arg0);
        }
        
        /**
         * compiler.warn.proc.illegal.file.name=\
         *    Cannot create file for illegal name ''{0}''.
         */
        public static LintWarning ProcIllegalFileName(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.illegal.file.name", arg0);
        }
        
        /**
         * compiler.warn.proc.malformed.supported.string=\
         *    Malformed string ''{0}'' for a supported annotation interface returned by processor ''{1}''
         */
        public static LintWarning ProcMalformedSupportedString(String arg0, String arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.malformed.supported.string", arg0, arg1);
        }
        
        /**
         * compiler.warn.proc.redundant.types.with.wildcard=\
         *    Annotation processor ''{0}'' redundantly supports both ''*'' and other annotation interfaces
         */
        public static LintWarning ProcRedundantTypesWithWildcard(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.redundant.types.with.wildcard", arg0);
        }
        
        /**
         * compiler.warn.proc.suspicious.class.name=\
         *    Creating file for a type whose name ends in {1}: ''{0}''
         */
        public static LintWarning ProcSuspiciousClassName(String arg0, String arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.suspicious.class.name", arg0, arg1);
        }
        
        /**
         * compiler.warn.proc.type.already.exists=\
         *    A file for type ''{0}'' already exists on the sourcepath or classpath
         */
        public static LintWarning ProcTypeAlreadyExists(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.type.already.exists", arg0);
        }
        
        /**
         * compiler.warn.proc.type.recreate=\
         *    Attempt to create a file for type ''{0}'' multiple times
         */
        public static LintWarning ProcTypeRecreate(String arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PROCESSING, "compiler", "proc.type.recreate", arg0);
        }
        
        /**
         * compiler.warn.raw.class.use=\
         *    found raw type: {0}\n\
         *    missing type arguments for generic class {1}
         */
        public static LintWarning RawClassUse(Type arg0, Type arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.RAWTYPES, "compiler", "raw.class.use", arg0, arg1);
        }
        
        /**
         * compiler.warn.redundant.cast=\
         *    redundant cast to {0}
         */
        public static LintWarning RedundantCast(Type arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CAST, "compiler", "redundant.cast", arg0);
        }
        
        /**
         * "requires.automatic"
         */
        public static final LintWarning RequiresAutomatic = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.REQUIRES_AUTOMATIC, "compiler", "requires.automatic");
        
        /**
         * "requires.transitive.automatic"
         */
        public static final LintWarning RequiresTransitiveAutomatic = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.REQUIRES_TRANSITIVE_AUTOMATIC, "compiler", "requires.transitive.automatic");
        
        /**
         * compiler.warn.restricted.method=\
         *    {0}.{1} is a restricted method.\n\
         *    (Restricted methods are unsafe and, if used incorrectly, might crash the Java runtime or corrupt memory)
         */
        public static LintWarning RestrictedMethod(Symbol arg0, Symbol arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.RESTRICTED, "compiler", "restricted.method", arg0, arg1);
        }
        
        /**
         * compiler.warn.runtime.invisible.parameter.annotations=\
         *    the RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations attributes \
         *    in: {0} \
         *    cannot be mapped to the method''s parameters
         */
        public static LintWarning RuntimeInvisibleParameterAnnotations(File arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.invisible.parameter.annotations", arg0);
        }
        
        /**
         * compiler.warn.runtime.invisible.parameter.annotations=\
         *    the RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations attributes \
         *    in: {0} \
         *    cannot be mapped to the method''s parameters
         */
        public static LintWarning RuntimeInvisibleParameterAnnotations(JavaFileObject arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.invisible.parameter.annotations", arg0);
        }
        
        /**
         * compiler.warn.runtime.invisible.parameter.annotations=\
         *    the RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations attributes \
         *    in: {0} \
         *    cannot be mapped to the method''s parameters
         */
        public static LintWarning RuntimeInvisibleParameterAnnotations(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.invisible.parameter.annotations", arg0);
        }
        
        /**
         * compiler.warn.runtime.visible.invisible.param.annotations.mismatch=\
         *    the length of parameters in RuntimeVisibleParameterAnnotations attribute and \
         *    RuntimeInvisibleParameterAnnotations attribute in: {0} \
         *    do not match, ignoring both attributes
         */
        public static LintWarning RuntimeVisibleInvisibleParamAnnotationsMismatch(File arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.visible.invisible.param.annotations.mismatch", arg0);
        }
        
        /**
         * compiler.warn.runtime.visible.invisible.param.annotations.mismatch=\
         *    the length of parameters in RuntimeVisibleParameterAnnotations attribute and \
         *    RuntimeInvisibleParameterAnnotations attribute in: {0} \
         *    do not match, ignoring both attributes
         */
        public static LintWarning RuntimeVisibleInvisibleParamAnnotationsMismatch(JavaFileObject arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.visible.invisible.param.annotations.mismatch", arg0);
        }
        
        /**
         * compiler.warn.runtime.visible.invisible.param.annotations.mismatch=\
         *    the length of parameters in RuntimeVisibleParameterAnnotations attribute and \
         *    RuntimeInvisibleParameterAnnotations attribute in: {0} \
         *    do not match, ignoring both attributes
         */
        public static LintWarning RuntimeVisibleInvisibleParamAnnotationsMismatch(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "runtime.visible.invisible.param.annotations.mismatch", arg0);
        }
        
        /**
         * compiler.warn.serial.concrete.instance.method=\
         *    serialization-related method {0} must be a concrete instance method to be effective, neither abstract nor static
         */
        public static LintWarning SerialConcreteInstanceMethod(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.concrete.instance.method", arg0);
        }
        
        /**
         * compiler.warn.serial.method.no.args=\
         *    to be effective serialization-related method {0} must have no parameters
         */
        public static LintWarning SerialMethodNoArgs(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.no.args", arg0);
        }
        
        /**
         * compiler.warn.serial.method.not.private=\
         *    serialization-related method {0} not declared private
         */
        public static LintWarning SerialMethodNotPrivate(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.not.private", arg0);
        }
        
        /**
         * compiler.warn.serial.method.one.arg=\
         *    to be effective serialization-related method {0} must have exactly one parameter rather than {1} parameters
         */
        public static LintWarning SerialMethodOneArg(Name arg0, int arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.one.arg", arg0, arg1);
        }
        
        /**
         * compiler.warn.serial.method.parameter.type=\
         *    sole parameter of serialization-related method {0} must have type {1} to be effective rather than type {2}
         */
        public static LintWarning SerialMethodParameterType(Name arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.parameter.type", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.serial.method.static=\
         *    serialization-related method {0} declared static; must instead be an instance method to be effective
         */
        public static LintWarning SerialMethodStatic(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.static", arg0);
        }
        
        /**
         * compiler.warn.serial.method.unexpected.exception=\
         *    serialization-related method {0} declared to throw an unexpected type {1}
         */
        public static LintWarning SerialMethodUnexpectedException(Name arg0, Type arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.unexpected.exception", arg0, arg1);
        }
        
        /**
         * compiler.warn.serial.method.unexpected.return.type=\
         *    serialization-related method {0} declared with a return type of {1} rather than expected type {2}.\n\
         *    As declared, the method will be ineffective for serialization
         */
        public static LintWarning SerialMethodUnexpectedReturnType(Name arg0, Type arg1, Type arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serial.method.unexpected.return.type", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.serializable.missing.access.no.arg.ctor=\
         *    cannot access a no-arg constructor in first non-serializable superclass {0}
         */
        public static LintWarning SerializableMissingAccessNoArgCtor(Name arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.SERIAL, "compiler", "serializable.missing.access.no.arg.ctor", arg0);
        }
        
        /**
         * compiler.warn.source.no.bootclasspath=\
         *    bootstrap class path is not set in conjunction with -source {0}\n{1}
         */
        public static LintWarning SourceNoBootclasspath(String arg0, Fragment arg1) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "source.no.bootclasspath", arg0, arg1);
        }
        
        /**
         * compiler.warn.source.no.system.modules.path=\
         *    location of system modules is not set in conjunction with -source {0}\n{1}
         */
        public static LintWarning SourceNoSystemModulesPath(String arg0, Fragment arg1) {
            return new LintWarning(EnumSet.of(DEFAULT_ENABLED), LintCategory.OPTIONS, "compiler", "source.no.system.modules.path", arg0, arg1);
        }
        
        /**
         * compiler.warn.static.not.qualified.by.type=\
         *    static {0} should be qualified by type name, {1}, instead of by an expression
         */
        public static LintWarning StaticNotQualifiedByType(KindName arg0, Symbol arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.STATIC, "compiler", "static.not.qualified.by.type", arg0, arg1);
        }
        
        /**
         * compiler.warn.static.not.qualified.by.type2=\
         *    static {0} should not be used as a member of an anonymous class
         */
        public static LintWarning StaticNotQualifiedByType2(KindName arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.STATIC, "compiler", "static.not.qualified.by.type2", arg0);
        }
        
        /**
         * "strictfp"
         */
        public static final LintWarning Strictfp = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.STRICTFP, "compiler", "strictfp");
        
        /**
         * "trailing.white.space.will.be.removed"
         */
        public static final LintWarning TrailingWhiteSpaceWillBeRemoved = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TEXT_BLOCKS, "compiler", "trailing.white.space.will.be.removed");
        
        /**
         * "try.explicit.close.call"
         */
        public static final LintWarning TryExplicitCloseCall = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TRY, "compiler", "try.explicit.close.call");
        
        /**
         * compiler.warn.try.resource.can.throw.interrupted.exc=\
         *    close() method can throw InterruptedException in auto-closeable class {0}
         */
        public static LintWarning TryResourceCanThrowInterruptedExc(Type arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TRY, "compiler", "try.resource.can.throw.interrupted.exc", arg0);
        }
        
        /**
         * compiler.warn.try.resource.not.referenced=\
         *    auto-closeable resource {0} is never referenced in body of corresponding try statement
         */
        public static LintWarning TryResourceNotReferenced(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TRY, "compiler", "try.resource.not.referenced", arg0);
        }
        
        /**
         * compiler.warn.try.resource.throws.interrupted.exc=\
         *    auto-closeable resource {0} has a member method close() that could throw InterruptedException
         */
        public static LintWarning TryResourceThrowsInterruptedExc(Type arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.TRY, "compiler", "try.resource.throws.interrupted.exc", arg0);
        }
        
        /**
         * "unchecked.assign"
         */
        public static final LintWarning UncheckedAssign = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.UNCHECKED, "compiler", "unchecked.assign");
        
        /**
         * compiler.warn.unchecked.assign.to.var=\
         *    unchecked assignment to variable {0} as member of raw type {1}
         */
        public static LintWarning UncheckedAssignToVar(Symbol arg0, Type arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "unchecked.assign.to.var", arg0, arg1);
        }
        
        /**
         * compiler.warn.unchecked.call.mbr.of.raw.type=\
         *    unchecked call to {0} as a member of the raw type {1}
         */
        public static LintWarning UncheckedCallMbrOfRawType(Symbol arg0, Type arg1) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "unchecked.call.mbr.of.raw.type", arg0, arg1);
        }
        
        /**
         * "unchecked.cast.to.type"
         */
        public static final LintWarning UncheckedCastToType = new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.UNCHECKED, "compiler", "unchecked.cast.to.type");
        
        /**
         * compiler.warn.unchecked.generic.array.creation=\
         *    unchecked generic array creation for varargs parameter of type {0}
         */
        public static LintWarning UncheckedGenericArrayCreation(Type arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "unchecked.generic.array.creation", arg0);
        }
        
        /**
         * compiler.warn.unchecked.meth.invocation.applied=\
         *    unchecked method invocation: {0} {1} in {4} {5} is applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}
         */
        public static LintWarning UncheckedMethInvocationApplied(KindName arg0, Name arg1, Object arg2, Object arg3, KindName arg4, Symbol arg5) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "unchecked.meth.invocation.applied", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.warn.unchecked.varargs.non.reifiable.type=\
         *    Possible heap pollution from parameterized vararg type {0}
         */
        public static LintWarning UncheckedVarargsNonReifiableType(Type arg0) {
            return new LintWarning(EnumSet.of(AGGREGATE, MANDATORY, DEFAULT_ENABLED), LintCategory.UNCHECKED, "compiler", "unchecked.varargs.non.reifiable.type", arg0);
        }
        
        /**
         * compiler.warn.unexpected.archive.file=\
         *    Unexpected extension for archive file: {0}
         */
        public static LintWarning UnexpectedArchiveFile(Path arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.PATH, "compiler", "unexpected.archive.file", arg0);
        }
        
        /**
         * compiler.warn.unknown.enum.constant=\
         *    unknown enum constant {1}.{2}
         */
        public static LintWarning UnknownEnumConstant(JavaFileObject arg0, Symbol arg1, Name arg2) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "unknown.enum.constant", arg0, arg1, arg2);
        }
        
        /**
         * compiler.warn.unknown.enum.constant.reason=\
         *    unknown enum constant {1}.{2}\n\
         *    reason: {3}
         */
        public static LintWarning UnknownEnumConstantReason(JavaFileObject arg0, Symbol arg1, Name arg2, JCDiagnostic arg3) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "unknown.enum.constant.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.warn.unknown.enum.constant.reason=\
         *    unknown enum constant {1}.{2}\n\
         *    reason: {3}
         */
        public static LintWarning UnknownEnumConstantReason(JavaFileObject arg0, Symbol arg1, Name arg2, Fragment arg3) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.CLASSFILE, "compiler", "unknown.enum.constant.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.warn.varargs.redundant.trustme.anno=\
         *    Redundant {0} annotation. {1}
         */
        public static LintWarning VarargsRedundantTrustmeAnno(Symbol arg0, JCDiagnostic arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.VARARGS, "compiler", "varargs.redundant.trustme.anno", arg0, arg1);
        }
        
        /**
         * compiler.warn.varargs.redundant.trustme.anno=\
         *    Redundant {0} annotation. {1}
         */
        public static LintWarning VarargsRedundantTrustmeAnno(Symbol arg0, Fragment arg1) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.VARARGS, "compiler", "varargs.redundant.trustme.anno", arg0, arg1);
        }
        
        /**
         * compiler.warn.varargs.unsafe.use.varargs.param=\
         *    Varargs method could cause heap pollution from non-reifiable varargs parameter {0}
         */
        public static LintWarning VarargsUnsafeUseVarargsParam(Symbol arg0) {
            return new LintWarning(EnumSet.noneOf(DiagnosticFlag.class), LintCategory.VARARGS, "compiler", "varargs.unsafe.use.varargs.param", arg0);
        }
    }
    public static class Notes {
        /**
         * "compressed.diags"
         */
        public static final Note CompressedDiags = new Note(EnumSet.of(MANDATORY), "compiler", "compressed.diags");
        
        /**
         * compiler.note.deferred.method.inst=\
         *    Deferred instantiation of method {0}\n\
         *    instantiated signature: {1}\n\
         *    target-type: {2}
         */
        public static Note DeferredMethodInst(Symbol arg0, Type arg1, Type arg2) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deferred.method.inst", arg0, arg1, arg2);
        }
        
        /**
         * compiler.note.deprecated.filename=\
         *    {0} uses or overrides a deprecated API.
         */
        public static Note DeprecatedFilename(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename", arg0);
        }
        
        /**
         * compiler.note.deprecated.filename=\
         *    {0} uses or overrides a deprecated API.
         */
        public static Note DeprecatedFilename(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename", arg0);
        }
        
        /**
         * compiler.note.deprecated.filename=\
         *    {0} uses or overrides a deprecated API.
         */
        public static Note DeprecatedFilename(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename", arg0);
        }
        
        /**
         * compiler.note.deprecated.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API.
         */
        public static Note DeprecatedFilenameAdditional(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename.additional", arg0);
        }
        
        /**
         * compiler.note.deprecated.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API.
         */
        public static Note DeprecatedFilenameAdditional(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename.additional", arg0);
        }
        
        /**
         * compiler.note.deprecated.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API.
         */
        public static Note DeprecatedFilenameAdditional(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.filename.additional", arg0);
        }
        
        /**
         * "deprecated.plural"
         */
        public static final Note DeprecatedPlural = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.plural");
        
        /**
         * "deprecated.plural.additional"
         */
        public static final Note DeprecatedPluralAdditional = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.plural.additional");
        
        /**
         * "deprecated.recompile"
         */
        public static final Note DeprecatedRecompile = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "deprecated.recompile");
        
        /**
         * "implicit.annotation.processing"
         */
        public static final Note ImplicitAnnotationProcessing = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "implicit.annotation.processing");
        
        /**
         * compiler.note.lambda.deserialization.stat=\
         *    Generating lambda deserialization\n\
         *    functionalInterfaceClass: {0}\n\
         *    functionalInterfaceMethodName: {1}\n\
         *    functionalInterfaceMethodSignature:{2}\n\
         *    implMethodKind: {3}\n\
         *    implClass: {4}\n\
         *    implMethodName: {5}\n\
         *    implMethodSignature: {6}\n\
         *    instantiatedMethodType: {7}
         */
        public static Note LambdaDeserializationStat(String arg0, String arg1, String arg2, int arg3, String arg4, String arg5, String arg6, String arg7) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lambda.deserialization.stat", arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
        
        /**
         * compiler.note.lambda.stat=\
         *    Translating lambda expression\n\
         *    alternate metafactory = {0}\n\
         *    synthetic method = {1}
         */
        public static Note LambdaStat(boolean arg0, Symbol arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lambda.stat", arg0, arg1);
        }
        
        /**
         * compiler.note.method.ref.search.results.multi=\
         *    {0} search results for {1}, with most specific {2}\n\
         *    applicable candidates:
         */
        public static Note MethodRefSearchResultsMulti(Fragment arg0, String arg1, int arg2) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "method.ref.search.results.multi", arg0, arg1, arg2);
        }
        
        /**
         * compiler.note.mref.stat=\
         *    Translating method reference\n\
         *    alternate metafactory = {0}\n\
         */
        public static Note MrefStat(boolean arg0, Void arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "mref.stat", arg0, arg1);
        }
        
        /**
         * compiler.note.mref.stat.1=\
         *    Translating method reference\n\
         *    alternate metafactory = {0}\n\
         *    bridge method = {1}
         */
        public static Note MrefStat1(boolean arg0, Symbol arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "mref.stat.1", arg0, arg1);
        }
        
        /**
         * compiler.note.multiple.elements=\
         *    Multiple elements named ''{1}'' in modules ''{2}'' were found by javax.lang.model.util.Elements.{0}.
         */
        public static Note MultipleElements(String arg0, String arg1, String arg2) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "multiple.elements", arg0, arg1, arg2);
        }
        
        /**
         * "note"
         */
        public static final Note Note = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "note");
        
        /**
         * compiler.note.preview.filename=\
         *    {0} uses preview features of Java SE {1}.
         */
        public static Note PreviewFilename(File arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.filename=\
         *    {0} uses preview features of Java SE {1}.
         */
        public static Note PreviewFilename(JavaFileObject arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.filename=\
         *    {0} uses preview features of Java SE {1}.
         */
        public static Note PreviewFilename(Path arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.filename.additional=\
         *    {0} has additional uses of preview features of Java SE {1}.
         */
        public static Note PreviewFilenameAdditional(File arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename.additional", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.filename.additional=\
         *    {0} has additional uses of preview features of Java SE {1}.
         */
        public static Note PreviewFilenameAdditional(JavaFileObject arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename.additional", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.filename.additional=\
         *    {0} has additional uses of preview features of Java SE {1}.
         */
        public static Note PreviewFilenameAdditional(Path arg0, Source arg1) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.filename.additional", arg0, arg1);
        }
        
        /**
         * compiler.note.preview.plural=\
         *    Some input files use preview features of Java SE {0}.
         */
        public static Note PreviewPlural(Source arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.plural", arg0);
        }
        
        /**
         * compiler.note.preview.plural.additional=\
         *    Some input files additionally use preview features of Java SE {0}.
         */
        public static Note PreviewPluralAdditional(Source arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.plural.additional", arg0);
        }
        
        /**
         * "preview.recompile"
         */
        public static final Note PreviewRecompile = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "preview.recompile");
        
        /**
         * compiler.note.proc.messager=\
         *    {0}
         */
        public static Note ProcMessager(String arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "proc.messager", arg0);
        }
        
        /**
         * compiler.note.removal.filename=\
         *    {0} uses or overrides a deprecated API that is marked for removal.
         */
        public static Note RemovalFilename(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename", arg0);
        }
        
        /**
         * compiler.note.removal.filename=\
         *    {0} uses or overrides a deprecated API that is marked for removal.
         */
        public static Note RemovalFilename(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename", arg0);
        }
        
        /**
         * compiler.note.removal.filename=\
         *    {0} uses or overrides a deprecated API that is marked for removal.
         */
        public static Note RemovalFilename(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename", arg0);
        }
        
        /**
         * compiler.note.removal.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API that is marked for removal.
         */
        public static Note RemovalFilenameAdditional(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename.additional", arg0);
        }
        
        /**
         * compiler.note.removal.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API that is marked for removal.
         */
        public static Note RemovalFilenameAdditional(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename.additional", arg0);
        }
        
        /**
         * compiler.note.removal.filename.additional=\
         *    {0} has additional uses or overrides of a deprecated API that is marked for removal.
         */
        public static Note RemovalFilenameAdditional(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.filename.additional", arg0);
        }
        
        /**
         * "removal.plural"
         */
        public static final Note RemovalPlural = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.plural");
        
        /**
         * "removal.plural.additional"
         */
        public static final Note RemovalPluralAdditional = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.plural.additional");
        
        /**
         * "removal.recompile"
         */
        public static final Note RemovalRecompile = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "removal.recompile");
        
        /**
         * compiler.note.unchecked.filename=\
         *    {0} uses unchecked or unsafe operations.
         */
        public static Note UncheckedFilename(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename", arg0);
        }
        
        /**
         * compiler.note.unchecked.filename=\
         *    {0} uses unchecked or unsafe operations.
         */
        public static Note UncheckedFilename(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename", arg0);
        }
        
        /**
         * compiler.note.unchecked.filename=\
         *    {0} uses unchecked or unsafe operations.
         */
        public static Note UncheckedFilename(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename", arg0);
        }
        
        /**
         * compiler.note.unchecked.filename.additional=\
         *    {0} has additional unchecked or unsafe operations.
         */
        public static Note UncheckedFilenameAdditional(File arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename.additional", arg0);
        }
        
        /**
         * compiler.note.unchecked.filename.additional=\
         *    {0} has additional unchecked or unsafe operations.
         */
        public static Note UncheckedFilenameAdditional(JavaFileObject arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename.additional", arg0);
        }
        
        /**
         * compiler.note.unchecked.filename.additional=\
         *    {0} has additional unchecked or unsafe operations.
         */
        public static Note UncheckedFilenameAdditional(Path arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.filename.additional", arg0);
        }
        
        /**
         * "unchecked.plural"
         */
        public static final Note UncheckedPlural = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.plural");
        
        /**
         * "unchecked.plural.additional"
         */
        public static final Note UncheckedPluralAdditional = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.plural.additional");
        
        /**
         * "unchecked.recompile"
         */
        public static final Note UncheckedRecompile = new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.recompile");
        
        /**
         * compiler.note.verbose.l2m.deduplicate=\
         *    deduplicating lambda implementation method {0}
         */
        public static Note VerboseL2mDeduplicate(Symbol arg0) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.l2m.deduplicate", arg0);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, List<? extends Type> arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, List<? extends Type> arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, List<? extends Type> arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, JCDiagnostic arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, JCDiagnostic arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, JCDiagnostic arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, Fragment arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, Fragment arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi=\
         *    resolving method {0} in type {1} to candidate {2}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti(Name arg0, Symbol arg1, int arg2, String arg3, Fragment arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, List<? extends Type> arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, List<? extends Type> arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, List<? extends Type> arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, JCDiagnostic arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, JCDiagnostic arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, JCDiagnostic arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, Fragment arg4, List<? extends Type> arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, Fragment arg4, JCDiagnostic arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.note.verbose.resolve.multi.1=\
         *    erroneous resolution for method {0} in type {1}\n\
         *    phase: {3}\n\
         *    with actuals: {4}\n\
         *    with type-args: {5}\n\
         *    candidates:
         */
        public static Note VerboseResolveMulti1(Name arg0, Symbol arg1, Void arg2, String arg3, Fragment arg4, Fragment arg5) {
            return new Note(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.resolve.multi.1", arg0, arg1, arg2, arg3, arg4, arg5);
        }
    }
    public static class Fragments {
        /**
         * "accessor.method.cant.throw.exception"
         */
        public static final Fragment AccessorMethodCantThrowException = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "accessor.method.cant.throw.exception");
        
        /**
         * "accessor.method.must.not.be.generic"
         */
        public static final Fragment AccessorMethodMustNotBeGeneric = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "accessor.method.must.not.be.generic");
        
        /**
         * "accessor.method.must.not.be.static"
         */
        public static final Fragment AccessorMethodMustNotBeStatic = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "accessor.method.must.not.be.static");
        
        /**
         * compiler.misc.accessor.return.type.doesnt.match=\
         *    return type of accessor method {0} must match the type of record component {1}
         */
        public static Fragment AccessorReturnTypeDoesntMatch(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "accessor.return.type.doesnt.match", arg0, arg1);
        }
        
        /**
         * compiler.misc.anachronistic.module.info=\
         *    module declaration found in version {0}.{1} classfile
         */
        public static Fragment AnachronisticModuleInfo(String arg0, String arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anachronistic.module.info", arg0, arg1);
        }
        
        /**
         * "anonymous"
         */
        public static final Fragment Anonymous = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anonymous");
        
        /**
         * compiler.misc.anonymous.class=\
         *    <anonymous {0}>
         */
        public static Fragment AnonymousClass(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "anonymous.class", arg0);
        }
        
        /**
         * compiler.misc.applicable.method.found=\
         */
        public static Fragment ApplicableMethodFound(int arg0, Symbol arg1, Void arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.applicable.method.found.1=\
         *    ({2})
         */
        public static Fragment ApplicableMethodFound1(int arg0, Symbol arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found.1", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.applicable.method.found.1=\
         *    ({2})
         */
        public static Fragment ApplicableMethodFound1(int arg0, Symbol arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found.1", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.applicable.method.found.2=\
         */
        public static Fragment ApplicableMethodFound2(int arg0, Fragment arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found.2", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.applicable.method.found.3=\
         *    ({3})
         */
        public static Fragment ApplicableMethodFound3(int arg0, Fragment arg1, Symbol arg2, JCDiagnostic arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found.3", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.applicable.method.found.3=\
         *    ({3})
         */
        public static Fragment ApplicableMethodFound3(int arg0, Fragment arg1, Symbol arg2, Fragment arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "applicable.method.found.3", arg0, arg1, arg2, arg3);
        }
        
        /**
         * "arg.length.mismatch"
         */
        public static final Fragment ArgLengthMismatch = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "arg.length.mismatch");
        
        /**
         * compiler.misc.bad.class.file=\
         *    class file is invalid for class {0}
         */
        public static Fragment BadClassFile(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file", arg0);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(File arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(File arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(JavaFileObject arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(JavaFileObject arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(Path arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.class.file.header=\
         *    bad class file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the classpath.
         */
        public static Fragment BadClassFileHeader(Path arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.file.header", arg0, arg1);
        }
        
        /**
         * "bad.class.signature"
         */
        public static final Fragment BadClassSignature = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.signature");
        
        /**
         * compiler.misc.bad.class.truncated.at.offset=\
         *    class file truncated at offset {0}
         */
        public static Fragment BadClassTruncatedAtOffset(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.class.truncated.at.offset", arg0);
        }
        
        /**
         * compiler.misc.bad.const.pool.entry=\
         *    bad constant pool entry in {0}\n\
         *    expected {1} at index {2}
         */
        public static Fragment BadConstPoolEntry(File arg0, String arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.entry", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.const.pool.entry=\
         *    bad constant pool entry in {0}\n\
         *    expected {1} at index {2}
         */
        public static Fragment BadConstPoolEntry(JavaFileObject arg0, String arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.entry", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.const.pool.entry=\
         *    bad constant pool entry in {0}\n\
         *    expected {1} at index {2}
         */
        public static Fragment BadConstPoolEntry(Path arg0, String arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.entry", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.const.pool.index=\
         *    bad constant pool index in {0}\n\
         *    index {1} is not within pool size {2}.
         */
        public static Fragment BadConstPoolIndex(File arg0, int arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.index", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.const.pool.index=\
         *    bad constant pool index in {0}\n\
         *    index {1} is not within pool size {2}.
         */
        public static Fragment BadConstPoolIndex(JavaFileObject arg0, int arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.index", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.const.pool.index=\
         *    bad constant pool index in {0}\n\
         *    index {1} is not within pool size {2}.
         */
        public static Fragment BadConstPoolIndex(Path arg0, int arg1, int arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.index", arg0, arg1, arg2);
        }
        
        /**
         * "bad.const.pool.tag"
         */
        public static final Fragment BadConstPoolTag = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.tag");
        
        /**
         * "bad.const.pool.tag.at"
         */
        public static final Fragment BadConstPoolTagAt = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.const.pool.tag.at");
        
        /**
         * compiler.misc.bad.constant.range=\
         *    constant value ''{0}'' for {1} is outside the expected range for {2}
         */
        public static Fragment BadConstantRange(String arg0, Symbol arg1, Type arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.constant.range", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.constant.value=\
         *    bad constant value ''{0}'' for {1}, expected {2}
         */
        public static Fragment BadConstantValue(String arg0, Symbol arg1, String arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.constant.value", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.bad.constant.value.type=\
         *    variable of type ''{0}'' cannot have a constant value, but has one specified
         */
        public static Fragment BadConstantValueType(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.constant.value.type", arg0);
        }
        
        /**
         * "bad.enclosing.class"
         */
        public static final Fragment BadEnclosingClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.enclosing.class");
        
        /**
         * compiler.misc.bad.enclosing.method=\
         *    bad enclosing method attribute for class {0}
         */
        public static Fragment BadEnclosingMethod(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.enclosing.method", arg0);
        }
        
        /**
         * compiler.misc.bad.instance.method.in.unbound.lookup=\
         *    unexpected instance {0} {1} found in unbound lookup
         */
        public static Fragment BadInstanceMethodInUnboundLookup(Kind arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.instance.method.in.unbound.lookup", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.intersection.target.for.functional.expr=\
         *    bad intersection type target for lambda or method reference\n\
         *    {0}
         */
        public static Fragment BadIntersectionTargetForFunctionalExpr(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.intersection.target.for.functional.expr", arg0);
        }
        
        /**
         * compiler.misc.bad.intersection.target.for.functional.expr=\
         *    bad intersection type target for lambda or method reference\n\
         *    {0}
         */
        public static Fragment BadIntersectionTargetForFunctionalExpr(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.intersection.target.for.functional.expr", arg0);
        }
        
        /**
         * "bad.module-info.name"
         */
        public static final Fragment BadModuleInfoName = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.module-info.name");
        
        /**
         * "bad.requires.flag"
         */
        public static final Fragment BadRequiresFlag = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.requires.flag");
        
        /**
         * "bad.signature"
         */
        public static final Fragment BadSignature = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.signature");
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(File arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(File arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(JavaFileObject arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(JavaFileObject arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(Path arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.source.file.header=\
         *    bad source file: {0}\n\
         *    {1}\n\
         *    Please remove or make sure it appears in the correct subdirectory of the sourcepath.
         */
        public static Fragment BadSourceFileHeader(Path arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.source.file.header", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.static.method.in.bound.lookup=\
         *    unexpected static {0} {1} found in bound lookup
         */
        public static Fragment BadStaticMethodInBoundLookup(Kind arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.static.method.in.bound.lookup", arg0, arg1);
        }
        
        /**
         * compiler.misc.bad.static.method.in.unbound.lookup=\
         *    unexpected static {0} {1} found in unbound lookup
         */
        public static Fragment BadStaticMethodInUnboundLookup(Kind arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.static.method.in.unbound.lookup", arg0, arg1);
        }
        
        /**
         * "bad.type.annotation.value"
         */
        public static final Fragment BadTypeAnnotationValue = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.type.annotation.value");
        
        /**
         * compiler.misc.bad.utf8.byte.sequence.at=\
         *    bad UTF-8 byte sequence at {0}
         */
        public static Fragment BadUtf8ByteSequenceAt(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bad.utf8.byte.sequence.at", arg0);
        }
        
        /**
         * compiler.misc.binding.pattern=\
         *    {0} _
         */
        public static Fragment BindingPattern(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "binding.pattern", arg0);
        }
        
        /**
         * "bound"
         */
        public static final Fragment Bound = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "bound");
        
        /**
         * "canonical"
         */
        public static final Fragment Canonical = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical");
        
        /**
         * "canonical.cant.have.return.statement"
         */
        public static final Fragment CanonicalCantHaveReturnStatement = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.cant.have.return.statement");
        
        /**
         * "canonical.must.not.contain.explicit.constructor.invocation"
         */
        public static final Fragment CanonicalMustNotContainExplicitConstructorInvocation = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.must.not.contain.explicit.constructor.invocation");
        
        /**
         * "canonical.must.not.declare.type.variables"
         */
        public static final Fragment CanonicalMustNotDeclareTypeVariables = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.must.not.declare.type.variables");
        
        /**
         * compiler.misc.canonical.must.not.have.stronger.access=\
         *    attempting to assign stronger access privileges; was {0}
         */
        public static Fragment CanonicalMustNotHaveStrongerAccess(Set<? extends FlagsEnum> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.must.not.have.stronger.access", arg0);
        }
        
        /**
         * compiler.misc.canonical.must.not.have.stronger.access=\
         *    attempting to assign stronger access privileges; was {0}
         */
        public static Fragment CanonicalMustNotHaveStrongerAccess(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.must.not.have.stronger.access", arg0);
        }
        
        /**
         * "canonical.with.name.mismatch"
         */
        public static final Fragment CanonicalWithNameMismatch = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "canonical.with.name.mismatch");
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, List<? extends Type> arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, List<? extends Type> arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, JCDiagnostic arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, JCDiagnostic arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, Fragment arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(List<? extends Type> arg0, Fragment arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, List<? extends Type> arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, List<? extends Type> arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, JCDiagnostic arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, JCDiagnostic arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, Fragment arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(JCDiagnostic arg0, Fragment arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, List<? extends Type> arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, List<? extends Type> arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, JCDiagnostic arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, JCDiagnostic arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, Fragment arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.array.ctor=\
         *    cannot create array from given types\n\
         *    required: {0}\n\
         *    found:    {1}\n\
         *    reason:   {2}
         */
        public static Fragment CantApplyArrayCtor(Fragment arg0, Fragment arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.array.ctor", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(JCDiagnostic arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(JCDiagnostic arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(Fragment arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(Fragment arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(Type arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.diamond.1=\
         *    cannot infer type arguments for {0}\n\
         *    reason: {1}
         */
        public static Fragment CantApplyDiamond1(Type arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.diamond.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, List<? extends Type> arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, JCDiagnostic arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, List<? extends Type> arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, List<? extends Type> arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, JCDiagnostic arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, JCDiagnostic arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, Fragment arg3, Kind arg4, Type arg5, JCDiagnostic arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbol=\
         *    {0} {1} in {4} {5} cannot be applied to given types\n\
         *    required: {2}\n\
         *    found:    {3}\n\
         *    reason: {6}
         */
        public static Fragment CantApplySymbol(Kind arg0, Name arg1, Fragment arg2, Fragment arg3, Kind arg4, Type arg5, Fragment arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbol", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.cant.apply.symbols=\
         *    no suitable {0} found for {1}({2})
         */
        public static Fragment CantApplySymbols(Kind arg0, Name arg1, List<? extends Type> arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.apply.symbols", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.cant.hide=\
         *    {0} in {1} cannot hide {2} in {3}
         */
        public static Fragment CantHide(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.hide", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.cant.implement=\
         *    {0} in {1} cannot implement {2} in {3}
         */
        public static Fragment CantImplement(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.implement", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.cant.override=\
         *    {0} in {1} cannot override {2} in {3}
         */
        public static Fragment CantOverride(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.override", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.cant.resolve.args=\
         *    cannot find symbol\n\
         *    symbol: {0} {1}({3})
         */
        public static Fragment CantResolveArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.args", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.cant.resolve.location.args=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}({3})\n\
         *    location: {4}
         */
        public static Fragment CantResolveLocationArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3, JCDiagnostic arg4) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.misc.cant.resolve.location.args=\
         *    cannot find symbol\n\
         *    symbol:   {0} {1}({3})\n\
         *    location: {4}
         */
        public static Fragment CantResolveLocationArgs(KindName arg0, Name arg1, Void arg2, List<? extends Type> arg3, Fragment arg4) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.misc.cant.resolve.location.args.params=\
         *    cannot find symbol\n\
         *    symbol:   {0} <{2}>{1}({3})\n\
         *    location: {4}
         */
        @SuppressWarnings("rawtypes")
        public static Fragment CantResolveLocationArgsParams(KindName arg0, Name arg1, List<? extends Type> arg2, List arg3, JCDiagnostic arg4) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args.params", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * compiler.misc.cant.resolve.location.args.params=\
         *    cannot find symbol\n\
         *    symbol:   {0} <{2}>{1}({3})\n\
         *    location: {4}
         */
        @SuppressWarnings("rawtypes")
        public static Fragment CantResolveLocationArgsParams(KindName arg0, Name arg1, List<? extends Type> arg2, List arg3, Fragment arg4) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.location.args.params", arg0, arg1, arg2, arg3, arg4);
        }
        
        /**
         * "cant.resolve.modules"
         */
        public static final Fragment CantResolveModules = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "cant.resolve.modules");
        
        /**
         * compiler.misc.captured.type=\
         *    CAP#{0}
         */
        public static Fragment CapturedType(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "captured.type", arg0);
        }
        
        /**
         * compiler.misc.clashes.with=\
         *    {0} in {1} clashes with {2} in {3}
         */
        public static Fragment ClashesWith(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "clashes.with", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.class.file.not.found=\
         *    class file for {0} not found
         */
        public static Fragment ClassFileNotFound(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.file.not.found", arg0);
        }
        
        /**
         * "class.file.wrong.class"
         */
        public static final Fragment ClassFileWrongClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.file.wrong.class");
        
        /**
         * compiler.misc.class.is.not.sealed=\
         *    {0} must be sealed
         */
        public static Fragment ClassIsNotSealed(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "class.is.not.sealed", arg0);
        }
        
        /**
         * "compact"
         */
        public static final Fragment Compact = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "compact");
        
        /**
         * "conditional.target.cant.be.void"
         */
        public static final Fragment ConditionalTargetCantBeVoid = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "conditional.target.cant.be.void");
        
        /**
         * compiler.misc.count.error=\
         *    {0} error
         */
        public static Fragment CountError(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.error", arg0);
        }
        
        /**
         * compiler.misc.count.error.plural=\
         *    {0} errors
         */
        public static Fragment CountErrorPlural(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.error.plural", arg0);
        }
        
        /**
         * compiler.misc.count.error.recompile=\
         *    only showing the first {0} errors, of {1} total; use -Xmaxerrs if you would like to see more
         */
        public static Fragment CountErrorRecompile(int arg0, int arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.error.recompile", arg0, arg1);
        }
        
        /**
         * compiler.misc.count.warn=\
         *    {0} warning
         */
        public static Fragment CountWarn(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.warn", arg0);
        }
        
        /**
         * compiler.misc.count.warn.plural=\
         *    {0} warnings
         */
        public static Fragment CountWarnPlural(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.warn.plural", arg0);
        }
        
        /**
         * compiler.misc.count.warn.recompile=\
         *    only showing the first {0} warnings, of {1} total; use -Xmaxwarns if you would like to see more
         */
        public static Fragment CountWarnRecompile(int arg0, int arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "count.warn.recompile", arg0, arg1);
        }
        
        /**
         * compiler.misc.descriptor=\
         *    descriptor: {2} {0}({1})
         */
        public static Fragment Descriptor(Name arg0, List<? extends Type> arg1, Type arg2, List<? extends Type> arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "descriptor", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.descriptor.throws=\
         *    descriptor: {2} {0}({1}) throws {3}
         */
        public static Fragment DescriptorThrows(Name arg0, List<? extends Type> arg1, Type arg2, List<? extends Type> arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "descriptor.throws", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.diamond=\
         *    {0}<>
         */
        public static Fragment Diamond(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond", arg0);
        }
        
        /**
         * compiler.misc.diamond.and.explicit.params=\
         *    cannot use ''<>'' with explicit type parameters for constructor
         */
        public static Fragment DiamondAndExplicitParams(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.and.explicit.params", arg0);
        }
        
        /**
         * "diamond.anonymous.methods.implicitly.override"
         */
        public static final Fragment DiamondAnonymousMethodsImplicitlyOverride = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.anonymous.methods.implicitly.override");
        
        /**
         * compiler.misc.diamond.invalid.arg=\
         *    type argument {0} inferred for {1} is not allowed in this context\n\
         *    inferred argument is not expressible in the Signature attribute
         */
        public static Fragment DiamondInvalidArg(List<? extends Type> arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.invalid.arg", arg0, arg1);
        }
        
        /**
         * compiler.misc.diamond.invalid.arg=\
         *    type argument {0} inferred for {1} is not allowed in this context\n\
         *    inferred argument is not expressible in the Signature attribute
         */
        public static Fragment DiamondInvalidArg(List<? extends Type> arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.invalid.arg", arg0, arg1);
        }
        
        /**
         * compiler.misc.diamond.invalid.args=\
         *    type arguments {0} inferred for {1} are not allowed in this context\n\
         *    inferred arguments are not expressible in the Signature attribute
         */
        public static Fragment DiamondInvalidArgs(List<? extends Type> arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.invalid.args", arg0, arg1);
        }
        
        /**
         * compiler.misc.diamond.invalid.args=\
         *    type arguments {0} inferred for {1} are not allowed in this context\n\
         *    inferred arguments are not expressible in the Signature attribute
         */
        public static Fragment DiamondInvalidArgs(List<? extends Type> arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.invalid.args", arg0, arg1);
        }
        
        /**
         * compiler.misc.diamond.non.generic=\
         *    cannot use ''<>'' with non-generic class {0}
         */
        public static Fragment DiamondNonGeneric(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "diamond.non.generic", arg0);
        }
        
        /**
         * compiler.misc.doesnt.extend.sealed=\
         *    class {0} must extend sealed class
         */
        public static Fragment DoesntExtendSealed(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "doesnt.extend.sealed", arg0);
        }
        
        /**
         * compiler.misc.doesnt.implement.sealed=\
         *    {0} {1} must extend sealed interface
         */
        public static Fragment DoesntImplementSealed(KindName arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "doesnt.implement.sealed", arg0, arg1);
        }
        
        /**
         * compiler.misc.enum.constant.pattern=\
         *    {0}.{1}
         */
        public static Fragment EnumConstantPattern(Type arg0, Name arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "enum.constant.pattern", arg0, arg1);
        }
        
        /**
         * compiler.misc.eq.bounds=\
         *        equality constraints: {0}
         */
        public static Fragment EqBounds(List<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "eq.bounds", arg0);
        }
        
        /**
         * compiler.misc.exception.message=\
         *    {0}
         */
        public static Fragment ExceptionMessage(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "exception.message", arg0);
        }
        
        /**
         * compiler.misc.explicit.param.do.not.conform.to.bounds=\
         *    explicit type argument {0} does not conform to declared bound(s) {1}
         */
        public static Fragment ExplicitParamDoNotConformToBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "explicit.param.do.not.conform.to.bounds", arg0, arg1);
        }
        
        /**
         * "fatal.err.cant.close"
         */
        public static final Fragment FatalErrCantClose = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fatal.err.cant.close");
        
        /**
         * compiler.misc.fatal.err.cant.locate.ctor=\
         *    Fatal Error: Unable to find constructor for {0}
         */
        public static Fragment FatalErrCantLocateCtor(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fatal.err.cant.locate.ctor", arg0);
        }
        
        /**
         * compiler.misc.fatal.err.cant.locate.field=\
         *    Fatal Error: Unable to find field {0}
         */
        public static Fragment FatalErrCantLocateField(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fatal.err.cant.locate.field", arg0);
        }
        
        /**
         * compiler.misc.fatal.err.cant.locate.meth=\
         *    Fatal Error: Unable to find method {0}
         */
        public static Fragment FatalErrCantLocateMeth(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "fatal.err.cant.locate.meth", arg0);
        }
        
        /**
         * "feature.case.null"
         */
        public static final Fragment FeatureCaseNull = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.case.null");
        
        /**
         * "feature.deconstruction.patterns"
         */
        public static final Fragment FeatureDeconstructionPatterns = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.deconstruction.patterns");
        
        /**
         * "feature.diamond.and.anon.class"
         */
        public static final Fragment FeatureDiamondAndAnonClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.diamond.and.anon.class");
        
        /**
         * "feature.flexible.constructors"
         */
        public static final Fragment FeatureFlexibleConstructors = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.flexible.constructors");
        
        /**
         * "feature.implicit.classes"
         */
        public static final Fragment FeatureImplicitClasses = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.implicit.classes");
        
        /**
         * "feature.java.base.transitive"
         */
        public static final Fragment FeatureJavaBaseTransitive = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.java.base.transitive");
        
        /**
         * "feature.module.imports"
         */
        public static final Fragment FeatureModuleImports = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.module.imports");
        
        /**
         * "feature.modules"
         */
        public static final Fragment FeatureModules = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.modules");
        
        /**
         * "feature.multiple.case.labels"
         */
        public static final Fragment FeatureMultipleCaseLabels = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.multiple.case.labels");
        
        /**
         * compiler.misc.feature.not.supported.in.source=\
         *   {0} is not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Fragment FeatureNotSupportedInSource(JCDiagnostic arg0, String arg1, String arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.not.supported.in.source", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.feature.not.supported.in.source=\
         *   {0} is not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Fragment FeatureNotSupportedInSource(Fragment arg0, String arg1, String arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.not.supported.in.source", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.feature.not.supported.in.source.plural=\
         *   {0} are not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Fragment FeatureNotSupportedInSourcePlural(JCDiagnostic arg0, String arg1, String arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.not.supported.in.source.plural", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.feature.not.supported.in.source.plural=\
         *   {0} are not supported in -source {1}\n\
         *    (use -source {2} or higher to enable {0})
         */
        public static Fragment FeatureNotSupportedInSourcePlural(Fragment arg0, String arg1, String arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.not.supported.in.source.plural", arg0, arg1, arg2);
        }
        
        /**
         * "feature.pattern.matching.instanceof"
         */
        public static final Fragment FeaturePatternMatchingInstanceof = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.pattern.matching.instanceof");
        
        /**
         * "feature.pattern.switch"
         */
        public static final Fragment FeaturePatternSwitch = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.pattern.switch");
        
        /**
         * "feature.primitive.patterns"
         */
        public static final Fragment FeaturePrimitivePatterns = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.primitive.patterns");
        
        /**
         * "feature.private.intf.methods"
         */
        public static final Fragment FeaturePrivateIntfMethods = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.private.intf.methods");
        
        /**
         * "feature.records"
         */
        public static final Fragment FeatureRecords = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.records");
        
        /**
         * "feature.reifiable.types.instanceof"
         */
        public static final Fragment FeatureReifiableTypesInstanceof = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.reifiable.types.instanceof");
        
        /**
         * "feature.sealed.classes"
         */
        public static final Fragment FeatureSealedClasses = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.sealed.classes");
        
        /**
         * "feature.switch.expressions"
         */
        public static final Fragment FeatureSwitchExpressions = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.switch.expressions");
        
        /**
         * "feature.switch.rules"
         */
        public static final Fragment FeatureSwitchRules = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.switch.rules");
        
        /**
         * "feature.text.blocks"
         */
        public static final Fragment FeatureTextBlocks = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.text.blocks");
        
        /**
         * "feature.unconditional.patterns.in.instanceof"
         */
        public static final Fragment FeatureUnconditionalPatternsInInstanceof = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.unconditional.patterns.in.instanceof");
        
        /**
         * "feature.unnamed.variables"
         */
        public static final Fragment FeatureUnnamedVariables = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.unnamed.variables");
        
        /**
         * "feature.var.in.try.with.resources"
         */
        public static final Fragment FeatureVarInTryWithResources = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.var.in.try.with.resources");
        
        /**
         * "feature.var.syntax.in.implicit.lambda"
         */
        public static final Fragment FeatureVarSyntaxInImplicitLambda = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "feature.var.syntax.in.implicit.lambda");
        
        /**
         * "file.does.not.contain.module"
         */
        public static final Fragment FileDoesNotContainModule = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.does.not.contain.module");
        
        /**
         * compiler.misc.file.does.not.contain.package=\
         *    file does not contain package {0}
         */
        public static Fragment FileDoesNotContainPackage(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.does.not.contain.package", arg0);
        }
        
        /**
         * compiler.misc.file.doesnt.contain.class=\
         *    file does not contain class {0}
         */
        public static Fragment FileDoesntContainClass(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "file.doesnt.contain.class", arg0);
        }
        
        /**
         * "guard"
         */
        public static final Fragment Guard = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "guard");
        
        /**
         * "illegal.flag.combo"
         */
        public static final Fragment IllegalFlagCombo = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.flag.combo");
        
        /**
         * compiler.misc.illegal.signature=\
         *    illegal signature attribute for type {1}
         */
        public static Fragment IllegalSignature(Symbol arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.signature", arg0, arg1);
        }
        
        /**
         * "illegal.start.of.class.file"
         */
        public static final Fragment IllegalStartOfClassFile = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "illegal.start.of.class.file");
        
        /**
         * "implicit.and.explicit.not.allowed"
         */
        public static final Fragment ImplicitAndExplicitNotAllowed = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "implicit.and.explicit.not.allowed");
        
        /**
         * compiler.misc.inaccessible.varargs.type=\
         *    formal varargs element type {0} is not accessible from {1} {2}
         */
        public static Fragment InaccessibleVarargsType(Type arg0, Kind arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inaccessible.varargs.type", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.inapplicable.method=\
         *    {0} {1}.{2} is not applicable\n\
         *    ({3})
         */
        public static Fragment InapplicableMethod(KindName arg0, Symbol arg1, Symbol arg2, JCDiagnostic arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inapplicable.method", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.inapplicable.method=\
         *    {0} {1}.{2} is not applicable\n\
         *    ({3})
         */
        public static Fragment InapplicableMethod(KindName arg0, Symbol arg1, Symbol arg2, Fragment arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inapplicable.method", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.incompatible.abstract.default=\
         *    {0} {1} inherits abstract and default for {2}({3}) from types {4} and {5}
         */
        public static Fragment IncompatibleAbstractDefault(KindName arg0, Type arg1, Name arg2, List<? extends Type> arg3, Symbol arg4, Symbol arg5) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.abstract.default", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.misc.incompatible.abstracts=\
         *    multiple non-overriding abstract methods found in {0} {1}
         */
        public static Fragment IncompatibleAbstracts(KindName arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.abstracts", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(List<? extends Type> arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(List<? extends Type> arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(List<? extends Type> arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(JCDiagnostic arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(JCDiagnostic arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(JCDiagnostic arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(Fragment arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(Fragment arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.arg.types.in.lambda=\
         *    incompatible parameter types in lambda expression\n\
         *    required: {0}\n\
         *    found:    {1}
         */
        public static Fragment IncompatibleArgTypesInLambda(Fragment arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.lambda", arg0, arg1);
        }
        
        /**
         * "incompatible.arg.types.in.mref"
         */
        public static final Fragment IncompatibleArgTypesInMref = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.arg.types.in.mref");
        
        /**
         * compiler.misc.incompatible.bounds=\
         *    inference variable {0} has incompatible bounds\n\
         *    {1}\n\
         *    {2}
         */
        public static Fragment IncompatibleBounds(Type arg0, Fragment arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.bounds", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.incompatible.descs.in.functional.intf=\
         *    incompatible function descriptors found in {0} {1}
         */
        public static Fragment IncompatibleDescsInFunctionalIntf(KindName arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.descs.in.functional.intf", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.diff.ret=\
         *    both define {0}({1}), but with unrelated return types
         */
        public static Fragment IncompatibleDiffRet(Name arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.diff.ret", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.eq.bounds=\
         *    inference variable {0} has incompatible equality constraints {1}
         */
        public static Fragment IncompatibleEqBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.eq.bounds", arg0, arg1);
        }
        
        /**
         * compiler.misc.incompatible.ret.type.in.lambda=\
         *    bad return type in lambda expression\n\
         *    {0}
         */
        public static Fragment IncompatibleRetTypeInLambda(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.ret.type.in.lambda", arg0);
        }
        
        /**
         * compiler.misc.incompatible.ret.type.in.lambda=\
         *    bad return type in lambda expression\n\
         *    {0}
         */
        public static Fragment IncompatibleRetTypeInLambda(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.ret.type.in.lambda", arg0);
        }
        
        /**
         * compiler.misc.incompatible.ret.type.in.mref=\
         *    bad return type in method reference\n\
         *    {0}
         */
        public static Fragment IncompatibleRetTypeInMref(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.ret.type.in.mref", arg0);
        }
        
        /**
         * compiler.misc.incompatible.ret.type.in.mref=\
         *    bad return type in method reference\n\
         *    {0}
         */
        public static Fragment IncompatibleRetTypeInMref(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.ret.type.in.mref", arg0);
        }
        
        /**
         * compiler.misc.incompatible.type.in.conditional=\
         *    bad type in conditional expression\n\
         *    {0}
         */
        public static Fragment IncompatibleTypeInConditional(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.type.in.conditional", arg0);
        }
        
        /**
         * compiler.misc.incompatible.type.in.conditional=\
         *    bad type in conditional expression\n\
         *    {0}
         */
        public static Fragment IncompatibleTypeInConditional(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.type.in.conditional", arg0);
        }
        
        /**
         * compiler.misc.incompatible.type.in.switch.expression=\
         *    bad type in switch expression\n\
         *    {0}
         */
        public static Fragment IncompatibleTypeInSwitchExpression(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.type.in.switch.expression", arg0);
        }
        
        /**
         * compiler.misc.incompatible.type.in.switch.expression=\
         *    bad type in switch expression\n\
         *    {0}
         */
        public static Fragment IncompatibleTypeInSwitchExpression(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.type.in.switch.expression", arg0);
        }
        
        /**
         * compiler.misc.incompatible.unrelated.defaults=\
         *    {0} {1} inherits unrelated defaults for {2}({3}) from types {4} and {5}
         */
        public static Fragment IncompatibleUnrelatedDefaults(KindName arg0, Type arg1, Name arg2, List<? extends Type> arg3, Symbol arg4, Symbol arg5) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.unrelated.defaults", arg0, arg1, arg2, arg3, arg4, arg5);
        }
        
        /**
         * compiler.misc.incompatible.upper.bounds=\
         *    inference variable {0} has incompatible upper bounds {1}
         */
        public static Fragment IncompatibleUpperBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "incompatible.upper.bounds", arg0, arg1);
        }
        
        /**
         * compiler.misc.inconvertible.types=\
         *    {0} cannot be converted to {1}
         */
        public static Fragment InconvertibleTypes(Type arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inconvertible.types", arg0, arg1);
        }
        
        /**
         * compiler.misc.infer.arg.length.mismatch=\
         *    cannot infer type-variable(s) {0}\n\
         *    (actual and formal argument lists differ in length)
         */
        public static Fragment InferArgLengthMismatch(List<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.arg.length.mismatch", arg0);
        }
        
        /**
         * compiler.misc.infer.no.conforming.assignment.exists=\
         *    cannot infer type-variable(s) {0}\n\
         *    (argument mismatch; {1})
         */
        public static Fragment InferNoConformingAssignmentExists(List<? extends Type> arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.no.conforming.assignment.exists", arg0, arg1);
        }
        
        /**
         * compiler.misc.infer.no.conforming.assignment.exists=\
         *    cannot infer type-variable(s) {0}\n\
         *    (argument mismatch; {1})
         */
        public static Fragment InferNoConformingAssignmentExists(List<? extends Type> arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.no.conforming.assignment.exists", arg0, arg1);
        }
        
        /**
         * compiler.misc.infer.no.conforming.instance.exists=\
         *    no instance(s) of type variable(s) {0} exist so that {1} conforms to {2}
         */
        public static Fragment InferNoConformingInstanceExists(List<? extends Type> arg0, Type arg1, Type arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.no.conforming.instance.exists", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.infer.varargs.argument.mismatch=\
         *    cannot infer type-variable(s) {0}\n\
         *    (varargs mismatch; {1})
         */
        public static Fragment InferVarargsArgumentMismatch(List<? extends Type> arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.varargs.argument.mismatch", arg0, arg1);
        }
        
        /**
         * compiler.misc.infer.varargs.argument.mismatch=\
         *    cannot infer type-variable(s) {0}\n\
         *    (varargs mismatch; {1})
         */
        public static Fragment InferVarargsArgumentMismatch(List<? extends Type> arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "infer.varargs.argument.mismatch", arg0, arg1);
        }
        
        /**
         * compiler.misc.inferred.do.not.conform.to.eq.bounds=\
         *    inferred type does not conform to equality constraint(s)\n\
         *    inferred: {0}\n\
         *    equality constraints(s): {1}
         */
        public static Fragment InferredDoNotConformToEqBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inferred.do.not.conform.to.eq.bounds", arg0, arg1);
        }
        
        /**
         * compiler.misc.inferred.do.not.conform.to.lower.bounds=\
         *    inferred type does not conform to lower bound(s)\n\
         *    inferred: {0}\n\
         *    lower bound(s): {1}
         */
        public static Fragment InferredDoNotConformToLowerBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inferred.do.not.conform.to.lower.bounds", arg0, arg1);
        }
        
        /**
         * compiler.misc.inferred.do.not.conform.to.upper.bounds=\
         *    inferred type does not conform to upper bound(s)\n\
         *    inferred: {0}\n\
         *    upper bound(s): {1}
         */
        public static Fragment InferredDoNotConformToUpperBounds(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inferred.do.not.conform.to.upper.bounds", arg0, arg1);
        }
        
        /**
         * "inner.cls"
         */
        public static final Fragment InnerCls = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "inner.cls");
        
        /**
         * compiler.misc.intersection.type=\
         *    INT#{0}
         */
        public static Fragment IntersectionType(int arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "intersection.type", arg0);
        }
        
        /**
         * compiler.misc.invalid.default.interface=\
         *    default method found in version {0}.{1} classfile
         */
        public static Fragment InvalidDefaultInterface(String arg0, String arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.default.interface", arg0, arg1);
        }
        
        /**
         * compiler.misc.invalid.generic.lambda.target=\
         *    invalid functional descriptor for lambda expression\n\
         *    method {0} in {1} {2} is generic
         */
        public static Fragment InvalidGenericLambdaTarget(Type arg0, KindName arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.generic.lambda.target", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.invalid.mref=\
         *    invalid {0} reference\n\
         *    {1}
         */
        public static Fragment InvalidMref(KindName arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.mref", arg0, arg1);
        }
        
        /**
         * compiler.misc.invalid.mref=\
         *    invalid {0} reference\n\
         *    {1}
         */
        public static Fragment InvalidMref(KindName arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.mref", arg0, arg1);
        }
        
        /**
         * compiler.misc.invalid.static.interface=\
         *    static method found in version {0}.{1} classfile
         */
        public static Fragment InvalidStaticInterface(String arg0, String arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "invalid.static.interface", arg0, arg1);
        }
        
        /**
         * compiler.misc.is.a.type.variable=\
         *    must not include type variables: {0}
         */
        public static Fragment IsATypeVariable(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "is.a.type.variable", arg0);
        }
        
        /**
         * compiler.misc.is.duplicated=\
         *    must not contain duplicates: {0}
         */
        public static Fragment IsDuplicated(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "is.duplicated", arg0);
        }
        
        /**
         * "kindname.annotation"
         */
        public static final Fragment KindnameAnnotation = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.annotation");
        
        /**
         * "kindname.class"
         */
        public static final Fragment KindnameClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.class");
        
        /**
         * "kindname.constructor"
         */
        public static final Fragment KindnameConstructor = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.constructor");
        
        /**
         * "kindname.enum"
         */
        public static final Fragment KindnameEnum = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.enum");
        
        /**
         * "kindname.instance.init"
         */
        public static final Fragment KindnameInstanceInit = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.instance.init");
        
        /**
         * "kindname.interface"
         */
        public static final Fragment KindnameInterface = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.interface");
        
        /**
         * "kindname.method"
         */
        public static final Fragment KindnameMethod = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.method");
        
        /**
         * "kindname.module"
         */
        public static final Fragment KindnameModule = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.module");
        
        /**
         * "kindname.package"
         */
        public static final Fragment KindnamePackage = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.package");
        
        /**
         * "kindname.record"
         */
        public static final Fragment KindnameRecord = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.record");
        
        /**
         * "kindname.record.component"
         */
        public static final Fragment KindnameRecordComponent = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.record.component");
        
        /**
         * "kindname.static"
         */
        public static final Fragment KindnameStatic = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.static");
        
        /**
         * "kindname.static.init"
         */
        public static final Fragment KindnameStaticInit = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.static.init");
        
        /**
         * "kindname.type.variable"
         */
        public static final Fragment KindnameTypeVariable = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.type.variable");
        
        /**
         * "kindname.type.variable.bound"
         */
        public static final Fragment KindnameTypeVariableBound = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.type.variable.bound");
        
        /**
         * "kindname.value"
         */
        public static final Fragment KindnameValue = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.value");
        
        /**
         * "kindname.variable"
         */
        public static final Fragment KindnameVariable = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "kindname.variable");
        
        /**
         * "lambda"
         */
        public static final Fragment Lambda = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lambda");
        
        /**
         * "local"
         */
        public static final Fragment Local = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local");
        
        /**
         * "local.array.missing.target"
         */
        public static final Fragment LocalArrayMissingTarget = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.array.missing.target");
        
        /**
         * "local.cant.infer.null"
         */
        public static final Fragment LocalCantInferNull = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.cant.infer.null");
        
        /**
         * "local.cant.infer.void"
         */
        public static final Fragment LocalCantInferVoid = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.cant.infer.void");
        
        /**
         * "local.lambda.missing.target"
         */
        public static final Fragment LocalLambdaMissingTarget = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.lambda.missing.target");
        
        /**
         * "local.missing.init"
         */
        public static final Fragment LocalMissingInit = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.missing.init");
        
        /**
         * "local.mref.missing.target"
         */
        public static final Fragment LocalMrefMissingTarget = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.mref.missing.target");
        
        /**
         * "local.self.ref"
         */
        public static final Fragment LocalSelfRef = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "local.self.ref");
        
        /**
         * compiler.misc.location=\
         *    {0} {1}
         */
        public static Fragment Location(KindName arg0, Type arg1, Void arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "location", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.location=\
         *    {0} {1}
         */
        public static Fragment Location(KindName arg0, Symbol arg1, Void arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "location", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.location.1=\
         *    {0} {1} of type {2}
         */
        public static Fragment Location1(KindName arg0, Symbol arg1, Type arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "location.1", arg0, arg1, arg2);
        }
        
        /**
         * "locn.module_path"
         */
        public static final Fragment LocnModule_path = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.module_path");
        
        /**
         * "locn.module_source_path"
         */
        public static final Fragment LocnModule_source_path = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.module_source_path");
        
        /**
         * "locn.system_modules"
         */
        public static final Fragment LocnSystem_modules = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.system_modules");
        
        /**
         * "locn.upgrade_module_path"
         */
        public static final Fragment LocnUpgrade_module_path = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "locn.upgrade_module_path");
        
        /**
         * compiler.misc.lower.bounds=\
         *        lower bounds: {0}
         */
        public static Fragment LowerBounds(List<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "lower.bounds", arg0);
        }
        
        /**
         * "malformed.vararg.method"
         */
        public static final Fragment MalformedVarargMethod = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "malformed.vararg.method");
        
        /**
         * compiler.misc.method.descriptor.invalid=\
         *    method descriptor invalid for {0}
         */
        public static Fragment MethodDescriptorInvalid(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "method.descriptor.invalid", arg0);
        }
        
        /**
         * "method.must.be.public"
         */
        public static final Fragment MethodMustBePublic = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "method.must.be.public");
        
        /**
         * compiler.misc.missing.ret.val=\
         *    missing return value
         */
        public static Fragment MissingRetVal(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "missing.ret.val", arg0);
        }
        
        /**
         * "module.info.definition.expected"
         */
        public static final Fragment ModuleInfoDefinitionExpected = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.info.definition.expected");
        
        /**
         * "module.info.invalid.super.class"
         */
        public static final Fragment ModuleInfoInvalidSuperClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.info.invalid.super.class");
        
        /**
         * compiler.misc.module.name.mismatch=\
         *    module name {0} does not match expected name {1}
         */
        public static Fragment ModuleNameMismatch(Name arg0, Name arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.name.mismatch", arg0, arg1);
        }
        
        /**
         * compiler.misc.module.non.zero.opens=\
         *    open module {0} has non-zero opens_count
         */
        public static Fragment ModuleNonZeroOpens(Name arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "module.non.zero.opens", arg0);
        }
        
        /**
         * "mref.infer.and.explicit.params"
         */
        public static final Fragment MrefInferAndExplicitParams = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "mref.infer.and.explicit.params");
        
        /**
         * "must.not.be.same.class"
         */
        public static final Fragment MustNotBeSameClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "must.not.be.same.class");
        
        /**
         * compiler.misc.must.not.be.supertype=\
         *    illegal reference to supertype {0}
         */
        public static Fragment MustNotBeSupertype(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "must.not.be.supertype", arg0);
        }
        
        /**
         * compiler.misc.no.abstracts=\
         *    no abstract method found in {0} {1}
         */
        public static Fragment NoAbstracts(KindName arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.abstracts", arg0, arg1);
        }
        
        /**
         * "no.args"
         */
        public static final Fragment NoArgs = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.args");
        
        /**
         * compiler.misc.no.conforming.assignment.exists=\
         *    argument mismatch; {0}
         */
        public static Fragment NoConformingAssignmentExists(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.conforming.assignment.exists", arg0);
        }
        
        /**
         * compiler.misc.no.conforming.assignment.exists=\
         *    argument mismatch; {0}
         */
        public static Fragment NoConformingAssignmentExists(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.conforming.assignment.exists", arg0);
        }
        
        /**
         * compiler.misc.no.suitable.functional.intf.inst=\
         *    cannot infer functional interface descriptor for {0}
         */
        public static Fragment NoSuitableFunctionalIntfInst(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.suitable.functional.intf.inst", arg0);
        }
        
        /**
         * compiler.misc.no.unique.maximal.instance.exists=\
         *    no unique maximal instance exists for type variable {0} with upper bounds {1}
         */
        public static Fragment NoUniqueMaximalInstanceExists(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.unique.maximal.instance.exists", arg0, arg1);
        }
        
        /**
         * compiler.misc.no.unique.minimal.instance.exists=\
         *    no unique minimal instance exists for type variable {0} with lower bounds {1}
         */
        public static Fragment NoUniqueMinimalInstanceExists(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "no.unique.minimal.instance.exists", arg0, arg1);
        }
        
        /**
         * "non.static"
         */
        public static final Fragment NonStatic = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "non.static");
        
        /**
         * compiler.misc.not.a.functional.intf=\
         *    {0} is not a functional interface
         */
        public static Fragment NotAFunctionalIntf(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.a.functional.intf", arg0);
        }
        
        /**
         * compiler.misc.not.a.functional.intf.1=\
         *    {0} is not a functional interface\n\
         *    {1}
         */
        public static Fragment NotAFunctionalIntf1(Symbol arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.a.functional.intf.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.a.functional.intf.1=\
         *    {0} is not a functional interface\n\
         *    {1}
         */
        public static Fragment NotAFunctionalIntf1(Symbol arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.a.functional.intf.1", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.an.intf.component=\
         *    component type {0} is not an interface or java.lang.Object
         */
        public static Fragment NotAnIntfComponent(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.an.intf.component", arg0);
        }
        
        /**
         * compiler.misc.not.an.intf.component=\
         *    component type {0} is not an interface or java.lang.Object
         */
        public static Fragment NotAnIntfComponent(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.an.intf.component", arg0);
        }
        
        /**
         * compiler.misc.not.applicable.method.found=\
         *    ({2})
         */
        public static Fragment NotApplicableMethodFound(int arg0, Symbol arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.applicable.method.found", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.applicable.method.found=\
         *    ({2})
         */
        public static Fragment NotApplicableMethodFound(int arg0, Symbol arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.applicable.method.found", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.def.access.class.intf.cant.access=\
         *    {1}.{0} is defined in an inaccessible class or interface
         */
        public static Fragment NotDefAccessClassIntfCantAccess(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.class.intf.cant.access.reason=\
         *    {1}.{0} in package {2} is not accessible\n\
         *    ({3})
         */
        public static Fragment NotDefAccessClassIntfCantAccessReason(Symbol arg0, Symbol arg1, Symbol arg2, JCDiagnostic arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.not.def.access.class.intf.cant.access.reason=\
         *    {1}.{0} in package {2} is not accessible\n\
         *    ({3})
         */
        public static Fragment NotDefAccessClassIntfCantAccessReason(Symbol arg0, Symbol arg1, Symbol arg2, Fragment arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.class.intf.cant.access.reason", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.not.def.access.does.not.read=\
         *    package {1} is declared in module {2}, but module {0} does not read it
         */
        public static Fragment NotDefAccessDoesNotRead(Symbol arg0, Symbol arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.does.not.read", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.def.access.does.not.read.from.unnamed=\
         *    package {0} is declared in module {1}, which is not in the module graph
         */
        public static Fragment NotDefAccessDoesNotReadFromUnnamed(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.does.not.read.from.unnamed", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.does.not.read.unnamed=\
         *    package {0} is declared in the unnamed module, but module {1} does not read it
         */
        public static Fragment NotDefAccessDoesNotReadUnnamed(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.does.not.read.unnamed", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.not.exported=\
         *    package {0} is declared in module {1}, which does not export it
         */
        public static Fragment NotDefAccessNotExported(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.not.exported", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.not.exported.from.unnamed=\
         *    package {0} is declared in module {1}, which does not export it
         */
        public static Fragment NotDefAccessNotExportedFromUnnamed(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.not.exported.from.unnamed", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.not.exported.to.module=\
         *    package {0} is declared in module {1}, which does not export it to module {2}
         */
        public static Fragment NotDefAccessNotExportedToModule(Symbol arg0, Symbol arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.not.exported.to.module", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.def.access.not.exported.to.module.from.unnamed=\
         *    package {0} is declared in module {1}, which does not export it to the unnamed module
         */
        public static Fragment NotDefAccessNotExportedToModuleFromUnnamed(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.not.exported.to.module.from.unnamed", arg0, arg1);
        }
        
        /**
         * compiler.misc.not.def.access.package.cant.access=\
         *    {0} is not visible\n\
         *    ({2})
         */
        public static Fragment NotDefAccessPackageCantAccess(Symbol arg0, Symbol arg1, JCDiagnostic arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.package.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.def.access.package.cant.access=\
         *    {0} is not visible\n\
         *    ({2})
         */
        public static Fragment NotDefAccessPackageCantAccess(Symbol arg0, Symbol arg1, Fragment arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.access.package.cant.access", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.not.def.public.cant.access=\
         *    {0} is not public in {1}; cannot be accessed from outside package
         */
        public static Fragment NotDefPublicCantAccess(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "not.def.public.cant.access", arg0, arg1);
        }
        
        /**
         * compiler.misc.overridden.default=\
         *    method {0} is overridden in {1}
         */
        public static Fragment OverriddenDefault(Symbol arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "overridden.default", arg0, arg1);
        }
        
        /**
         * compiler.misc.package.not.visible=\
         *    package {0} is not visible\n\
         *    ({1})
         */
        public static Fragment PackageNotVisible(Symbol arg0, JCDiagnostic arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.not.visible", arg0, arg1);
        }
        
        /**
         * compiler.misc.package.not.visible=\
         *    package {0} is not visible\n\
         *    ({1})
         */
        public static Fragment PackageNotVisible(Symbol arg0, Fragment arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "package.not.visible", arg0, arg1);
        }
        
        /**
         * compiler.misc.partial.inst.sig=\
         *    partially instantiated to: {0}
         */
        public static Fragment PartialInstSig(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "partial.inst.sig", arg0);
        }
        
        /**
         * compiler.misc.possible.loss.of.precision=\
         *    possible lossy conversion from {0} to {1}
         */
        public static Fragment PossibleLossOfPrecision(Type arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "possible.loss.of.precision", arg0, arg1);
        }
        
        /**
         * compiler.misc.prob.found.req=\
         *    incompatible types: {0}
         */
        public static Fragment ProbFoundReq(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "prob.found.req", arg0);
        }
        
        /**
         * compiler.misc.prob.found.req=\
         *    incompatible types: {0}
         */
        public static Fragment ProbFoundReq(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "prob.found.req", arg0);
        }
        
        /**
         * compiler.misc.record.pattern=\
         *    {0}({1})
         */
        public static Fragment RecordPattern(Type arg0, List<? extends JCDiagnostic> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "record.pattern", arg0, arg1);
        }
        
        /**
         * compiler.misc.redundant.supertype=\
         *    redundant interface {0} is extended by {1}
         */
        public static Fragment RedundantSupertype(Symbol arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "redundant.supertype", arg0, arg1);
        }
        
        /**
         * compiler.misc.redundant.supertype=\
         *    redundant interface {0} is extended by {1}
         */
        public static Fragment RedundantSupertype(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "redundant.supertype", arg0, arg1);
        }
        
        /**
         * compiler.misc.ref.ambiguous=\
         *    reference to {0} is ambiguous\n\
         *    both {1} {2} in {3} and {4} {5} in {6} match
         */
        public static Fragment RefAmbiguous(Name arg0, Kind arg1, Symbol arg2, Symbol arg3, Kind arg4, Symbol arg5, Symbol arg6) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "ref.ambiguous", arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }
        
        /**
         * compiler.misc.report.access=\
         *    {0} has {1} access in {2}
         */
        public static Fragment ReportAccess(Symbol arg0, Set<? extends Modifier> arg1, Symbol arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "report.access", arg0, arg1, arg2);
        }
        
        /**
         * "resume.abort"
         */
        public static final Fragment ResumeAbort = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "resume.abort");
        
        /**
         * compiler.misc.source.no.bootclasspath=\
         *  not setting the bootstrap class path may lead to class files that cannot run on JDK {0}\n\
         *  --release {0} is recommended instead of -source {0} because it sets the bootstrap class path automatically
         */
        public static Fragment SourceNoBootclasspath(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.no.bootclasspath", arg0);
        }
        
        /**
         * compiler.misc.source.no.bootclasspath.with.target=\
         *  not setting the bootstrap class path may lead to class files that cannot run on JDK 8\n\
         *  --release {0} is recommended instead of -source {0} -target {1} because it sets the bootstrap class path automatically
         */
        public static Fragment SourceNoBootclasspathWithTarget(String arg0, String arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.no.bootclasspath.with.target", arg0, arg1);
        }
        
        /**
         * compiler.misc.source.no.system.modules.path=\
         *  not setting the location of system modules may lead to class files that cannot run on JDK {0}\n\
         *  --release {0} is recommended instead of -source {0} because it sets the location of system modules automatically
         */
        public static Fragment SourceNoSystemModulesPath(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.no.system.modules.path", arg0);
        }
        
        /**
         * compiler.misc.source.no.system.modules.path.with.target=\
         *  not setting the location of system modules may lead to class files that cannot run on JDK {0}\n\
         *  --release {0} is recommended instead of -source {0} -target {1} because it sets the location of system modules automatically
         */
        public static Fragment SourceNoSystemModulesPathWithTarget(String arg0, String arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.no.system.modules.path.with.target", arg0, arg1);
        }
        
        /**
         * "source.unavailable"
         */
        public static final Fragment SourceUnavailable = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "source.unavailable");
        
        /**
         * "stat.expr.expected"
         */
        public static final Fragment StatExprExpected = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "stat.expr.expected");
        
        /**
         * "static"
         */
        public static final Fragment Static = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "static");
        
        /**
         * "static.mref.with.targs"
         */
        public static final Fragment StaticMrefWithTargs = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "static.mref.with.targs");
        
        /**
         * "switch.expression.target.cant.be.void"
         */
        public static final Fragment SwitchExpressionTargetCantBeVoid = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "switch.expression.target.cant.be.void");
        
        /**
         * compiler.misc.synthetic.name.conflict=\
         *    the symbol {0} conflicts with a compiler-synthesized symbol in {1}
         */
        public static Fragment SyntheticNameConflict(Symbol arg0, Symbol arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "synthetic.name.conflict", arg0, arg1);
        }
        
        /**
         * compiler.misc.throws.clause.not.allowed.for.canonical.constructor=\
         *    throws clause not allowed for {0} constructor
         */
        public static Fragment ThrowsClauseNotAllowedForCanonicalConstructor(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "throws.clause.not.allowed.for.canonical.constructor", arg0);
        }
        
        /**
         * "token.bad-symbol"
         */
        public static final Fragment TokenBadSymbol = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.bad-symbol");
        
        /**
         * "token.character"
         */
        public static final Fragment TokenCharacter = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.character");
        
        /**
         * "token.double"
         */
        public static final Fragment TokenDouble = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.double");
        
        /**
         * "token.end-of-input"
         */
        public static final Fragment TokenEndOfInput = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.end-of-input");
        
        /**
         * "token.float"
         */
        public static final Fragment TokenFloat = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.float");
        
        /**
         * "token.identifier"
         */
        public static final Fragment TokenIdentifier = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.identifier");
        
        /**
         * "token.integer"
         */
        public static final Fragment TokenInteger = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.integer");
        
        /**
         * "token.long-integer"
         */
        public static final Fragment TokenLongInteger = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.long-integer");
        
        /**
         * "token.string"
         */
        public static final Fragment TokenString = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "token.string");
        
        /**
         * compiler.misc.try.not.applicable.to.type=\
         *    try-with-resources not applicable to variable type\n\
         *    ({0})
         */
        public static Fragment TryNotApplicableToType(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.not.applicable.to.type", arg0);
        }
        
        /**
         * compiler.misc.try.not.applicable.to.type=\
         *    try-with-resources not applicable to variable type\n\
         *    ({0})
         */
        public static Fragment TryNotApplicableToType(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "try.not.applicable.to.type", arg0);
        }
        
        /**
         * compiler.misc.type.annotation=\
         *    type annotations {0} are
         */
        public static Fragment TypeAnnotation(List<? extends Compound> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.annotation", arg0);
        }
        
        /**
         * compiler.misc.type.annotation.1=\
         *    type annotation {0} is
         */
        public static Fragment TypeAnnotation1(Compound arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.annotation.1", arg0);
        }
        
        /**
         * compiler.misc.type.captureof=\
         *    capture#{0} of {1}
         */
        public static Fragment TypeCaptureof(Name arg0, Type arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.captureof", arg0, arg1);
        }
        
        /**
         * "type.captureof.1"
         */
        public static final Fragment TypeCaptureof1 = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.captureof.1");
        
        /**
         * "type.must.be.identical.to.corresponding.record.component.type"
         */
        public static final Fragment TypeMustBeIdenticalToCorrespondingRecordComponentType = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.must.be.identical.to.corresponding.record.component.type");
        
        /**
         * "type.none"
         */
        public static final Fragment TypeNone = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.none");
        
        /**
         * "type.null"
         */
        public static final Fragment TypeNull = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.null");
        
        /**
         * compiler.misc.type.parameter=\
         *    type parameter {0}
         */
        public static Fragment TypeParameter(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.parameter", arg0);
        }
        
        /**
         * "type.req.array.or.iterable"
         */
        public static final Fragment TypeReqArrayOrIterable = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.req.array.or.iterable");
        
        /**
         * "type.req.class"
         */
        public static final Fragment TypeReqClass = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.req.class");
        
        /**
         * "type.req.class.array"
         */
        public static final Fragment TypeReqClassArray = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.req.class.array");
        
        /**
         * "type.req.exact"
         */
        public static final Fragment TypeReqExact = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.req.exact");
        
        /**
         * "type.req.ref"
         */
        public static final Fragment TypeReqRef = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.req.ref");
        
        /**
         * compiler.misc.type.var=\
         *    {0}#{1}
         */
        public static Fragment TypeVar(Name arg0, int arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "type.var", arg0, arg1);
        }
        
        /**
         * "unable.to.access.file"
         */
        public static final Fragment UnableToAccessFile = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unable.to.access.file");
        
        /**
         * "unbound"
         */
        public static final Fragment Unbound = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unbound");
        
        /**
         * "unchecked.assign"
         */
        public static final Fragment UncheckedAssign = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.assign");
        
        /**
         * "unchecked.cast.to.type"
         */
        public static final Fragment UncheckedCastToType = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.cast.to.type");
        
        /**
         * compiler.misc.unchecked.clash.with=\
         *    {0} in {1} overrides {2} in {3}
         */
        public static Fragment UncheckedClashWith(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.clash.with", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.unchecked.implement=\
         *    {0} in {1} implements {2} in {3}
         */
        public static Fragment UncheckedImplement(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.implement", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.unchecked.override=\
         *    {0} in {1} overrides {2} in {3}
         */
        public static Fragment UncheckedOverride(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unchecked.override", arg0, arg1, arg2, arg3);
        }
        
        /**
         * "undecl.type.var"
         */
        public static final Fragment UndeclTypeVar = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "undecl.type.var");
        
        /**
         * "unexpected.const.pool.tag.at"
         */
        public static final Fragment UnexpectedConstPoolTagAt = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unexpected.const.pool.tag.at");
        
        /**
         * "unexpected.ret.val"
         */
        public static final Fragment UnexpectedRetVal = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unexpected.ret.val");
        
        /**
         * "unicode.str.not.supported"
         */
        public static final Fragment UnicodeStrNotSupported = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unicode.str.not.supported");
        
        /**
         * "unnamed.module"
         */
        public static final Fragment UnnamedModule = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unnamed.module");
        
        /**
         * "unnamed.package"
         */
        public static final Fragment UnnamedPackage = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "unnamed.package");
        
        /**
         * compiler.misc.upper.bounds=\
         *        upper bounds: {0}
         */
        public static Fragment UpperBounds(List<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "upper.bounds", arg0);
        }
        
        /**
         * "user.selected.completion.failure"
         */
        public static final Fragment UserSelectedCompletionFailure = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "user.selected.completion.failure");
        
        /**
         * "var.and.explicit.not.allowed"
         */
        public static final Fragment VarAndExplicitNotAllowed = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.and.explicit.not.allowed");
        
        /**
         * "var.and.implicit.not.allowed"
         */
        public static final Fragment VarAndImplicitNotAllowed = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "var.and.implicit.not.allowed");
        
        /**
         * compiler.misc.varargs.argument.mismatch=\
         *    varargs mismatch; {0}
         */
        public static Fragment VarargsArgumentMismatch(JCDiagnostic arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.argument.mismatch", arg0);
        }
        
        /**
         * compiler.misc.varargs.argument.mismatch=\
         *    varargs mismatch; {0}
         */
        public static Fragment VarargsArgumentMismatch(Fragment arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.argument.mismatch", arg0);
        }
        
        /**
         * compiler.misc.varargs.clash.with=\
         *    {0} in {1} overrides {2} in {3}
         */
        public static Fragment VarargsClashWith(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.clash.with", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.varargs.implement=\
         *    {0} in {1} implements {2} in {3}
         */
        public static Fragment VarargsImplement(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.implement", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.varargs.override=\
         *    {0} in {1} overrides {2} in {3}
         */
        public static Fragment VarargsOverride(Symbol arg0, Symbol arg1, Symbol arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.override", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.varargs.trustme.on.non.varargs.accessor=\
         *    Accessor {0} is not a varargs method.
         */
        public static Fragment VarargsTrustmeOnNonVarargsAccessor(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.trustme.on.non.varargs.accessor", arg0);
        }
        
        /**
         * compiler.misc.varargs.trustme.on.non.varargs.meth=\
         *    Method {0} is not a varargs method.
         */
        public static Fragment VarargsTrustmeOnNonVarargsMeth(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.trustme.on.non.varargs.meth", arg0);
        }
        
        /**
         * compiler.misc.varargs.trustme.on.reifiable.varargs=\
         *    Varargs element type {0} is reifiable.
         */
        public static Fragment VarargsTrustmeOnReifiableVarargs(Type arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.trustme.on.reifiable.varargs", arg0);
        }
        
        /**
         * compiler.misc.varargs.trustme.on.virtual.varargs=\
         *    Instance method {0} is neither final nor private.
         */
        public static Fragment VarargsTrustmeOnVirtualVarargs(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.trustme.on.virtual.varargs", arg0);
        }
        
        /**
         * compiler.misc.varargs.trustme.on.virtual.varargs.final.only=\
         *    Instance method {0} is not final.
         */
        public static Fragment VarargsTrustmeOnVirtualVarargsFinalOnly(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "varargs.trustme.on.virtual.varargs.final.only", arg0);
        }
        
        /**
         * compiler.misc.verbose.checking.attribution=\
         *    [checking {0}]
         */
        public static Fragment VerboseCheckingAttribution(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.checking.attribution", arg0);
        }
        
        /**
         * compiler.misc.verbose.classpath=\
         *    [search path for class files: {0}]
         */
        public static Fragment VerboseClasspath(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.classpath", arg0);
        }
        
        /**
         * compiler.misc.verbose.loading=\
         *    [loading {0}]
         */
        public static Fragment VerboseLoading(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.loading", arg0);
        }
        
        /**
         * compiler.misc.verbose.parsing.done=\
         *    [parsing completed {0}ms]
         */
        public static Fragment VerboseParsingDone(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.parsing.done", arg0);
        }
        
        /**
         * compiler.misc.verbose.parsing.started=\
         *    [parsing started {0}]
         */
        public static Fragment VerboseParsingStarted(File arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.parsing.started", arg0);
        }
        
        /**
         * compiler.misc.verbose.parsing.started=\
         *    [parsing started {0}]
         */
        public static Fragment VerboseParsingStarted(JavaFileObject arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.parsing.started", arg0);
        }
        
        /**
         * compiler.misc.verbose.parsing.started=\
         *    [parsing started {0}]
         */
        public static Fragment VerboseParsingStarted(Path arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.parsing.started", arg0);
        }
        
        /**
         * compiler.misc.verbose.sourcepath=\
         *    [search path for source files: {0}]
         */
        public static Fragment VerboseSourcepath(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.sourcepath", arg0);
        }
        
        /**
         * compiler.misc.verbose.total=\
         *    [total {0}ms]
         */
        public static Fragment VerboseTotal(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.total", arg0);
        }
        
        /**
         * compiler.misc.verbose.wrote.file=\
         *    [wrote {0}]
         */
        public static Fragment VerboseWroteFile(File arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.wrote.file", arg0);
        }
        
        /**
         * compiler.misc.verbose.wrote.file=\
         *    [wrote {0}]
         */
        public static Fragment VerboseWroteFile(JavaFileObject arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.wrote.file", arg0);
        }
        
        /**
         * compiler.misc.verbose.wrote.file=\
         *    [wrote {0}]
         */
        public static Fragment VerboseWroteFile(Path arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "verbose.wrote.file", arg0);
        }
        
        /**
         * "version.not.available"
         */
        public static final Fragment VersionNotAvailable = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "version.not.available");
        
        /**
         * compiler.misc.where.captured=\
         *    {0} extends {1} super: {2} from capture of {3}
         */
        public static Fragment WhereCaptured(Type arg0, Type arg1, Type arg2, Type arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.captured", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.where.captured.1=\
         *    {0} extends {1} from capture of {3}
         */
        public static Fragment WhereCaptured1(Type arg0, Type arg1, Void arg2, Type arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.captured.1", arg0, arg1, arg2, arg3);
        }
        
        /**
         * "where.description.captured"
         */
        public static final Fragment WhereDescriptionCaptured = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.captured");
        
        /**
         * compiler.misc.where.description.captured.1=\
         *    where {0} are fresh type-variables:
         */
        public static Fragment WhereDescriptionCaptured1(Set<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.captured.1", arg0);
        }
        
        /**
         * compiler.misc.where.description.intersection=\
         *    where {0} is an intersection type:
         */
        public static Fragment WhereDescriptionIntersection(Set<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.intersection", arg0);
        }
        
        /**
         * compiler.misc.where.description.intersection.1=\
         *    where {0} are intersection types:
         */
        public static Fragment WhereDescriptionIntersection1(Set<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.intersection.1", arg0);
        }
        
        /**
         * compiler.misc.where.description.typevar=\
         *    where {0} is a type-variable:
         */
        public static Fragment WhereDescriptionTypevar(Set<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.typevar", arg0);
        }
        
        /**
         * compiler.misc.where.description.typevar.1=\
         *    where {0} are type-variables:
         */
        public static Fragment WhereDescriptionTypevar1(Set<? extends Type> arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.description.typevar.1", arg0);
        }
        
        /**
         * compiler.misc.where.fresh.typevar=\
         *    {0} extends {1}
         */
        public static Fragment WhereFreshTypevar(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.fresh.typevar", arg0, arg1);
        }
        
        /**
         * compiler.misc.where.intersection=\
         *    {0} extends {1}
         */
        public static Fragment WhereIntersection(Type arg0, List<? extends Type> arg1) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.intersection", arg0, arg1);
        }
        
        /**
         * compiler.misc.where.typevar=\
         *    {0} extends {1} declared in {2} {3}
         */
        public static Fragment WhereTypevar(Type arg0, List<? extends Type> arg1, Kind arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.typevar", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.where.typevar.1=\
         *    {0} declared in {2} {3}
         */
        public static Fragment WhereTypevar1(Type arg0, List<? extends Type> arg1, Kind arg2, Symbol arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "where.typevar.1", arg0, arg1, arg2, arg3);
        }
        
        /**
         * compiler.misc.wrong.number.args.in.lambda=\
         *    wrong number of parameters in lambda expression for functional interface {0}
         */
        public static Fragment WrongNumberArgsInLambda(Symbol arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "wrong.number.args.in.lambda", arg0);
        }
        
        /**
         * compiler.misc.wrong.number.type.args=\
         *    wrong number of type arguments; required {0}
         */
        public static Fragment WrongNumberTypeArgs(String arg0) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "wrong.number.type.args", arg0);
        }
        
        /**
         * "wrong.version"
         */
        public static final Fragment WrongVersion = new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "wrong.version");
        
        /**
         * compiler.misc.x.print.processor.info=\
         *    Processor {0} matches {1} and returns {2}.
         */
        public static Fragment XPrintProcessorInfo(String arg0, String arg1, boolean arg2) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "x.print.processor.info", arg0, arg1, arg2);
        }
        
        /**
         * compiler.misc.x.print.rounds=\
         *    Round {0}:\n\tinput files: {1}\n\tannotations: {2}\n\tlast round: {3}
         */
        public static Fragment XPrintRounds(int arg0, String arg1, Set<? extends Symbol> arg2, boolean arg3) {
            return new Fragment(EnumSet.noneOf(DiagnosticFlag.class), "compiler", "x.print.rounds", arg0, arg1, arg2, arg3);
        }
    }
}
