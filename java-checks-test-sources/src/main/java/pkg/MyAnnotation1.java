package pkg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.TYPE_USE})
public @interface MyAnnotation1 {
}
