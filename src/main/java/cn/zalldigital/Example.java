package cn.zalldigital;


import cn.zalldigital.consumer.BatchConsumer;
import cn.zalldigital.exception.InvalidArgumentException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Example {

    public static void main(String[] args) throws InvalidArgumentException {
        final String serverUrl = "http://{ip}:{prot}/a?project={project}&service={service}";
        final int bulkSize = 80;
        final ZallDataAnalytics za = new ZallDataAnalytics(new BatchConsumer(serverUrl, bulkSize));


        final String logDirectory = ".";
//        final ZallDataAnalytics za = new ZallDataAnalytics(new LoggerConsumer(logDirectory));

        // 用户未登录时，可以使用产品自己生成的cookieId来标注用户
        String cookieId = "abcdefgABCDEFG123456789";
        Map<String, Object> properties = new HashMap<>();

        properties.put("$time", new Date());
        properties.put("$os", "Windows");
        properties.put("$os_version", "8.1");
        properties.put("$ip", "127.0.0.1");
        properties.put("aaa", "qwerqwer");
        properties.put("$project","shoptest");
        properties.put("$token","qwertyuiop");

        // 记录访问首页这个event
        za.track(cookieId, false, "ghc001", properties);

        za.flush();
        za.shutdown();
    }

}
