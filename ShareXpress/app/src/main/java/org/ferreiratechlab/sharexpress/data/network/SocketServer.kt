package org.ferreiratechlab.sharexpress.data.network

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import org.ferreiratechlab.sharexpress.data.model.ServerCallback
import org.ferreiratechlab.sharexpress.ui.FileTransferListener
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class SocketServer: Thread() {

    private lateinit var serverSocket: ServerSocket
    private var isRunning = false
    private lateinit var context: Context
    private var fileTransferListener: FileTransferListener? = null

    private var onServerStartedCallback: (() -> Unit)? = null

    private var onFileProgressCallback: ((String, Int) -> Unit)? = null

    private var serverCallback: ServerCallback? = null


    fun startServer(port: Int, context: Context, listener: FileTransferListener) {
        this.context = context
        this.fileTransferListener = listener
        serverSocket = ServerSocket(port)
        isRunning = true
        onServerStartedCallback?.invoke()
    }


    override fun run() {
        val sharexpressDir = createDirectory()
        try {
            while (isRunning) {
                try {
                    val clientSocket: Socket = serverSocket.accept()
                    handleClient(clientSocket, sharexpressDir)
                } catch (e: SocketException) {
                    if (!isRunning) {
                        Log.d("Server", "Servidor parado, exceção esperada: ${e.message}")
                    } else {
                        Log.e("Server", "Erro ao aceitar conexão: ${e.message}", e)
                    }
                }
            }
        } finally {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                Log.e("Server", "Erro ao fechar ServerSocket: ${e.message}", e)
            }
        }
    }

    fun setOnServerStartedCallback(callback: () -> Unit) {
        this.onServerStartedCallback = callback
    }


    private fun isSpaceAvailable(): Boolean {
        val stat = StatFs(context.filesDir.path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val megabytesAvailable = bytesAvailable / (1024 * 1024)
        Log.d("Server", "Espaço disponível: $megabytesAvailable MB")
        return megabytesAvailable > 10
    }

    private fun createDirectory(): File? {
        if (!isSpaceAvailable()) {
            Log.e("Server", "No space left on device")
            return null
        }
        Log.d("Server", "Criando diretório")

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val sharexpressDir = File(documentsDir, "sharexpress")

        if (!sharexpressDir.exists()) {
            try {
                if (sharexpressDir.mkdirs()) {
                    Log.d("Server", "Diretório criado: ${sharexpressDir.absolutePath}")
                } else {
                    Log.e("Server", "Falha ao criar diretório: ${sharexpressDir.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e("Server", "Erro ao criar diretório: ${e.message}", e)
            }
        } else {
            Log.d("Server", "Diretório já existe")
        }
        return sharexpressDir
    }

    fun stopServer() {
        isRunning = false
        try{
            serverSocket.close()
        } catch (e: Exception) {
            Log.e("Server", "Erro ao fechar o servidor: ${e.message}", e)
        }
    }

    fun setOnFileProgressCallback(callback: (String, Int) -> Unit) {
        this.onFileProgressCallback = callback
    }

    private fun handleClient(clientSocket: Socket, saveDirectory: File?) {
        Log.d("Server", "Novo cliente conectado")

        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val fileInfo = reader.readLine() ?: return
            val (fileName, fileSizeString) = fileInfo.split(",")
            val fileSize = fileSizeString.toLong()

            Log.d("Server", "Recebendo arquivo: $fileName, tamanho: $fileSize bytes")

            val file = File(saveDirectory, fileName)
            FileOutputStream(file).use { fileOutputStream ->
                val buffer = ByteArray(4096)
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
                    fileTransferListener?.onFileProgress(fileName, progress)
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

    val serverIp: String
        get() = InetAddress.getLocalHost().hostAddress
}
