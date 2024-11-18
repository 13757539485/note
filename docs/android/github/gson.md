https://github.com/google/gson

### bean转json
```kotlin
private val gson = Gson()
fun beantojson(bean: Bean): String {
    return gson.toJson(
        ActionBean("xxx", bean),
        object : TypeToken<ActionBean<Bean>>() {}.type
    )
}
```
### 自定义解析
1.定义数据类
```kotlin
data class ActionBean<T>(
    val action: String,
    val data: T,
)
```
2.定义解析器
```kotlin
class ActionBeanDeserializer : JsonDeserializer<ActionBean<*>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): ActionBean<*> {
    val jsonObject = json!!.asJsonObject
    val action = jsonObject["action"].asString
    val dataElement = jsonObject["data"]

    if ("xxx" == action) {
        return ActionBean<Boolean>(
            action,
            context.deserialize(dataElement, Boolean::class.java)
        )
    } else {
        return ActionBean("", 0, 0)
    }
```
3.使用
```kotlin
private val recvGson = GsonBuilder()
    .registerTypeAdapter(ActionBean::class.java, ActionBeanDeserializer())
    .create()
val actionBean = recvGson.fromJson(json, ActionBean::class.java)
    when (actionBean.action) {
        //todo
    }
```