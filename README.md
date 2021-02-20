## Java SDK

Java SDK 主要用于服务端 Java 应用

### 1.集成卓尔数科智能分析SDK

    直接从 GitHub 下载 Java SDK 的源代码，并将其作为模块添加进项目中使用；

### 2. 初始化卓尔数科智能分析 SDK

在程序启动时（如 ***public static void main(String[] args)*** 方法中），调用构造函数 ***new ZallDataAnalytics(Consumer)*** 初始化 Java SDK 实例。

```java
public static void main(String[] args) {
    // 上报数据的url
    final String serverUrl = "http://${ip}:${port}/a?project=${project}&service=${service}";
    
    // 当缓存的数据量达到80条时，批量发送数据
    final int bulkSize = 80;
    
    // 使用 BatchConsumer 初始化 ZallDataAnalytics
    final ZallDataAnalytics za = new ZallDataAnalytics(new BatchConsumer(serverUrl, bulkSize));

    // 用户的 Distinct ID
    String distinctId = "abcdefgABCDEFG123456789";

    // 记录用户登录事件
    za.track(distinctId, true, "UserLogin");

    // 使用卓尔数科智能分析记录用户行为数据
    // ... 
}

```

### 3. 用户识别

在服务端应用中，卓尔数科智能分析也要求为每个事件设置用户的 Distinct ID，这有助于智能分析提供更准确的留存率等数据。

对于注册用户，推荐使用客户业务系统中的用户 ID 作为 Distinct ID，不建议使用用户名、Email、手机号码等可以被修改的信息作为 Distinct ID；

对于未注册的匿名用户，获取用户匿名 ID 的方法如下：

1. 后端获取前端 JavaScript SDK 生成的匿名 ID 的方式：
   可以在 Cookie 里面找到 key 为 ***zalldatajssdkcross*** 的 value 值然后进行 ***decodeURIComponent*** 解码，最后通过 ***JSON.parse*** 方法得到一个对象，对象里面的 distinct_id 的值即为所需要的 ID
   (注意，如果前端已经调用过 login 方法，那么此时 distinct_id 为登录 ID，所以需要先获取 first_id 字段的值，如果获取不到，就去获取 distinct_id 的值)。
2. 如果 App 中嵌入了 Login 方法，需要客户端使用神策提供的获取匿名 ID 接口，将匿名 ID 传给服务端，服务端使用客户端传过来的匿名 ID 作为 Distinct ID。

所有的 ***track*** 和 ***profile*** 系列方法都必须同时指定用户 ID（***distinctId***）和用户 ID 是否为登录 ID (***isLoginId***) 这两个参数，以便明确告知智能分析用户 ID 的类型。

#### 3.1. 用户注册/登录

通过 ***trackSignUp()*** 将匿名 ID 和登录 ID 关联，以保证用户分析的准确性。例如：

```java
// 前端的匿名 ID,获取匿名 ID 的方式参考上文
String anonymousId = "72235f35-a9ae-47fe-aeb5-14ce93339fbd";

String registerId = "9876543210";
// 用户注册/登录时，将用户登录 ID 与匿名 ID 关联
za.trackSignUp(registerId, anonymousId);
```



注意以下问题:

- **trackSignUp()**

   

  建议在用户注册/登录时调用。如果客户端也有采集任意事件，在注册/登录时，也需要在客户端调用一次关联方法 login() 将匿名 ID 和登录 ID 关联。 注册/登录时，客户端和服务端都各自调用一次关联方法的原因如下：

  - 一对一关联机制下，避免出现用户注册/登录时，客户端的关联信息 $SignUp 事件没有发送成功/延迟发送到分析系统，而服务端触发的事件( $is_login_id=true )先发送到分析系统中，导致登录 ID 自关联，从而导致登录 ID 无法再和匿名 ID 关联，客户端匿名行为和登录后的行为识别两个用户的行为。
  - 客户端调用一次关联方法 login() 的作用，除了将匿名 ID 和 登录 ID 关联之外，还会会将客户端标记用户的 distinctId 值从匿名 ID 切换为登录 ID。这样查看用户行为序列时，可以很好的根据 distinctId 的值判断用户行为是登录后的行为还是匿名行为。**因此强烈建议用户登录/注册时，在客户端调用一次 login() 的同时，在服务端也调用一次 trackSignUp() 。**

- 如果服务端只在用户登录成功之后，才会采集相关事件或者设置用户属性，要保证 track 事件/profileSet 设置用户属性（***$is_login_id*** 设置为 true）的代码在 trackSignUp() 方法之后调用，从而可以保证先将匿名 ID 和登录 ID 关联之后，再采集登录用户的行为事件/设置用户属性。对于不清楚关联关系时，在必要时联系我们的技术支持人员。

### 4. 追踪事件

第一次接入卓尔数科智能分析时，建议先追踪 3~5 个关键的事件，只需要几行代码，便能体验卓尔数科智能分析的分析功能。例如：

- 图片社交产品，可以追踪用户浏览图片和评论事件
- 电商产品，可以追踪用户注册、浏览商品和下订单等事件

卓尔数科智能分析 SDK 初始化成功后，可以通过 ***track()*** 记录事件，必须包含用户 ID（***distinctId***）、用户 ID 是否为登录 ID (***isLoginId***)、事件名（***eventName***）这三个参数，同时可以传入一个 ***Map*** 对象，为事件添加自定义事件属性。以电商产品为例，可以这样追踪一次购物行为：

```java
// 上报数据的url
final String serverUrl = "http://${ip}:${port}/a?project=${project}&service=${service}";

// 当缓存的数据量达到80条时，批量发送数据
final int bulkSize = 80;

// 使用 BatchConsumer 初始化 ZallDataAnalytics
final ZallDataAnalytics za = new ZallDataAnalytics(new BatchConsumer(serverUrl, bulkSize));

// 用户的 Distinct ID
String distinctId = "abcdefgABCDEFG123456789";


// 用户浏览商品
{
	Map<String, Object> properties = new HashMap<String, Object>();

	// '$time' 属性是系统预置属性，表示事件发生的时间，如果不填入该属性，则默认使用系统当前时间
	properties.put("$time", new Date());
	// '$ip' 属性是系统预置属性，如果服务端中能获取用户 IP 地址，并填入该属性，智能分析会自动根据 IP 地址解析用户的省份、城市信息
	properties.put("$ip", "123.123.123.123");
	// 商品 ID
	properties.put("ProductId", "987654");
	// 商品类别
	properties.put("ProductCatalog", "Numerical Code");
	// 是否加入收藏夹，Boolean 类型的属性
	properties.put("isAddedToFav", true);

	// 记录用户浏览商品事件
	za.track(distinctId, true, "ViewProduct", properties);
}

// 用户订单付款
{
	// 订单中的商品 ID 列表
	List<String> productIdList = new ArrayList<String>();
	productIdList.add("123456");
	productIdList.add("234567");
	productIdList.add("345678");

	Map<String, Object> properties = new HashMap<String, Object>();

	properties.put("$ip", "123.123.123.123");
	// 订单 ID
	properties.put("OrderId", "abcdefg");
	// 商品 ID 列表，List<String> 类型的属性
	properties.put("ProductIdList", productIdList);
	// 订单金额
	properties.put("OrderPaid", 666.66);

	// 记录用户订单付款事件
	za.track(distinctId, true, "PaidOrder", properties);
}
```



通过 调试模式 ，可以校验追踪的事件及属性是否正确。正常模式下，数据导入后，在卓尔数科智能分析中稍等片刻，便能看到追踪结果。

#### 4.1. 事件属性

如前文中的样例，追踪的事件可以设置自定义的事件属性，例如浏览商品事件中，将商品 ID、商品分类等信息作为事件属性。在后续的分析工作中，事件属性可以作为统计过滤条件使用，也可以作为维度进行多维分析。对于事件属性，智能分析有一些约束:

- 事件属性是一个 ***Map*** 对象；
- ***Map*** 中每个元素描述一个属性，Key 为属性名称，必需是 String 类型；
- ***Map*** 中，每个元素的 Value 是属性的值，支持 ***String***、***Boolean***、***Number***、***List*** 和 ***Date。***

对于卓尔数科智能分析中事件属性的更多约束，请参考 数据格式。**在开发多线程程序时，开发者不能在线程间复用传入的属性对象**。

##### 4.1.1. 系统预置属性

如前文中样例，事件属性中以 '$' 开头的属性为系统预置属性，在自定义事件属性中填入对应 '$' 开头的属性值可以覆盖这些预置属性：

- ***$ip*** - 填入该属性，智能分析会自动根据 IP 地址解析用户的省份、城市信息，该属性值为 String 类型；
- ***$time*** - 填入该属性，智能分析将事件时间设置为属性值的时间，该属性值必须为 Date 类型。请注意，智能分析默认会过滤忽略 2 年前或 1 小时后的数据，如需修改请联系我们；
- ***$project*** - 填入该属性，智能分析某些导入工具例如 LogAgent （LogAgent 的配置中未指定 project 参数时）会将数据导入指定项目。

关于其他更多预置属性，请参考 数据格式 中 '预置属性' 一节。

##### 4.1.2. 事件公共属性

特别地，如果某个事件的属性，在所有事件中都会出现，可以通过 ***registerSuperProperties()*** 将该属性设置为事件公共属性。例如将服务器的应用版本及机房地址设置为事件的公共属性，设置方法如下:

```java
Map<String, Object> properties = new HashMap<String, Object>();
// 服务器应用版本
properties.put("ServerVersion", "6.6");
// 服务器机房地址
properties.put("Location", "ShangHai");
// 设置事件公共属性
za.registerSuperProperties(properties);
```



成功设置事件公共属性后，再通过 track() 追踪事件时，事件公共属性会被添加进每个事件中，例如：

```java
Map<String, Object> properties = new HashMap<String, Object>();
// 登录客户端 IP 地址
properties.put("$ip", "123.123.123.123");
// 追踪用户登录事件
za.track("abcdefgABCDEFG123456789", true, "UserLogin", properties);
```



在设置事件公共属性后，实际发送的事件中会被加入 ***ServerVersion*** 和 ***Location*** 属性，等价于

```java
Map<String, Object> properties = new HashMap<String, Object>();
// 事件公共属性
properties.put("ServerVersion", "6.6");
properties.put("Location", "ShangHai");
// 登录客户端 IP 地址
properties.put("$ip", "123.123.123.123");
// 追踪用户登录事件
za.track("abcdefgABCDEFG123456789", true, "UserLogin", properties);
```



使用 ***clearSuperProperties()*** 会删除所有已设置的事件公共属性。

当事件公共属性和事件属性的 Key 冲突时，事件属性优先级最高，它会覆盖事件公共属性。

### 5. 设置用户属性

为了更准确地提供针对人群的分析服务，智能分析 SDK 可以设置用户属性，如年龄、性别等。用户可以在留存分析、分布分析等功能中，使用用户属性作为过滤条件或以用户属性作为维度进行多维分析。

使用 ***profileSet()*** 设置用户属性:

```java
String distinctId = "abcdefgABCDEFG123456789";

// 设置用户性别属性（Sex）为男性
za.profileSet(distinctId, true, "Sex", "Male");

Map<String, Object> properties = new HashMap<String, Object>();
// 设置用户等级属性（Level）为 VIP
properties.put("UserLv", "VIP");

za.profileSet(distinctId, true, properties);
```



对于不再需要的用户属性，可以通过 ***profileUnset()*** 接口将属性删除。

用户属性中，属性名称与属性值的约束条件与事件属性相同，详细说明请参考 数据格式。

#### 5.1. 记录初次设定的属性

对于只在首次设置时有效的属性，我们可以使用 ***profileSetOnce()*** 记录这些属性。与 ***profileSet()*** 接口不同的是，如果被设置的用户属性已存在，则这条记录会被忽略而不会覆盖已有数据，如果属性不存在则会自动创建。因此，***profileSetOnce()*** 比较适用于为用户设置首次激活时间、首次注册时间等属性。例如：

```java
String distinctId = "abcdefgABCDEFG123456789";

// 设置用户渠道属性（AdSource）为 "App Store"
za.profileSetOnce(distinctId, true, "AdSource", "App Store");

// 再次设置用户渠道属性（AdSource），设定无效，属性 "AdSource" 的值仍为 "App Store"
za.profileSetOnce(distinctId, true, "AdSource", "Search Engine");
```



#### 5.2. 数值类型的属性

对于数值型的用户属性，可以使用 ***profileIncrement()*** 对属性值进行累加。常用于记录用户付费次数、付费额度、积分等属性。例如：

```java
String distinctId = "abcdefgABCDEFG123456789";

// 设置用户游戏次数属性（GamePlayed），将次数累加1次
za.profileIncrement(distinctId, true, "GamePlayed", 1);
```



#### 5.3. 列表类型的属性

对于用户喜爱的电影、用户点评过的餐厅等属性，可以记录列表型属性。需要注意的是，列表型属性中的元素必须为 String 类型，且元素的值会自动去重。关于列表类型限制请见 数据格式 属性长度限制。

```java
String distinctId = "abcdefgABCDEFG123456789";

// 电影列表
List<String> movies = new ArrayList<String>();
movies.add("The Shawshank Redemption");
movies.add("The Pursuit of Happyness");

// 游戏列表
List<String> games = new ArrayList<String>();
games.add("LOL");
games.add("Halo");

// 用户属性
Map<String, Object> properties = new HashMap<String, Object>();
properties.put("movies", movies);
properties.put("games", games);

// 传入properties，设置用户喜欢的电影属性（movies）和喜欢的游戏属性（games）
// 设置成功后，"movies" 属性值为 ["The Shawshank Redemption", "The Pursuit of Happyness"];"games" 属性值为 ["LOL", "Halo"]
za.profileAppend(distinctId, true, properties);

// 传入属性名称和需要插入属性的值，设置用户喜欢的电影属性（movies）
// 设置成功后 "movies" 属性值为 ["The Shawshank Redemption", "The Pursuit of Happyness", "Sicario"]
za.profileAppend(distinctId, true, "movies", "Sicario");

// 传入属性名称和需要插入属性的值，设置用户喜欢的电影属性（movies），
// 但属性值 "Sicario" 与已列表中已有元素重复，操作无效，
// "movies" 属性值仍然为 ["The Shawshank Redemption", "The Pursuit of Happyness", "Sicario"]
za.profileAppend(distinctId, true, "movies", "Sicario");
```



#### 6. 物品元数据上报

在卓尔推荐项目中，客户需要将物品元数据上报，以开展后续推荐业务的开发与维护。智能分析 SDK 提供了设置与删除物品元数据的方法。

**item_id（物品 ID ）**与 **item_type （物品所属类型）**共同组成了一个物品的唯一标识。所有的 item 系列方法都必须同时指定**物品 ID** 及**物品所属类型**这两个参数，来完成对物品的操作。

##### 6.1. 设置物品

直接设置一个物品，如果已存在则覆盖。除物品 ID 与物品所属类型外，其他物品属性需在 ***properties*** 中定义。

物品属性中，属性名称与属性值的约束条件与事件属性相同，详细说明请参考 数据格式。

```java
public void itemSet(String itemType, String itemId, Map<String, Object> properties);

// 例如
Map<String, Object> properties = new LinkedHashMap<>();
properties.put("name", "JAVA 编程思想");
properties.put("price", 79.20);
sensorsAnalytics.itemSet("book", "0123456789", properties);
```



##### 6.2. 删除一个物品

如果物品不可被推荐需要下线，删除该物品即可，如不存在则忽略。

除物品 ID 与 物品所属类型外，不解析其他物品属性。

```java
public void itemDelete(String itemType, String itemId, Map<String, Object> properties);

// 例如
sensorsAnalytics.itemDelete("book", "0123456789", null);
```



### 7. 立刻上报缓存数据

如果想要事件数据、用户数据或者物品数据立刻上报，可以调用 ***flush()*** 方法：

```undefined
// 立刻上报缓存数据
za.flush();
```



### 8. 设置智能分析 SDK

以下内容说明如何更精细地控制智能分析 SDK 的行为。

#### 8.1. 数据采集

Java SDK 主要由以下两个组件构成:

- **ZallDataAnalytics:** 用于发送数据的接口对象，构造函数需要传入一个 Consumer 实例
- **Consumer:** Consumer 会进行实际的数据发送

为了让开发者更灵活的接入数据，智能分析 SDK 实现了以下 Consumer。

##### 8.1.1. DebugConsumer

用于校验数据导入是否正确，关于调试模式的详细信息，请进入相关页面查看。

请注意：Debug 模式是为方便开发者调试而设置的模式，该模式会逐条校验数据并在校验失败时抛出异常，性能远低于正常模式。

线上环境使用 Debug 模式会严重影响性能并存在崩溃风险，产品上线前请务必替换掉/关闭 Debug 模式。

```java
// 从智能分析获取的数据接收的 URL
final String serverUrl = "YOUR_SERVER_URL";
// 使用 Debug 模式，并且导入 Debug 模式下所发送的数据
final boolean writeData = true;

// 使用 DebugConsumer 初始化 ZallDataAnalytics
final ZallDataAnalytics za = new ZallDataAnalytics(new DebugConsumer(serverUrl, writeData));

// 使用智能分析记录用户行为数据
// ...
```



##### 8.1.2. BatchConsumer

批量发送数据的 ***Consumer***，当数据达到指定的量时（默认80条，最多可指定1000条），才将数据进行发送。也可以调用 ***flush()*** 方法去强制发送。

通常用于导入小规模历史数据，或者离线 / 旁路导入数据的场景。由于是网络直接发送数据，如果网络出现异常可能会导致数据重发或丢失，因此不要用在任何线上服务中。

```java
// 从智能分析获取的数据接收的 URL
final String serverUrl = "YOUR_SERVER_URL";

// 当缓存的数据量达到80条时，批量发送数据
final int bulkSize = 80;

// 使用 BatchConsumer 初始化 ZallDataAnalytics
// 不要在任何线上的服务中使用此 Consumer
final ZallDataAnalytics za = new ZallDataAnalytics(new BatchConsumer(serverUrl, bulkSize));

// 使用智能分析记录用户行为数据
// ...
```



##### 8.1.3. ConsoleConsumer

用于将数据输出到特定 Writer，一般用于在生产环境的 Java 程序中处理历史数据，生成日志文件并使用 BatchImporter 等工具导入

```java
// 将数据输出到标准输出
final Writer writer = new PrintWriter(System.out);

// 使用 ConsoleConsumer 初始化 ZallDataAnalytics
final ZallDataAnalytics za = new ZallDataAnalytics(new ConsoleConsumer(writer));

// 使用智能分析记录用户行为数据
// ...

// Flush the writer
writer.flush();
```



**8.1.4.LoggerConsumer**

批量实时写本地文件，文件以天为分隔，需要搭配 LogBus 进行上传，***logDirectory***为写入本地的文件夹地址，您只需将 LogBus 的监听文件夹地址设置为此处的地址，即可使用 LogBus 进行数据的监听上传。

```java
// 本地的文件夹地址
final String logDirectory = ".";

//使用LoggerConsumer，默认按天切分文件
final ZallDataAnalytics za = new ZallDataAnalytics(new LoggerConsumer(logDirectory));

```



如果您想按小时切分文件，您可以初始化代码如下：

```java
//LoggerConsumer的配置类
LoggerConsumer.Config config = new LoggerConsumer.Config(logDirectory);

//配置按小时切分，默认是LogrotateEnum.DAILY 按天切分
config.setRotateMode(LoggerConsumer.LogrotateEnum.HOURLY);

final ZallDataAnalytics za = new ZallDataAnalytics(new LoggerConsumer(config));

```



如果您想按大小切分，您可以初始化代码如下：

```java
//LoggerConsumer的配置类
LoggerConsumer.Config config = new LoggerConsumer.Config(logDirectory);

//设置在按天切分的前提下，按大小切分文件，单位是M,例如设置2G切分文件
config.setFileSize(2*1024);

final ZallDataAnalytics za = new ZallDataAnalytics(new LoggerConsumer(config));
```





#### 8.2. 关闭 SDK

如果您想要主动关闭 SDK，可以参考以下使用方式：

```java
// 关闭智能分析 SDK 所有服务并销毁实例
za.shutdown();
```



#### 8.3. 其它设置

导入历史数据：默认情况下，神策会过滤发生时间比较久远数据（例如 10 天之前，具体取决于服务端设置），如果想导入历史数据，可以通过开启 ***Time Free*** 选项来绕过这个限制。

```java
// 初始化 ZallDataAnalytics
final ZallDataAnalytics za = new ZallDataAnalytics(...);
// 开启 Time Free 以便导入历史数据
za.setEnableTimeFree(true);
```
