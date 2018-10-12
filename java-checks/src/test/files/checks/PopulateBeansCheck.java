import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.beans.PropertyAccessor;

class Company {
  String name;
  void fun(PropertyAccessor pa){
    Company bean = new Company();
    java.util.Map<String, Object> map = new java.util.HashMap();
    Enumeration names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      map.put(name, request.getParameterValues(name));
    }
    BeanUtils.populate(bean, map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    BeanUtils.setProperty(bean, "name", "value"); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    BeanUtilsBean.populate(bean, map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    BeanUtilsBean bub = new BeanUtilsBean();
    bub.setProperty(bean, "name", "value"); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    pa.setPropertyValue("name", bean); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
    pa.setPropertyValues(map); // Noncompliant {{Make sure that setting JavaBean properties is safe here.}}
  }
}
