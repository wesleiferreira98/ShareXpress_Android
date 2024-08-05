package org.ferreiratechlab.sharexpress.data.network
import android.util.Log
import kotlinx.coroutines.*
import org.ferreiratechlab.sharexpress.ui.FileTransferListener
import java.io.*
import java.net.Socket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

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
                val (fileName, fileSize, content) = try {
                    clientSocket.getOutputStream().write("OK".encodeToByteArray())
                    clientSocket.getOutputStream().flush()
                    val jsonObject = JSONObject(fileInfo)
                    Triple(
                        jsonObject.getString("fileName"),
                        jsonObject.getLong("fileSize"),
                        jsonObject.getString("content")
                    )

                } catch (e: Exception) {
                    Log.e("Server", "Erro ao processar cabeçalho ou conteúdo: ${e.message}", e)
                    // Se falhar, considera como CSV
                    val parts = fileInfo.split(",")
                    if (parts.size < 2) return@launch // Se a estrutura estiver incorreta
                    Triple(parts[0], parts[1].toLong(), "") // Não há conteúdo para CSV
                }

                Log.d("Server", "Recebendo arquivo: $fileName, tamanho: $fileSize bytes")

                if(fileName == "clipboard_content"){
                    // Receber e processar o conteúdo da área de transferência
                    withContext(Dispatchers.Main) {
                        fileTransferListener?.onClipboardContentReceived(content)
                    }
                    Log.d("Server", "Conteúdo da área de transferência recebido e processado")
                }else{
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

                }


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

    private fun readContentFromSocket(fileSize: Long): String {
        val inputStream = clientSocket.getInputStream()
        val buffer = ByteArray(fileSize.toInt())
        var totalBytesRead = 0
        while (totalBytesRead < fileSize) {
            val bytesRead = inputStream.read(buffer, totalBytesRead, buffer.size - totalBytesRead)
            if (bytesRead == -1) break
            totalBytesRead += bytesRead
        }
        return String(buffer, 0, totalBytesRead, StandardCharsets.UTF_8)
    }
}