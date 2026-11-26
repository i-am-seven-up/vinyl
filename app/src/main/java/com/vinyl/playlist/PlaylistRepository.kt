package com.vinyl.playlist

import com.vinyl.shared.database.PlaylistDao
import com.vinyl.shared.database.SongDao
import java.util.UUID

internal class PlaylistRepository(private val dao: PlaylistDao, private val songs: SongDao) {
    fun observePlaylists() = dao.observePlaylists()
    fun observeSongs(id: String) = dao.observeSongs(id)
    fun observeLibrary() = songs.observeSongs()
    suspend fun createPlaylist(id: String, name: String) { dao.createPlaylist(id, name) }
    suspend fun renamePlaylist(id: String, name: String) { dao.renamePlaylist(id, name) }
    suspend fun deletePlaylist(id: String) { dao.deletePlaylist(id) }
    suspend fun addSong(id: String, uri: String) {
        try { dao.addSong(id, UUID.randomUUID().toString(), uri) }
        catch (error: android.database.sqlite.SQLiteConstraintException) {
            throw IllegalStateException("Bài hát hoặc playlist đã bị xóa; hãy quét lại thư viện", error)
        }
    }
    suspend fun removeItem(id: String, itemId: String) { dao.removeItem(id, itemId) }
    suspend fun moveItem(id: String, itemId: String, offset: Int) { dao.moveItem(id, itemId, offset) }
}
