package checks;

import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.GetMapping;

public class MissingPathVariableAnnotationCheck_classAndRecord {
  static class ReportPeriod {
    private String project;
    private int year;
    private String month;

    public String getProject() {
      return project;
    }

    public int getYear() {
      return year;
    }

    public String getMonth() {
      return month;
    }

    public void setProject(String project) {
      this.project = project;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public void setMonth(String month) {
      this.month = month;
    }
  }

  record ReportPeriodRecord(String project, int year, String month) {
  }

  static class ReportPeriodBind {
    @GetMapping("/reports/{project}/{year}/{month}")
    public String getReport(ReportPeriod period) {
      // Spring sees {project} in the URL and calls period.setProject()
      // Spring sees {year} in the URL and calls period.setYear()
      return "reportDetails";
    }

    @GetMapping("/reports/{project}/{year}/{month}")
    public String getAnotherReport(ReportPeriodRecord period) {
      // Spring sees {project} in the URL and calls period.project()
      // Spring sees {year} in the URL and calls period.year()
      return "reportDetails";
    }

    public record Order(@BindParam("order-name") String orderName, String details){}

    @GetMapping("/{order-name}/details")
    public String getOrderDetails(Order order){
      // Spring sees {order-name} in the URL and calls order.orderName()
      return order.details();
    }

    @GetMapping("/{orderName}/details") // Noncompliant {{Bind template variable "orderName" to a method parameter.}}
    public String getOrderDetailsWrongParameterName(Order order){
      // Spring sees {orderName} in the URL and can't find order's orderName because of the wrong binding
      return order.details();
    }
  }

}
