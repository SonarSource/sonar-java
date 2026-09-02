package com.example.modulea;

import com.example.common.ReportingService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ExcelReportingService implements ReportingService {
    @Override
    public String generate() {
        return "excel-report";
    }
}
