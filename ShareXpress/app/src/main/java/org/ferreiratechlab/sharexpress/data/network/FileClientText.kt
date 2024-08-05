package org.ferreiratechlab.sharexpress.data.network

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class FileClientText(
    private val ip: String,
    private val port: Int,
    private val context: Context
) {

    fun sendText(text: String) {
        try {
            Socket(ip, port).use { socket ->
                val outputStream: OutputStream = socket.getOutputStream()
                val writer = PrintWriter(outputStream, true)

                // Enviar informações sobre o "arquivo"
                val fileInfo = JSONObject().apply {
                    put("fileName", "clipboard_content")
                    put("fileSize", text.length)
                    put("content", text)
                }.toString()

                writer.println(fileInfo)
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Conteúdo da área de transferência enviado com sucesso.", Toast.LENGTH_LONG).show()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Falha ao enviar conteúdo da área de transferência: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
