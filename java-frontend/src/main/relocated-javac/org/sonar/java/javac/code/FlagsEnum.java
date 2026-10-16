package org.sonar.java.javac.code;

public enum FlagsEnum {

    PUBLIC(1L<<0, "public"),
    PRIVATE(1L<<1, "private"),
    PROTECTED(1L<<2, "protected"),
    STATIC(1L<<3, "static"),
    FINAL(1L<<4, "final"),
    SYNCHRONIZED_OR_ACC_SUPER(1L<<5, "synchronized"),
    VOLATILE_OR_ACC_BRIDGE(1L<<6, "volatile"),
    TRANSIENT_OR_ACC_VARARGS(1L<<7, "transient"),
    NATIVE(1L<<8, "native"),
    INTERFACE(1L<<9, "interface"),
    ABSTRACT(1L<<10, "abstract"),
    STRICTFP(1L<<11, "strictfp"),
    SYNTHETIC(1L<<12, "synthetic"),
    ANNOTATION(1L<<13, "annotation"),
    ENUM(1L<<14, "enum"),
    MANDATED_OR_ACC_MODULE(1L<<15, "mandated"),
    DEPRECATED_OR_BODY_ONLY_FINALIZE(1L<<17, "deprecated or body_only_finalize"),
    HASINIT(1L<<18, "hasinit"),
    IMPLICIT_CLASS_OR_VAR_VARIABLE(1L<<19, "implicit_class or var_variable"),
    BLOCK_OR_LAMBDA_PARAMETER(1L<<20, "block or lambda_parameter"),
    FROM_SOURCE(1L<<21, "from_source"),
    NOOUTERTHIS(1L<<22, "noouterthis"),
    EXISTS(1L<<23, "exists"),
    COMPOUND_OR_GENERATED_MEMBER(1L<<24, "compound or generated_member"),
    CLASS_SEEN(1L<<25, "class_seen"),
    SOURCE_SEEN(1L<<26, "source_seen"),
    LOCKED(1L<<27, "locked"),
    UNATTRIBUTED(1L<<28, "unattributed"),
    ANONCONSTR_OR_SUPER_OWNER_ATTRIBUTED(1L<<29, "anonconstr or super_owner_attributed"),
    ACYCLIC(1L<<30, "acyclic"),
    BRIDGE(1L<<31, "bridge"),
    PARAMETER(1L<<33, "parameter"),
    VARARGS(1L<<34, "varargs"),
    ACYCLIC_ANN(1L<<35, "acyclic_ann"),
    GENERATEDCONSTR(1L<<36, "generatedconstr"),
    HYPOTHETICAL(1L<<37, "hypothetical"),
    PROPRIETARY(1L<<38, "proprietary"),
    UNION(1L<<39, "union"),
    RECOVERABLE(1L<<40, "recoverable"),
    EFFECTIVELY_FINAL(1L<<41, "effectively_final"),
    CLASH(1L<<42, "clash"),
    DEFAULT(1L<<43, "default"),
    AUXILIARY(1L<<44, "auxiliary"),
    NOT_IN_PROFILE_OR_BAD_OVERRIDE(1L<<45, "not_in_profile or bad_override"),
    SIGNATURE_POLYMORPHIC(1L<<46, "signature_polymorphic"),
    THROWS(1L<<47, "throws"),
    SEALED(1L<<48, "sealed"),
    LAMBDA_METHOD_OR_LOCAL_CAPTURE_FIELD(1L<<49, "lambda_method or local_capture_field"),
    TYPE_TRANSLATED(1L<<50, "type_translated"),
    MODULE_OR_COMPACT_RECORD_CONSTRUCTOR_OR_UNINITIALIZED_FIELD(1L<<51, "module or compact_record_constructor or uninitialized_field"),
    AUTOMATIC_MODULE_OR_HAS_RESOURCE_OR_NAME_FILLED(1L<<52, "automatic_module or has_resource or name_filled"),
    SYSTEM_MODULE_OR_VALUE_BASED_OR_FIELD_INIT_TYPE_ANNOTATIONS_QUEUED(1L<<53, "system_module or value_based or field_init_type_annotations_queued"),
    DEPRECATED_ANNOTATION(1L<<54, "deprecated_annotation"),
    DEPRECATED_REMOVAL(1L<<55, "deprecated_removal"),
    PREVIEW_API(1L<<56, "preview_api"),
    ANONCONSTR_BASED(1L<<57, "anonconstr_based"),
    PREVIEW_REFLECTIVE(1L<<58, "preview_reflective"),
    MATCH_BINDING(1L<<59, "match_binding"),
    MATCH_BINDING_TO_OUTER(1L<<60, "match_binding_to_outer"),
    RECORD(1L<<61, "record"),
    RESTRICTED_OR_REQUIRES_IDENTITY(1L<<62, "restricted or requires_identity"),
    NON_SEALED(1L<<63, "non-sealed"),
    ;

    private final long value;
    private final String toString;
    private FlagsEnum(long value, String toString) {
        this.value = value;
        this.toString = toString;
    }
    public long value() {
        return value;
    }
    public String toString() {
        return toString;
    }
}

