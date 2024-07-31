package org.ferreiratechlab.sharexpress.data.network
import android.util.Log
import kotlinx.coroutines.*
import org.ferreiratechlab.sharexpress.ui.FileTransferListener
import java.io.*
import java.net.Socket
class ClientHandler(
    private val clientSocket: Socket,
    private val saveDirectory: File?,
    private val fileTransferListener: FileTransferListener?
) {

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    fun run() {
        handlerScope.launch {
            Log.d("Server", "Novo cliente conectado")

            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val fileInfo = reader.readLine() ?: return@launch
                val (fileName, fileSizeString) = fileInfo.split(",")
                val fileSize = fileSizeString.toLong()

                Log.d("Server", "Recebendo arquivo: $fileName, tamanho: $fileSize bytes")

                val file = File(saveDirectory, fileName)
                FileOutputStream(file).use { fileOutputStream ->
                    val buffer = ByteArray(10 * 1024 * 1024 )
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    val inputStream = clientSocket.getInputStream()

                    // Confirmar recebimento do cabeçalho
                    clientSocket.getOutputStream().write("OK".toByteArray())
                    clientSocket.getOutputStream().flush() // Garantir que a mensagem seja enviada

                    while (totalBytesRead < fileSize) {
                        bytesRead = inputStream.read(buffer)
                        Log.d("Server", "Bytes lidos: $bytesRead")
                        if (bytesRead == -1) break
                        fileOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        Log.d("Server", "Recebido: $totalBytesRead de $fileSize bytes")

                        val progress = ((totalBytesRead.toFloat() / fileSize.toFloat()) * 100).toInt()
                        withContext(Dispatchers.Main) {
                            fileTransferListener?.onFileProgress(fileName, progress)
                        }
                    }
                    fileOutputStream.flush()
                }

                Log.d("Server", "Arquivo recebido e salvo: ${file.absolutePath}")

                // Enviar confirmação de recebimento ao cliente
                clientSocket.getOutputStream().write("OK".toByteArray())
                clientSocket.getOutputStream().flush() // Garantir que a mensagem seja enviada
            } catch (e: Exception) {
                Log.e("Server", "Erro ao receber arquivo: ${e.message}", e)
            } finally {
                try {
                    clientSocket.close()
                } catch (e: IOException) {
                    Log.e("Server", "Erro ao fechar socket do cliente: ${e.message}", e)
                }
            }
        }
    }
}