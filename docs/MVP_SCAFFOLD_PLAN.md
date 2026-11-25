# Kế hoạch scaffold MVP — Vinyl

Trạng thái: kế hoạch, chưa triển khai. Mục tiêu là một base chạy bằng nhạc thật để Quý làm engine, Khánh mở rộng UI theo cùng pattern.

## 1. Các quyết định chung

- Một module `:app`, Kotlin + Compose + Material 3, package theo tính năng.
- Giữ Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21 và Compose BOM hiện tại. Gradle dùng JDK 17 hoặc 21; bytecode target 17. Không nâng version chỉ vì có bản mới.
- `minSdk 26`, `compileSdk 36`, `targetSdk 35` như project hiện tại.
- Luồng chính: Screen → ViewModel → Repository → Room / MediaStore. Repository chỉ giữ logic dữ liệu thực sự cần dùng chung; Settings đơn giản có thể dùng DataStore trực tiếp từ ViewModel. Player có service riêng.
- Đề xuất thay Hilt trong PLANNING.md bằng DI thủ công: `VinylApplication` giữ một `AppContainer`, tạo database/repository dùng chung; ViewModel nhận dependency qua constructor và factory chuẩn của AndroidX. Không tạo dependency trong Composable.
- Chỉ thêm dependency khi bước đó cần. Ghim phiên bản stable tương thích build hiện tại, chạy build ngay sau khi thêm.

## 2. Thư viện và phần tự viết

| Việc | Chọn | Phần code của team |
|---|---|---|
| UI | Compose, Material 3, Navigation Compose | Screen, điều hướng, theme sáng/tối |
| State | AndroidX ViewModel, Lifecycle Compose, Coroutines/StateFlow | UiState cụ thể cho từng màn |
| Database | Room + KSP | Entity, DAO, query, migration |
| Quét nhạc | MediaStore có sẵn của Android | Query, lọc file, đồng bộ Room |
| Phát nhạc | Media3 ExoPlayer + MediaSessionService + MediaController | Queue commands, fade, resume và lịch sử |
| Ảnh | Coil Compose, một ImageLoader dùng chung | Đọc bìa, cache file local và ảnh mặc định |
| Cài đặt | DataStore Preferences | Theme, tùy chọn của app |
| Màu theo bìa | AndroidX Palette | Tính một lần theo bìa, cache kết quả |
| Kiểm tra | JUnit, coroutine-test; Room instrumentation khi cần | Logic lọc, playlist, lịch sử, restore |

Không thêm Retrofit/OkHttp cho app offline, thư viện permissions, framework kiến trúc hoặc thư viện animation riêng khi Android/Compose đã đủ dùng. Room tạo code qua KSP để tránh tự viết SQLite; Media3 phụ trách giải mã và media session, team không tự làm audio decoder.

Không thể bảo đảm một thư viện hoàn toàn không ảnh hưởng performance. Chọn các thư viện trên vì phù hợp tác vụ và có tài liệu chính thức; kiểm tra thực tế ở các đường quét nhạc, load bìa và playback.

## 3. Quy tắc code dễ đọc

- Hàm đặt tên theo hành động: `scanLibrary`, `createPlaylist`, `playAlbum`, `restorePlayback`.
- Dùng `data class`, `enum class`, `if`, `when`, `for`, `suspend`. Tránh custom DSL, reflection, operator overload, generic framework và chuỗi scope function lồng nhau.
- Dùng Flow khi quan sát dữ liệu; dùng suspend cho thao tác một lần. Không biến mọi hàm thành Flow.
- State màn hình là data class đơn giản: `isLoading`, `items`, `errorMessage`; UI tự suy ra empty khi đã tải xong và danh sách trống.
- Repository là class cụ thể; chưa tạo interface + implementation nếu chưa có nhu cầu thay thế thực tế. Không thêm UseCase chỉ để gọi lại một hàm repository.
- Screen nhận state/callback để Preview dễ viết; hàm Route lấy ViewModel và collect state bằng lifecycle, đặt ngay trong cùng file Screen. Không tạo một file riêng cho từng hàm hoặc data class nhỏ.
- Bắt lỗi tại thao tác có thể thất bại, hiển thị retry; không nuốt exception, không dùng GlobalScope hoặc `!!` để vượt lỗi.
- Comment giải thích quyết định/bẫy, không diễn giải lại từng dòng code.

## 4. Cấu trúc nhỏ, ít tầng

```text
com.vinyl/
  MainActivity.kt
  VinylApplication.kt
  AppContainer.kt       # Chỉ khởi tạo dependency dùng chung
  AppNavHost.kt
  shared/
    database/
      VinylDatabase.kt
      SongDao.kt        # Song và query model nhỏ có thể cùng file
      PlaylistDao.kt    # Entity playlist và DAO có thể cùng file
    ui/
      Theme.kt
      EmptyState.kt     # Khi nhiều màn dùng thật
  library/
    LibraryRepository.kt
    MediaStoreScanner.kt
    ArtworkStore.kt
    AlbumsViewModel.kt  # AlbumsUiState đặt cùng file
    AlbumsScreen.kt     # Route, Screen, Preview cùng file
    LibrarySampleData.kt
  player/
    PlaybackService.kt
    PlayerController.kt
    PlaybackStateStore.kt
    PlayEventLogger.kt
    NowPlayingViewModel.kt
    NowPlayingScreen.kt
    MiniPlayer.kt
  playlist/
    PlaylistsViewModel.kt
    PlaylistsScreen.kt
    PlaylistRepository.kt  # Khi cần gom transaction/quy tắc ghi
  search/
    SearchViewModel.kt
    SearchScreen.kt
  settings/
    SettingsStore.kt     # Một chỗ khai báo DataStore và preference keys
    SettingsViewModel.kt
    SettingsScreen.kt
```

- Code của cùng tính năng nằm cạnh nhau, không chia thêm `feature/data/domain/core` và không tạo nhiều module.
- Đây là bản đồ đích khi MVP đủ tính năng, không phải danh sách class cần tạo trước. Chỉ tạo file khi có code dùng thật.
- Entity Room có thể dùng trực tiếp trong app nhỏ. Chỉ tạo model riêng khi query trả dữ liệu khác hoặc UI cần hình dạng khác; không copy cùng một Song thành entity/domain/UI model rồi viết mapper.
- Hàm normalize và format đặt tại file sử dụng trước; chỉ tách ra file dùng chung khi có nơi thứ hai cần dùng.
- Không tạo BaseViewModel, BaseRepository, BaseScreen, generic UiState/Result, UseCase, navigator riêng hoặc event bus.
- Không tách một class thành interface + implementation chỉ vì convention. Với logic cần test, ưu tiên hàm thuần nhận dữ liệu đầu vào; chỉ thêm interface nhỏ ở ranh giới thật sự cần fake.
- AppContainer chỉ nối dependency, không chứa nghiệp vụ. ViewModel giữ state/thao tác màn hình; service giữ playback. Tránh dồn cả app vào MainActivity hoặc một ViewModel duy nhất.
- Khi file khó đọc vì nhiều trách nhiệm, tách theo trách nhiệm cụ thể. Không tách theo số dòng hoặc nhu cầu giả định sau này.
- SampleData chỉ dùng cho Preview/test, không thay thư viện thật lúc chạy app.

### 4.1. Chọn chỗ đặt file mới — dành cho người và AI

1. Đọc code hiện có và tìm file cùng trách nhiệm trước khi tạo file mới.
2. Code thuộc một tính năng đặt trong package tương ứng (`library`, `player`, `playlist`, `search`, `settings`). UI và logic của tính năng nằm cạnh nhau, không tạo thêm `data/domain/presentation` bên trong.
3. Code nhiều tính năng thực sự dùng chung mới đặt trong `shared/<trách nhiệm>`. Database Room tập trung tại `shared/database`; theme/component tái sử dụng tại `shared/ui`.
4. Code dùng chung về dữ liệu không bắt buộc chuyển sang shared: search có thể dùng LibraryRepository; mini player có thể dùng PlayerController. Nếu vẫn do một tính năng sở hữu thì giữ ở tính năng đó. Không chuyển cả repository vào shared chỉ vì có caller thứ hai.
5. Chỉ bổ sung package shared khi có trường hợp dùng cụ thể. Ví dụ `shared/permissions` cho luồng quyền dùng ở nhiều nơi; `shared/media` cho xử lý URI/metadata trung lập mà nhiều tính năng cần; `shared/model` chỉ khi có model chung không thuộc Room hoặc tính năng nào. Không tạo sẵn các thư mục này.
6. Shared không import code từ các package tính năng. Nếu phải làm vậy, xem lại nơi sở hữu code trước khi thêm interface để xử lý vòng phụ thuộc.
7. File ở package root chỉ dành cho điểm khởi động và nối app: Activity, Application, AppContainer, AppNavHost. Không đặt tiện ích nghiệp vụ tại root.
8. Giữ package trùng đường dẫn dưới `app/src/main/java/com/vinyl/`. Unit test có đường dẫn tương ứng dưới `app/src/test/java/com/vinyl/`; test cần Android/Room ở `app/src/androidTest/java/com/vinyl/`.

Ví dụ: thêm màn hàng đợi → `player/QueueScreen.kt`, `player/QueueViewModel.kt`; thêm đổi tên playlist → mở rộng file trong `playlist/`; nút riêng của Queue → private Composable cùng file; component thực sự dùng ở Library và Playlist → `shared/ui/<TênCụThể>.kt`.

### 4.2. Đặt tên

| Loại | Convention | Ví dụ |
|---|---|---|
| Package | Chữ thường, tên ngắn theo tính năng; một từ khi có thể | `playlist`, `shared.database` |
| File Kotlin | PascalCase, tên theo khai báo chính hoặc trách nhiệm cụ thể | `AlbumsScreen.kt`, `SongDao.kt` |
| Class/interface/data class/enum | PascalCase | `AlbumsViewModel`, `AlbumsUiState`, `AlbumSort` |
| Hàm/property/parameter | camelCase, rõ nghĩa; hàm thao tác dùng động từ | `scanLibrary`, `albumId`, `onAlbumClick` |
| Composable trả UI | PascalCase theo convention Compose | `AlbumsScreen`, `AlbumRow` |
| Boolean | Diễn đạt được true/false | `isLoading`, `hasPermission`, `canSeek` |
| Hằng số const | UPPER_SNAKE_CASE | `MIN_SONG_DURATION_MS` |
| Unit test | Tên class chính + Test; tên hàm mô tả điều kiện/kết quả | `LibraryRepositoryTest`, `scan_skipsShortSongs` |

- Dùng hậu tố nhất quán: `Screen`, `ViewModel`, `UiState`, `Repository`, `Dao`, `Service`, `Store`. Store lưu/truy xuất state hoặc preference; Repository gom thao tác dữ liệu/nghiệp vụ. Không tạo cả hai nếu cùng làm một việc.
- Đặt tên đơn vị trong giá trị khi dễ nhầm: `durationMs`, `positionMs`; không dùng số không rõ nghĩa.
- Tránh `Utils.kt`, `Helpers.kt`, `Common.kt`, `Manager`, `Base...`, `...Impl` khi tên cụ thể mô tả được trách nhiệm. Theme ở `Theme.kt`; component chung tách theo tên cụ thể khi cần, không gom mọi UI vào một CommonComponents khổng lồ.
- Sample data đặt theo tính năng: `LibrarySampleData.kt`; Android resource dùng snake_case (`app_name`, `ic_play`), tên hiển thị tách khỏi tên định danh code.

### 4.3. Nhiều class trong một file và nested class

Mặc định một class chính cho một trách nhiệm trong một file. Các khai báo nhỏ liên quan chặt có thể đặt cạnh nhau dưới dạng top-level; không bắt buộc một file cho mỗi class.

- `AlbumsViewModel.kt`: `AlbumsViewModel` và `AlbumsUiState` là hai khai báo top-level cùng file.
- `AlbumsScreen.kt`: `AlbumsRoute`, `AlbumsScreen`, các private Composable chỉ màn này dùng, và private Preview. Không tạo file riêng cho từng hàng/nút nhỏ.
- `SongDao.kt`: có thể giữ `Song`, `SongDao` và query model album nhỏ cùng file khi file vẫn dễ đọc. Khi entity/query model có logic riêng, lớn lên hoặc gây xung đột sửa code, tách `Song.kt`/`AlbumSummary.kt` cùng package.
- `PlaylistDao.kt`: entity liên kết và kết quả query liên quan có thể cùng file; không gom tất cả entity của app vào một file lớn.
- Helper chỉ file cần dùng khai báo `private`; helper chỉ một class dùng ưu tiên private function trong class đó.
- Chỉ nested class khi type thực sự thuộc riêng class ngoài, ví dụ cấu hình hoặc kết quả nội bộ. Không lồng UiState trong ViewModel, không lồng DAO/Entity trong Database để gom file.
- Không dùng Kotlin `inner class` nếu không thật sự cần giữ tham chiếu đến instance ngoài.
- Khi tách file, giữ nguyên package sở hữu; không đưa class sang shared chỉ vì đã tách thành file mới.

AI trước khi sửa cần nêu ngắn gọn nơi đặt code và lý do; chỉ sinh file có caller hoặc mục đích kiểm tra cụ thể trong task hiện tại. Khi đổi cấu trúc, cập nhật import, caller và tài liệu liên quan trong cùng thay đổi.

### 4.4. Hợp đồng tên hàm và giá trị trả về — bắt buộc

Tách thao tác đọc và thao tác thay đổi dữ liệu. Tên hàm phải nói đúng việc caller có thể kỳ vọng; không ghép thêm side effect để tiện một caller.

| Nhóm | Hợp đồng | Ví dụ |
|---|---|---|
| `get...` | Đọc một kết quả bắt buộc có; không ghi dữ liệu hoặc thay đổi playback/state nghiệp vụ. Không có thì báo lỗi rõ ràng theo hợp đồng. | `getSong(id): Song` |
| `find...` | Đọc kết quả có thể không tồn tại; trả nullable. | `findSong(id): Song?` |
| `get...` danh sách | Trả danh sách, không có dữ liệu thì danh sách rỗng. | `getSongs(): List<Song>` |
| `observe...` | Trả Flow chỉ quan sát; không scan, seed, insert hoặc sửa dữ liệu khi bắt đầu collect. | `observeAlbums(): Flow<List<AlbumSummary>>` |
| `is...`, `has...`, `can...` | Kiểm tra, trả Boolean, không thay đổi nghiệp vụ. | `hasAudioPermission(): Boolean` |
| `calculate...`, `format...`, `normalize...` | Tính kết quả từ input; không sửa input, không truy cập DB/player. | `normalizeSearchText(text): String` |
| `set...` | Gán đúng thuộc tính được đặt tên, có thể validate; không toggle, không phát nhạc/quét thư viện kèm theo; trả Unit. | `setTheme(theme): Unit` |
| `create...`, `add...`, `update...`, `rename...`, `delete...`, `remove...` | Thay đổi dữ liệu được nêu trong tên, trả Unit. | `renamePlaylist(id, name): Unit` |
| `play...`, `pause...`, `seek...`, `scan...`, `restore...` | Thực hiện hành động cụ thể, trả Unit. Tiến độ/state hiển thị qua state riêng. | `scanLibrary(): Unit` |
| Callback `on...` | Xử lý một sự kiện xác định, trả Unit. | `onAlbumClick(albumId): Unit` |

- Kotlin dùng `Unit` tương đương void ở đây. Hàm hành động viết block body, có thể bỏ `: Unit` vì Kotlin suy ra Unit; lambda hành động khai báo `(Input) -> Unit`. Không dùng expression body làm vô tình lộ kết quả của DAO.
- Không dùng tên mơ hồ `handle`, `process`, `execute`, `doWork` mà không chỉ ra sự kiện hoặc đối tượng cụ thể. Override bắt buộc của framework giữ tên/signature framework.
- Không dùng `getOrCreate...`, `getAndUpdate...`: caller gọi riêng `find...` và command, hoặc dùng một command transaction có tên nghiệp vụ rõ ràng. Không tách transaction thành nhiều bước chỉ để tuân thủ tên nếu làm mất tính nhất quán.
- Ví dụ tạo playlist và điều hướng: caller tạo ID trước, truyền vào `createPlaylist(id, name): Unit`; điều hướng sau khi suspend command hoàn tất. Không thêm hàm trả ID chỉ vì tiện, không gọi lại get để tìm ID theo tên có thể trùng.
- Command thất bại phải báo lỗi qua exception cụ thể, ViewModel bắt và cập nhật error state. Không trả Boolean thành công/thất bại hoặc nuốt lỗi cho vừa chữ ký Unit. Command suspend chỉ hoàn tất khi thao tác đã xong; không tự launch rồi báo thành công sớm.
- Query có thể suspend/đọc IO theo nhu cầu nhưng không âm thầm refresh cache, ghi DB, xin quyền hoặc phát nhạc. Việc refresh là command riêng. Cache kỹ thuật của thư viện như Coil do thư viện quản lý không phải side effect nghiệp vụ của getter.
- Ngoại lệ hẹp: Room/Media3/framework yêu cầu signature riêng (ví dụ insert trả row ID hoặc callback trả kết quả). Giữ ngoại lệ tại ranh giới đó; không truyền kết quả ra API command của app nếu không cần. API kết hợp sửa + trả kết quả ngoài các trường hợp này phải được thống nhất trước và ghi lý do; AI không tự tạo.
- Composable là hàm dựng UI theo convention Compose, không phải query chỉ vì có tên PascalCase. Không gọi command trực tiếp trong body recomposition; thao tác đi qua callback hoặc effect có lifecycle/key rõ ràng.

### 4.5. Visibility và phạm vi biến — bắt buộc

- Chọn phạm vi nhỏ nhất đủ dùng: biến cục bộ → member/file `private` → `internal` khi cần dùng giữa file trong module. Không để mặc định public một cách vô tình.
- Class/type top-level cần dùng trong module khai báo `internal`; helper top-level chỉ file dùng khai báo `private`. Kotlin không có package-private: `internal` vẫn cho toàn bộ module truy cập, không tự bảo vệ ranh giới feature.
- Member trong class internal có thể giữ visibility mặc định khi đó là API caller thật sự cần; phạm vi hiệu lực vẫn bị giới hạn bởi class. Helper và dependency fields vẫn phải `private`. Không thêm public chỉ để code dễ gọi.
- Activity/Application/Service, override và type do framework/code generation truy cập dùng visibility mà framework yêu cầu. Không ép private/internal làm hỏng manifest, Room hoặc toolchain. Ghi lý do ngắn khi phải mở rộng phạm vi ngoài convention.
- Dependency truyền vào constructor dùng `private val` nếu chỉ class đó cần. Constructor/factory không phơi bày database/player mutable cho UI.
- Ưu tiên `val`; `var` phải có lý do cần thay đổi và phạm vi nhỏ. Không khai báo biến ở cấp class nếu chỉ một hàm dùng; không giữ Activity/View trong singleton hoặc AppContainer.
- MutableStateFlow/MutableList nằm private; chỉ expose StateFlow và dữ liệu read-only khi caller cần. Dùng `asStateFlow()` cho state; không trả trực tiếp mutable collection nội bộ dưới kiểu List nếu caller có thể giữ tham chiếu đến dữ liệu còn bị sửa. Trả snapshot khi cần.
- Property caller được đọc nhưng class tự sửa dùng private setter khi thích hợp. Không expose setter hoặc hàm mutable chỉ để test; test qua hành vi/API cần có thật.
- Hằng số private trong file/class dùng nó; chỉ internal khi nhiều file thật sự cần. Không dùng object Global hoặc companion object làm nơi chứa state mutable dùng chung.
- Khi review: mỗi hàm/property được expose phải có caller hoặc lý do framework; kiểm tra getter không ghi, command không trả dữ liệu và helper không bị mở scope thừa. Test tập trung vào rủi ro hành vi, không viết test chỉ để kiểm tra cách đặt tên.

## 5. Thứ tự triển khai và bàn giao

### B0 — Khung app dùng chung

- Tách Theme, AppNavHost và màn Library khỏi MainActivity. Thêm ViewModel/lifecycle/navigation.
- Tạo VinylApplication/AppContainer, navigation Library → Playlists → Settings; màn đang xây phải ghi rõ trạng thái.
- Hiển thị Loading/Empty/Error trực tiếp trong màn mẫu và có Preview sáng/tối; chỉ tách component chung khi nhiều màn dùng lại.
- Sửa README: đường dẫn mới, JDK 17/21, SDK 36, lệnh build và chạy emulator.

Đạt khi: clone → sync → Run; điều hướng và Preview chạy, không cần tài khoản/API key. Khánh bắt đầu theme/component trong lúc Quý làm B1.

### B1 / L1 — Thư viện thật, lát mẫu cho teammate

- Room schema v1 cho songs; album/nghệ sĩ lấy bằng query group theo ID thay vì giữ thêm bảng tổng hợp chưa cần thiết. Đây là cách đơn giản hóa schema trong planning gốc.
- Scanner dùng MediaStore/content URI, lấy metadata theo batch, lọc duration < 30 giây và ringtone/notification/alarm. Giữ cột title/artist/album chuẩn hóa cho tìm kiếm.
- Scan trên IO, ghi Room bằng transaction. Chỉ xóa record cũ sau khi quét hoàn tất thành công; bị từ chối quyền hoặc scan lỗi không được xóa trắng thư viện.
- Xin READ_MEDIA_AUDIO trên API 33+, READ_EXTERNAL_STORAGE với maxSdkVersion 32 trên máy cũ. Có trạng thái từ chối và nút thử lại/mở Settings.
- ArtworkStore đọc bìa ngoài main thread, thử bìa nhúng rồi cover.jpg khi URI/quyền cho phép; không truy cập path trực tiếp để vượt scoped storage. Không đọc được thì dùng placeholder.
- AlbumsViewModel + AlbumsScreen có loading/empty/error, danh sách album và một màn album detail thô có bài hát.
- Handoff L1 ghi rõ file khuôn và query để Khánh làm Songs/Artists/detail/sort, không gọi DAO từ UI.

Đạt khi: đẩy nhạc hợp pháp vào emulator bằng Device Explorer, scan thấy đúng; rescan không trùng; lỗi quyền không crash; bìa lỗi vẫn cuộn được. Có test logic lọc và bỏ dấu (bao gồm đ → d).

### B2 / L2 — Player nền và mini player

- PlaybackService giữ một ExoPlayer và MediaSession; UI kết nối bằng MediaController qua PlayerController.
- Set cả danh sách MediaItem để phát liên tiếp; dùng queue và repeat/shuffle có sẵn của Media3.
- API rõ ràng: playSongs, playPause, next, previous, seekTo, playNext, addToQueue, removeFromQueue. Dùng media ID ổn định, đồng bộ UI bằng listener.
- Thêm foreground service/media playback permissions và khai báo service theo tài liệu Media3. Chỉ xin POST_NOTIFICATIONS khi luồng thông báo thực tế cần quyền này; không chặn playback chỉ vì người dùng từ chối notification.
- Audio focus và xử lý rút tai nghe dùng cấu hình Media3. Release controller/session/player đúng lifecycle.
- Mini player thật + NowPlaying thô có seek/next/previous; Khánh mở rộng UI và Queue từ đây.

Đạt khi: phát được nhạc thật, khóa màn hình vẫn nghe, notification/tai nghe điều khiển được, vào lại màn hình không tạo player thứ hai. Test gapless bằng file/codec phù hợp, không giả định mọi file đều gapless.

### B3 — Hoàn thiện engine MVP: fade, lịch sử, resume

- Fade play/pause 300ms bằng một coroutine job, hủy job trước khi nhận lệnh mới; các lệnh app/notification/Bluetooth đi qua cùng một điểm xử lý ở service. Next/seek không để volume kẹt ở 0.
- PlayEventLogger lưu thời gian thực sự đang phát bằng đồng hồ monotonic; pause/buffering/seek không làm tăng thời lượng nghe. Dữ liệu timestamp dùng cho thống kê lưu riêng.
- Room lưu queue, current index, position, repeat/shuffle trong snapshot; lưu khi đổi bài/pause và định kỳ khi đang phát, không ghi mỗi frame. Logger cũng checkpoint để process chết không mất cả lượt nghe.
- Restore bỏ URI không còn truy cập được; prepare ở trạng thái pause. Bluetooth resumption qua callback Media3. Force-stop của hệ điều hành không được xem là tình huống luôn tự resume được.

Đạt khi: đóng/mở app khôi phục bài và vị trí; file bị xóa không crash; pause không tăng lịch sử. Có test tính thời gian nghe và lọc queue restore; kiểm tra Bluetooth/notification/fade bằng thiết bị thật.

### B4 / L3 — Playlist và pattern ghi dữ liệu

- Room thêm playlists và playlist_songs; mỗi lần thay schema tăng version + migration, không dùng destructive migration để xóa dữ liệu người dùng.
- Playlist item có ID riêng và position; cho phép cùng bài xuất hiện nhiều lần. Đổi thứ tự/xóa/cập nhật position trong transaction.
- Repo cung cấp create/rename/delete/addSong/removeItem; kiểm tra tên rỗng và xử lý bài đã bị xóa khỏi máy.
- Màn danh sách + dialog tạo + chi tiết thô chạy thật; Khánh hoàn thiện menu và UI theo mẫu.

Đạt khi: tạo playlist, thêm bài, phát playlist, tắt/mở app dữ liệu còn nguyên. Test DAO cho thứ tự và xóa item.

### B5 — Search, settings, bàn giao MVP

- Search dùng Room LIKE trên cột bỏ dấu, debounce khoảng 300ms; tham số query có binding, thống nhất xử lý ký tự % và _ như chữ tìm kiếm khi cần.
- DataStore lưu theme; Settings có rescan và feedback đang scan/lỗi. Palette xử lý theo bìa, không tính lại mỗi lần recomposition.
- Khánh hoàn thiện Songs/Artists, sort, Queue, NowPlaying và onboarding theo API đã bàn giao. Quý hỗ trợ engine và review pattern.
- Chạy một luồng xuyên suốt: cấp quyền → scan → chọn album → phát nền → thêm playlist → tìm kiếm → mở lại app.

Đạt khi: teammate tự clone/run bằng README và nhạc test, không cần Quý sửa cấu hình trên máy teammate.

## 6. Performance và kiểm tra bắt buộc

- Quét file, đọc bìa và DB ngoài main thread; không scan lại ở mỗi lần recomposition hoặc chuyển tab.
- LazyColumn/LazyVerticalGrid có key ổn định; Coil load ảnh theo kích thước hiển thị. Không đưa Bitmap vào bảng Room.
- Chỉ poll position khi UI player đang hiển thị; service không chạy vòng lặp cập nhật UI liên tục.
- Thử thư viện khoảng 3.000 bài, quan sát scroll/scan và bộ nhớ bằng profiler; nếu có chậm mới tối ưu chỗ đo được.
- Mỗi lát chạy assembleDebug, unit test liên quan và lintDebug; playback/permission cần emulator API 32 + 36 và kiểm tra máy thật khi có.
- CI dùng wrapper + JDK 17, chạy assembleDebug/testDebugUnitTest/lintDebug khi PR. Không cần CI emulator trong lần scaffold đầu.

## 7. Cách bàn giao

Mỗi lát là một PR chạy được, kèm `docs/handoff/Lx.md`: bước chạy thử, luồng file cụ thể, API hiện có, file mẫu để copy, việc Khánh làm tiếp và giới hạn đã biết. Không đánh dấu hoàn tất bằng màn chỉ hiển thị dữ liệu giả.

Base hoàn tất khi L1/L2/L3 hoạt động cùng nhau, có error/empty/loading, Preview, test cho logic rủi ro và teammate tự chạy được trên máy khác. Không cần UI đẹp để bàn giao.

## 8. Ngoài đợt scaffold MVP

Crossfade hai player, album overrides/crop, Wrapped, Story sharing và Vinyl animation theo phase 2 của PLANNING.md. Chưa tạo placeholder engine/API cho chúng; play_events và playback state trong MVP là phần nền thực sự cần giữ lại. Cài đặt crossfade chỉ xuất hiện khi engine đó đã hoạt động.

## Tài liệu tham chiếu

- Media3 background playback: https://developer.android.com/media/media3/session/background-playback
- DI thủ công: https://developer.android.com/training/dependency-injection/manual
- Room: https://developer.android.com/training/data-storage/room
- Coil Compose: https://coil-kt.github.io/coil/compose/
- Coil ImageLoader/cache: https://coil-kt.github.io/coil/image_loaders/
