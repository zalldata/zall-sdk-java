package cn.zalldigital;

import cn.zalldigital.consumer.Consumer;
import cn.zalldigital.exception.InvalidArgumentException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ZallDataAnalytics {

    public static final String SDK_VERSION = "1.0.0";

    private static final Pattern KEY_PATTERN = Pattern.compile("^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$", Pattern.CASE_INSENSITIVE);

    private final Consumer consumer;

    private final Map<String, Object> superProperties;

    /**
     * 历史数据导入
     */
    private boolean enableTimeFree = false;

    public ZallDataAnalytics(final Consumer consumer) {
        this.consumer = consumer;

        this.superProperties = new ConcurrentHashMap<>();
        clearSuperProperties();
    }

    public boolean isEnableTimeFree() {
        return enableTimeFree;
    }

    public void setEnableTimeFree(boolean enableTimeFree) {
        this.enableTimeFree = enableTimeFree;
    }

    /**
     * 设置每个事件都带有的一些公共属性
     *
     * 当track的Properties，superProperties和SDK自动生成的automaticProperties有相同的key时，遵循如下的优先级：
     *    track.properties 高于 superProperties 高于 automaticProperties
     *
     * 另外，当这个接口被多次调用时，是用新传入的数据去merge先前的数据
     *
     * 例如，在调用接口前，dict是 {"a":1, "b": "bbb"}，传入的dict是 {"b": 123, "c": "asd"}，则merge后
     * 的结果是 {"a":1, "b": 123, "c": "asd"}
     *
     * @param superPropertiesMap    一个或多个公共属性
     */
    public void registerSuperProperties(Map<String, Object> superPropertiesMap) {
        for (Map.Entry<String, Object> item : superPropertiesMap.entrySet()) {
            this.superProperties.put(item.getKey(), item.getValue());
        }
    }

    /**
     * 记录一个拥有一个或多个属性的事件。属性取值可接受类型为{@link Number}, {@link String}, {@link Date}和
     * {@link List}；
     * 若属性包含 $time 字段，则它会覆盖事件的默认时间属性，该字段只接受{@link Date}类型；
     * 若属性包含 $project 字段，则它会指定事件导入的项目；
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     * @param eventName     事件名称
     * @param properties    事件的属性
     *
     * @throws InvalidArgumentException eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void track(String distinctId, boolean isLoginId, String eventName,
                      Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "track", eventName, properties);
    }

    /**
     * 记录用户注册事件
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * 并在必要时联系我们的技术支持人员。
     *
     * 属性取值可接受类型为{@link Number}, {@link String}, {@link Date}和{@link List}；
     * 若属性包含 $time 字段，它会覆盖事件的默认时间属性，该字段只接受{@link Date}类型；
     * 若属性包含 $project 字段，则它会指定事件导入的项目；
     *
     * @param loginId       登录 ID
     * @param anonymousId   匿名 ID
     * @param properties    事件的属性
     *
     * @throws InvalidArgumentException eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void trackSignUp(String loginId, String anonymousId,
                            Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(loginId, false, anonymousId, "track_signup", "$SignUp", properties);
    }

    /**
     * 设置用户的属性。属性取值可接受类型为{@link Number}, {@link String}, {@link Date}和{@link List}；
     *
     * 如果要设置的properties的key，之前在这个用户的profile中已经存在，则覆盖，否则，新创建
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID， false 表示该 ID 是一个匿名 ID
     * @param properties    用户的属性
     *
     * @throws InvalidArgumentException eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void profileSet(String distinctId, boolean isLoginId,
                           Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "profile_set", null, properties);
    }

    /**
     * 首次设置用户的属性。
     * 属性取值可接受类型为{@link Number}, {@link String}, {@link Date}和{@link List}；
     *
     * 与profileSet接口不同的是：
     * 如果要设置的properties的key，在这个用户的profile中已经存在，则不处理，否则，新创建
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     * @param properties    用户的属性
     *
     * @throws InvalidArgumentException     eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void profileSetOnce(String distinctId, boolean isLoginId,
                               Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "profile_set_once", null, properties);
    }

    /**
     * 为用户的一个或多个数值类型的属性累加一个数值，若该属性不存在，则创建它并设置默认值为0。属性取值只接受
     * {@link Number}类型
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     * @param properties    用户的属性
     *
     * @throws InvalidArgumentException     eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void profileIncrement(String distinctId, boolean isLoginId,
                                 Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "profile_increment", null, properties);
    }

    /**
     * 为用户的一个或多个数组类型的属性追加字符串，属性取值类型必须为 {@link List}，且列表中元素的类型
     * 必须为 {@link String}
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     * @param properties    用户的属性
     *
     * @throws InvalidArgumentException     eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void profileAppend(String distinctId, boolean isLoginId,
                              Map<String, Object> properties) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "profile_append", null, properties);
    }

    /**
     * 删除用户某一个属性
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     * @param property      属性名称
     *
     * @throws InvalidArgumentException     eventName 或 properties 不符合命名规范和类型规范时抛出该异常
     */
    public void profileUnset(String distinctId, boolean isLoginId, String property) throws InvalidArgumentException {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(property, true);
        addEvent(distinctId, isLoginId, null, "profile_unset", null, properties);
    }

    /**
     * 删除用户所有属性
     *
     * @param distinctId    用户 ID
     * @param isLoginId     用户 ID 是否是登录 ID，false 表示该 ID 是一个匿名 ID
     *
     * @throws InvalidArgumentException     distinctId 不符合命名规范时抛出该异常
     */
    public void profileDelete(String distinctId, boolean isLoginId) throws InvalidArgumentException {

        addEvent(distinctId, isLoginId, null, "profile_delete", null, new HashMap<String, Object>());
    }

    /**
     * 设置 item
     *
     * @param itemType      item 类型
     * @param itemId        item ID
     * @param properties    item 相关属性
     * @throws InvalidArgumentException     取值不符合规范抛出该异常
     */
    public void itemSet(String itemType, String itemId,
                        Map<String, Object> properties) throws InvalidArgumentException {

        addItem(itemType, itemId, "item_set", properties);
    }

    /**
     * 删除 item
     *
     * @param itemType      item 类型
     * @param itemId        item ID
     * @throws InvalidArgumentException     取值不符合规范抛出该异常
     */
    public void itemDelete(String itemType, String itemId) throws InvalidArgumentException {

        addItem(itemType, itemId, "item_delete", null);
    }

    /**
     * 清除公共属性
     */
    public void clearSuperProperties() {
        this.superProperties.clear();
        this.superProperties.put("$lib", "Java");
        this.superProperties.put("$lib_version", SDK_VERSION);
    }

    /**
     * 立即发送缓存中的所有日志
     */
    public void flush() {
        this.consumer.flush();
    }

    /**
     * 停止ZallDataAPI所有线程，API停止前会清空所有本地数据
     */
    public void shutdown() {
        this.consumer.close();
    }

    private void addEvent(String distinctId, boolean isLoginId, String originDistinceId, String actionType,
                          String eventName, Map<String, Object> properties) throws InvalidArgumentException {

        assertKey("Distinct Id", distinctId);
        assertProperties(actionType, properties);
        if ("track".equals(actionType)) {
            assertKeyWithRegex("Event Name", eventName);
        } else if ("track_signup".equals(actionType)) {
            assertKey("Original Distinct Id", originDistinceId);
        }

        // Event time
        long time = System.currentTimeMillis();
        if (properties != null && properties.containsKey("$time")) {
            Date eventTime = (Date) properties.get("$time");
            properties.remove("$time");
            time = eventTime.getTime();
        }

        String eventProject = null;
        if (properties != null && properties.containsKey("$project")) {
            eventProject = (String) properties.get("$project");
            properties.remove("$project");
        }

        Map<String, Object> eventProperties = new HashMap<>();
        if ("track".equals(actionType) || "track_signup".equals(actionType)) {
            eventProperties.putAll(superProperties);
        }
        if (properties != null) {
            eventProperties.putAll(properties);
        }

        if (isLoginId) {
            eventProperties.put("$is_login_id", true);
        }

        Map<String, String> libProperties = getLibProperties();

        Map<String, Object> event = new HashMap<String, Object>();

        event.put("type", actionType);
        event.put("time", time);
        event.put("distinct_id", distinctId);
        event.put("properties", eventProperties);
        event.put("lib", libProperties);

        if (eventProject != null) {
            event.put("project", eventProject);
        }

        if (enableTimeFree) {
            event.put("time_free", true);
        }

        if ("track".equals(actionType)) {
            event.put("event", eventName);
        } else if ("track_signup".equals(actionType)) {
            event.put("event", eventName);
            event.put("original_id", originDistinceId);
        }

        this.consumer.send(event);
    }

    private void addItem(String itemType, String itemId, String actionType,
                         Map<String, Object> properties) throws InvalidArgumentException {

        assertKeyWithRegex("Item Type", itemType);
        assertKey("Item Id", itemId);
        assertProperties(actionType, properties);

        String eventProject = null;
        if (properties != null && properties.containsKey("$project")) {
            eventProject = (String) properties.get("$project");
            properties.remove("$project");
        }

        Map<String, Object> eventProperties = new HashMap<String, Object>();
        if (properties != null) {
            eventProperties.putAll(properties);
        }

        Map<String, String> libProperties = getLibProperties();

        Map<String, Object> record = new HashMap<String, Object>();
        record.put("type", actionType);
        record.put("time", System.currentTimeMillis());
        record.put("properties", eventProperties);
        record.put("lib", libProperties);

        if (eventProject != null) {
            record.put("project", eventProject);
        }

        record.put("item_type", itemType);
        record.put("item_id", itemId);
        this.consumer.send(record);
    }

    private Map<String, String> getLibProperties() {
        Map<String, String> libProperties = new HashMap<String, String>();
        libProperties.put("$lib", "Java");
        libProperties.put("$lib_version", SDK_VERSION);
        libProperties.put("$lib_method", "code");

        if (this.superProperties.containsKey("$app_version")) {
            libProperties.put("$app_version", (String) this.superProperties.get("$app_version"));
        }

        StackTraceElement[] trace = (new Exception()).getStackTrace();

        if (trace.length > 3) {
            StackTraceElement traceElement = trace[3];
            libProperties.put("$lib_detail", String.format("%s##%s##%s##%s", traceElement.getClassName(),
                    traceElement.getMethodName(), traceElement.getFileName(), traceElement.getLineNumber()));
        }

        return libProperties;
    }

    private void assertKey(String type, String key) throws InvalidArgumentException {
        if (key == null || key.length() < 1) {
            throw new InvalidArgumentException("The " + type + " is empty.");
        }
        if (key.length() > 255) {
            throw new InvalidArgumentException("The " + type + " is too long, max length is 255.");
        }
    }

    private void assertKeyWithRegex(String type, String key) throws InvalidArgumentException {
        assertKey(type, key);
        if (!(KEY_PATTERN.matcher(key).matches())) {
            throw new InvalidArgumentException("The " + type + "'" + key + "' is invalid.");
        }
    }

    private void assertProperties(String eventType, Map<String, Object> properties) throws InvalidArgumentException {
        if (null == properties) { return; }

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if ("$is_login_id".equals(property.getKey())) {
                if (!(property.getValue() instanceof  Boolean)) {
                    throw new InvalidArgumentException("The property value of '$is_login_id' should be "
                            + "Boolean.");
                }
                continue;
            }

            assertKeyWithRegex("property", property.getKey());

            if (!(property.getValue() instanceof Number) && !(property.getValue() instanceof Date) && !
                    (property.getValue() instanceof String) && !(property.getValue() instanceof Boolean) &&
                    !(property.getValue() instanceof List<?>)) {
                throw new InvalidArgumentException("The property '" + property.getKey() + "' should be a basic type: "
                        + "Number, String, Date, Boolean, List<String>.");
            }

            if ("$time".equals(property.getKey()) && !(property.getValue() instanceof Date)) {
                throw new InvalidArgumentException(
                        "The property '$time' should be a java.util.Date.");
            }

            // List 类型的属性值，List 元素必须为 String 类型
            if (property.getValue() instanceof List<?>) {
                for (final ListIterator<Object> it = ((List<Object>)property.getValue()).listIterator
                        (); it.hasNext();) {
                    Object element = it.next();
                    if (!(element instanceof String)) {
                        throw new InvalidArgumentException("The property '" + property.getKey() + "' should be a list of String.");
                    }
                    if (((String) element).length() > 8192) {
                        it.set(((String) element).substring(0, 8192));
                    }
                }
            }

            // String 类型的属性值，长度不能超过 8192
            if (property.getValue() instanceof String) {
                String value = (String) property.getValue();
                if (value.length() > 8192) {
                    property.setValue(value.substring(0, 8192));
                }
            }

            if ("profile_increment".equals(eventType)) {
                if (!(property.getValue() instanceof Number)) {
                    throw new InvalidArgumentException("The property value of PROFILE_INCREMENT should be a "
                            + "Number.");
                }
            } else if ("profile_append".equals(eventType)) {
                if (!(property.getValue() instanceof List<?>)) {
                    throw new InvalidArgumentException("The property value of PROFILE_INCREMENT should be a "
                            + "List<String>.");
                }
            }
        }
    }
}
