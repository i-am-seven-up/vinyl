# Kiểm tra B4

Ngày: 26/11/2026. Emulator Pixel 3a API 36, JDK/Gradle theo repo.

## Tự động

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

- Build thành công; 10 JVM unit test và 15 instrumentation test pass (0 failed/skipped).
- Hai test B4: migration v2→v3 giữ songs/snapshot/queue/history; playlist tên trắng bị từ chối, trim/rename, bài trùng, reorder/remove đúng occurrence, position, rescan cập nhật metadata giữ items, reopen database, cascade khi bài mất/xóa playlist và FK từ chối bài không tồn tại.
- Test migration v1 được cập nhật đi qua cả 1→2→3, giữ dữ liệu và transaction playback. Test snapshot scan không hợp lệ xác nhận validation URI trùng trước upsert giữ thư viện cũ.
- Test scan 3.000 record Room sẵn có vẫn pass; chưa thay thế profiling thư viện nhạc thật.
- Lint: 0 errors; warnings còn lại từ dependency/target đã ghim, exported media service, icon và gợi ý KTX hiện có. Không thêm baseline/suppression.
- `git diff --check` sạch.

Lượt đầu instrumentation không có device; đã khởi động emulator. Lượt kế tiếp phát hiện test scan URI trùng thay đổi do upsert; đã bổ sung validation thay vì bỏ kiểm tra. Bộ kiểm tra đầy đủ sau sửa đã pass.

## Smoke UI với nhạc thật

Cài debug APK, cấp READ_MEDIA_AUDIO, mở Thư viện và scan bộ nhạc B1 trong emulator.

1. Tạo `B4 test`; thêm Đường về 1, Đường về 2, Đường về 1.
2. Chuyển occurrence thứ ba lên một vị trí; bỏ occurrence đầu. Còn Đường về 1, Đường về 2 theo đúng thứ tự.
3. Phát tất cả: mini player hiện Pause; dumpsys media_session báo PLAYING, Đường về 1, queue size 2, không playback error.
4. Đổi tên thành `B4 test renamed`; pause rồi force-stop/mở lại (không clear data).
5. Mở Playlist: tên mới và hai bài theo thứ tự còn nguyên; mini player ở trạng thái Play/paused. [Ảnh đã kiểm tra](screenshots/api36-playlist.png).

Xóa/cascade, dữ liệu sau reopen và rescan được kiểm tra qua Room device tests; chưa claim mọi thao tác đó đã bấm lại bằng UI.

## Chưa kiểm tra

API 32, máy thật, font lớn/màn hẹp, hiệu năng thư viện thật 3.000 bài. Bluetooth/fade/gapless còn giới hạn từ B2/B3. MVP playlist dùng nút đổi thứ tự, chưa drag/drop hoặc tìm kiếm trong picker.
