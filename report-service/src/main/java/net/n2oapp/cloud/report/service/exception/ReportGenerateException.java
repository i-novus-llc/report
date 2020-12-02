package net.n2oapp.cloud.report.service.exception;

public class ReportGenerateException extends RuntimeException {

    public ReportGenerateException() {
    }

    public ReportGenerateException(String message) {
        super(message);
    }

    public ReportGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportGenerateException(Throwable cause) {
        super(cause);
    }

    public ReportGenerateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
