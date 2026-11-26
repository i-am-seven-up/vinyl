package com.vinyl.library

import com.vinyl.shared.database.SongDao
import com.vinyl.shared.database.Song
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LibraryRepository(
    private val getDeviceSongs: suspend () -> List<Song>,
    private val songDao: SongDao,
) {
    private val scanMutex = Mutex()

    fun observeAlbums() = songDao.observeAlbums()

    fun observeAlbumSongs(albumId: String) = songDao.observeAlbumSongs(albumId)

    fun observeSongs() = songDao.observeSongs()

    fun observeSearchSongs(query: String) = songDao.observeSearchSongs(createSearchPattern(query))

    suspend fun scanLibrary() {
        scanMutex.withLock {
            val songs = getDeviceSongs()
            // Failed/cancelled scans never reach the replacing transaction.
            songDao.replaceSongs(songs)
        }
    }
}

internal fun createSearchPattern(query: String): String {
    val normalized = normalizeSearchText(query.trim())
    return "%" + normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"
}
