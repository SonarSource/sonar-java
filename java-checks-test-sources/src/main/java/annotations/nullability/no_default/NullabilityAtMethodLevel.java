package annotations.nullability.no_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.DefaultLocation;

/**
 * Test Nullability annotation when the element is indirectly annotated, via method annotation
 */
public class NullabilityAtMethodLevel {

  Object id2000_type_NO_ANNOTATION_level_PACKAGE;

  public Object id2001_type_NO_ANNOTATION_level_PACKAGE(
    Object id2002_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }

  // ============== Method level annotations changing the nullability value of parameters ==============
  @javax.annotation.ParametersAreNonnullByDefault
  public Object id2003_type_NO_ANNOTATION_level_PACKAGE(
    Object id2004_type_NON_NULL_level_METHOD) {
    return new Object();
  }

  @org.eclipse.jdt.annotation.NonNullByDefault
  public Object id2005_type_NON_NULL_level_METHOD(
    Object id2006_type_NON_NULL_level_METHOD) {
    return new Object();
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER)
  public Object id2007_type_NO_ANNOTATION_level_PACKAGE(
    Object id2008_type_NON_NULL_level_METHOD) {
    return new Object();
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
  public Object id2009_type_NO_ANNOTATION_level_PACKAGE(
    Object id2010_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }

  // ============== VARIABLE level annotations have the priority over METHOD level ==============
  @javax.annotation.ParametersAreNonnullByDefault
  public Object id2011_type_NO_ANNOTATION_level_PACKAGE(
    @javax.annotation.Nullable Object id2012_type_WEAK_NULLABLE_level_VARIABLE,
    @javax.annotation.CheckForNull Object id2013_type_STRONG_NULLABLE_level_VARIABLE) {
    return new Object();
  }

  // ============== annotations targeting method have no effects on parameters ==============
  @javax.annotation.Nullable
  public Object id2014_type_WEAK_NULLABLE_level_METHOD(
    Object id2015_type_NO_ANNOTATION_level_PACKAGE,
    @javax.annotation.CheckForNull Object id2016_type_STRONG_NULLABLE_level_VARIABLE) {
    return new Object();
  }

  // ============== ParametersAreNullableByDefault ==============
  @javax.annotation.ParametersAreNullableByDefault
  public Object id2017_type_NO_ANNOTATION_level_PACKAGE(
    Object id2018_type_WEAK_NULLABLE_level_METHOD) {
    return new Object();
  }

  @javax.annotation.ParametersAreNullableByDefault
  public Object id2019_type_NO_ANNOTATION_level_PACKAGE(
    // Variable level has the priority
    @Nonnull Object id2020_type_NON_NULL_level_VARIABLE) {
    return new Object();
  }

}

abstract class NullabilityAtMethodLevelParent {
  abstract int id2021_type_NO_ANNOTATION_level_PACKAGE();

  @Nullable
  Object id2022_type_WEAK_NULLABLE_level_METHOD() {
    return new NullabilityAtMethodLevelParent() {
      @Override
      int id2021_type_NO_ANNOTATION_level_PACKAGE() {
        return 0;
      }
    };
  }
}
