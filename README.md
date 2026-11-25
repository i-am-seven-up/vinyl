# Vinyl

Android app nghe nhạc offline, viết bằng Kotlin và Jetpack Compose. Đây là project khởi đầu chạy được để bắt đầu lát L1 trong `PLANNING.md`.

## Mở và chạy

1. Cài [Android Studio Narwhal 4 Feature Drop (2025.1.4) hoặc mới hơn](https://developer.android.com/studio/releases), Android SDK Platform 36 và một emulator Android API 26 trở lên.
2. Clone repo, mở **thư mục gốc `Vinyl`** trong Android Studio. Chọn **Gradle JDK 17 hoặc 21 (không chọn JDK 25 với Gradle 8.13)** khi Studio hỏi Gradle JDK, rồi chờ Gradle sync.
3. Trong Device Manager, tạo/chọn emulator; chọn cấu hình `app` và bấm **Run**. Màn hình hiện chữ **Vinyl** là startup thành công.

Trên terminal Windows, có thể chạy `./gradlew.bat :app:assembleDebug` (PowerShell: `.\gradlew.bat :app:assembleDebug`). Gradle wrapper tải bản Gradle đã ghim; lần sync đầu cần Internet. Không commit `local.properties` vì SDK path khác nhau trên từng máy.

## Cấu trúc

- Một module `:app`, package `com.vinyl`.
- `MainActivity.kt` là màn hình khởi động Compose tối thiểu.
- Khi triển khai MVP, chia package theo tính năng (`library`, `player`, `playlist`, `search`, `settings`), dùng `shared` cho phần dùng chung; khởi tạo dependency thủ công qua `AppContainer`.
- Đọc [kế hoạch scaffold MVP](docs/MVP_SCAFFOLD_PLAN.md) để biết milestone B0–B5, vị trí file và convention. AI và teammate tuân thủ [AGENTS.md](AGENTS.md).
- `PLANNING.md` mô tả yêu cầu sản phẩm; quyết định cấu trúc và cách triển khai base theo kế hoạch scaffold MVP.

Project hiện là khung chạy, chưa quét nhạc hoặc phát nhạc.

Android Studio Iguana (2023.2.1) đang cài trên máy khởi tạo project này quá cũ cho AGP 8.13 và SDK 36; cần cập nhật Studio để mở bằng giao diện. Build bằng Gradle wrapper và JDK 17 đã được kiểm tra thành công, APK đã mở trên emulator API 36.
