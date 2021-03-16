package cn.zalldigital;


import cn.zalldigital.consumer.BatchConsumer;
import cn.zalldigital.exception.InvalidArgumentException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Example {

    public static void main(String[] args) throws InvalidArgumentException, ParseException {
        /**
         * hostname  : 域名
         * project   : 项目名称
         * service   : 服务名称
         * token     : 校验token
         */
        final String serverUrl = "http://${hostname}/a?project=${project}&service=${service}&token=${token}";
        final int bulkSize = 80;
        final ZallDataAnalytics za = new ZallDataAnalytics(new BatchConsumer(serverUrl, bulkSize));


        final String logDirectory = ".";
//        final ZallDataAnalytics za = new ZallDataAnalytics(new LoggerConsumer(logDirectory));

        // 用户未登录时，可以使用产品自己生成的cookieId来标注用户
        String cookieId = "abcdefgABCDEFG123456789";
        Map<String, Object> properties = new HashMap<>();

        // 1.1 访问首页

        // 前面有$开头的property字段，是ZA提供给用户的预置字段
        // 对于预置字段，已经确定好了字段类型和字段的显示名
        properties.clear();
        properties.put("$time", new Date());                // 这条event发生的时间，如果不设置的话，则默认是当前时间
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("Channel", "baidu");                 // 用户是通过baidu这个渠道过来的
        properties.put("$project","zall");
        properties.put("$token","fe490ad5-06ec-4c1f-8531-330ad9b6704b");
        za.track(cookieId, false, "ViewHomePage", properties); // 记录访问首页这个event

        // 1.2 搜索商品a
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("KeyWord", "XX手机");                 // 搜索引擎引流过来时使用的关键词
        za.track(cookieId, false, "SearchProduct", properties);      // 记录搜索商品这个event

        // 1.3 浏览商品
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("ProductName", "xx手机");             // 商品名称
        properties.put("ProductType", "智能手机");            // 商品类别
        properties.put("ShopName", "XX官方旗舰店");           // 店铺名称
        za.track(cookieId, false, "ViewProduct", properties);      // 记录浏览商品这个event

        // 2. 用户决定注册了
        String registerId = "123456789";                    // 用户注册时，分配给用户的注册Id

        // 2.1 通过，trackSignUP，把匿名ID和注册ID贯通起来
        properties.clear();
        properties.put("RegisterChannel", "baidu");         // 用户的注册渠道
        za.trackSignUp(registerId, cookieId);

        // 2.2 用户注册时，填充了一些个人信息，可以用Profile接口记录下来
        Map<String, Object> profiles = new HashMap<String, Object>();
        profiles.put("$city", "武汉");                        // 用户所在城市
        profiles.put("$province", "湖北");                    // 用户所在省份
        profiles.put("$name", "昵称abc");                     // 用户的昵称
        profiles.put("$signup_time", new Date());            // 注册时间
        profiles.put("Gender", "male");                      // 用户的性别
                                                             // 用户的出生日期，特别注意，这个地方填入年龄是不合适的，因为年龄会随着时间而变化
        profiles.put("Birthday", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1988-11-03 00:00:00"));
        profiles.put("RegisterChannel", "baidu");            // 用户的注册渠道
        za.profileSet(registerId, true, profiles);  // 此时传入的是注册ID了

        // 2.3 立刻刷新一下，让数据传到ZA中
        za.flush();

        // 3. 用户注册后，进行后续行为
        // 3.1 提交订单和提交订单详情 (这个订单里面包含一个手机和两个手机膜,订单的信息)
        properties.clear();
        properties.put("$os", "Windows");                    // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");                // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");            // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");        // 订单ID
        properties.put("ShipPrice", 10.0);                   // 运费
        properties.put("OrderTotalPrice", 2999.0);           // 订单的总价格，默认是元
        za.track(registerId, true, "SubmitOrder", properties); // 注意，此时使用的已经是注册ID了


        // 订单中手机这个商品的信息
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ProductName", "XX手机");             // 商品名称
        properties.put("ProductType", "智能手机");            // 商品类别
        properties.put("ShopName", "XX官方旗舰店");           // 店铺名称
        properties.put("ProductUnitPrice", 2999.0);         // 商品单价
        properties.put("ProductAmount", 1.0);               // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 2999.0);        // 商品总价
        za.track(registerId, true, "SubmitOrderDetail", properties);


        // 订单中手机膜这个商品的信息
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ProductName", "5寸钢化膜");           // 商品名称
        properties.put("ProductType", "手机配件");            // 商品类别
        properties.put("ShopName", "XX手机外设店");           // 店铺名称
        properties.put("ProductUnitPrice", 23.0);           // 商品单价
        properties.put("ProductAmount", 2.0);               // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 46.0);          // 商品总价
        za.track(registerId, true, "SubmitOrderDetail", properties);


        // 3.2 支付订单和支付订单详情 (整个订单的支付情况)
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ShipPrice", 10.0);                  // 运费
        properties.put("OrderTotalPrice", 2999.0);          // 订单的总价格，默认是元
        properties.put("PaymentMethod", "AliPay");          // 支付方式
        properties.put("AllowanceAmount", 30.0);            // 补贴金额
        properties.put("AllowanceType", "首次下单红包");      // 补贴类型
        properties.put("PaymentAmount", 2969.0);            // 实际支付的订单金额
        za.track(registerId, true, "PayOrder", properties);


        // 手机这个商品的支付情况
        properties.clear();
        properties.put("$os", "Windows");                     // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");                 // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");             // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");         // 订单ID
        properties.put("ProductName", "XX手机");               // 商品名称
        properties.put("ProductType", "智能手机");              // 商品类别
        properties.put("ShopName", "XX官方旗舰店");             // 店铺名称
        properties.put("ProductUnitPrice", 2999.0);           // 商品单价
        properties.put("ProductAmount", 1.0);                 // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 2999.0);          // 商品总价
        properties.put("ProductAllowanceAmount", 30.0);       // 假设这个补贴是在手机上的折扣
        properties.put("ProductAllowanceType", "首次下单红包");  // 补贴类型
        properties.put("ProductPaymentAmount", 2969.0);        // 手机实际支付了这么多
        properties.put("PaymentMethod", "AliPay");             // 与订单保持一致
        za.track(registerId, true, "PayOrderDetail", properties);


        // 手机膜这个商品的支付情况
        properties.clear();
        properties.put("$os", "Windows");                    // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");                // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");            // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");        // 订单ID
        properties.put("ProductName", "5寸钢化膜");            // 商品名称
        properties.put("ProductType", "手机配件");             // 商品类别
        properties.put("ShopName", "XX手机外设店");            // 店铺名称
        properties.put("ProductUnitPrice", 23.0);            // 商品单价
        properties.put("ProductAmount", 2.0);                // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 46.0);           // 商品总价
        properties.put("ProductAllowanceAmount", 0.0);       // 手机膜上并没有补贴
        properties.put("ProductAllowanceType", "无补贴");     // 补贴类型
        properties.put("ProductPaymentAmount", 46.0);        // 手机膜实际支付了这么多
        properties.put("PaymentMethod", "AliPay");           // 与订单保持一致
        za.track(registerId, true, "PayOrderDetail", properties);


        // 3.3 假设这个用户支付后反悔了，要取消订单 (整个订单的取消情况)
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ShipPrice", 10.0);                  // 运费
        properties.put("OrderTotalPrice", 2999.0);          // 订单的总价格，默认是元
        properties.put("CancelReason", "地址填写错误");       // 取消订单的原因
        properties.put("CancelTiming", "AfterPay");         // 取消订单的时机
        za.track(registerId, true, "CancelOrder", properties);


        // 手机这个商品的取消情况
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ProductName", "XX手机");             // 商品名称
        properties.put("ProductType", "智能手机");            // 商品类别
        properties.put("ShopName", "XX官方旗舰店");            // 店铺名称
        properties.put("ProductUnitPrice", 2999.0);          // 商品单价
        properties.put("ProductAmount", 1.0);                // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 2999.0);         // 商品总价
        properties.put("CancelReason", "地址填写错误");         // 取消订单的原因
        properties.put("CancelTiming", "AfterPay");           // 取消订单的时机
        za.track(registerId, true, "CancelOrderDetail", properties);


        // 手机膜这个商品的取消情况
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ProductName", "5寸钢化膜");           // 商品名称
        properties.put("ProductType", "手机配件");            // 商品类别
        properties.put("ShopName", "XX手机外设店");            // 店铺名称
        properties.put("ProductUnitPrice", 23.0);            // 商品单价
        properties.put("ProductAmount", 2.0);                // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 46.0);           // 商品总价
        properties.put("CancelReason", "地址填写错误");         // 取消订单的原因
        properties.put("CancelTiming", "AfterPay");           // 取消订单的时机
        za.track(registerId, true, "CancelOrderDetail", properties);


        // 4. 假设用户并没有取消订单，然后成功完成了配送 (手机这个商品的配送情况)
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");        // 订单ID
        properties.put("ProductName", "XX手机");             // 商品名称
        properties.put("ProductType", "智能手机");            // 商品类别
        properties.put("ShopName", "XX官方旗舰店");           // 店铺名称
        properties.put("ProductUnitPrice", 2999.0);         // 商品单价
        properties.put("ProductAmount", 1.0);               // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 2999.0);        // 商品总价
        properties.put("ProductAllowanceAmount", 30.0);     // 假设这个补贴是在手机上的折扣
        properties.put("ProductPaymentAmount", 2969.0);     // 手机实际支付了这么多
        properties.put("PaymentMethod", "AliPay");          // 与订单保持一致
        properties.put("SupplyTime", 48.0);                 // 订单运送用了48小时
        properties.put("SupplyMethod", "顺丰");              // 通过顺丰运送的
        za.track(registerId, true, "ReceiveProduct", properties);


        // 手机膜这个商品的配送情况
        properties.clear();
        properties.put("$os", "Windows");                   // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");               // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");           // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");       // 订单ID
        properties.put("ProductName", "5寸钢化膜");           // 商品名称
        properties.put("ProductType", "手机配件");            // 商品类别
        properties.put("ShopName", "XX手机外设店");           // 店铺名称
        properties.put("ProductUnitPrice", 23.0);           // 商品单价
        properties.put("ProductAmount", 2.0);               // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 46.0);          // 商品总价
        properties.put("ProductAllowanceAmount", 0.0);      // 手机膜上并没有补贴
        properties.put("ProductPaymentAmount", 46.0);       // 手机膜实际支付了这么多
        properties.put("PaymentMethod", "AliPay");          // 与订单保持一致
        properties.put("SupplyTime", 68.0);                 // 订单运送用了68小时
        properties.put("SupplyMethod", "中通");              // 通过中通运送的
        za.track(registerId, true, "ReceiveProduct", properties);


        // 5. 假设产品有质量问题，用户申请了售后服务
        // 5.1 手机申请了维修 (提交了售后服务)
        properties.clear();
        properties.put("$os", "Windows");               // 通过请求中的UA，可以解析出用户使用设备的操作系统是windows的
        properties.put("$os_version", "9.2");           // 操作系统的具体版本
        properties.put("$ip", "123.123.123.123");       // 请求中能够拿到用户的IP，则把这个传递给ZA，ZA会自动根据这个解析省份、城市
        properties.put("OrderId", "202001024897111");   // 订单ID
        properties.put("ProductName", "XX手机");         // 商品名称
        properties.put("ProductType", "智能手机");        // 商品类别
        properties.put("ShopName", "XX官方旗舰店");       // 店铺名称
        properties.put("ProductUnitPrice", 2999.0);     // 商品单价
        properties.put("ProductAmount", 1.0);           // 商品数量，可以是个数，也可以是重量
        properties.put("ProductTotalPrice", 2999.0);    // 商品总价
        properties.put("ProductAllowanceAmount", 30.0); // 假设这个补贴是在手机上的折扣
        properties.put("ProductPaymentAmount", 2969.0); // 手机实际支付了这么多
        properties.put("PaymentMethod", "AliPay");      // 与订单保持一致
        properties.put("ServiceContent", "维修");        // 手机申请了维修
        properties.put("ServiceStatus", "新提交");       // 售后服务的状态
        za.track(registerId, true, "ServiceAfterSale", properties);
        // 产品被收到，开始维修了，记录一条新的数据
        properties.put("ServiceStatus", "维修中");       // 售后服务的状态
        za.track(registerId, true, "ServiceAfterSale", properties);
        // 维修完成了，再记录一条新的数据
        properties.put("ServiceStatus", "维修完成");      // 售后服务的状态
        za.track(registerId, true, "ServiceAfterSale", properties);

        za.shutdown();                                  // 关闭API，关闭时会自动调用flush
    }

}
