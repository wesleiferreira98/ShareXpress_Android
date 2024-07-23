package org.ferreiratechlab.sharexpress.ui

interface FileTransferListener {
    fun onFileProgress(fileName: String, progress: Int)
    fun onFileReceived(fileName: String)
}
