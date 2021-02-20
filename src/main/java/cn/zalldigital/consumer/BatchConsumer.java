package cn.zalldigital.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BatchConsumer implements Consumer {

    private final List<Map<String, Object>> messageList;
    private final static int MAX_FLUSH_BULK_SIZE = 50;
    private final HttpConsumer httpConsumer;
    private final boolean isThrowException;
    private final int bulkSize;

    public BatchConsumer (final String serverUrl) {
        this(serverUrl, MAX_FLUSH_BULK_SIZE);
    }

    public BatchConsumer (final String serverUrl, final int bulkSize) {
        this(serverUrl, null, bulkSize, true);
    }

    public BatchConsumer (final String serverUrl, final Map<String, String> httpHeaders, final int bulkSize,
                          final boolean isThrowException) {

        this.messageList = new LinkedList<>();
        this.httpConsumer = new HttpConsumer(serverUrl, httpHeaders);
        this.isThrowException = isThrowException;
        this.bulkSize = Math.min(bulkSize, MAX_FLUSH_BULK_SIZE);
    }

    @Override
    public void send(Map<String, Object> message) {
        synchronized (messageList) {
            messageList.add(message);
            if (messageList.size() >= bulkSize) {
                flush();
            }
        }
    }

    @Override
    public void flush() {
        synchronized (messageList) {
            while (!messageList.isEmpty()) {
                List<Map<String, Object>> sendList = messageList.subList(0, Math.min(bulkSize, messageList.size()));

                String sendingData;
                try {
                    sendingData = new Gson().toJson(sendList);
                } catch (JsonIOException e) {
                    sendList.clear();
                    if (isThrowException) {
                        throw new RuntimeException("Failed to serialize data.", e);
                    }
                    continue;
                }

                try {
                    sendList.clear();
                    httpConsumer.consume(sendingData);
                } catch (Exception e) {
                    if (isThrowException) {
                        throw new RuntimeException("Failed to dump message with BatchConsumer.", e);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void close() {
        flush();
        httpConsumer.close();
    }
}
