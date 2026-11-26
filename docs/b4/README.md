# B4 / L3 — Playlist MVP

Ngày bàn giao: 26/11/2026. Branch `codex/b4-playlists`, từ `origin/main` sau khi B3/PR #4 merge.

## Phạm vi

- Danh sách playlist, tạo/đổi tên, xác nhận xóa, chi tiết và chọn bài từ thư viện Room thật.
- Thêm cùng bài nhiều lần; chọn từng occurrence để phát, phát tất cả, bỏ từng occurrence, nút Lên/Xuống đổi thứ tự.
- Loading/empty/error/retry, chặn tên trắng, khóa thao tác ghi trong khi lưu; Preview sáng/tối.
- Room schema v3, migration 2→3 giữ songs và dữ liệu playback/lịch sử v2; giữ migration 1→2 cho máy B1.
- Mỗi item có UUID riêng và position. Thêm/xóa/đổi thứ tự trong transaction; FK cascade khi xóa playlist hoặc bài mất sau scan.
- Rescan upsert bài còn tồn tại rồi chỉ xóa bài mất; không delete toàn thư viện làm mất playlist.

## Delivery

- [Handoff L3](../handoff/L3.md)
- [Kiểm tra](TESTING.md)
- [Ảnh playlist sau mở lại app](screenshots/api36-playlist.png)

APK local: `docs/b4/vinyl-b4-debug.apk` (gitignore). Clone mới build bằng `./gradlew.bat :app:assembleDebug`.

## Giới hạn

MVP dùng nút Lên/Xuống, chưa kéo thả, tìm bài trong picker hoặc multi-select. Khánh cải thiện UI/UX sau. Picker dùng thư viện đã scan; cấp quyền/scan ở tab Thư viện trước. File mất sau scan được bỏ khỏi playlist; file mất trước scan báo lỗi playback theo player hiện có. Không thay đổi queue đang phát khi sửa playlist; bấm phát lại để lấy thứ tự mới.

Chưa kiểm tra API 32, máy thật và thư viện khoảng 3.000 bài. B5 còn search/settings/theme và hoàn thiện MVP theo plan.
