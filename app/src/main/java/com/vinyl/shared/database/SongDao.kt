package com.vinyl.shared.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Upsert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
internal data class Song(
    @PrimaryKey val contentUri: String,
    val albumId: String,
    val artistId: String,
    val title: String,
    val artist: String,
    val album: String,
    val titleNorm: String,
    val artistNorm: String,
    val albumNorm: String,
    val durationMs: Long,
    val trackNumber: Int,
    val dateModified: Long,
    val relativePath: String?,
    val volumeName: String,
)

internal data class AlbumSummary(
    val albumId: String,
    val title: String,
    val artist: String,
    val songCount: Int,
    val artworkUri: String,
    val artworkVersion: Long,
    val relativePath: String?,
    val volumeName: String,
)

@Dao
internal abstract class SongDao {
    @Query("""
        SELECT albumId, MIN(album) AS title, MIN(artist) AS artist, COUNT(*) AS songCount,
            MIN(contentUri) AS artworkUri, MAX(dateModified) AS artworkVersion,
            MIN(relativePath) AS relativePath, MIN(volumeName) AS volumeName
        FROM songs GROUP BY albumId ORDER BY MIN(albumNorm), albumId
    """)
    abstract fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber, titleNorm, contentUri")
    abstract fun observeAlbumSongs(albumId: String): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY titleNorm, contentUri")
    abstract suspend fun getSongs(): List<Song>

    @Query("SELECT * FROM songs ORDER BY titleNorm, contentUri")
    abstract fun observeSongs(): Flow<List<Song>>

    // Update retained rows in place so rescanning preserves playlist references.
    @Transaction
    open suspend fun replaceSongs(songs: List<Song>) {
        val retained = songs.map { it.contentUri }.toHashSet()
        require(retained.size == songs.size) { "Scan contains duplicate song URIs" }
        upsertSongs(songs)
        getSongs().filter { it.contentUri !in retained }.forEach { deleteSong(it.contentUri) }
    }

    @Query("DELETE FROM songs WHERE contentUri = :uri")
    protected abstract suspend fun deleteSong(uri: String)

    @Upsert
    protected abstract suspend fun upsertSongs(songs: List<Song>)
}
