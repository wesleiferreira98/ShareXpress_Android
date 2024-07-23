package org.ferreiratechlab.sharexpress.data.network

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import org.ferreiratechlab.sharexpress.ui.FileAdapter
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class FileClient(
    private val ip: String,
    private val port: Int,
    private val inputStream: InputStream,
    private val context: Context,
    private val fileName: String,
    private val adapter: FileAdapter
) : Thread() {

    override fun run() {
        try {
            val fileSize = inputStream.available().toLong()

            Socket(ip, port).use { socket ->
                val outputStream: OutputStream = socket.getOutputStream()
                var fileInfo = "$fileName,$fileSize\n".toByteArray(Charsets.UTF_8)

                // Tenta enviar a informação do arquivo com UTF-8
                try {
                    outputStream.write(fileInfo)
                    outputStream.flush()
                } catch (e: Exception) {
                    // Se falhar, tenta enviar como JSON
                    val json = JSONObject()
                    json.put("fileName", fileName)
                    json.put("fileSize", fileSize)
                    fileInfo = json.toString().toByteArray(Charsets.UTF_8)
                    outputStream.write(fileInfo)
                    outputStream.flush()
                }

                // Enviar arquivo
                inputStream.use { fileInputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var bytesSent = 0L

                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesSent += bytesRead
                        Log.d("FileClient", "Enviando Arquivo: $fileName $bytesSent de $fileSize")
                        val progress = (bytesSent * 100 / fileSize).toInt()
                        Log.d("FileClient", "Progresso do envio: $progress%")

                        // Atualizar a barra de progresso
                        (context as? Activity)?.runOnUiThread {
                            adapter.updateProgress(fileName, progress)
                        }
                        sleep(10)
                    }
                    Log.d("FileClient", "Arquivo enviado: $fileName")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //Toast.makeText(context, "Falha na conexão ou arquivo não encontrado", Toast.LENGTH_LONG).show()
        }
    }
}
