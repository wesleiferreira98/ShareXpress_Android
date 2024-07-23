package org.ferreiratechlab.sharexpress.data.model

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FileViewModel : ViewModel() {
    private val _fileProgress = MutableLiveData<Pair<String, Int>>()
    val fileProgress: LiveData<Pair<String, Int>> get() = _fileProgress

    private val _fileReceived = MutableLiveData<String>()
    val fileReceived: LiveData<String> get() = _fileReceived

    private val mainHandler = Handler(Looper.getMainLooper())

    fun updateFileProgress(fileName: String, progress: Int) {
        mainHandler.post {
            _fileProgress.value = Pair(fileName, progress) // Safe on main thread
        }
    }

    fun addFileReceived(fileName: String) {
        mainHandler.post {
            _fileReceived.value = fileName // Safe on main thread
        }
    }
}
