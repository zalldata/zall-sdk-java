package cn.zalldigital.consumer;

import cn.zalldigital.exception.HttpConsumerException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugConsumer implements Consumer {

    final HttpConsumer httpConsumer;

    public DebugConsumer(final String serverUrl, final boolean writeData) {
        String debugUrl = null;
        try {
            // 将 URI Path 替换成 Debug 模式的 '/debug'
            URIBuilder builder = new URIBuilder(new URI(serverUrl));
            String[] urlPathes = builder.getPath().split("/");
            urlPathes[urlPathes.length - 1] = "debug";
            builder.setPath(strJoin(urlPathes, "/"));
            debugUrl = builder.build().toURL().toString();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }

        Map<String, String> headers = new HashMap<String, String>();
        if (!writeData) {
            headers.put("Dry-Run", "true");
        }

        this.httpConsumer = new HttpConsumer(debugUrl, headers);
//        this.httpUtils = new HttpUtils(serverUrl, headers);
    }

    @Override public void send(Map<String, Object> message) {
        List<Map<String, Object>> messageList = new ArrayList<Map<String, Object>>();
        messageList.add(message);

        String sendingData;
        try {
            sendingData = new Gson().toJson(messageList);
        } catch (JsonIOException e) {
            throw new RuntimeException("Failed to serialize data.", e);
        }

        System.out.println("==========================================================================");

        try {
            httpConsumer.consume(sendingData);
            System.out.printf("valid message: %s%n", sendingData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message with DebugConsumer.", e);
        } catch (HttpConsumerException e) {
            System.out.printf("invalid message: %s%n", e.getSendingData());
            System.out.printf("http status code: %d%n", e.getHttpStatusCode());
            System.out.printf("http content: %s%n", e.getHttpContent());
            throw new RuntimeException(e);
        }
    }

    @Override public void flush() {
        // do NOTHING
    }

    @Override public void close() {
        httpConsumer.close();
    }

    private static String strJoin(String[] arr, String sep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = arr.length; i < il; i++) {
            if (i > 0)
                sbStr.append(sep);
            sbStr.append(arr[i]);
        }
        return sbStr.toString();
    }
}
