package checks.helpers;

import kotlin.Metadata;
import kotlin.coroutines.jvm.internal.DebugMetadata;

@Metadata(
  d1 = {"U+200B zero-width space: '​'"}, // Noncompliant {{Recognized as generated.}}
  d2 = {"U+200B zero-width space: '​'"}) // Compliant
public class GeneratedStringLiteralRecognizerSample {

  String value = "U+200B zero-width space: '​'"; // Compliant
  char character = 'a'; // Compliant

}

@kotlin.jvm.internal.SourceDebugExtension({
  "U+200B zero-width space: '​'", // Noncompliant
  // Noncompliant@+1
  """
    U+200B zero-width space: '​'
    """})
class SourceDebugExtensionSample {
}

@DebugMetadata(
  c = "U+200B zero-width space: '​'", // Noncompliant
  f = "U+200B zero-width space: '​'", // Noncompliant
  m = "U+200B zero-width space: '​'", // Noncompliant
  n = {"U+200B zero-width space: '​'"}) // Noncompliant
class DebugMetadataSample {
}

@NotKotlinMetadata(d1 = {"U+200B zero-width space: '​'"}) // Compliant
class MetadataLookalikeSample {
}

class ShadowedMetadataSample {

  @interface Metadata {
    String[] d1();
  }

  // This is not kotlin.Metadata, so its d1 value is not considered generated
  @Metadata(d1 = {"U+200B zero-width space: '​'"}) // Compliant
  class Annotated {
  }
}

@interface NotKotlinMetadata {
  String[] d1();
}
