package cn.zalldigital.consumer;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class ConsoleConsumer implements Consumer {

    private final Writer writer;

    public ConsoleConsumer(final Writer writer) {
        this.writer = writer;
    }

    @Override public void send(Map<String, Object> message) {
        try {
            synchronized (writer) {
                writer.write(new Gson().toJson(message));
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump message with ConsoleConsumer.", e);
        }
    }

    @Override public void flush() {
        try {
            synchronized (writer) {
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to flush with ConsoleConsumer.", e);
        }
    }

    @Override public void close() {
        flush();
        try {
            synchronized (writer) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
