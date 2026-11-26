package com.vinyl.shared.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
internal data class Playlist(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "playlist_songs", foreignKeys = [
    ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Song::class, parentColumns = ["contentUri"], childColumns = ["songUri"], onDelete = ForeignKey.CASCADE),
], indices = [Index("playlistId"), Index("songUri")])
internal data class PlaylistItem(@PrimaryKey val id: String, val playlistId: String, val songUri: String, val position: Int)

internal data class PlaylistSong(@Embedded val item: PlaylistItem, @Embedded(prefix = "song_") val song: Song)

@Dao
internal abstract class PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE, id")
    abstract fun observePlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    abstract suspend fun findPlaylist(id: String): Playlist?

    @Query("""SELECT p.*, s.contentUri AS song_contentUri, s.albumId AS song_albumId,
        s.artistId AS song_artistId, s.title AS song_title, s.artist AS song_artist,
        s.album AS song_album, s.titleNorm AS song_titleNorm, s.artistNorm AS song_artistNorm,
        s.albumNorm AS song_albumNorm, s.durationMs AS song_durationMs, s.trackNumber AS song_trackNumber,
        s.dateModified AS song_dateModified, s.relativePath AS song_relativePath, s.volumeName AS song_volumeName
        FROM playlist_songs p JOIN songs s ON s.contentUri = p.songUri
        WHERE playlistId = :playlistId ORDER BY position, p.id""")
    abstract fun observeSongs(playlistId: String): Flow<List<PlaylistSong>>

    @Insert protected abstract suspend fun insertPlaylist(playlist: Playlist)
    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    protected abstract suspend fun updateName(id: String, name: String)
    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: String)
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position, id")
    abstract suspend fun getItems(playlistId: String): List<PlaylistItem>
    @Insert protected abstract suspend fun insertItem(item: PlaylistItem)
    @Query("DELETE FROM playlist_songs WHERE id = :id")
    protected abstract suspend fun deleteItem(id: String)
    @Query("UPDATE playlist_songs SET position = :position WHERE id = :id")
    protected abstract suspend fun updatePosition(id: String, position: Int)

    open suspend fun createPlaylist(id: String, name: String) {
        require(name.isNotBlank()) { "Tên playlist không được trống" }
        insertPlaylist(Playlist(id, name.trim()))
    }

    @Transaction
    open suspend fun renamePlaylist(id: String, name: String) {
        require(name.isNotBlank()) { "Tên playlist không được trống" }
        checkNotNull(findPlaylist(id)) { "Playlist đã bị xóa" }
        updateName(id, name.trim())
    }

    @Transaction
    open suspend fun addSong(playlistId: String, itemId: String, songUri: String) {
        checkNotNull(findPlaylist(playlistId)) { "Playlist đã bị xóa" }
        val items = getItems(playlistId)
        insertItem(PlaylistItem(itemId, playlistId, songUri, (items.maxOfOrNull { it.position } ?: -1) + 1))
    }

    @Transaction
    open suspend fun removeItem(playlistId: String, itemId: String) {
        require(getItems(playlistId).any { it.id == itemId }) { "Bài hát không còn trong playlist" }
        deleteItem(itemId)
        getItems(playlistId).forEachIndexed { index, item -> updatePosition(item.id, index) }
    }

    @Transaction
    open suspend fun moveItem(playlistId: String, itemId: String, offset: Int) {
        val items = getItems(playlistId).toMutableList()
        val from = items.indexOfFirst { it.id == itemId }
        require(from >= 0 && from + offset in items.indices) { "Vị trí không hợp lệ" }
        items.add(from + offset, items.removeAt(from))
        items.forEachIndexed { index, item -> updatePosition(item.id, index) }
    }
}
