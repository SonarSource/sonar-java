package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Three beans of type BeanClassLoaderAware exist; the unique @Primary among them is profiled, so with no
// profile active PlainClassLoaderComponentA and PlainClassLoaderComponentB compete: issue expected.
@Service
public class ClassLoaderConsumer {

  @Autowired
  private BeanClassLoaderAware contextAware;
}
