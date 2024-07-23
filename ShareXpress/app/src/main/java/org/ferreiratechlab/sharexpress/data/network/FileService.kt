package org.ferreiratechlab.sharexpress.data.network

import org.ferreiratechlab.sharexpress.data.model.FileItem
import java.io.*
import java.net.Socket

class FileService(private val socketClient: SocketClient) {

    fun sendFile(fileItem: FileItem) {
        val socket = socketClient.getSocket() ?: throw IllegalStateException("Socket not connected")
        val file = File(fileItem.path)
        val fileInputStream = FileInputStream(file)
        val outputStream = socket.getOutputStream()

        outputStream.write(file.readBytes())
        outputStream.flush()
        fileInputStream.close()
    }

    fun receiveFile(fileItem: FileItem, destinationPath: String) {
        val socket = socketClient.getSocket() ?: throw IllegalStateException("Socket not connected")
        val file = File(destinationPath, fileItem.name)
        val fileOutputStream = FileOutputStream(file)
        val inputStream = socket.getInputStream()

        file.writeBytes(inputStream.readBytes())
        fileOutputStream.close()
    }
}