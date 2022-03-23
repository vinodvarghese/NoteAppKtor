package com.androiddevs.ktornoteapp.repositories

import android.app.Application
import android.content.Context
import android.provider.ContactsContract
import com.androiddevs.ktornoteapp.data.local.NoteDao
import com.androiddevs.ktornoteapp.data.local.entities.LocallyDeletedNoteID
import com.androiddevs.ktornoteapp.data.local.entities.Note
import com.androiddevs.ktornoteapp.data.remote.NoteApi
import com.androiddevs.ktornoteapp.data.remote.requests.AccountRequest
import com.androiddevs.ktornoteapp.data.remote.requests.AddOwnerRequest
import com.androiddevs.ktornoteapp.data.remote.requests.DeleteNoteRequest
import com.androiddevs.ktornoteapp.other.Resource
import com.androiddevs.ktornoteapp.other.checkForInternetConnection
import com.androiddevs.ktornoteapp.other.networkBoundResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashSet

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteApi: NoteApi,
    private val context: Application
) {

    suspend fun insertNote(note: Note) {
        val response = try {
            noteApi.addNote(note)
        } catch (e: Exception) {
            null
        }

        if (response != null && response.isSuccessful) {
            noteDao.insertNote(note.apply {
                isSynced = true
            })
        } else {
            noteDao.insertNote(note)
        }
    }

    suspend fun insertNotes(notes: List<Note>) {
        notes.forEach { insertNote(it) }
    }

    suspend fun deleteNote(noteID: String) {
        val response = try {
            noteApi.deleteNote(DeleteNoteRequest(noteID))
        } catch (e: Exception) {
            null
        }
        noteDao.deleteNoteById(noteID)
        if(response == null || response.isSuccessful){
            noteDao.insertLocallyDeletedNoteID(LocallyDeletedNoteID(noteID))
        }else {
            deleteLocallyDeletedNoteID(noteID)
        }
    }

    fun observeNoteByID(noteID:String) = noteDao.observeNoteById(noteID)

    suspend fun deleteLocallyDeletedNoteID(deletedNoteID: String) {
        noteDao.deleteLocallyDeletedNoteIDs(deletedNoteID)
    }

    suspend fun getNoteById(noteId: String) = noteDao.getNoteById(noteId)

    private var curNotesResponse: Response<List<Note>>? = null

    suspend fun syncNotes(){
        val locallyDeletedNoteIDs = noteDao.getAllLocallyDeletedNoteIDs()
        locallyDeletedNoteIDs.forEach { id->
            deleteNote(id.deletedNoteId)
        }
        val unSyncedNotes = noteDao.getAllUnsychedNotes()
        unSyncedNotes.forEach { note ->
            insertNote(note)
        }
        curNotesResponse = noteApi.getNotes()

        curNotesResponse?.body()?.let { notes ->
            noteDao.deleteAllNotes()
            insertNotes(notes.onEach { note -> note.isSynced = true })
        }
    }

    fun getAllNotes(): Flow<Resource<List<Note>>> {
        return networkBoundResource(
            query = {
                noteDao.getAllNotes()
            },
            fetch = {
                syncNotes()
                curNotesResponse
            },
            saveFetchResult = { response ->
                response?.body()?.let {
                    insertNotes(it.onEach { note -> note.isSynced = true })
                }
            },
            shouldFetch = {
                checkForInternetConnection(context)
            }
        )
    }

    suspend fun register(email: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val response = noteApi.register(AccountRequest(email, password))
            if (response.isSuccessful && response.body()!!.successful) {
                Resource.success(response.body()?.message)
            } else {
                Resource.error(response.body()?.message ?: response.message(), null)
            }
        } catch (e: Exception) {
            Resource.error("Couldn't connect to the servers. Check your internet connection", null)
        }
    }

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val response = noteApi.login(AccountRequest(email, password))
            if (response.isSuccessful && response.body()!!.successful) {
                Resource.success(response.body()?.message)
            } else {
                Resource.error(response.body()?.message ?: response.message(), null)
            }
        } catch (e: Exception) {
            Resource.error("Couldn't connect to the servers. Check your internet connection", null)
        }
    }

    suspend fun addOwnerToNote(owner: String, noteID: String) = withContext(Dispatchers.IO) {
        try {
            val response = noteApi.addOwnerToNote(AddOwnerRequest(owner, noteID))
            if (response.isSuccessful && response.body()!!.successful) {
                Resource.success(response.body()?.message)
            } else {
                Resource.error(response.body()?.message ?: response.message(), null)
            }
        } catch (e: Exception) {
            Resource.error("Couldn't connect to the servers. Check your internet connection", null)
        }
    }


}