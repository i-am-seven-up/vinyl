# Kiểm tra B3

Ngày: 26/11/2026. Giữ toolchain/dependency đã ghim.

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug | PASS |
| testDebugUnitTest | PASS — 10 test (4 B1 + 6 B3) |
| lintDebug | PASS — 0 errors, 16 warnings |
| connectedDebugAndroidTest API 36 | PASS — 13 test (10 trước + 3 B3) |
| git diff --check | PASS |

## Bằng chứng tự động

- Logger: pause/buffering không cộng thời gian; seek không dùng position làm duration; đổi wall clock không ảnh hưởng; checkpoint không cộng trùng; cùng media ID lặp tạo event riêng; restore pause không tạo lượt nghe.
- Restore filtering: URI mất trước current, current mất, mất bài cuối/tất cả; giữ occurrence trùng, ánh xạ index, reset position đúng và giữ repeat/shuffle.
- Migration mở database v1 với một Song thật qua Room v2 và validate schema. Songs còn nguyên; snapshot/queue transaction rollback khi vị trí trùng; event checkpoint update theo ID không nhân đôi; rescan không xóa history.
- FadingPlayer chạy với ExoPlayer giải mã WAV thật: quan sát volume trung gian khi fade, pause xong volume=1, seek giữa fade không kẹt mute, rapid play/pause/play và thay queue giữa fade đều trở lại phát/volume=1.
- Service restore: đọc snapshot có URI mất và duplicate, mở qua MediaController; queue lọc đúng, currentIndex=1, position=1700 ms, repeat all/shuffle bật, pause giữ position và không thêm history.

## Smoke process death API 36

1. Cài APK đè lên app hiện có, cấp audio permission; album Đường về còn 2 bài.
2. Phát MP3 thật, pause bằng system media button (đi qua session/fade).
3. Dừng đúng process debug bằng run-as kill -9, không xóa app data, rồi mở MainActivity.
4. Session PAUSED; database snapshot index=0, position=2010 ms, queue=[Đường về 1, Đường về 2]. NowPlaying hiện 2s/31s và nút Play.
5. Hai lần đọc database khi vẫn pause đều có listenedMs=2250, position=2010; history không tăng khi pause/restore. Kiểm tra log không có FATAL EXCEPTION. Đã xem ảnh restore paused.

## Chạy lại

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

Lint warnings: dependency/target đã ghim, exported service/receiver cho system media clients, icon launcher từ B0 và gợi ý KTX. API Media3 UnstableApi được opt-in tại đúng class sử dụng; không baseline/suppress lint.

Chưa kiểm tra: API 32, thiết bị thật, Bluetooth resumption phần cứng, notification bấm tay/fade cảm nhận bằng tai, audio focus interruption, reboot carousel, gapless và profiler 3.000 file. Đã triển khai callback Media3 1.8.0 nhưng không coi test system media button là test Bluetooth phần cứng. Không bảo đảm tự resume sau force-stop.
