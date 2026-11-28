package com.mbwbill.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mbwbill.app.data.AppDatabase
import com.mbwbill.app.data.Customer
import com.mbwbill.app.data.CustomerRepository
import com.mbwbill.app.util.StorageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CustomerRepository(AppDatabase.get(app).customerDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val customers: StateFlow<List<Customer>> = _searchQuery
        .debounce(200)
        .flatMapLatest { repo.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _searchQuery.value = q }

    suspend fun getById(id: Long) = repo.getById(id)

    fun save(c: Customer, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                if (c.id == 0L) repo.insert(c) else repo.update(c)
                backupData() // Data save hote hi folder me backup karo
                onDone()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { 
            repo.deleteById(id) 
            backupData() // Delete hote hi folder me update karo
        }
    }

    // Folder me JSON backup save karta hai
    fun backupData() {
        viewModelScope.launch {
            val allCustomers = repo.getAll().first()
            StorageManager.saveBackupJson(getApplication(), allCustomers)
        }
    }

    // Folder select karte hi purana data Room DB me restore karta hai
    fun importDataFromFolder(onComplete: () -> Unit) {
        viewModelScope.launch {
            val backup = StorageManager.loadBackupJson(getApplication())
            if (backup != null) {
                for (c in backup) {
                    // Agar customer pehle se hai to replace karega, nahi to naya add karega
                    repo.insert(c)
                }
            }
            onComplete()
        }
    }
}
