
## 背景

最近在使用微信开放平台的内容审核功能的时候需要接收微信的回调消息来获取审核的结果

原本的审核执行的时序图如下

<img src="http://oss.dhx.icu/dhx/image-20230910114853731.png" style="zoom:80%;" />

> 微信内容审核的接口参数如下 :
>
> ***请求参数***
>
> | 属性         | 类型   | 必填 | 说明                                                         |
> | :----------- | :----- | :--- | :----------------------------------------------------------- |
> | access_token | string | 是   | 接口调用凭证，该参数为 URL 参数，非 Body 参数。使用[access_token](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-access-token/getAccessToken.html)或者[authorizer_access_token](https://developers.weixin.qq.com/doc/oplatform/openApi/OpenApiDoc/ticket-token/getAuthorizerAccessToken.html) |
> | media_url    | string | 是   | 要检测的图片或音频的url，支持图片格式包括jpg, jepg, png, bmp, gif（取首帧），支持的音频格式包括mp3, aac, ac3, wma, flac, vorbis, opus, wav |
> | media_type   | number | 是   | 1:音频;2:图片                                                |
> | version      | number | 是   | 接口版本号，2.0版本为固定值2                                 |
> | scene        | number | 是   | 场景枚举值（1 资料；2 评论；3 论坛；4 社交日志）             |
> | openid       | string | 是   | 用户的openid（用户需在近两小时访问过小程序）                 |
>
> ***返回参数***
>
> | 属性     | 类型   | 说明                                             |
> | :------- | :----- | :----------------------------------------------- |
> | errcode  | number | 错误码                                           |
> | errmsg   | string | 错误信息                                         |
> | trace_id | string | 唯一请求标识，标记单次请求，用于匹配异步推送结果 |
>
> ### 异步检测结果推送
>
> 异步检测结果在 30 分钟内会推送到你的消息接收服务器。[点击查看消息接收服务器配置](https://developers.weixin.qq.com/miniprogram/dev/framework/server-ability/message-push.html)
> 返回的 JSON 数据包
>
> | 属性         | 类型   | 说明                    |
> | :----------- | :----- | :---------------------- |
> | ToUserName   | string | 小程序的username        |
> | FromUserName | string | 平台推送服务UserName    |
> | CreateTime   | number | 发送时间                |
> | MsgType      | string | 默认为：event           |
> | Event        | string | 默认为：wxa_media_check |
> | appid        | string | 小程序的appid           |
> | trace_id     | string | 任务id                  |
> | version      | number | 可用于区分接口版本      |
> | result       | object | 综合结果                |
> | detail       | array  | 详细检测结果            |
>
> result**为综合结果**，包含的属性有
>
> | 属性    | 类型   | 说明                                                         |
> | :------ | :----- | :----------------------------------------------------------- |
> | suggest | string | 建议，有risky、pass、review三种值                            |
> | label   | number | 命中标签枚举值，100 正常；20001 时政；20002 色情；20006 违法犯罪；21000 其他 |
>
> detail为**详细检测结果**，包含的属性有
>
> | 属性     | 类型   | 说明                                                         |
> | :------- | :----- | :----------------------------------------------------------- |
> | strategy | string | 策略类型                                                     |
> | errcode  | number | 错误码，仅当该值为0时，该项结果有效                          |
> | suggest  | string | 建议，有risky、pass、review三种值                            |
> | label    | number | 命中标签枚举值，100 正常；20001 时政；20002 色情；20006 违法犯罪；21000 其他 |
> | prob     | number | 0-100，代表置信度，越高代表越有可能属于当前返回的标签（label） |
>
> ***异步检测结果推送示例***
>
> ```json
> {
>    "ToUserName": "gh_9df7d78a1234",
>    "FromUserName": "o4_t144jTUSEoxydysUA2E234_tc",
>    "CreateTime": 1626959646,
>    "MsgType": "event",
>    "Event": "wxa_media_check",
>    "appid": "wx8f16a5be77871234",
>    "trace_id": "60f96f1d-3845297a-1976a3ae",
>    "version": 2,
>    "detail": [{
>         "strategy": "content_model",
>         "errcode": 0,
>         "suggest": "pass",
>         "label": 100,
>         "prob": 90
>    }],
>    "errcode": 0,
>    "errmsg": "ok",
>    "result": {
>         "suggest": "pass",
>         "label": 100
>    }
> }
> ```

我们通过微信内容审核服务响应的trace_id 来定位到对应的图像 。

但是微信的内容审核消息配置有一个问题 ， 就是我们只能填写一个回调消息的返回地址。

由于我们的服务器一般有测试服务器以及正式服务器，这里就需要使用的本文的主角：回调系统。

> 当然解决这个问题的方法并不唯一 , 使用下面的方法也是可以的
>
> - **消息广播** : 使用类似于Nginx的服务器来对回调消息转发 ,  这样做虽然可以达到目的, 但是会增大服务器的负载
> - **消息队列** :  定义一个消息队列,   使得我们的测试服务器与正式服务器都订阅其中的消息 , 不过这样做也会增大服务器的负载, 并且需要使用额外的消息队列服务。

## 回调系统简介

回调系统是我们处理异步操作的机制，它通常用于接收异步服务的回调请求，并将请求转发到指定的处理程序。回调系统可以帮助我们处理异步服务所带来的挑战，包括处理来自多个异步服务的请求，并确保每个请求都得到了正确的处理。
在一个典型的回调系统中，我们可以为每个异步服务定义一个回调URL，当服务完成处理并准备发送回调请求时，它会向这个URL发送一个请求。这个请求包含异步服务所返回的数据，我们的回调系统会接收这个请求并解析其中的数据，然后根据我们的业务逻辑，将数据转发到相应的处理程序。这个处理程序可能是我们自己的服务器，也可能是另一个异步服务，我们的回调系统需要负责将请求正确地转发到目标处理程序。

> 例如 **微信的图像审核**就是一个异步的服务，当我们发送一个图像审核的请求给微信后，他会返回一个**trace_id**用于后续的图像审核追踪。一段时间后微信会自动给一个回调地址发送图像审核的结果。回调系统就是要接收回调的审核结果，并发送到相关服务的链接上

另一方面回调系统也可以帮助降低系统之间的耦合度 , 同时可以将一些通用的功能给抽离出来 , 提高代码的复用性。

那么当我们的图像审核服务引入了回调系统 , 执行流程就变成了:

![](http://oss.dhx.icu/dhx/image-20230910121255715.png)

实际上在callback-backend看来,  任何项目的审核消息都是没有区别的 , 也就是说不论是`test dev prod` , **Project A , Project B** , 我们的回调系统都可以去进行工作

在第六步中 , 我们的业务服务器发送了审核结果的接收地址与trace_id到回调服务器 , 举个例子

```json
{
    "trace_id":"eghe45gves-ge56wer34=44564gews",
    "server_uri":"https://test.projectA.com/api/review/moderate/result"
}
```

通过这个例子不难看出回调系统的工作方式

```java
@Data
public class TraceServerInfo {

    private String uri;

    private String traceId;
}
```

通过接收参数以及持久化  , 保证回调系统的健壮性。

由于

> *"异步检测结果推送*
>
> *异步检测结果在 30 分钟内会推送到你的消息接收服务器。[点击查看消息接收服务器配置](https://developers.weixin.qq.com/miniprogram/dev/framework/server-ability/message-push.html)*
> *返回的 JSON 数据包"*

因此需要设置定时任务来定期的处理超时没有完成审核的任务

然而定时任务的时间无论如何设定, 始终会存在一些问题 , 比如redis突然宕机 , 如何知道没有持久化的数据的时间?

因此这里使用MySQL与Redis来存储 , 同时使用时间戳来存储审核的时间 , 对于超过30min 没有完成审核的任务, 视为失败 , 进行失败处理。

```java
@PostMapping("/moderate/trace")
public ResponseEntity<String> receiveTraceId(@RequestBody TraceServerInfo serverInfo) {
    if (serverInfo == null) {
        return ResponseEntity.badRequest().body("Param not available.");
    }
    long timeStamp = System.currentTimeMillis();
    // 1. 持久化到mysql
    String uri = serverInfo.getUri();
    String traceId = serverInfo.getTraceId();
    CallbackInfo callbackInfo = new CallbackInfo(serverInfo, timeStamp);
    boolean save = callbackInfoService.save(callbackInfo);
    if (!save) {
        log.error("持久化业务服务器信息失败, serverInfo: {}", serverInfo);
    }
    // 2. 缓存到redis
    String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
    String jsonStr = JSONUtil.toJsonStr(serverInfo);
    // 使用String来存储server信息 { k : timestamp} : {value : TraceServerInfo}
    stringRedisTemplate.opsForValue().set(key, jsonStr, RedisConstant.CALLBACK_MODERATE_TTL, TimeUnit.SECONDS);
    // 通过zset来快速获取超时的数据
    stringRedisTemplate.opsForZSet().add("key", traceId, timeStamp);
    return ResponseEntity.ok("callback接收成功");
}
```

> 使用redis需要满足以下的两个要求
>
> - 通过**时间戳**快速获取超时没有完成的审核任务
> - 通过`trace_id`快速定位到对应的*`server_uri`*
>
> 因此这里使用ZSET来进行存储 , score为对应的时间戳 , 可以保证我们快速的获取到超时的trace_id ,
>
> 使用String来存储 server_uri , 保证我们快速的获取到对应的接口信息

## 功能完善

网络请求的稳定性是难以保证的,  因此我们需要一些简单的操作来帮助我们进行错误的排查

> 如果审核失败  , 问题会出在哪里?
>
> 微信审核? 回调系统 ? 还是业务服务器 ?

这里设定了两个表

- **回调信息表**
- **消息转发表**

对应的SQL如下

> 这里的表的定义是非常灵活的 , 解耦的回调系统修改起来也很容易
>
> - 由于项目会涉及到app_id , 因此这里额外添加了一个app_id字段 , 帮助快速的进行查询

```sql
-- ----------------------------
-- Table structure for t_callback_info
-- ----------------------------
DROP TABLE IF EXISTS `t_callback_info`;
CREATE TABLE `t_callback_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'trace_id',
  `server_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目的服务器接口地址',
  `app_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '小程序app_id',
  `is_received` tinyint(1) NULL DEFAULT 0 COMMENT '是否接收到回调消息',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳信息',
  `err_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_forward_info
-- ----------------------------
DROP TABLE IF EXISTS `t_forward_info`;
CREATE TABLE `t_forward_info`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `server_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目的服务器路径',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求参数',
  `body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求体',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '目的服务器响应结果',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

```

那么对于回调系统 ,  执行的大致流程图如下

<img src="http://oss.dhx.icu/dhx/image-20230910122609191.png" style="zoom:80%;" />

为了方便进行错误排查 , 进行错误处理的同时定义枚举如下

```java
public enum CallbackError {
    WX_TIMEOUT_ERROR("微信为按时返回回调消息"),
    CALLBACK_ERROR("callback内部异常"),
    BIZ_SERVER_ERROR("业务服务器响应异常 ");

    /**
     * value
     */
    private String value;

    public String getValue() {
        return value;
    }

    CallbackError(String value) {
        this.value = value;
    }
}

```

### 接收审核请求

这里在前面的代码以及给出 , 关键点在于如何对业务服务器的接口地址进行合理的存储以及如何进行错误的排查 ,

### 接收回调消息

这里主要的场景是接收微信内容审核的回调消息进行处理并转发给对应的业务服务器

步骤

1. 通过消息中的trace_id获取对应的服务器的地址

2. 持久化回调消息内容

3. 进行转发 (构造请求头 , 存储请求体以及请求参数 ) : **这里建议加上一些请求头信息进行校验** , **保证转发的安全性**

4. 保存业务服务器的响应结果

**具体的代码如下**

```java
public ResponseEntity<String> forwardModerateResult(WxImageModerationAsyncResult callbackMessage, HttpServletRequest request) {
    // 获取转发的目标URL
    String targetUrl = getDestServer(callbackMessage);
    if (targetUrl.equals("")) {
        log.error("获取目的服务器路径失败, trace_id :{}", callbackMessage.getTraceId());
        return null;
    }
    try {
        String body = JSONUtil.toJsonStr(callbackMessage);
        if (StringUtils.isNotBlank(targetUrl)) {
            // 持久化转发信息
            ForwardInfo forwardInfo = new ForwardInfo(request);
            forwardInfo.setBody(body);
            forwardInfo.setServerUri(targetUrl);
            save(forwardInfo);
            // 进行转发
            // 1.获取到请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders headers = new HttpHeaders();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String header = request.getHeader(headerName);
                headers.add(headerName, header);
            }
            // 2.构造HttpEntity，包括请求头和请求体
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            // 3.转发请求并获取响应
            ResponseEntity<String> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            // 4.存储响应状态
            forwardInfo.setResponseResult(responseEntity.getStatusCode().toString());
            updateById(forwardInfo);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String traceId = callbackMessage.getTraceId();
                String appid = callbackMessage.getAppid();
                boolean update = callbackInfoService.update().set("is_received", 1).set("app_id", appid).eq("trace_id", traceId).update();
                if (!update) {
                    log.info("更新回调信息失败, traceId:{}", traceId);
                }
            } else {
                String traceId = callbackMessage.getTraceId();
                String appid = callbackMessage.getAppid();
                String reasonPhrase = responseEntity.getStatusCode().getReasonPhrase();
                boolean update = callbackInfoService.update()
                    .set("is_received", 1)
                    .set("app_id", appid)
                    .set("err_msg", CallbackError.BIZ_SERVER_ERROR + "," + reasonPhrase)
                    .eq("trace_id", traceId).update();
                if (!update) {
                    log.info("更新回调信息失败, traceId:{}", traceId);
                }
            }
            log.info("审核回调消息转发成功, targetUrl:{}", targetUrl);
            // 5. 删除redis中存储的trace_id相关信息
            String traceId = callbackMessage.getTraceId();
            String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
            // remove from string
            stringRedisTemplate.delete(key);
            // remove from ZSET
            stringRedisTemplate.opsForZSet().remove(RedisConstant.CALLBACK_MODERATE_ZSET_KEY, traceId);
            return responseEntity;
        } else {
            return ResponseEntity.badRequest().body("Target URL not available.");
        }
    } catch (RuntimeException e) {
        String traceId = callbackMessage.getTraceId();
        String appid = callbackMessage.getAppid();
        boolean update = callbackInfoService.update()
            .set("app_id", appid)
            .set("err_msg", CallbackError.CALLBACK_ERROR + "," + e.getMessage())
            .eq("trace_id", traceId).update();
        if (!update) {
            log.info("更新回调信息失败, traceId:{}", traceId);
        }
    }
    return null;
}
```

### 处理超时未完成的任务

仍然是内容审核的场景 , 由于微信内容审核服务一般在30MIN之后返回结果,

前面我们已经通过时间戳保准审核的开始时间 , 只需要简单的定时获取超时30MIN没有删除的key来进行操作即可

> 这里设置的执行时间为 10min

对于超时未完成审核的任务 , 我们需要

1. 保存错误信息
2. 删除redis中对应的数据
3. 通知业务服务器本次审核失败

```java
@Scheduled(cron = "0 */10 * * * ?")
public void moderateTimeOut() {
    log.info("[start]开始执行检测审核超时定时任务");
    long timeStamp = System.currentTimeMillis();
    Set<String> traceIds = stringRedisTemplate.opsForZSet().rangeByScore(
        CALLBACK_MODERATE_ZSET_KEY, timeStamp - WX_MODERATE_TTL, timeStamp);
    if (traceIds == null) {
        log.info("[end]检测审核超时定时任务执行结束");
        return;
    }
    if (traceIds.size() != 0) {
        traceIds.forEach(traceId -> {
            log.info("微信为按时返回审核结果, traceId:{}", traceId);
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.setTraceId(traceId);
            callbackInfo.setErrMsg(CallbackError.WX_TIMEOUT_ERROR.getValue());
            boolean update = callbackInfoService.update().eq("trace_id", traceId).set("err_msg", callbackInfo.getErrMsg()).update();
            if (!update) {
                log.error("更新回调消息状态失败, traceId:{}", traceId);
            }
            // 通知业务服务器
            informTargetServer(traceId);
            // 删除redis中存储的trace_id相关信息
            String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
            // remove from string
            stringRedisTemplate.delete(key);
            // remove from ZSET
            stringRedisTemplate.opsForZSet().remove(RedisConstant.CALLBACK_MODERATE_ZSET_KEY, traceId);
        });
    }
    log.info("[end]检测审核超时定时任务执行结束, 本次超时的审核任务个数:{}", traceIds.size());
}
```

到这里 , 我们的回调系统基本是设计完成了,  接下来需要的就是简单的进行测试

## 测试

### 单机测试

### 准备工作

通过controller来模拟不同的服务器 , 进行基本的测试 , 需要提前准备

- 模拟控制器
- 模拟数据
- Postman模拟微信内容审核服务器发送审核结果

这里给出Prod的Controller , 对应的test与其基本没有区别

> 只需要全局替换prod -> test即可

```java
public class ProdServerController {

    @Resource
    PropertiesConfig properties;

    @Resource
    RestTemplate restTemplate;

    @GetMapping("/send/moderate")
    public String simulateSendModerate(@RequestParam("trace_id") String traceId) {
        // 1. 请求wx服务获取trace_id
        // 2. 保存traceId
        // 3. 发送traceId以及回调url给callback服务器
        /*
        ....执行图像审核逻辑 : 模拟图像审核逻辑 , 获取trace_id
        */
        if (StringUtils.isBlank(traceId)) {
            traceId = properties.getTraceId();
        }
        log.info("prod-server执行图像审核: trace_id:{}", traceId);
        // 发送信息到callback-server
        TraceServerInfo serverInfo = new TraceServerInfo();
        String serverUrl = properties.getProdServerUrl();
        serverInfo.setUri(serverUrl);
        serverInfo.setTraceId(traceId);
        String callBack = "http://localhost:9000/api/moderate/trace";
        String post = HttpUtil.post(callBack, JSONUtil.toJsonStr(serverInfo));
        log.info("回调服务器响应结果:{}", post);
        return null;
    }

    /**
     * 接收微信图像审核回调消息
     *
     * @param response 响应
     * @return {@link Map}
     */
    @PostMapping("/review/image/callback")
    public Map receiveImageModerateResult(@RequestBody WxImageModerationAsyncResult response) {
        WxModerationResult result = response.getResult();
        String traceId = response.getTraceId();
        log.info("prod-server接收到了回调消息: {}", response);
        return null;
    }

}
```

接着我们准备模拟的数据

```properties
spring.application.name=callback
server.port=9000
server.servlet.context-path=/api
# TODO ??redis??
spring.redis.host=192.168.159.134
spring.redis.password=adorabled4
# ????
callback.test.url=http://localhost:9000/api/test/review/image/callback
callback.prod.url=http://localhost:9000/api/prod/review/image/callback
callback.traceId=60f96f1d-3845297a-1976a3ae
```

同时将相关的数据注入到Bean中方便获取

```java
@Component
public class PropertiesConfig {

    /**
     * 测试服务器url
     */
    @Value("${callback.test.url}")
    String testServerUrl;

    /**
     * 正式服务器url
     */
    @Value("${callback.prod.url}")
    String prodServerUrl;

    /**
     * traceId
     */
    @Value("${callback.traceId}")
    String traceId;


    public String getTestServerUrl() {
        return testServerUrl;
    }

    public String getProdServerUrl() {
        return prodServerUrl;
    }

    public String getTraceId() {
        return traceId;
    }
}
```

#### 进行测试

启动项目

访问

`http://localhost:9000/api/prod/send/moderate?trace_id=60f96f1d-3845297a-1976a3ae`

![](http://oss.dhx.icu/dhx/image-20230910133358843.png)

可以看到发送成功,  并且回调controller接收到了对应的信息

redis中也保存了对应的数据

> 分别是**String**以及**ZSET**

![](http://oss.dhx.icu/dhx/image-20230910133445006.png)

接着我们通过Postman人工模拟微信内容审核服务器发送异步回调消息

![](http://oss.dhx.icu/dhx/image-20230910133724032.png)

查看项目日志,  可以看到模拟的Prod server已经成功接收到了审核的结果:

![](http://oss.dhx.icu/dhx/image-20230910133715630.png)

查看数据库 , 可以看到对应的数据以及记录成功

![](http://oss.dhx.icu/dhx/image-20230910133848619.png)

<img src="http://oss.dhx.icu/dhx/image-20230910134414487.png" style="zoom:80%;" />

到这里 , 基本的测试已经完成 ,  但是健壮的项目远非如此 , 更多的问题还在线上的环境等待着我们去完善。



### 内网穿透-Ngrok- TODO

由于微信的消息配置每个月仅仅可以修改三次 , 修改相关的接口地址就需要十分谨慎

这里选择通过 模拟业务系统+内网穿透 + Postman进行测试

在尝试了包括wenat  花生壳 ngrok等内网穿透工具之后,  这里推荐使用ngrok进行内网穿透 , 包括但不局限于以下的优点

- 集成方便 : 一行代码即可开启内网穿透
- 配置简单 : 只需要两行配置, 即可完成springboot项目的内网穿透
- 整洁的控制台页面 : 对应的接口的响应信息,  延迟等一目了然

Ngrok可以创建一个http隧道，并为您提供一个公共URL，重定向到本地机器上的指定端口。它是一个很棒的开发或者测试目的使用的工具。
Ngrok的官网地址是：[https://ngrok.com/](https://link.zhihu.com/?target=https%3A//ngrok.com/) 。

> 访问 https://ngrok.com/download **快速下载**
>
> <img src="http://oss.dhx.icu/dhx/image-20230910123416004.png" style="zoom:80%;" />

Spring Boot的Web端口可以通过**Ngrok Spring Boot Starter**暴露到互联网。
**Ngrok Spring Boot Starter**将会根据你的操作系统自动下载Ngrok的二进制文件并缓存到*home_directory/.ngrok2* 目录。
每次运行Spring Boot程序的时候，Ngrok会自动构建指向Spring Boot Web程序的http隧道。

添加maven依赖

```xml
<dependency>
    <groupId>io.github.kilmajster</groupId>
    <artifactId>ngrok-spring-boot-starter</artifactId>
    <version>0.6.0</version>
</dependency>
```

接着在配置文件中启动内网穿透

```yml
ngrok:
  enabled: true # 开启ngrok
  auth-token: ******************** # 复制上面获取的authtoken到此处
```

> 获取**token**  , 这里直接去官网注册账号,  然后查看token即可
>
> ![](http://oss.dhx.icu/dhx/image-20230910124639082.png)

接着我们打开Ngrok

在控制台中输入

`ngrok http ${port}`

> 这里记得替换成自己的**项目端口**

![](http://oss.dhx.icu/dhx/image-20230910124231879.png)



## 参考

- https://zhuanlan.zhihu.com/p/575429954
- https://blog.csdn.net/qq_27818541/article/details/105719962
- https://developers.weixin.qq.com/miniprogram/dev/wxcloud/basis/getting-started.html
- https://zhuanlan.zhihu.com/p/575429954
- https://cloud.tencent.com/developer/article/1192033
