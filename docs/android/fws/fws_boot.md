### 总体介绍
不依赖于view实现

逐帧动画：灵活、细腻，图片太大会不流畅

动画文件存放路径：/system/media/

Opengl动画：通过编程绘制动画，跨平台，绘制二维、三维等复杂动画或视图

### 源码分析
#### 源码路径

1.bootanimation: frameworks/base/cmds/bootanimation/

相关文件

frameworks/base/cmds/bootanimation/bootanim.rc

编译生成：out/target/product/oriole/system/bin/bootanimation

2.surfaceflinger: frameworks/native/services/surfaceflinger/

相关文件

frameworks/native/services/surfaceflinger/

- surfaceflinger.rc
- main_surfaceflinger.cpp
- SurfaceFlinger.cpp
- StartPropertySetThread.cpp

编译生成：out/target/product/oriole/system/bin/surfaceflinger

3.init: system/core/init/

相关文件

system/core/init/

- main.cpp
- init.cpp
- property_service.cpp

编译生成：out/target/product/oriole/system/bin/init

#### 启动流程
init.rc调用surfaceflinger.rc和bootanim.rc

其中bootanim服务不会被启动，会执行disabled，但是bootanim这个service会被记录添加到Service列表中
```rc
service bootanim /system/bin/bootanimation
    class core animation
    user graphics
    group graphics audio
    disabled
    oneshot
    ioprio rt 0
    task_profiles MaxPerformance
```
main_surfaceflinger.cpp
```cpp
int main(int, char**) {
    //...
    sp<SurfaceFlinger> flinger = surfaceflinger::createSurfaceFlinger();
    //...
    flinger->init();
    //...
    flinger->run();
    //...
}
```
SurfaceFlinger.cpp
```cpp
void SurfaceFlinger::init() FTL_FAKE_GUARD(kMainThreadContext) {
    //...
    mStartPropertySetThread = getFactory().createStartPropertySetThread(presentFenceReliable);

    if (mStartPropertySetThread->Start() != NO_ERROR) {
        ALOGE("Run StartPropertySetThread failed!");
    }
    //...
}
```
StartPropertySetThread.cpp，启动线程，调用Start后回调用threadLoop
```cpp
status_t StartPropertySetThread::Start() {
    return run("SurfaceFlinger::StartPropertySetThread", PRIORITY_NORMAL);
}

bool StartPropertySetThread::threadLoop() {
    //...
    // Clear BootAnimation exit flag
    property_set("service.bootanim.exit", "0");
    property_set("service.bootanim.progress", "0");
    // Start BootAnimation if not started
    property_set("ctl.start", "bootanim");//启动bootanim进程
    // Exit immediately
    return false;
}
```
为何property_set就能启动bootanim服务呢，在init进程启动时会监听属性并启动相应服务

main.cpp
```cpp
int main(int argc, char** argv) {
    //...
    if (!strcmp(argv[1], "second_stage")) {
        return SecondStageMain(argc, argv);
    }
    //...
}
```
init.cpp
```cpp
nt SecondStageMain(int argc, char** argv) {
    //...
    PropertyInit();
    //...
    StartPropertyService(&property_fd);
    //...
    HandleControlMessages();
    //...
}
```
property_service.cpp，创建socket并创建线程来监听property_set设置的属性
```cpp
void StartPropertyService(int* epoll_socket) {
    //...
    *epoll_socket = from_init_socket = sockets[0];
    init_socket = sockets[1];
    StartSendingMessages();

    StartThread(PROP_SERVICE_FOR_SYSTEM_NAME, 0660, AID_SYSTEM, property_service_for_system_thread,
                true);
    StartThread(PROP_SERVICE_NAME, 0666, 0, property_service_thread, false);

    //...
    }
}
void StartThread(const char* name, int mode, int gid, std::thread& t, bool listen_init) {
    int fd = -1;
    if (auto result = CreateSocket(name, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK,
                                   /*passcred=*/false, /*should_listen=*/false, mode, /*uid=*/0,
                                   /*gid=*/gid, /*socketcon=*/{});
        result.ok()) {
        fd = *result;
    } else {
        LOG(FATAL) << "start_property_service socket creation failed: " << result.error();
    }

    listen(fd, 8);

    auto new_thread = std::thread(PropertyServiceThread, fd, listen_init);
    t.swap(new_thread);
}
```
启动线程PropertyServiceThread
```cpp
static void PropertyServiceThread(int fd, bool listen_init) {
    Epoll epoll;
    //...
    if (auto result = epoll.RegisterHandler(fd, std::bind(handle_property_set_fd, fd));
        !result.ok()) {
        LOG(FATAL) << result.error();
    }
    //...
}
static void handle_property_set_fd(int fd) {
    //...
    switch (cmd) {
    case PROP_MSG_SETPROP: {
        //...
        auto result = HandlePropertySetNoSocket(prop_name, prop_value, source_context, cr, &error);
    //...
    case PROP_MSG_SETPROP2: {
        auto result = HandlePropertySet(name, value, source_context, cr, &socket, &error);
    //...
}
```
最终会调用HandlePropertySet，之前设置的是ctl.start
```cpp
std::optional<uint32_t> HandlePropertySet(const std::string& name, const std::string& value,
                                          const std::string& source_context, const ucred& cr,
                                          SocketConnection* socket, std::string* error) {
    //...
    if (StartsWith(name, "ctl.")) {
        return {SendControlMessage(name.c_str() + 4, value, cr.pid, socket, error)};
    }
    //...
}

static uint32_t SendControlMessage(const std::string& msg, const std::string& name, pid_t pid,
                                   SocketConnection* socket, std::string* error) {
    //...
    bool queue_success = QueueControlMessage(msg, name, pid, fd);
    //...
}

```
init.cpp
```cpp
bool QueueControlMessage(const std::string& message, const std::string& name, pid_t pid, int fd) {
    //...
    pending_control_messages.push({message, name, pid, fd});
    WakeMainInitThread();
    //...
}
```
WakeMainInitThread会触发HandleControlMessages
```cpp
static void HandleControlMessages() {
    auto lock = std::unique_lock{pending_control_messages_lock};
    if (!pending_control_messages.empty()) {
        auto control_message = pending_control_messages.front();
        pending_control_messages.pop();
        lock.unlock();
        bool success = HandleControlMessage(control_message.message, control_message.name,
                                            control_message.pid);
        //...
        lock.lock();
    }
    //...
}
static bool HandleControlMessage(std::string_view message, const std::string& name,
                                 pid_t from_pid) {
    //...
    Service* service = nullptr;
    if (ConsumePrefix(&action, "interface_")) {
        service = ServiceList::GetInstance().FindInterface(name);
    } else {
        service = ServiceList::GetInstance().FindService(name);通过name=bootanim找到对应的服务，init的时候被记录到ServiceList并disabled的
    }
    const auto& map = GetControlMessageMap();
    const auto it = map.find(action);//action=start
    //...
    const auto& function = it->second;

    if (auto result = function(service); !result.ok()) {
        //...
    }
    //...
}
static const std::map<std::string, ControlMessageFunction, std::less<>>& GetControlMessageMap() {
    // clang-format off
    static const std::map<std::string, ControlMessageFunction, std::less<>> control_message_functions = {
        {"sigstop_on",        [](auto* service) { service->set_sigstop(true); return Result<void>{}; }},
        {"sigstop_off",       [](auto* service) { service->set_sigstop(false); return Result<void>{}; }},
        {"oneshot_on",        [](auto* service) { service->set_oneshot(true); return Result<void>{}; }},
        {"oneshot_off",       [](auto* service) { service->set_oneshot(false); return Result<void>{}; }},
        {"start",             DoControlStart},
        {"stop",              DoControlStop},
        {"restart",           DoControlRestart},
    };
    // clang-format on

    return control_message_functions;
}
static Result<void> DoControlStart(Service* service) {
    return service->Start();
}
```
即bootanim这个服务又被启动了
#### 结束流程

### opengl绘制

### zip形式绘制

### 实战
[案例](./fws_boot_case.md)