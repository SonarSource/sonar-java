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
    BeanUtils.populate(bean, map); // Noncompliant {{Validate that properties are not set by user input. Sanitize it if this is the case.}}
    BeanUtilsBean.populate(bean, map); // Noncompliant {{Validate that properties are not set by user input. Sanitize it if this is the case.}}
    pa.setPropertyValue("name", bean); // Noncompliant {{Validate that properties are not set by user input. Sanitize it if this is the case.}}
  }
}
