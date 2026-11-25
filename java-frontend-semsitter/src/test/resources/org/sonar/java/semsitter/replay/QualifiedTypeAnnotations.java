package annotated;

import annotated.Tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class Tags {
  @Target(ElementType.TYPE_USE)
  @interface Tag {
  }
}

abstract class QualifiedTypeAnnotations {
  class Inner {
  }

  abstract <T> @Tag QualifiedTypeAnnotations.Inner m();
}
