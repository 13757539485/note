### 文字内容
```kotlin
Text(text = "app_name")
Text(text = stringResource(id = R.string.app_name))
```
### 文字颜色
```kotlin
color = Color.Blue
color = Color(0XFFFFFF00)
color = Color(1F, 1F, 0F, 1f)
color = Color(255, 255, 0, 0xFF)
color = colorResource(id = R.color.black)
```
### 文字大小
```kotlin
fontSize = 16.sp
fontSize = TextUnit(22f, TextUnitType.Sp)
```
### 文字粗细
```kotlin
fontWeight = FontWeight.Bold
```
### 文字样式
```kotlin
fontStyle = FontStyle.Normal
fontStyle = FontStyle.Italic
```
### 字体
```kotlin
fontFamily = FontFamily.SansSerif
```
### 文字间距
```kotlin
letterSpacing = 10.sp
```
### 文字装饰
```kotlin
textDecoration = TextDecoration.Underline//下划线
textDecoration = TextDecoration.combine(listOf(
            TextDecoration.LineThrough,//删除线
            TextDecoration.Underline//下划线
        ))//同时存在
```
### 文字位置
```kotlin
textAlign = TextAlign.Center
```
### 行高
```kotlin
lineHeight = 15.sp
```
### 文字行数以及裁剪
```kotlin
overflow = TextOverflow.Clip
maxLines = 1
```
### 文字样式可设置很多属性，包括以上以及额外的缩进等
```kotlin
style = TextStyle()
```

### 文字可选择
```kotlin
SelectionContainer {
    Text()
}
```

### 可点击文本(富文本)
案例：用户协议和隐私政策点击超链接
```kotlin
val annoStr = buildAnnotatedString {
    append("点击登录代码同意")
    pushStringAnnotation("protocol", "https://www.baidu.com")
    withStyle(
        SpanStyle(
            color = Color.Blue,
            textDecoration = TextDecoration.Underline
        )
    ) {
        append("用户协议")
    }
    pop()
    append("和")
    pushStringAnnotation("private", "https://developer.android.google.cn/jetpack/compose")
    withStyle(
        SpanStyle(
            color = Color.Blue,
            textDecoration = TextDecoration.Underline
        )
    ) {
        append("隐私政策")
    }
    pop()
}
var showStr by remember {
    mutableStateOf("")
}
Column {
    ClickableText(text = annoStr, onClick = { position ->
        showStr = annoStr.getStringAnnotations("protocol", position, position)
            .firstOrNull()?.item ?: annoStr.getStringAnnotations("private", position, position)
            .firstOrNull()?.item ?: ""
    })
    Text(text = showStr)
}
```