# B1 / L1 — Thư viện thật

Branch: `codex/b1-library`, tạo từ `origin/main` sau khi B0 merge.

## Phạm vi đã có

- Room schema v1: một bảng `songs`, album lấy bằng `GROUP BY albumId`. ID album/nghệ sĩ gồm volume + MediaStore ID; bài hát dùng content URI làm khóa.
- Một query MediaStore lấy metadata theo batch trên IO. Loại bài dưới 30 giây và ringtone/notification/alarm; lưu title/artist/album chuẩn hóa, gồm `đ → d`.
- `scanLibrary(): Unit` chỉ ghi sau khi scan hoàn tất. Transaction thay snapshot nguyên tử; lỗi/cancel/quyền bị từ chối giữ dữ liệu cũ. Kiểm tra quyền cả trước và sau đọc MediaStore.
- Quyền `READ_MEDIA_AUDIO` trên API 33+, `READ_EXTERNAL_STORAGE` tới API 32; UI có cấp quyền/thử lại/mở Settings và kiểm tra lại khi resume.
- Danh sách album, detail bài hát theo track, Back về album; loading/empty/error/retry và Preview sáng/tối, tên dài, thiếu bìa, từ chối quyền.
- ArtworkStore đọc bìa nhúng trên IO, thử `cover.jpg` qua MediaStore URI khi truy cập được; Coil dùng chung một ImageLoader, decode theo kích thước hiển thị. Bìa thiếu/hỏng dùng placeholder.
- CI workflow cho PR: build, unit test, lint, JDK JetBrains 21. Không đổi toolchain/SDK hiện tại.

Kết quả kiểm tra và ảnh: [TESTING.md](TESTING.md).

## Luồng file mẫu

```text
VinylApplication → AppContainer
  → VinylDatabase.songDao()
  → MediaStoreScanner.getSongs() → LibraryRepository.scanLibrary()
  → ArtworkStore → Coil ImageLoader dùng chung

AppNavHost → AlbumsRoute → AlbumsViewModel → LibraryRepository → SongDao → Room
                 └──── AlbumsScreen(state, callbacks) → album/detail
```

| Trách nhiệm | File dưới `app/src/main/java/com/vinyl/` |
|---|---|
| Nối dependency/factory | `AppContainer.kt`, `VinylApplication.kt` |
| Entity, album query, transaction | `shared/database/SongDao.kt` |
| Database và schema version | `shared/database/VinylDatabase.kt`; schema export ở `app/schemas/` |
| Query thiết bị, bộ lọc, bỏ dấu | `library/MediaStoreScanner.kt` |
| API dữ liệu, tuần tự hóa scan | `library/LibraryRepository.kt` |
| Bìa nhúng/URI fallback và Coil fetcher | `library/ArtworkStore.kt` |
| State, thao tác, chọn album | `library/AlbumsViewModel.kt` |
| Route, UI, quyền, detail, Preview | `library/AlbumsScreen.kt` |

UI chỉ nhận state/callback; không gọi DAO hoặc tự tạo dependency. `AlbumsUiState` là top-level cùng ViewModel. Các type dùng giữa file là internal, dependency/mutable state private.

## API để Khánh mở rộng

- `LibraryRepository.observeAlbums(): Flow<List<AlbumSummary>>`: chỉ quan sát, không scan/seed.
- `LibraryRepository.observeAlbumSongs(albumId): Flow<List<Song>>`: theo track rồi tên chuẩn hóa; không có bài trả danh sách rỗng.
- `LibraryRepository.scanLibrary(): Unit` (suspend): hoàn tất sau transaction, lỗi ném về ViewModel. Constructor nhận hàm đọc thiết bị để test nguồn lỗi mà không tạo interface/implementation.
- `AlbumsViewModel.onAlbumClick(id)` / `onAlbumsClick()`: chọn/detail và quay lại; ID lưu qua SavedStateHandle.
- `ArtworkRequest(songUri, version, relativePath, volumeName)` dùng với `AsyncImage`; version lấy `dateModified`, không truyền Bitmap qua UiState/Room.
- `SongDao.getSongs()` là query snapshot để kiểm thử; không dùng DAO trực tiếp trong Composable.

Songs/Artists/sort chưa có màn riêng trong B1. Khi làm, thêm query cụ thể ở SongDao, expose qua LibraryRepository rồi collect trong ViewModel của feature. Mẫu query để mở rộng khi có caller:

```sql
-- Songs: ORDER BY titleNorm, contentUri
SELECT * FROM songs ORDER BY titleNorm, contentUri;

-- Artists: group theo ID, không theo tên có thể trùng
SELECT artistId, MIN(artist) AS name, COUNT(*) AS songCount
FROM songs GROUP BY artistId ORDER BY MIN(artistNorm), artistId;
```

Giữ state/Route/Screen theo mẫu Albums; thêm sort enum/query khi có UI thật. Album nhiều nghệ sĩ hiện hiển thị một tên đại diện (`MIN(artist)`). Không thêm bảng album/artists tổng hợp hoặc bản sao Song/domain/UI.

## Chạy thử với nhạc hợp pháp

1. Làm theo [SETUP](../SETUP.md), build và Run trên emulator API 32 hoặc 36.
2. Dùng nhạc mình có quyền sử dụng, hoặc tạo âm thanh tổng hợp bằng script này (Python 3 và một executable FFmpeg local):

   ```powershell
   python docs/b1/generate_test_music.py --ffmpeg C:\Tools\ffmpeg\bin\ffmpeg.exe
   ```

3. Script tạo 10 MP3 trong `app/build/b1-test-music`: 3 album × 2 bài, một bài 10 giây, ringtone/notification/alarm. Album A có bìa nhúng; B có `cover.jpg`; C có bìa nhúng cố tình hỏng. Đây là tone tự tổng hợp, không tải nhạc có bản quyền.
4. Device Explorer: chép nội dung các thư mục `Music`, `Ringtones`, `Notifications`, `Alarms` vào thư mục tương ứng dưới `/sdcard/`. Để Android index file; nếu chưa thấy, reboot emulator hoặc gửi `MEDIA_SCANNER_SCAN_FILE` cho từng URI `file:///sdcard/...` qua adb.
5. Cấp quyền trong app: thấy 3 album/6 bài. Chọn album → đúng hai bài 0:31. Quét lại → không trùng. Các file ngắn/system sound không xuất hiện.
6. Thu hồi quyền trong Android Settings, mở lại app → thấy hướng dẫn và nút cấp quyền, dữ liệu Room vẫn còn. Cấp lại → scan được.

APK sau build ở `app/build/outputs/apk/debug/app-debug.apk`. Bản local bàn giao cũng được copy vào `docs/b1/vinyl-b1-debug.apk` (APK bị gitignore, cần build/copy lại sau clone).

## Giới hạn và việc tiếp theo

- `cover.jpg` chỉ đọc khi scoped storage cho phép URI; không xin thêm quyền ảnh/all-files và không mở filesystem path để vượt quyền. Trên API 26–28 chưa có relative path qua MediaStore nên chỉ thử bìa nhúng. Bộ nhạc ngoài app trên API 32/36 cho thấy cover.jpg bị ẩn và placeholder hoạt động đúng.
- Scan tự chạy lần đầu mỗi ViewModel khi có quyền; chuyển tab/recomposition không scan lại. Có nút quét lại thủ công; chưa tự theo dõi thay đổi file nền.
- Snapshot B1 dùng delete + insert trong cùng transaction, phù hợp schema chỉ có songs. **Trước B4 có quan hệ playlist**, đổi sang upsert + xóa riêng bài mất để tránh ảnh hưởng reference. Tăng version/migration khi đổi schema, không destructive migration.
- B2 thêm playback/service/mini player. B1 không có nút phát giả; Playlist/Settings vẫn ghi rõ milestone tiếp theo.
- Chưa walkthrough trực tiếp với teammate, chạy máy thật hoặc profile scroll 3.000 file; không suy ra hiệu năng UI từ test Room 3.000 record.

Tham chiếu: [Room](https://developer.android.com/jetpack/androidx/releases/room), [quyền/media storage](https://developer.android.com/training/data-storage/shared/media), [Coil ImageLoader](https://coil-kt.github.io/coil/image_loaders/).
