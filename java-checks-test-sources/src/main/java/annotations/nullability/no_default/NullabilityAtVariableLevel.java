package annotations.nullability.no_default;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.meta.When;
import org.eclipse.jdt.annotation.DefaultLocation;

/**
 * Test Nullability annotation when the element is directly annotated
 *
 * [Missing Dependencies]
 * Some nullability annotations are not tested because it was too hard to add the related dependencies compare to
 * the benefit of testing those exact cases.
 * - Android dependencies are not present in maven central, it requires setting up a google's maven repository
 *    Missing annotations:
 *      - @android.annotation.Nullable
 *      - @android.annotation.NonNull
 *      - @androidx.annotation.Nullable
 *      - @android.annotation.NonNull
 *      - @android.support.annotation.NonNull (see [Missing Dependencies] above)
 *      - @androidx.annotation.NonNull
 * - com.sun.istack is an 10 years old dependency, marked as internal and not available anymore.
 *      - @com.sun.istack.internal.Nullable
 *      - @com.sun.istack.internal.NotNull
 * - Not available on maven central:
 *      - @org.jmlspecs.annotation.Nullable
 *      - @org.jmlspecs.annotation.NonNull
 *
 */
public class NullabilityAtVariableLevel {
  // ============== Fields ==============
  Object id1000_type_UNKNOWN;
  // ============== Strong nullable ==============
  @javax.annotation.CheckForNull
  Object id1001_type_STRONG_NULLABLE_level_VARIABLE;
  @edu.umd.cs.findbugs.annotations.CheckForNull
  Object id1002_type_STRONG_NULLABLE_level_VARIABLE;
  //  @org.netbeans.api.annotations.common.CheckForNull // Not applicable to fields
  //  Object id1004_type_STRONG_NULLABLE_level_VARIABLE;
  @org.springframework.lang.Nullable
  Object id1003_type_STRONG_NULLABLE_level_VARIABLE;
  @reactor.util.annotation.Nullable
  Object id1005_type_STRONG_NULLABLE_level_VARIABLE;
  @org.eclipse.jdt.annotation.Nullable
  Object id1006_type_STRONG_NULLABLE_level_VARIABLE;
  @org.eclipse.jgit.annotations.Nullable
  Object id1007_type_STRONG_NULLABLE_level_VARIABLE;

  // ============== Weak Nullable ==============
  //  @android.annotation.Nullable (see [Missing Dependencies] above)
  //  Object id1008_type_WEAK_NULLABLE_level_VARIABLE;
  //  @android.support.annotation.Nullable (see [Missing Dependencies] above)
  //  Object id1009_type_WEAK_NULLABLE_level_VARIABLE;
  //  @androidx.annotation.Nullable (see [Missing Dependencies] above)
  //  Object id1010_type_WEAK_NULLABLE_level_VARIABLE;
  //  @com.sun.istack.internal.Nullable (see [Missing Dependencies] above)
  //  Object id1011_type_WEAK_NULLABLE_level_VARIABLE;
  @com.mongodb.lang.Nullable
  Object id1012_type_WEAK_NULLABLE_level_VARIABLE;
  @edu.umd.cs.findbugs.annotations.Nullable
  Object id1013_type_WEAK_NULLABLE_level_VARIABLE;
  @io.reactivex.annotations.Nullable
  Object id1014_type_WEAK_NULLABLE_level_VARIABLE;
  @io.reactivex.rxjava3.annotations.Nullable
  Object id1015_type_WEAK_NULLABLE_level_VARIABLE;
  @javax.annotation.Nullable
  Object id1016_type_WEAK_NULLABLE_level_VARIABLE;
  @org.checkerframework.checker.nullness.compatqual.NullableDecl
  Object id1017_type_WEAK_NULLABLE_level_VARIABLE;
  @org.checkerframework.checker.nullness.compatqual.NullableType
  Object id1018_type_WEAK_NULLABLE_level_VARIABLE;
  @org.checkerframework.checker.nullness.qual.Nullable
  Object id1019_type_WEAK_NULLABLE_level_VARIABLE;
  @org.jetbrains.annotations.Nullable
  Object id1020_type_WEAK_NULLABLE_level_VARIABLE;
  //  @org.jmlspecs.annotation.Nullable (see [Missing Dependencies] above)
  //  Object id1021_type_WEAK_NULLABLE_level_VARIABLE;
  @org.netbeans.api.annotations.common.NullAllowed
  Object id1022_type_WEAK_NULLABLE_level_VARIABLE;
  @org.netbeans.api.annotations.common.NullUnknown
  Object id1023_type_WEAK_NULLABLE_level_VARIABLE;

  // ============== Non Null ==============
  //  @android.annotation.NonNull (see [Missing Dependencies] above)
  //  Object id1024_type_NON_NULL_level_VARIABLE;
  //  @android.support.annotation.NonNull (see [Missing Dependencies] above)
  //  Object id1025_type_NON_NULL_level_VARIABLE;
  //  @androidx.annotation.NonNull (see [Missing Dependencies] above)
  //  Object id1026_type_NON_NULL_level_VARIABLE;
  //  @com.sun.istack.internal.NotNull (see [Missing Dependencies] above)
  //  Object id1027_type_NON_NULL_level_VARIABLE;
  @com.mongodb.lang.NonNull
  Object id1028_type_NON_NULL_level_VARIABLE;
  @edu.umd.cs.findbugs.annotations.NonNull
  Object id1029_type_NON_NULL_level_VARIABLE;
  @io.reactivex.annotations.NonNull
  Object id1030_type_NON_NULL_level_VARIABLE;
  @io.reactivex.rxjava3.annotations.NonNull
  Object id1031_type_NON_NULL_level_VARIABLE;
  @javax.annotation.Nonnull
  Object id1032_type_NON_NULL_level_VARIABLE;
  @javax.validation.constraints.NotNull
  Object id1033_type_NON_NULL_level_VARIABLE;
  @lombok.NonNull
  Object id1034_type_NON_NULL_level_VARIABLE;
  @org.checkerframework.checker.nullness.compatqual.NonNullDecl
  Object id1035_type_NON_NULL_level_VARIABLE;
  @org.checkerframework.checker.nullness.compatqual.NonNullType
  Object id1036_type_NON_NULL_level_VARIABLE;
  @org.checkerframework.checker.nullness.qual.NonNull
  Object id1037_type_NON_NULL_level_VARIABLE;
  @org.eclipse.jdt.annotation.NonNull
  Object id1038_type_NON_NULL_level_VARIABLE;
  @org.eclipse.jgit.annotations.NonNull
  Object id1039_type_NON_NULL_level_VARIABLE;
  @org.jetbrains.annotations.NotNull
  Object id1040_type_NON_NULL_level_VARIABLE;
  //  @org.jmlspecs.annotation.NonNull (see [Missing Dependencies] above)
  //  Object id1041_type_NON_NULL_level_VARIABLE;
  @org.netbeans.api.annotations.common.NonNull
  Object id1042_type_NON_NULL_level_VARIABLE;
  @org.springframework.lang.NonNull
  Object id1043_type_NON_NULL_level_VARIABLE;
  @reactor.util.annotation.NonNull
  Object id1044_type_NON_NULL_level_VARIABLE;

  // ============== javax.annotation.Nonnull specific behavior ==============
  @javax.annotation.Nonnull()
  Object id1045_type_NON_NULL_level_VARIABLE;
  @javax.annotation.Nonnull(when = When.ALWAYS)
  Object id1046_type_NON_NULL_level_VARIABLE;
  @javax.annotation.Nonnull(when = When.MAYBE)
  Object id1047_type_STRONG_NULLABLE_level_VARIABLE;
  @javax.annotation.Nonnull(when = When.NEVER)
  Object id1048_type_STRONG_NULLABLE_level_VARIABLE;
  @javax.annotation.Nonnull(when = When.UNKNOWN)
  Object id1049_type_WEAK_NULLABLE_level_VARIABLE;

  // ============== Test priority at the same level ==============
  // Strong nullable has the priority over everything, order does not matter
  @javax.annotation.CheckForNull
  @javax.annotation.Nullable
  Object id1050_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.Nullable
  @javax.annotation.CheckForNull
  Object id1051_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.Nonnull
  @javax.annotation.CheckForNull
  Object id1052_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.CheckForNull
  @javax.annotation.Nonnull
  Object id1053_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.CheckForNull
  @javax.annotation.Nonnull
  @javax.annotation.Nullable
  Object id1054_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.Nonnull
  @javax.annotation.CheckForNull
  @javax.annotation.Nullable
  Object id1055_type_STRONG_NULLABLE_level_VARIABLE;

  @javax.annotation.Nonnull
  @javax.annotation.Nullable
  @javax.annotation.CheckForNull
  Object id1056_type_STRONG_NULLABLE_level_VARIABLE;

  // Nullable has the priority over non null
  @javax.annotation.Nonnull
  @javax.annotation.Nullable
  Object id1057_type_WEAK_NULLABLE_level_VARIABLE;
  @javax.annotation.Nullable
  @javax.annotation.Nonnull
  Object id1058_type_WEAK_NULLABLE_level_VARIABLE;

  // ============== "NonNullByDefault" by eclipse can also apply to fields ==============
  @org.eclipse.jdt.annotation.NonNullByDefault
  Object id1059_type_NON_NULL_level_VARIABLE;
  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
  Object id1060_type_NON_NULL_level_VARIABLE;
  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object id1061_type_UNKNOWN;

  // ============== Should work without fully qualified name ==============
  @CheckForNull
  Object id1062_type_STRONG_NULLABLE_level_VARIABLE;
  @Nullable
  Object id1063_type_WEAK_NULLABLE_level_VARIABLE;
  @Nonnull
  Object id1064_type_NON_NULL_level_VARIABLE;

  // ============== Return values of methods and arguments support the same set of annotation ==============
  @edu.umd.cs.findbugs.annotations.CheckForNull
  public Object id1065_type_STRONG_NULLABLE_level_METHOD(
    // Annotations on method does not impact arguments
    Object id1066_type_UNKNOWN,
    @edu.umd.cs.findbugs.annotations.CheckForNull Object id1067_type_STRONG_NULLABLE_level_VARIABLE
  ) {
    return new Object();
  }

  @org.jetbrains.annotations.Nullable
  public Object id1068_type_WEAK_NULLABLE_level_METHOD(
    // Annotations on method does not impact arguments
    Object id1069_type_UNKNOWN,
    @org.jetbrains.annotations.Nullable Object id1070_type_WEAK_NULLABLE_level_VARIABLE
  ) {
    return new Object();
  }

  @org.eclipse.jgit.annotations.NonNull
  public Object id1071_type_NON_NULL_level_METHOD(
    // Annotations on method does not impact arguments
    Object id1072_type_UNKNOWN,
    @org.eclipse.jgit.annotations.NonNull Object id1073_type_NON_NULL_level_VARIABLE
  ) {
    return new Object();
  }

  // ============== "NonNullByDefault" by eclipse can also apply to methods or arguments ==============
  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  public Object id1074_type_NON_NULL_level_METHOD(
    // Annotations on method does not impact arguments
    Object id1075_type_UNKNOWN,
    @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER) Object id1076_type_NON_NULL_level_VARIABLE
  ) {
    return new Object();
  }

  public Object id1077_type_UNKNOWN(
    Object id1078_type_UNKNOWN,
    @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER) Object id1079_type_NON_NULL_level_VARIABLE
  ) {
    return new Object();
  }

}
