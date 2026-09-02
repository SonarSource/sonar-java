package com.example.modulea;

import com.example.common.ReportingService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class PdfReportingService implements ReportingService {
    @Override
    public String generate() {
        return "pdf-report";
    }
}
