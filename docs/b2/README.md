# B2 / L2 — Player nền và mini player

PR: [#3 — B2 background playback](https://github.com/i-am-seven-up/vinyl/pull/3) → `main`.

Branch: `codex/b2-background-player`, từ `origin/main` sau B1. Ngày bàn giao: 26/11/2026.

## Đã triển khai

- PlaybackService sở hữu một ExoPlayer/MediaSession; audio focus và rút tai nghe do Media3 xử lý. Có foreground media playback permissions, session notification và PendingIntent mở app.
- PlayerController kết nối bất đồng bộ, listener đồng bộ state read-only; release controller theo ViewModel, release session/player khi service destroy. UI không khởi tạo ExoPlayer.
- Chạm bài trong album phát nguyên danh sách từ vị trí chọn, media ID là content URI. Queue, repeat/shuffle dùng Media3.
- Mini player dùng chung giữa các tab; NowPlaying có seek, previous/next, play/pause, repeat/shuffle và Preview. Chỉ poll position 500ms khi lifecycle UI STARTED; không poll trong service.
- MVP Queue mở từ NowPlaying: danh sách thật, đánh dấu bài hiện tại, chọn bài/xóa occurrence và empty/error. Album detail có menu Phát tiếp theo/Thêm vào hàng đợi. Khánh cải thiện UI sau; chưa kéo thả đổi thứ tự.
- Lỗi playback/connection hiển thị, có retry kết nối. Không xin POST_NOTIFICATIONS vì media-session notification thuộc diện miễn quyền này; playback không bị chặn.
- Media3 1.8.0 được ghim và đã build với toolchain hiện tại; không nâng AGP/Kotlin/SDK.

## Delivery

- [Handoff cho Khánh](../handoff/L2.md)
- [Kết quả kiểm tra](TESTING.md)
- [Queue API 36](screenshots/api36-queue.png) / [Queue rỗng](screenshots/api36-queue-empty.png)
- [NowPlaying API 36](screenshots/api36-now-playing.png)

APK local: `docs/b2/vinyl-b2-debug.apk` (gitignore); build lại bằng `./gradlew.bat :app:assembleDebug` sau clone.

B3 mới làm fade, lịch sử và resume qua process death. Chưa xác nhận toàn bộ tiêu chí thiết bị thật của B2; xem TESTING.
