package org.ferreiratechlab.sharexpress.data.repository

import org.ferreiratechlab.sharexpress.data.model.FileItem

class FileRepository {

    private val fileList = mutableListOf<FileItem>()

    fun addFile(fileItem: FileItem) {
        fileList.add(fileItem)
    }

    fun removeFile(fileItem: FileItem) {
        fileList.remove(fileItem)
    }

    fun getAllFiles(): List<FileItem> {
        return fileList
    }

    fun getFileById(id: String): FileItem? {
        return fileList.find { it.id == id }
    }
}