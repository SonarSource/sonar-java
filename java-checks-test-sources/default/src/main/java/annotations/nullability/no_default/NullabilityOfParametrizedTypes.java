package annotations.nullability.no_default;


import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * This check ensures that type parameters and nested annotations are properly handled by JSymbolMetadata
 */
public class NullabilityOfParametrizedTypes {

  public Map<Object, Object> id1000_type_NO_ANNOTATION_level_PACKAGE;
  public List<Object> id1001_type_NO_ANNOTATION_level_PACKAGE;

  public Map<Object, @Nullable Object> id1002_type_NO_ANNOTATION_level_PACKAGE;
  public List<@Nullable Object> id1003_type_NO_ANNOTATION_level_PACKAGE;

  public Map<Object, @NonNull Object> id1004_type_NO_ANNOTATION_level_PACKAGE;
  public List<@NonNull Object> id1005_type_NO_ANNOTATION_level_PACKAGE;

  public Map<Object, @org.eclipse.jdt.annotation.Nullable Object> id1006_type_NO_ANNOTATION_level_PACKAGE;
  public List<@org.eclipse.jdt.annotation.Nullable Object> id1007_type_NO_ANNOTATION_level_PACKAGE;

  public Map.@Nullable Entry<@NonNull Object, @NonNull Object> id1008_type_STRONG_NULLABLE_level_VARIABLE;

  public List<@Nullable Object> id1009_type_NO_ANNOTATION_level_PACKAGE(
    Map<Object, @NonNull Object> id10010_type_NO_ANNOTATION_level_PACKAGE
  ) {
    return List.of();
  }
}
