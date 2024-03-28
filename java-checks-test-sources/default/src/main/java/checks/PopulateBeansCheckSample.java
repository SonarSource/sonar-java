package checks;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.beans.PropertyAccessor;

class S4512_Company {

  void fun(PropertyAccessor pa) throws Exception {
    S4512_Company bean = new S4512_Company();
    java.util.Map<String, Object> map = new java.util.HashMap();
    BeanUtils.populate(bean, map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    BeanUtils.setProperty(bean, "name", "value"); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    BeanUtilsBean bub = new BeanUtilsBean();
    bub.populate(bean, map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    bub.setProperty(bean, "name", "value"); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    pa.setPropertyValue("name", bean); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    pa.setPropertyValues(map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
  }
}
