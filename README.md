# Vinyl

App nghe nhạc offline, Kotlin + Jetpack Compose. Hiện đã có khung B0: ba tab, theme theo hệ thống và mẫu màn hình có ViewModel. Chưa quét hoặc phát nhạc.

## Mở và chạy

Hướng dẫn thống nhất cho mọi thành viên: [Setup Vinyl](docs/SETUP.md).

1. Clone repo và mở thư mục gốc trong Android Studio Narwhal 4 (2025.1.4) hoặc mới hơn; Rabbit 2 dùng được.
2. Gradle Daemon dùng chung **JetBrains JDK 21**, được ghim trong `gradle/gradle-daemon-jvm.properties`. Android Studio và terminal dùng cùng tiêu chí này; Gradle tự tìm JDK phù hợp hoặc tải từ URL đã lưu nếu máy chưa có. Lần đầu cần Internet; không cần cùng đường dẫn cài JDK giữa các máy.
3. Cài Android SDK Platform 36 qua SDK Manager, chờ Gradle sync.
4. Device Manager → tạo/chọn emulator API 26 trở lên, chọn cấu hình `app` → **Run**. App mở tab **Thư viện**; có thể chuyển sang **Playlist** và **Cài đặt**.

Clone vào thư mục local ngoài OneDrive. Teammate có thể dùng đường dẫn riêng, không cần sửa file build. Android Studio tự tạo `local.properties` theo SDK của từng máy; không commit file này.

## Kiểm tra bằng terminal

PowerShell, tại thư mục gốc (`JAVA_HOME` trỏ tới JDK 17 hoặc 21 để khởi chạy wrapper; daemon tự chọn JetBrains JDK 21 theo cấu hình repo):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Kiểm tra bằng `.\gradlew.bat --version`: mục Daemon JVM phải hiển thị Java 21, JetBrains từ `gradle/gradle-daemon-jvm.properties`. Không đổi sang JDK 25 vì Gradle 8.13 chưa hỗ trợ. Bytecode của app vẫn giữ Java/Kotlin 17.

macOS/Linux dùng `bash ./gradlew` với các task tương tự. Wrapper tự tải Gradle đã ghim; lần sync đầu cần Internet. APK ở `app/build/outputs/apk/debug/app-debug.apk`.

## Bắt đầu code

- [Bàn giao B0](docs/b0/README.md): luồng file, cách thử và phần làm tiếp.
- [Kế hoạch scaffold MVP](docs/MVP_SCAFFOLD_PLAN.md): milestone B0–B5, cấu trúc và convention.
- [AGENTS.md](AGENTS.md): quy tắc cho AI và teammate.
- [PLANNING.md](PLANNING.md): yêu cầu sản phẩm; quyết định cấu trúc và cách triển khai base theo kế hoạch scaffold.

Một module `:app`, package `com.vinyl`. Root nối app; code tính năng nằm trong package tương ứng; theme chung ở `shared/ui`. Chỉ thêm database, Media3 hoặc dependency khác ở milestone cần dùng.
