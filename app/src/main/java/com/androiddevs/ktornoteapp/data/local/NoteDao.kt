package com.androiddevs.ktornoteapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.androiddevs.ktornoteapp.data.local.entities.LocallyDeletedNoteID
import com.androiddevs.ktornoteapp.data.local.entities.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    //if the note already exists with the same id..it updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteID")
    suspend fun deleteNoteById(noteID: String)

    @Query("DELETE FROM notes WHERE isSynced = 1")
    suspend fun deleteAllSyncedNotes()

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("SELECT * FROM notes WHERE id =:noteID")
    fun observeNoteById(noteID: String): LiveData<Note>

    @Query("SELECT * FROM notes WHERE id =:noteID")
    suspend fun getNoteById(noteID: String): Note?

    @Query("SELECT * FROM notes ORDER BY date DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getAllUnsychedNotes(): List<Note>

    @Query("SELECT * FROM locally_deleted_note_ids")
    suspend fun getAllLocallyDeletedNoteIDs() : List<LocallyDeletedNoteID>

    @Query("DELETE FROM locally_deleted_note_ids WHERE deletedNoteId = :deletedNoteID")
    suspend fun deleteLocallyDeletedNoteIDs(deletedNoteID:String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocallyDeletedNoteID(locallyDeletedNoteID: LocallyDeletedNoteID)
}