# Kiểm tra B2

Ngày: 26/11/2026. Toolchain giữ nguyên kế hoạch.

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug | PASS |
| testDebugUnitTest | PASS — 4 test B1 |
| lintDebug | PASS — 0 errors, 15 warnings |
| connectedDebugAndroidTest API 36 | PASS — 10 test (8 B1 + 2 B2) |
| git diff --check | PASS |

B2 instrumentation: queue thêm cuối/chèn next/xóa occurrence, repeat/shuffle và reconnect giữ session; file WAV tổng hợp thật giải mã/phát tăng position, pause, seek và tự chuyển bài. WAV do test tạo trong filesDir, tự dọn sau test. Test queue dùng URI giả chỉ để kiểm tra queue; không dùng nó làm bằng chứng decoder.

Smoke API 36: thư viện B1 3 album/6 bài; chọn Đường về 1 → mini player và NowPlaying thật. Session PLAYING, foreground service `mediaPlayback`, notification transport có 3 action. Home và khóa màn hình: position tăng từ khoảng 15s lên 27s. Media button hệ thống pause trả PAUSED, play tiếp được. Mở lại activity vẫn session hiện có. Ảnh NowPlaying đã kiểm tra bố cục.

15 warning gồm dependency/target đã ghim, icon launcher từ B0, KTX gợi ý và exported MediaSessionService cần cho system controllers. Không suppress/baseline.

Chạy lại:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

Chưa kiểm tra: API 32 (máy hiện chỉ có AVD API 36), máy thật, tai nghe/Bluetooth/audio-focus interruption, notification bấm tay, gapless với file/codec có encoder delay/padding phù hợp, profiler 3.000 bài, Android Studio Design Preview. Preview đã biên dịch. Không suy ra gapless từ việc chuyển WAV hoặc MP3 tự động; cần nghe/đo riêng trên thiết bị thật.
