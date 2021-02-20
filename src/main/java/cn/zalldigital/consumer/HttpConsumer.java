package cn.zalldigital.consumer;


import cn.zalldigital.ZallDataAnalytics;
import cn.zalldigital.exception.HttpConsumerException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class HttpConsumer implements Closeable {

    private CloseableHttpClient httpClient;
    private final String serverUrl;
    private final Map<String, String> httpHeaders;

    public HttpConsumer(String serverUrl, Map<String, String> httpHeaders) {
        this.serverUrl = serverUrl;
        this.httpHeaders = httpHeaders;
        initHttpClient();
    }

    private void initHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClients.custom().setUserAgent("Zall Data Analytics Java SDK " + ZallDataAnalytics.SDK_VERSION)
                            .setMaxConnTotal(100)
                            .build();
                }
            }
        }
    }

    public void consume(final String data) throws IOException, HttpConsumerException {
        HttpUriRequest request = getHttpRequest(data);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            int httpStatusCode = response.getStatusLine().getStatusCode();
            if (httpStatusCode < 200 || httpStatusCode >= 300) {
                String httpContent = new String(EntityUtils.toByteArray(response.getEntity()), StandardCharsets.UTF_8);

                throw new HttpConsumerException(
                        String.format("Unexpected response %d from Zall Data Analytics: %s", httpStatusCode, httpContent),
                        data, httpStatusCode, httpContent);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    HttpUriRequest getHttpRequest(final String data) throws IOException {
        HttpPost httpPost = new HttpPost(this.serverUrl);
        httpPost.setEntity(getHttpEntry(data));

        if (this.httpHeaders != null) {
            for (Map.Entry<String, String> entry : this.httpHeaders.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return httpPost;
    }

    StringEntity getHttpEntry(final String data) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream os = new ByteArrayOutputStream(bytes.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(bytes);
        gos.close();

        byte[] compressed = os.toByteArray();
        os.close();

        return  new StringEntity(new String(Base64.encodeBase64(compressed)));
    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                synchronized (this) {
                    if (httpClient != null) {
                        httpClient.close();
                        httpClient = null;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
