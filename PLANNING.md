# 🎵 PLANNING — App nghe nhạc offline (Android · Kotlin)

> Tên tạm: **Vinyl** — *"Máy nghe nhạc offline biết kể chuyện về gu nhạc của bạn."*
> Thành viên: **Quý** (làm base / engine) · **Khánh** (mở rộng tính năng & giao diện)
> Thời lượng: **4 tuần (30 ngày)** · 5 lát base (L1–L5) · 3 mốc release
> Khối lượng ước tính: **Quý ≈ 135h** (~34h/tuần) · **Khánh ≈ 120h** (~30h/tuần)

---

## Mục lục
1. [Tầm nhìn & định vị](#1-tầm-nhìn--định-vị)
2. [Nhu cầu người dùng (có bằng chứng)](#2-nhu-cầu-người-dùng-có-bằng-chứng)
3. [Phạm vi tính năng](#3-phạm-vi-tính-năng)
4. [⭐ Mô hình làm việc: Quý làm base → Khánh mở rộng](#4--mô-hình-làm-việc-quý-làm-base--khánh-mở-rộng)
5. [Kiến trúc & công nghệ](#5-kiến-trúc--công-nghệ)
6. [Database schema](#6-database-schema)
7. [Timeline tổng quan](#7-timeline-tổng-quan)
8. [Chi tiết theo tuần](#8-chi-tiết-theo-tuần)
9. [Ghi chú kỹ thuật quan trọng](#9-ghi-chú-kỹ-thuật-quan-trọng)
10. [Quy trình làm việc](#10-quy-trình-làm-việc)
11. [Definition of Done](#11-definition-of-done)
12. [Kiểm thử](#12-kiểm-thử)
13. [Rủi ro & phương án dự phòng](#13-rủi-ro--phương-án-dự-phòng)
14. [Kịch bản demo & báo cáo](#14-kịch-bản-demo--báo-cáo)
15. [Backlog sau v1.0 (đã cắt)](#15-backlog-sau-v10-đã-cắt)

---

## 1. Tầm nhìn & định vị

**Vấn đề:** Người nghe nhạc file offline có player tốt (Poweramp, Musicolet…) nhưng:
- Không có **tổng kết kiểu Spotify Wrapped** chạy hoàn toàn offline để khoe lên mạng xã hội.
- Album chỉ là "thư mục": không có **tính cá nhân** (bìa, màu, tên do mình đặt).
- Chuyển bài **cụt / có khoảng lặng**, không mượt như app streaming.

**3 trục của bản 1 tháng:**
1. 🎚️ **Chuyển bài mượt:** gapless + fade play/pause + **crossfade**.
2. 🎨 **Cá nhân hóa album:** đổi bìa, bìa tự sinh, màu theo bìa, sửa tên/nghệ sĩ/năm, player kiểu Vinyl.
3. 📊 **Wrapped theo tháng** + **chia sẻ 1 chạm lên Instagram / Facebook Story**.

**Nguyên tắc:** Không phụ thuộc AI · Không cần tài khoản · 100% dữ liệu trên máy.

---

## 2. Nhu cầu người dùng (có bằng chứng)

| Nhu cầu | Nguồn quan sát | Tính năng đáp ứng |
|---|---|---|
| Thống kê / Wrapped cho nhạc offline | Forum Poweramp ("Listening statistics": bạn bè đều share Spotify Wrapped); SimpMusic chỉ có thống kê 7/30 ngày | Wrapped tháng + share IG/FB |
| Ẩn nhạc chuông, file rác khỏi thư viện | Spotify Community ("Choose the folder for local files"), review app trên store | Lọc file < 30s & ringtone/notification/alarm |
| Sắp xếp theo album, metadata sai | Spotify Community ("New Local Files system on Android is bad") | Thư viện theo album, sửa metadata |
| Mở lại app mất bài đang nghe | Kỳ vọng phổ biến của người dùng player | Khôi phục trạng thái phát + resume qua Bluetooth |
| Chuyển bài có khoảng lặng / giật | Kỳ vọng từ app streaming | Gapless, crossfade, fade |

---

## 3. Phạm vi tính năng

### PHASE 1 — MVP (Ngày 1 → 14)

| ID | Tính năng | Phạm vi | Base (Quý) | Mở rộng (Khánh) |
|---|---|---|---|---|
| M1 | Quét thư viện | MediaStore, bỏ file < 30s + ringtone/notification/alarm | Scanner, lọc | Màn onboarding xin quyền |
| M2 | Metadata & ảnh bìa | Bìa nhúng hoặc `cover.jpg` | Đọc & cache bìa | — |
| M3 | Thư viện | Tab Bài hát / Album / Nghệ sĩ, sort, chi tiết | Repo + **tab Album thô** | Tab Bài hát, Nghệ sĩ, Album detail, Artist detail, sort, làm đẹp |
| M4–M5 | Player nền & điều khiển | Service, notification, lock screen, tai nghe | Service + controller + **mini player thô** | Now Playing, làm đẹp mini player |
| M6 | Hàng đợi | Xem, Phát tiếp theo, Thêm vào cuối, xóa | Các thao tác queue | Màn Queue, menu bài hát |
| M7 | **Gapless** | `setMediaItems` cả danh sách | ✅ | — |
| M8 | **Fade play/pause** | 300ms, cả từ notification/Bluetooth | ✅ | — |
| M9 | Playlist | Tạo / đổi tên / xóa / thêm bài / xóa bài | DAO + repo + **màn danh sách thô** | Chi tiết, đổi tên, xóa, thêm/xóa bài |
| M10 | Tìm kiếm | `LIKE` trên cột bỏ dấu | Cột `*_norm` | Query search + màn Tìm kiếm |
| M11 | Ghi lịch sử nghe | `play_events` | ✅ | — |
| M12 | Màu theo bìa | Palette | — | ✅ |
| M13 | **Khôi phục trạng thái phát** | Mở lại đúng chỗ + `onPlaybackResumption` | ✅ | — |
| M14 | Cài đặt | Theme, quét lại, crossfade | — | ✅ |

### PHASE 2 — Tính năng ghi điểm (Ngày 15 → 26)

| ID | Tính năng | Base (Quý) | Mở rộng (Khánh) |
|---|---|---|---|
| S1 | **Crossfade** (2 player, equal-power, luật cùng album) | ✅ toàn bộ engine | Màn cài đặt crossfade |
| S2 | **Wrapped tháng** (6 slide) | StatsEngine, seeder, **Story player + slide 1** | 5 slide còn lại, tab Wrapped |
| S3 | **Share IG / FB** | Render thẻ → ảnh, intent IG/FB/Khác | Thiết kế thẻ Story & Feed, nút share |
| S4 | Đổi ảnh bìa + bìa tự sinh | API lưu ảnh đã crop | Màn crop, bìa tự sinh |
| S5 | Sửa metadata album | Override repo + **form sửa tên thô** | Đủ trường, Khôi phục gốc, hiển thị khắp app |
| S6 | Player style **Vinyl** | — | ✅ |

### Tùy chọn (nếu dư thời gian)
| ID | Tính năng | Người làm |
|---|---|---|
| O1 | Sleep timer | Quý |
| O2 | Playlist có ảnh bìa riêng | Khánh |

**Thứ tự hy sinh nếu trễ:** O1/O2 → S6 → bìa tự sinh → slide "Giờ vàng".
**Không cắt:** M4, M7, M11, M13, S1, S2, S3.

---

## 4. ⭐ Mô hình làm việc: Quý làm base → Khánh mở rộng

### 4.1 Nguyên tắc "lát dọc" (vertical slice)
Quý **không** làm xong toàn bộ base rồi mới giao. Mỗi lần Quý làm **một lát mỏng chạy được từ đầu đến cuối**:

```
DB (Entity + DAO) → Repository → ViewModel → 1 màn hình thô chạy được
```

Màn thô = đúng dữ liệu, đúng luồng, **chưa cần đẹp**. Đó là **"khuôn"** để Khánh:
1. **Nhân bản:** làm các màn/tính năng tương tự theo đúng khuôn (vd: có tab Album → làm tab Nghệ sĩ).
2. **Mở rộng:** thêm thao tác, trạng thái, chi tiết (vd: có danh sách playlist → làm màn chi tiết, đổi tên, xóa).
3. **Làm đẹp:** áp design system, animation.

### 5 lát base của Quý
| Lát | Nội dung base | Khuôn giao cho Khánh | Bàn giao |
|---|---|---|---|
| **L1 – Thư viện** | Room, scanner, bìa, `LibraryRepository`, `AlbumsViewModel`, tab Album thô, `SampleData` | Pattern Repo → ViewModel → Screen, `UiState` | **Ngày 4** |
| **L2 – Player** | `PlaybackService`, `PlayerController`, mini player thô | Cách đọc `PlayerController.state` trong UI | **Ngày 7** |
| **L3 – Playlist** | Entity/DAO/Repo playlist, màn danh sách + dialog tạo | Pattern ghi dữ liệu (suspend + Flow) | **Ngày 8** |
| **L4 – Cá nhân hóa** | `album_overrides`, `PersonalizationRepository`, API lưu ảnh crop, form sửa tên thô | Pattern form + lưu + hoàn tác | **Ngày 15** |
| **L5 – Wrapped** | `StatsEngine`, seeder, **Story player** + slide 1 | Pattern 1 slide nhận `WrappedData` | **Ngày 18** |

Ngoài các lát, Quý làm trọn những phần engine không có UI riêng: queue, gapless, fade, logger, khôi phục trạng thái, crossfade, render & share.

### 4.2 Quy trình bàn giao một lát
1. Quý merge lát vào `develop`, **chạy được trên máy Quý** với nhạc thật.
2. Quý viết `docs/handoff/Lx.md` (mẫu ở 4.3).
3. **Buổi walkthrough 30 phút**: Quý mở code, chạy debug, giải thích luồng dữ liệu từ DB lên màn hình.
4. Khánh pull về, **chạy được trên máy Khánh** (với `test-music/`), rồi mới bắt đầu task mở rộng.
5. **PR đầu tiên của Khánh trên mỗi lát** được Quý review kỹ (đúng pattern chưa) — các PR sau review nhanh hơn.

### 4.3 Mẫu `docs/handoff/Lx.md`
```markdown
# Lx – <tên lát>
## Chạy thử
- Bước để thấy tính năng chạy trên máy
## Luồng dữ liệu
Entity → DAO → Repository → ViewModel → Screen (tên file cụ thể)
## File khuôn để copy
- `.../AlbumsViewModel.kt` – cách expose UiState
- `.../AlbumsScreen.kt` – cách collect state, xử lý Loading/Empty/Error
## Việc Khánh làm tiếp
- [ ] K-x.x …
## Lưu ý / bẫy
- Không gọi DAO trực tiếp từ UI
- …
## API đã có sẵn cho Khánh dùng
- `LibraryRepository.artists(): Flow<List<Artist>>` …
```

### 4.4 Khánh làm gì khi chưa có lát?
Những việc **không phụ thuộc base**, làm ngay từ ngày 1:
- Design system (màu, chữ, shape, dark/light), component dùng chung.
- Wireframe, onboarding, màn Cài đặt, empty/error state.
- Màn crop (chỉ cần một ảnh bất kỳ), bìa tự sinh (chỉ cần tên album), thẻ share dạng `@Preview`, Vinyl animation (chỉ cần cờ `isPlaying`).

→ Các việc này được xếp vào **khoảng chờ** trước mỗi lát để Khánh không bao giờ phải ngồi chờ.

### 4.5 Mỗi người tự test trên máy mình
- **Quý:** mỗi lát và mỗi phần engine đều có cách kiểm trên máy thật qua **màn Debug** (chỉ có ở bản debug, mở từ Cài đặt): xem `play_events`, `playback_state`, đồ thị volume crossfade, nút chạy seeder, nút "kill process", render thử thẻ share.
- **Khánh:**
  - Chạy app debug với `test-music/` trên máy mình — vì đã dựa trên base thật nên không cần ghép mới thấy kết quả.
  - `SampleData.kt` (Quý tạo ở L1) để viết `@Preview` cho mọi màn, không cần chạy app.
  - Màn Debug có nút seeder → có dữ liệu Wrapped ngay mà không cần nghe nhạc thật nhiều tuần.

---

## 5. Kiến trúc & công nghệ

### Tech stack
| Mảng | Thư viện |
|---|---|
| Ngôn ngữ | Kotlin 2.x, Coroutines, Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Kiến trúc | MVVM: Screen → ViewModel → Repository → DAO / DataSource |
| DI | Hilt |
| Playback | Media3 (`exoplayer`, `session`) |
| DB | Room |
| Ảnh | Coil, androidx.palette |
| Cài đặt | DataStore Preferences |
| Test | JUnit, Turbine, Robolectric |

`minSdk 26`, `targetSdk 35`. Quyền: `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (≤ 32), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

### Cấu trúc: 1 module `:app`, chia package theo tính năng
Giữ **1 module** để đơn giản việc build & Hilt; phân ranh giới bằng package.

```
com.vinyl/
├── core/
│   ├── model/          ← Song, Album, Artist, Playlist, QueueItem…        (Quý)
│   ├── database/       ← AppDatabase, Entity, DAO                          (Quý; Khánh thêm DAO khi mở rộng)
│   ├── designsystem/   ← Theme, Color, Type, component dùng chung          (Khánh)
│   ├── ui/             ← UiState, ArtImage, EmptyState, ErrorState         (Quý tạo khung, Khánh mở rộng)
│   └── util/           ← bỏ dấu tiếng Việt, format thời gian              (Quý)
├── data/
│   ├── library/        ← MediaStoreScanner, LibraryRepository              (Quý)
│   ├── playlist/       ← PlaylistRepository                                (Quý base, Khánh mở rộng)
│   ├── personalization/← PersonalizationRepository, ArtworkStore           (Quý)
│   └── stats/          ← StatsEngine, Seeder                               (Quý)
├── playback/           ← PlaybackService, PlayerController, Crossfade,
│                          PlayEventLogger, PlaybackStateStore               (Quý)
├── share/              ← ShareCardRenderer, ShareHelper                    (Quý)
├── feature/
│   ├── library/        ← Album (Quý thô) · Songs, Artists, Detail (Khánh)
│   ├── player/         ← MiniPlayer (Quý thô) · NowPlaying, Queue, Vinyl (Khánh)
│   ├── playlist/       ← List (Quý thô) · Detail, actions (Khánh)
│   ├── album_edit/     ← Form tên (Quý thô) · đủ trường, crop, bìa tự sinh (Khánh)
│   ├── wrapped/        ← StoryPlayer + slide 1 (Quý) · slide 2–6, share card (Khánh)
│   ├── search/         ← (Khánh)
│   ├── settings/       ← (Khánh)
│   └── debug/          ← Màn Debug, chỉ bản debug (Quý)
└── VinylApp.kt, MainActivity.kt, AppNavHost.kt
```

### Pattern chuẩn (Quý viết mẫu ở L1, Khánh làm theo)
```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(repo: LibraryRepository) : ViewModel() {
    val state: StateFlow<UiState<List<Album>>> = repo.albums(AlbumSort.TITLE)
        .map<List<Album>, UiState<List<Album>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Lỗi")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}

@Composable
fun AlbumsScreen(vm: AlbumsViewModel = hiltViewModel(), onAlbumClick: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message)
        is UiState.Success -> if (s.data.isEmpty()) EmptyState("Chưa có album") else AlbumGrid(s.data, onAlbumClick)
    }
}
```
Quy tắc: **Screen không gọi Repository/DAO trực tiếp**; mọi thao tác đi qua ViewModel.

### Luồng dữ liệu
```
MediaStore ─► MediaStoreScanner ─► Room ◄── album_overrides
                                     │
                              Repository (Flow)
                                     │
                 ViewModel ◄─────────┴─────────► PlayerController ◄─► PlaybackService (Media3)
                     │                                                    │      │
                Compose Screen                            PlayEventLogger      PlaybackStateStore
                                                                │
                                                         play_events ─► StatsEngine ─► Wrapped ─► ShareHelper
```

---

## 6. Database schema

```sql
songs(
  id INTEGER PK, uri TEXT, path TEXT,
  title TEXT, artist TEXT, album TEXT, album_artist TEXT,
  album_id INTEGER, track INTEGER, disc INTEGER, year INTEGER,
  duration_ms INTEGER, date_added INTEGER, date_modified INTEGER,
  title_norm TEXT, artist_norm TEXT, album_norm TEXT     -- bỏ dấu, lowercase
)
albums(id PK, title, artist, year, art_uri, song_count, total_duration_ms)
artists(id PK, name, album_count, song_count)

album_overrides(
  album_id PK, title TEXT?, artist TEXT?, year INTEGER?,
  custom_art_path TEXT?, use_generated_art INTEGER DEFAULT 0,
  player_style TEXT DEFAULT 'default',                   -- 'default' | 'vinyl'
  updated_at INTEGER
)

playlists(id PK, name, created_at, updated_at)
playlist_songs(playlist_id, song_id, position, PK(playlist_id, position))

play_events(
  id PK AUTOINCREMENT, song_id, album_id, artist TEXT,
  started_at INTEGER, listened_ms INTEGER, completed INTEGER, skipped INTEGER,
  source TEXT                                            -- 'album'|'playlist'|'shuffle'|'search'|'queue'
)
INDEX(play_events.started_at), INDEX(play_events.song_id)

playback_state(
  id INTEGER PK CHECK(id = 1),
  queue_song_ids TEXT, current_index INTEGER, position_ms INTEGER,
  shuffle INTEGER, repeat_mode INTEGER, updated_at INTEGER
)
```
- **Ghi lượt nghe:** "nghe" khi `listened_ms ≥ min(30s, 50% duration)`; chuyển trước ngưỡng → `skipped = 1`; chỉ cộng thời gian đang phát thật.
- **Hiển thị = gốc ⊕ override:** `@DatabaseView albums_resolved` dùng `COALESCE`.
- Đổi schema → Quý duyệt và viết migration.

---

## 7. Timeline tổng quan

```
Ngày:   1   2   3   4   5   6   7   8   9  10  11  12  13  14 │ 15  16  17  18  19  20  21  22  23  24  25  26 │ 27  28  29  30
Quý:   [Setup][---L1---]📦[--L2--]📦L3📦[queue·gapless·fade·logger·restore]│[-L4-]📦[--L5--]📦[------crossfade------][share]│[fix·release]
Khánh: [Design system·wireframe·onboarding][Songs·Artists·Detail][NowPlaying·Queue][Playlist]│[Edit·Crop·GenArt·Vinyl][Slides 2–6·Share card]│[polish·demo]
                                                                          🚀 MVP v0.1 (ngày 14)          🎯 v0.3 feature complete (ngày 26)   🏁 v1.0 (ngày 30)
📦 = bàn giao lát
```

| Mốc | Ngày | Nội dung |
|---|---|---|
| 📦 L1 Thư viện | 4 | Khánh bắt đầu các tab & màn chi tiết |
| 📦 L2 Player | 7 | Khánh bắt đầu Now Playing, Queue |
| 📦 L3 Playlist | 8 | Khánh bắt đầu chi tiết playlist |
| 🚀 **MVP v0.1** | **14** | Ngồi chung 2h: checklist MVP, tag |
| 📦 L4 Cá nhân hóa | 15 | Khánh bắt đầu Edit album đầy đủ, crop, Vinyl |
| 📦 L5 Wrapped | 18 | Khánh bắt đầu slide 2–6 |
| ⏱️ Mốc crossfade | 22 | Chưa ổn → dùng phương án fallback |
| 🎯 **v0.3 feature complete** | **26** | Ngồi chung 2h: checklist, feature freeze |
| 🏁 **v1.0** | **30** | Release, demo, báo cáo |

---

## 8. Chi tiết theo tuần

> **Phụ thuộc**: task của Khánh ghi rõ cần lát nào. **Tự kiểm**: cách người làm tự xác nhận trên máy mình trước khi mở PR.

### 📅 TUẦN 1 (Ngày 1–7) — Setup, L1, L2

#### Quý (~36h)
| # | Việc | Ngày | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|
| Q1.1 | Repo, branch `main`/`develop`, PR template, CI (build + lint + test) | 1 | 3h | PR có check xanh |
| Q1.2 | Project, Hilt, Navigation, cấu trúc package mục 5, `UiState`, `LoadingState/ErrorState/EmptyState` thô | 1–2 | 5h | App mở được, chuyển tab trống |
| Q1.3 | **L1** Room: `songs`, `albums`, `artists` + DAO | 2 | 3h | Database Inspector thấy bảng |
| Q1.4 | **L1** Quyền đọc nhạc + `MediaStoreScanner` (lọc < 30s, ringtone…; cột `*_norm`) | 2–3 | 6h | Log số bài đúng, liệt kê file bị lọc |
| Q1.5 | **L1** Ảnh bìa (nhúng → `cover.jpg` → null), cache | 3 | 3h | Bìa hiện trên tab Album |
| Q1.6 | **L1** `LibraryRepository`, `AlbumsViewModel`, **tab Album thô**, `SampleData.kt` | 3–4 | 4h | Tab Album hiện nhạc thật |
| Q1.7 | **📦 Bàn giao L1** — `docs/handoff/L1.md` + walkthrough 30' | 4 | 1h | Khánh chạy được trên máy Khánh |
| Q1.8 | **L2** `PlaybackService` (MediaSessionService, audio focus, rút tai nghe → pause) | 4–5 | 5h | Phát nền khi tắt màn hình, notification điều khiển được |
| Q1.9 | **L2** `PlayerController` (playSongs, playPause, next, prev, seek, `state` mỗi 250ms) + **mini player thô** | 5–6 | 4h | Bấm album → phát, mini player chạy tiến độ |
| Q1.10 | **L2** Màn **Debug** khung (state player, nút kill process) | 6–7 | 1h | |
| Q1.11 | **📦 Bàn giao L2** — `L2.md` + walkthrough | 7 | 1h | |

#### Khánh (~30h)
| # | Việc | Ngày | Phụ thuộc | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|---|
| K1.1 | Bộ nhạc test CC0 `test-music/`: 3 album đủ tag, 1 thiếu bìa, 1 file < 30s, 1 album live | 1 | — | 2h | Chép vào máy cả 2 người |
| K1.2 | Wireframe 6 màn: Thư viện, Album, Now Playing, Queue, Playlist, Wrapped | 1–2 | — | 4h | Quý góp ý |
| K1.3 | **Design system**: màu, typography, shape, dark/light, dynamic color | 2–3 | Q1.2 | 5h | Màn "Gallery" trong Debug |
| K1.4 | Component: `SongRow`, `AlbumCard`, `ArtistRow`, làm đẹp `EmptyState`/`ErrorState` | 3–4 | — | 4h | `@Preview` light/dark/tên dài |
| K1.5 | Onboarding xin quyền (gọi hàm quyền của Quý) | 4 | L1 | 2h | Từ chối quyền → màn hướng dẫn |
| K1.6 | **Tab Bài hát** (nhân bản khuôn tab Album) | 4–5 | L1 | 3h | Hiện đủ bài từ `test-music/` |
| K1.7 | **Tab Nghệ sĩ** + màn Artist detail | 5 | L1 | 3h | |
| K1.8 | Làm đẹp tab Album (grid, sort menu) + **Album detail** (header bìa, màu Palette, danh sách bài, nút Phát/Shuffle) | 5–7 | L1, L2 | 7h | Album thiếu bìa vẫn đẹp; bấm Phát → nhạc chạy |

---

### 📅 TUẦN 2 (Ngày 8–14) — L3, engine MVP → 🚀 MVP

#### Quý (~32h)
| # | Việc | Ngày | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|
| Q2.1 | **L3** Entity/DAO/`PlaylistRepository` + màn danh sách playlist thô + dialog tạo | 8 | 4h | Tạo playlist, thấy trong danh sách |
| Q2.2 | **📦 Bàn giao L3** | 8 | 1h | |
| Q2.3 | Queue: playNext, addToQueue, remove, shuffle (giữ bài hiện tại), repeat; expose `queue: StateFlow` | 9 | 4h | Màn Debug in queue đúng sau mỗi thao tác |
| Q2.4 | **Gapless** (`setMediaItems` cả danh sách) | 9 | 1h | Album live không có khoảng lặng |
| Q2.5 | **Fade play/pause** 300ms qua `ForwardingPlayer` | 10 | 5h | Bấm 10 lần nhanh (app, notification, tai nghe) không kẹt âm lượng |
| Q2.6 | `PlayEventLogger` (mục 9.4) + bảng events trong màn Debug | 11 | 6h | Chạy 7 kịch bản mục 12, đối chiếu bảng |
| Q2.7 | **Khôi phục trạng thái** + `onPlaybackResumption` (mục 9.2) | 12–13 | 7h | Kill process → mở lại đúng bài/giây, không tự phát; tắt app → play trên tai nghe BT → phát tiếp |
| Q2.8 | Unit test logger & restore (Robolectric) | 13 | 3h | `./gradlew test` xanh |
| Q2.9 | Review PR của Khánh, pair khi Khánh bị chặn | 8–14 | 1h | |

#### Khánh (~30h)
| # | Việc | Ngày | Phụ thuộc | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|---|
| K2.1 | **Now Playing**: bìa lớn, nền gradient theo bìa, seek bar, điều khiển, shuffle/repeat; vuốt mini player lên để mở | 8–10 | L2 | 9h | Seek đúng, nút shuffle/repeat đổi trạng thái |
| K2.2 | Làm đẹp mini player | 10 | L2 | 1h | |
| K2.3 | **Màn Queue**: danh sách, highlight bài đang phát, vuốt để xóa | 10–11 | Q2.3 | 4h | Thao tác queue trên nhạc thật |
| K2.4 | **Bottom sheet menu bài hát**: Phát tiếp / Thêm hàng đợi / Thêm vào playlist / Đi tới album | 11 | Q2.3, L3 | 3h | Dùng được ở mọi danh sách bài |
| K2.5 | **Playlist mở rộng**: màn chi tiết, đổi tên, xóa playlist, thêm/xóa bài (thêm hàm vào `PlaylistRepository` theo khuôn L3) | 11–13 | L3 | 7h | Đóng mở app vẫn giữ playlist |
| K2.6 | **Tìm kiếm**: thêm `search()` vào `LibraryRepository` (LIKE trên `*_norm`) + màn Tìm kiếm debounce 250ms, nhóm kết quả | 13 | L1 | 4h | Gõ "son tung" ra "Sơn Tùng" |
| K2.7 | Màn **Cài đặt**: theme sáng/tối/hệ thống, quét lại thư viện | 14 | — | 2h | |

#### 🚀 MVP v0.1 (Ngày 14 — ngồi chung 2h)
- [ ] Tắt màn hình 30 phút vẫn phát; điều khiển từ notification, lock screen, tai nghe BT
- [ ] Rút tai nghe → pause; cuộc gọi → pause, gọi xong → phát tiếp
- [ ] Gapless với album live; pause/play có fade
- [ ] Kill app → mở lại đúng chỗ, không tự phát; play trên tai nghe → phát tiếp
- [ ] Thư viện, tìm kiếm, playlist, queue hoạt động trên nhạc thật
- [ ] `play_events` đúng
- [ ] **Từ hôm nay cả 2 dùng app làm player chính** → tích lũy dữ liệu thật cho Wrapped
- [ ] Tag **`v0.1.0-mvp`**

---

### 📅 TUẦN 3 (Ngày 15–21) — L4, L5, crossfade

#### Quý (~36h)
| # | Việc | Ngày | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|
| Q3.1 | **Lấy Facebook App ID** (Meta for Developers) — làm sớm vì có thể phải chờ | 15 | 1h | ID trong `local.properties` → `BuildConfig` |
| Q3.2 | **L4** `album_overrides` + view `albums_resolved`; `PersonalizationRepository` (setMetadata, setPlayerStyle, useGeneratedArt, reset) | 15 | 4h | Đổi tên album trong form thô → thư viện đổi theo |
| Q3.3 | **L4** `ArtworkStore.saveCropped(uri, CropRect)`: decode an toàn, xoay theo EXIF, cắt, resize 1024px, lưu JPEG; artwork notification dùng bìa override | 15–16 | 4h | Màn Debug: chọn ảnh dọc/ngang → ảnh vuông đúng vùng |
| Q3.4 | **L4** Form sửa **tên album** thô | 16 | 1h | |
| Q3.5 | **📦 Bàn giao L4** | 16 | 1h | |
| Q3.6 | **L5** `StatsEngine.monthlyWrapped` (tổng phút, top 5 bài & nghệ sĩ theo `listened_ms`, album của tháng, giờ vàng, histogram 24h) + unit test | 16–17 | 7h | Màn Debug dump JSON, khớp query tay |
| Q3.7 | **L5** Seeder 3 tháng `play_events` (chỉ debug) — nút trong màn Debug | 17 | 2h | |
| Q3.8 | **L5** **Story player** (full màn hình, thanh tiến độ, chạm trái/phải, giữ để dừng, tự chuyển 6s) + **slide 1 "Tổng phút"** | 17–18 | 6h | Lướt slide trên dữ liệu seeder |
| Q3.9 | **📦 Bàn giao L5** | 18 | 1h | |
| Q3.10 | **Crossfade**: `CrossfadePlayer` bọc 2 ExoPlayer, preload, equal-power, đồ thị volume trong Debug | 18–21 | 9h | Nút "nhảy 10s trước cuối bài" → nghe chồng bài, đồ thị cos/sin |

#### Khánh (~30h)
| # | Việc | Ngày | Phụ thuộc | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|---|
| K3.1 | **Màn crop** (pinch zoom + kéo, khung vuông, xuất `CropRect`) — làm trước với ảnh bất kỳ | 15–16 | — | 6h | Log `CropRect` đúng khi zoom/kéo tới mép |
| K3.2 | **Bìa tự sinh** (gradient 2 màu từ hash tên + chữ cái đầu + pattern nhẹ, Canvas) | 16 | — | 4h | Cùng tên → cùng bìa; tên khác → bìa khác |
| K3.3 | **Tùy chỉnh album đầy đủ**: tên, nghệ sĩ, năm, đổi bìa (Photo Picker → crop → `saveCropped`), bật bìa tự sinh, nút Khôi phục gốc | 16–18 | L4 | 6h | Sửa → mọi màn & notification đổi theo |
| K3.4 | Override hiển thị khắp nơi: thư viện, detail, Now Playing, mini player | 18 | L4 | 2h | |
| K3.5 | **Vinyl player**: đĩa than Canvas (rãnh, bìa ở tâm), quay 33⅓ rpm khi phát, dừng mượt, cần đĩa hạ/nâng; chọn style theo album | 18–20 | L2, L4 | 9h | Play/pause → quay/dừng mượt, không giật khi đổi bài |
| K3.6 | Slide 2 **Top 5 bài** (nhân bản khuôn slide 1) | 21 | L5 | 3h | Đúng với seeder |

---

### 📅 TUẦN 4a (Ngày 22–26) — Hoàn thiện crossfade, share, Wrapped → 🎯 Feature complete

#### Quý (~22h)
| # | Việc | Ngày | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|
| Q4.1 | Crossfade: hoán đổi active/standby, notification không giật | 22 | 5h | Notification đổi bài lúc bắt đầu fade |
| Q4.2 | **⏱️ Mốc ngày 22**: chưa ổn → fallback `mediaSession.player = newPlayer` | 22 | — | |
| Q4.3 | Crossfade: 10 edge case (mục 12) + luật cùng album / bấm next 1s; expose `setCrossfade(config)` | 22–23 | 7h | Chạy hết bảng trong Debug, ghi `docs/crossfade.md` |
| Q4.4 | **`ShareCardRenderer`**: render Composable offscreen → Bitmap 1080×1920 / 1080×1350 → `cacheDir/share/` | 24 | 5h | Debug: render thẻ mẫu, ảnh nét đúng kích thước |
| Q4.5 | **`ShareHelper`** (mục 9.3): IG Story, FB Story, Khác…; `FileProvider`, `<queries>`, `availableTargets()` | 25 | 4h | Có IG/FB → mở đúng màn story; gỡ IG → chỉ còn "Khác…" |
| Q4.6 | Review & pair | 22–26 | 1h | |

#### Khánh (~24h)
| # | Việc | Ngày | Phụ thuộc | Ước tính | Tự kiểm trên máy |
|---|---|---|---|---|---|
| K4.1 | Slide 3–6: **Top nghệ sĩ**, **Album của tháng** (nền Palette), **Giờ vàng** (biểu đồ 24 cột), **Tổng kết** | 22–24 | L5 | 10h | Đúng với seeder; dữ liệu ít/trống không vỡ |
| K4.2 | **Thiết kế thẻ share** dạng Composable: Story 9:16 (chừa 250px trên/dưới) & Feed 4:5; collage bìa top 5, số liệu, logo | 22–24 | — | 5h | `@Preview` đúng tỉ lệ |
| K4.3 | Nút **IG Story / FB Story / Khác…** ở slide tổng kết (theo `availableTargets()`) | 25 | Q4.4, Q4.5 | 2h | Share thật lên IG story của mình |
| K4.4 | **Tab Wrapped**: chọn tháng, trạng thái trống ("Nghe thêm vài ngày nhé") | 25 | L5 | 3h | |
| K4.5 | **Cài đặt crossfade**: slider 0–12s, toggle "Không crossfade trong cùng album" | 26 | Q4.3 | 2h | Đổi slider → nghe khác biệt |
| K4.6 | Bug fix các màn của mình | 26 | — | 2h | |

#### 🎯 v0.3 Feature complete (Ngày 26 — ngồi chung 2h)
- [ ] Crossfade qua UI thật; chạy lại 5 edge case quan trọng
- [ ] Đổi bìa + crop, sửa tên, Vinyl hoạt động trên nhạc thật
- [ ] Wrapped trên dữ liệu thật + seeder; số liệu khớp màn Debug
- [ ] Share IG / FB Story mở đúng; máy không cài → chỉ "Khác…"
- [ ] **Feature freeze**
- [ ] Tag `v0.3.0`

---

### 📅 TUẦN 4b (Ngày 27–30) — Hoàn thiện → 🏁 v1.0

| # | Việc | Ai | Ước tính |
|---|---|---|---|
| 5.1 | Bug bash chéo: Quý test màn của Khánh, Khánh test luồng phát/crossfade/khôi phục | Cả 2 | 3h/người |
| 5.2 | Sửa lỗi ưu tiên cao | Cả 2 | 5h/người |
| 5.3 | Hiệu năng (cuộn 3.000 bài), LeakCanary, kiểm tra pin | Quý | 3h |
| 5.4 | Polish: animation chuyển màn, icon app, splash | Khánh | 3h |
| 5.5 | *(Nếu dư)* O1 Sleep timer · O2 Playlist có bìa | Quý · Khánh | 4h · 3h |
| 5.6 | Video demo 3 phút + ảnh chụp màn hình | Khánh | 2h |
| 5.7 | Báo cáo (phân công ở mục 14) | Cả 2 | 5h/người |
| 5.8 | Slide + tập dượt demo 2 lần | Cả 2 | 2h/người |
| 5.9 | Tag **`v1.0.0`**, APK release ký | Quý | 1h |

---

## 9. Ghi chú kỹ thuật quan trọng

### 9.1 Crossfade 2 player
```kotlin
suspend fun crossfade(out: ExoPlayer, incoming: ExoPlayer, durationMs: Long) {
    incoming.volume = 0f
    incoming.play()
    val steps = (durationMs / 50).toInt().coerceAtLeast(1)
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        out.volume = cos(t * PI / 2).toFloat()       // equal-power
        incoming.volume = sin(t * PI / 2).toFloat()
        delay(50)
    }
    out.pause()
}
```
- Job fade trong `serviceScope`, luôn hủy job cũ trước job mới.
- Fade tối đa `min(cài đặt, durationBài / 3)`.
- `MediaSession` gắn vào `CrossfadePlayer` (ForwardingPlayer) → hoán đổi chỉ đổi player bên trong.

### 9.2 Khôi phục trạng thái phát
1. **Lưu** khi: pause, đổi bài, `onTaskRemoved`, app vào nền, mỗi 10s khi đang phát.
2. **Khôi phục** khi service khởi động: lọc bài không còn tồn tại → `setMediaItems(items, index, position)` → `prepare()`, không `play()`.
3. **Bluetooth khi app đã tắt:** `MediaSession.Callback.onPlaybackResumption()` trả `MediaItemsWithStartPosition`.
4. Bài hiện tại bị xóa → nhảy bài kế tiếp còn tồn tại, position = 0.

### 9.3 Share Instagram / Facebook Story
```kotlin
val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
    putExtra("source_application", BuildConfig.FB_APP_ID)
    setDataAndType(storyUri, "image/png")
    putExtra("top_background_color", "#%06X".format(0xFFFFFF and topColor))
    putExtra("bottom_background_color", "#%06X".format(0xFFFFFF and bottomColor))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.grantUriPermission("com.instagram.android", storyUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
if (context.packageManager.resolveActivity(intent, 0) != null) activity.startActivity(intent)
// Facebook Story: action "com.facebook.stories.ADD_TO_STORY",
// extra "com.facebook.platform.extra.APPLICATION_ID" = FB_APP_ID, package "com.facebook.katana"
```
- `FileProvider` + `file_paths.xml` cho `cacheDir/share/`; `<queries>` cho `com.instagram.android`, `com.facebook.katana`.
- Không có IG/FB → chỉ "Khác…" (`ACTION_SEND`, `image/png`, `createChooser`).

### 9.4 Ghi lượt nghe
- `onIsPlayingChanged` → bắt đầu/dừng đếm; `onMediaItemTransition` → chốt bản ghi bài trước.
- Crossfade: chốt bản ghi bài cũ tại thời điểm **bắt đầu fade**.

---

## 10. Quy trình làm việc

### Git
- `main` (release) ← `develop` ← `feature/<quy|khanh>-<id>-<tên>` (vd `feature/quy-Q2.7-restore`, `feature/khanh-K3.5-vinyl`)
- Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`); PR < 400 dòng.
- **Mỗi task = 1 PR, merge ngay khi xong** — không gom theo tuần.
- Review: Quý review mọi PR của Khánh trong 12h; Khánh review PR của Quý (đọc để hiểu engine, trả lời được hội đồng).
- Lát của Quý merge trước rồi mới bàn giao; Khánh luôn `git pull` `develop` trước khi bắt đầu task mới.

### Nhịp làm việc
| Khi nào | Việc |
|---|---|
| Hằng ngày (10 phút, chat) | Hôm qua / hôm nay / bị chặn gì |
| Mỗi lần bàn giao lát (30 phút) | Walkthrough code + chạy thử trên máy Khánh |
| 2 lần/tuần (1h) | Pair-programming ở task khó của Khánh (Now Playing, Vinyl, slide Wrapped) |
| Ngày 14, 26 (2h) | Ngồi chung chạy checklist, tag release |

### Công cụ
- GitHub Projects: mỗi task 1 issue theo ID (Q2.7, K3.5…), label `quy` / `khanh`, label `blocked-by-Lx` cho task chờ lát.
- `docs/`: `handoff/L1..L5.md`, `crossfade.md`, wireframe.

---

## 11. Definition of Done
- [ ] **Tự kiểm trên máy mình đạt** (cột "Tự kiểm")
- [ ] Đúng pattern: Screen → ViewModel → Repository; có Loading/Empty/Error
- [ ] Có `@Preview` (UI) hoặc unit test (logic engine, stats, logger)
- [ ] Dark & light mode
- [ ] CI xanh, người kia đã review, đã merge `develop`
- [ ] Với lát base: có `docs/handoff/Lx.md` + đã walkthrough

---

## 12. Kiểm thử

### Edge case Crossfade (Q4.3)
| # | Tình huống | Kỳ vọng |
|---|---|---|
| 1 | Bài tự hết, fade 6s | Chồng 6s, notification đổi bài lúc bắt đầu fade |
| 2 | Bấm next lúc đang fade | Bỏ fade hiện tại, fade 1s sang bài kế tiếp nữa |
| 3 | Bấm prev lúc đang fade | Dừng standby, về bài cũ, volume về 1 |
| 4 | Pause lúc đang fade | Cả 2 pause; resume fade tiếp đúng chỗ |
| 5 | Seek lúc đang fade | Hủy fade, dừng standby |
| 6 | Bài 8s, fade 6s | Fade rút còn ≤ 1/3 độ dài bài |
| 7 | Repeat one | Không crossfade với chính nó |
| 8 | Hết queue, repeat off | Dừng bình thường |
| 9 | 2 bài liên tiếp cùng album | Gapless, không fade |
| 10 | Cuộc gọi đến lúc đang fade | Cả 2 pause, khôi phục đúng |

### `PlayEventLogger` (Q2.6)
Nghe hết · skip ở 10s · pause 5 phút rồi nghe tiếp · seek tới cuối · repeat one 3 lần · app bị kill giữa bài · crossfade sang bài mới.

### Khôi phục trạng thái (Q2.7)
Kill khi đang phát · khi đang pause · xóa file bài hiện tại rồi mở app · khởi động lại máy · play trên tai nghe BT khi app đã tắt.

### Share (Q4.5 / K4.3)
Máy có IG + FB · chỉ IG · không có cả hai · thẻ nền tối / sáng.

### UI (Khánh)
Mỗi màn có `@Preview` với `SampleData`: bình thường / tên rất dài / không bìa / danh sách trống / dark mode.

### Thiết bị
Mỗi người 1 máy thật; trong nhóm có 1 máy Android 14+ và 1 máy Android 10–12 (ưu tiên Xiaomi/Samsung).

---

## 13. Rủi ro & phương án dự phòng

| Rủi ro | Khả năng | Ảnh hưởng | Phương án |
|---|---|---|---|
| Lát của Quý trễ → Khánh phải chờ | Trung bình | Cao | Khánh luôn có việc không phụ thuộc (mục 4.4); lát giao **mỏng trước**, bổ sung sau |
| Khánh làm lệch pattern | Trung bình | Trung bình | Walkthrough mỗi lát + review kỹ PR đầu tiên của mỗi lát |
| Khánh bị chặn lâu ở Hilt/Room/Coroutines | Trung bình | Trung bình | Quy tắc **bị chặn quá 1 giờ → nhắn Quý**; pair 2 lần/tuần |
| Crossfade + MediaSession khó | Cao | Cao | Mốc **ngày 22** → fallback `mediaSession.player = newPlayer` |
| Thiếu dữ liệu nghe thật cho Wrapped | Cao | Trung bình | Dùng app từ ngày 14; seeder Q3.7 |
| Meta xác minh App ID lâu | Trung bình | Thấp | Làm Q3.1 ngày 15; "Khác…" vẫn đăng được IG/FB |
| Render Compose offscreen lỗi trên vài máy | Trung bình | Trung bình | Fallback: chụp `GraphicsLayer` của thẻ hiển thị thật ở slide tổng kết |
| Máy Xiaomi/Samsung kill service nền | Trung bình | Cao | Foreground service chuẩn Media3; test từ ngày 5 |
| Quý quá tải | Trung bình | Cao | Cắt theo thứ tự mục 3; O1 bỏ trước |
| Scope creep | Cao | Cao | Ý tưởng mới ghi vào mục 15 |

---

## 14. Kịch bản demo & báo cáo

### Demo (~6 phút)
1. **Vấn đề** (30s): ảnh chụp các phàn nàn thật (mục 2).
2. **Thư viện sạch** (30s): nhạc chuông bị ẩn, album thiếu bìa có bìa tự sinh. *(Khánh)*
3. **Chuyển bài mượt** (1m): crossfade 8s, album live gapless, pause có fade; mở màn Debug cho xem đồ thị volume. *(Quý)*
4. **Khôi phục** (30s): kill app → mở lại đúng chỗ; play trên tai nghe → phát tiếp. *(Quý)*
5. **Cá nhân hóa** (1m30): đổi bìa + crop, sửa tên, bật Vinyl. *(Khánh)*
6. **Wrapped** (1m30): lướt story → bấm Instagram Story → story mở sẵn thẻ Wrapped. *(Khánh + Quý)*
7. **Kỹ thuật** (30s): kiến trúc, mô hình lát dọc. *(Quý)*

### Dàn ý báo cáo
1. Giới thiệu & khảo sát nhu cầu *(Khánh)*
2. Phân tích yêu cầu, phạm vi & lý do cắt giảm *(Khánh)*
3. Kiến trúc: MVVM, package, DB, luồng dữ liệu, pattern `UiState` *(Quý)*
4. Kỹ thuật nổi bật *(Quý)*: gapless & crossfade, khôi phục trạng thái, ghi lượt nghe & thống kê, render & chia sẻ IG/FB
5. Giao diện *(Khánh)*: design system, màu theo bìa, bìa tự sinh, crop, Vinyl, Wrapped story, thẻ share
6. Kiểm thử *(cả 2)*: bảng edge case, unit test, thiết bị thật
7. Quy trình nhóm *(cả 2)*: lát dọc + bàn giao, Git flow
8. Kết luận & hướng phát triển (mục 15)

---

## 15. Backlog sau v1.0 (đã cắt)

| Tính năng | Lý do cắt |
|---|---|
| Blacklist / chọn thư mục quét, tab Thư mục | Lọc tự động đủ cho bản 1 tháng |
| Kéo thả queue & playlist | Tốn công, không ghi điểm |
| Tìm kiếm FTS | `LIKE` + bỏ dấu đủ dùng |
| Gộp album, sửa metadata từng bài | Phạm vi lớn |
| Mixtape đầy đủ, Cassette player style | Vinyl đủ demo |
| Ghi chú / đánh giá album, nhật ký | Ngoài 3 trục chính |
| Wrapped tuần / năm, heatmap 7×24, streak | Tháng đủ demo |
| Smart album, sức khỏe thư viện, phát hiện trùng | Tốn công |
| M3U import/export, backup/restore | Ưu tiên bản sau |
| Lyrics `.lrc`, Karaoke L−R, đổi tone | Hướng "Karaoke offline" |
| Chia sẻ playlist qua QR, Widget, On This Day | Tính năng phụ |
| Lọc tạp âm, Android Auto | Ngoài phạm vi |
