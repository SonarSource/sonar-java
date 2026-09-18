package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClassLoaderConsumer {

  @Autowired
  private BeanClassLoaderAware contextAware;
}
