package cn.zalldigital.consumer;

import java.util.Map;

public interface Consumer {

    void send(Map<String, Object> message);

    void flush();

    void close();
}
