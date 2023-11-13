Okhttp默认提供get head post put delete patch

支持协议

类：Protocol

- HTTP_1_0("http/1.0")
- HTTP_1_1("http/1.1")
- SPDY_3("spdy/3.1")废弃改用HTTP_2("h2") 
- H2_PRIOR_KNOWLEDGE("h2_prior_knowledge")就是HTTP_2不加密版本
- QUIC("quic")就是HTTP3，基于UDP实现

```kotlin
,

[rfc_7230]: https://tools.ietf.org/html/rfc7230
,

@Deprecated("OkHttp has dropped support for SPDY. Prefer {@link #HTTP_2}.")
,

HTTP_2("h2"),
[rfc_7540_34]: https://tools.ietf.org/html/rfc7540.section-3.4

H2_PRIOR_KNOWLEDGE("h2_prior_knowledge"),

QUIC("quic");
```