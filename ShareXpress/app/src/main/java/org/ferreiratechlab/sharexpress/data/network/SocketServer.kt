package org.ferreiratechlab.sharexpress.data.network

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.*
import org.ferreiratechlab.sharexpress.ui.FileTransferListener
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class SocketServer : Thread() {

    private lateinit var serverSocket: ServerSocket
    private var isRunning = false
    private lateinit var context: Context
    private var fileTransferListener: FileTransferListener? = null

    private var onServerStartedCallback: (() -> Unit)? = null

    private var onFileProgressCallback: ((String, Int) -> Unit)? = null

    private val serverResponder = ServerResponder(12345)


    fun startServer(port: Int, context: Context, listener: FileTransferListener) {
        this.context = context
        this.fileTransferListener = listener
        serverSocket = ServerSocket(port)
        isRunning = true
        onServerStartedCallback?.invoke()
        // Iniciar a escuta do broadcast para descoberta do servidor
        serverResponder.startListening()
    }

    override fun run() {
        val sharexpressDir = createDirectory()
        val serverScope = CoroutineScope(Dispatchers.IO)
        try {
            while (isRunning) {
                try {
                    val clientSocket: Socket = serverSocket.accept()
                    serverScope.launch {
                        ClientHandler(clientSocket, sharexpressDir, fileTransferListener).run()
                    }
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
                serverScope.cancel()  // Cancelar todas as coroutines quando o servidor parar
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
        try {
            serverSocket.close()
        } catch (e: Exception) {
            Log.e("Server", "Erro ao fechar o servidor: ${e.message}", e)
        }
    }

    fun setOnFileProgressCallback(callback: (String, Int) -> Unit) {
        this.onFileProgressCallback = callback
    }
}


