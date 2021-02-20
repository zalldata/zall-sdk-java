package cn.zalldigital.exception;

public class HttpConsumerException extends Exception {

    final String sendingData;
    final int httpStatusCode;
    final String httpContent;

    public HttpConsumerException(String error, String sendingData, int httpStatusCode, String httpContent) {
        super(error);
        this.sendingData = sendingData;
        this.httpStatusCode = httpStatusCode;
        this.httpContent = httpContent;
    }

    public String getSendingData() {
        return sendingData;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getHttpContent() {
        return httpContent;
    }
}
