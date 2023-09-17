案例

```kotlin
fun MainSample() {
    log("1")
    var state by remember {
        mutableStateOf("init")
    }
    LaunchedEffect(key1 = Unit){
        delay(1000L)
        state = "modify"
    }
    Greeting(content = state)
    log("4")
}

@Composable
fun Greeting(content: String) {
    log("2 $content")
    Text(text = content)
    log("3 $content")
}

fun log(msg: String){
    Log.d("TAG", "log: $msg")
}

运行结果：

log: 1
log: 2 init
log: 3 init
log: 4
1s后
log: 1
log: 2 modify
log: 3 modify
log: 4
```
优化后

```kotlin
fun MainSample() {
    ...
    Greeting { state }
    ...
}

@Composable
fun Greeting(lambda: () -> String) {
    log("2 ${lambda()}")
    Text(text = lambda())
    log("3 ${lambda()}")
}

运行结果：

log: 1
log: 2 init
log: 3 init
log: 4
1s后
log: 2 modify
log: 3 modify
```
原因是lambda表达式是在调用的时候才执行，优化后的作用域变成了Greeting函数中，因为不会触发MainSample的函数作用域