# Kiểm tra B1

Ngày: 25/11/2026. Toolchain giữ theo scaffold: Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, JetBrains JDK 21; min 26/target 35/compile 36.

## Kết quả tự động

| Kiểm tra | Kết quả |
|---|---|
| `:app:assembleDebug` | PASS |
| `:app:testDebugUnitTest` | PASS — 4 test |
| `:app:lintDebug` | PASS — 0 error, 12 warning |
| `:app:connectedDebugAndroidTest`, emulator API 36 | PASS — 8 test |
| `:app:connectedDebugAndroidTest`, emulator API 32 | PASS — 8 test |
| Script tạo nhạc tổng hợp | PASS — 10 MP3, 3 album hợp lệ |
| `git diff --check` | PASS |

Unit test: ngưỡng 29.999/30.000ms, ba loại system sound, bỏ dấu tiếng Việt (đ/Đ và combining marks), không phụ thuộc locale Turkish.

Room instrumentation: rescan không trùng, cập nhật metadata/xóa record cũ, scan rỗng hợp lệ; IO error/SecurityException/cancel giữ dữ liệu; insert lỗi rollback cả deletion; observe không scan; group theo album ID, thứ tự track; hai snapshot liên tiếp 3.000 bài không vướng giới hạn bind SQLite.

MediaStore instrumentation: tạo WAV hợp lệ bằng code trong Music/Ringtones/Notifications/Alarms, đọc qua content URI thật, lọc duration/flags; từ chối quyền phải báo lỗi thay vì trả thư viện chỉ gồm file app sở hữu; bìa thiếu/URI không tồn tại trả fallback data null. Test tự xóa đúng các URI mình tạo.

12 warning lint: version/target SDK đã ghim, launcher icon chưa có từ B0 và gợi ý dùng KTX `toUri`. Không dùng baseline hoặc suppress để che lỗi API.

## Smoke test nhạc thật

- API 36: từ chối quyền → app còn hướng dẫn/thử lại/Settings; xin lại → hộp thoại quyền audio → cấp quyền và scan.
- API 32: hộp thoại quyền photos/media (`READ_EXTERNAL_STORAGE`) → cấp → đúng 3 album/6 bài.
- Hai API: album A có bìa nhúng; album B không truy cập được cover.jpg ngoài quyền audio → placeholder; album C bìa nhúng hỏng → placeholder, không crash.
- API 36: mở album Đường về → hai bài Đường về 1/2, duration 0:31; Back về danh sách.
- API 36: quét lại vẫn 3 album × 2 bài; đọc snapshot Room xác nhận 6 row/6 content URI khác nhau/3 album ID.
- API 36: thu hồi quyền bằng hệ thống rồi mở lại → màn xin quyền; kiểm tra DB vẫn còn đủ 6 bài.
- Đã xem ảnh sáng/tối và detail, không bị system bar che.

Ảnh minh chứng:

- [API 36 — album sáng](screenshots/api36-albums-light.png)
- [API 36 — album tối](screenshots/api36-albums-dark.png)
- [API 36 — detail](screenshots/api36-detail-light.png)
- [API 32 — album sáng](screenshots/api32-albums-light.png)

## Chạy lại

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Có emulator đang chạy:
.\gradlew.bat :app:connectedDebugAndroidTest
# Khi có nhiều emulator, chọn một máy:
$env:ANDROID_SERIAL = 'emulator-5556'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Báo cáo gốc nằm dưới `app/build/reports/` (không commit). Bảng trên là kết quả local; kết quả workflow `.github/workflows/android.yml` trên GitHub xem trong phần Checks của PR B1.

Chưa kiểm tra: máy thật/API 26–28, Android Studio Design Preview, profiler cuộn 3.000 file, clone trên máy teammate và walkthrough L1. Preview đã biên dịch cùng debug APK. Test 3.000 record chỉ chứng minh transaction/query, không thay benchmark thư viện thật.
