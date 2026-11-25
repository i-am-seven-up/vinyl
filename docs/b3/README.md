# B3 — Fade, lịch sử nghe và resume

PR: [#4 — B3 playback engine](https://github.com/i-am-seven-up/vinyl/pull/4) → `main`.

Ngày bàn giao: 26/11/2026. Branch: `codex/b3-playback-engine`, từ `origin/main` sau PR #3/B2 đã merge.

## Đã triển khai

- Fade play/pause 300 ms tại service qua FadingPlayer (Media3 ForwardingSimpleBasePlayer). App, notification và media button dùng chung điểm xử lý; một coroutine job, ngắt khi lệnh mới/seek/đổi hoặc xóa queue/stop/release. Volume trở lại 1 sau khi pause hoặc hủy fade.
- PlayEventLogger tính listenedMs bằng elapsedRealtime, chỉ cộng khi ExoPlayer thực sự isPlaying. Seek position và thay đổi đồng hồ hệ thống không cộng thời gian nghe. Timestamp bắt đầu lưu riêng; bài lặp có event ID riêng.
- Room schema v2: playback_snapshot, playback_queue và play_events. Migration 1→2 giữ songs; không destructive migration, không FK lịch sử tới songs để rescan không xóa lịch sử.
- Snapshot queue (giữ occurrence trùng), current index, position, repeat/shuffle được ghi transaction theo sự kiện và mỗi 5 giây khi phát. Cùng checkpoint lưu event hiện tại; một writer giữ thứ tự. Không ghi DB mỗi frame.
- Mở app lại đọc URI ngoài main thread, bỏ URI không truy cập được, ánh xạ lại index và prepare ở trạng thái pause. Nếu bài hiện tại mất, chọn bài còn lại kế tiếp (hoặc cuối) tại vị trí 0.
- MediaButtonReceiver và callback onPlaybackResumption dùng API Media3 1.8.0; callback có thể resume khi controller hệ thống yêu cầu. Khởi động bình thường không tự phát tiếng. Lỗi storage/resumption hiển thị qua session extras trên UI hiện có.

## Delivery

- [Handoff engine B3](../handoff/B3.md)
- [Kết quả kiểm tra](TESTING.md)
- [Ảnh restore paused](screenshots/api36-restored-paused.png)

APK local: `docs/b3/vinyl-b3-debug.apk` (gitignore); build lại sau clone bằng `./gradlew.bat :app:assembleDebug`.

Chưa có màn lịch sử/thống kê riêng; B3 cung cấp dữ liệu engine. B4/L3 tiếp theo là playlist và migration schema mới.

## Giới hạn

- Process chết đột ngột có thể mất phần từ checkpoint cuối (~5 giây khi storage hoạt động bình thường); không bảo đảm flush trong onDestroy khi hệ điều hành kill process.
- Snapshot lưu shuffle enabled, không lưu hoán vị shuffle cụ thể; thứ tự ngẫu nhiên được Media3 tạo lại sau restore.
- Không hứa Bluetooth tự đánh thức sau force-stop; chưa thử tai nghe/Bluetooth/fade nghe bằng máy thật và API 32. Cấu hình MediaSessionService không cung cấp System UI resumption carousel sau reboot như MediaLibraryService.
- File mất khi đang phát vẫn báo playback error theo B2; lọc URI ở bước restore, chưa tự skip mọi lỗi decoder khi đang phát.
