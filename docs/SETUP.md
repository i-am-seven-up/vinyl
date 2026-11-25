# Setup Vinyl cho cả team

Mỗi thành viên dùng cấu hình đã commit trong repo. Clone vào thư mục local ngoài OneDrive; đường dẫn cài repo, Android SDK và JDK có thể khác nhau giữa các máy.

## 1. Phiên bản dùng chung

| Thành phần | Phiên bản | Nguồn cấu hình |
|---|---|---|
| Gradle | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| JVM chạy Gradle Daemon | JetBrains JDK 21 | `gradle/gradle-daemon-jvm.properties` |
| Android Gradle Plugin | 8.13.2 | `build.gradle.kts` |
| Kotlin | 2.2.21 | `build.gradle.kts` |
| Compose BOM | 2025.10.00 | `app/build.gradle.kts` |
| Android SDK | compile 36, target 35, min 26 | `app/build.gradle.kts` |
| Bytecode Java và Kotlin | 17 | `app/build.gradle.kts` |

JDK chạy Gradle và bytecode của app là hai cấu hình khác nhau: daemon chạy JDK 21, app vẫn tạo bytecode 17. JDK 21 ở đây ghim phiên bản lớn và nhà cung cấp; các máy có thể dùng bản vá 21.x khác nhau.

## 2. Mở project lần đầu

1. Cài Android Studio Narwhal 4 (2025.1.4) hoặc mới hơn. Rabbit 2 dùng được.
2. Clone repo `https://github.com/i-am-seven-up/vinyl.git`, checkout nhánh chứa B0 hoặc `main` sau khi PR B0 được merge. Mở thư mục gốc có `settings.gradle.kts`, không mở riêng thư mục `app`.
3. Trong SDK Manager, cài Android SDK Platform 36, Android SDK Platform-Tools và Android Emulator. Chấp nhận SDK licenses khi được hỏi.
4. Sync project với Internet. Gradle dùng tiêu chí **Java 21 + JetBrains** trong file daemon; tự tìm JDK phù hợp hoặc tải theo URL đã lưu nếu chưa có. Không sửa URL hay thêm đường dẫn JDK của máy vào repo.
5. Trong Settings → Build, Execution, Deployment → Build Tools → Gradle, dùng Gradle Wrapper. Nếu IDE hiển thị Gradle JVM Criteria, kiểm tra Java 21 và JetBrains. Nếu cần chọn JDK để khởi chạy wrapper, chọn JDK 21; có thể dùng Download JDK để cài JetBrains 21. JDK 17 cũng dùng được để khởi chạy wrapper; không chọn JDK 25 với Gradle 8.13.
6. Trong Device Manager, tạo emulator API 36 để cùng kiểm tra trên một phiên bản. Chọn cấu hình `app`, chọn emulator và Run. B0 mở tab Thư viện, chuyển được Playlist và Cài đặt; chưa quét hoặc phát nhạc.

Android Studio tự tạo `local.properties` theo Android SDK của từng máy. `.idea/` và `local.properties` đã được ignore; không commit hai phần này.

## 3. Kiểm tra bằng terminal

Tại thư mục gốc, `JAVA_HOME` trỏ tới JDK 21 đã cài và `java` có trong PATH. Có thể dùng JDK 17 để khởi chạy wrapper; daemon vẫn phải chọn JetBrains 21.

Windows PowerShell:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS hoặc Linux:

```bash
bash ./gradlew --version
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Kết quả cần thấy:

- `Gradle 8.13`; mục `Daemon JVM` ghi `Compatible with Java 21, JetBrains` từ `gradle/gradle-daemon-jvm.properties`. `Launcher JVM` có thể là 17 hoặc 21.
- `BUILD SUCCESSFUL`; B0 chưa có unit test nên `testDebugUnitTest` trả `NO-SOURCE`, không phải lỗi.
- APK ở `app/build/outputs/apk/debug/app-debug.apk`.

## 4. Khi gặp lỗi setup

| Hiện tượng | Cách xử lý |
|---|---|
| `JAVA_HOME` sai hoặc không tìm thấy Java | Chọn lại JDK 21 đã cài, mở terminal mới và chạy lại `--version`. |
| Lỗi Java 25 hoặc Gradle JVM không tương thích | Chuyển JDK khởi chạy wrapper về 17/21; không nâng Gradle chỉ để khớp JDK của một máy. |
| Không tải được JDK/plugin/dependency | Kiểm tra Internet, proxy và chế độ offline; hoặc cài JetBrains JDK 21 rồi sync lại. |
| SDK location not found hoặc thiếu Platform 36 | Mở SDK Manager, cài Platform 36 và để Android Studio tạo lại `local.properties` trên máy đó. |
| IDE còn dùng cấu hình cũ sau khi pull | Sync lại project; nếu cần chạy `gradlew --stop` bằng wrapper của hệ điều hành rồi build lại. |

Không commit `org.gradle.java.home` chứa đường dẫn cá nhân. Muốn đổi phiên bản dùng chung, sửa cấu hình qua một PR, cập nhật hướng dẫn này và chạy lại ba task kiểm tra bên trên.
