package org.ferreiratechlab.sharexpress.data.network

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import org.ferreiratechlab.sharexpress.ui.FileAdapter
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.net.Socket
import java.util.concurrent.TimeUnit

class FileClient(
    private val ip: String,
    private val port: Int,
    private val inputStream: InputStream,
    private val context: Context,
    private val fileName: String,
    private val adapter: FileAdapter
) : Thread() {

    @SuppressLint("DefaultLocale")
    override fun run() {
        try {
            val fileSize = BigDecimal(inputStream.available().toLong())

            Socket(ip, port).use { socket ->
                val outputStream: OutputStream = socket.getOutputStream()
                var fileInfo = "$fileName,$fileSize\n".encodeToByteArray()

                Log.d("FileClient", "Enviando cabeçalho do arquivo: $fileName")

                // Tenta enviar a informação do arquivo com UTF-8
                try {
                    outputStream.write(fileInfo)
                    outputStream.flush()
                } catch (e: Exception) {
                    // Se falhar, tenta enviar como JSON
                    val json = JSONObject()
                    json.put("fileName", fileName)
                    json.put("fileSize", fileSize)
                    fileInfo = json.toString().toByteArray()
                    outputStream.write(fileInfo)
                    outputStream.flush()
                }
                Log.d("FileClient", "Esperando confirmação do servidor para o cabeçalho do arquivo: $fileName")

                // Confirmar recebimento do cabeçalho
                val confirmation = socket.getInputStream().bufferedReader().readLine()
                if (confirmation != "OK") {
                    throw Exception("Falha na confirmação do cabeçalho do servidor")
                }

                Log.d("FileClient", "Confirmação recebida: $fileName")

                // Enviar arquivo
                inputStream.use { fileInputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var bytesSent = BigDecimal.ZERO
                    val startTime = System.currentTimeMillis()

                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesSent += BigDecimal(bytesRead)
                        val progress = (bytesSent.multiply(BigDecimal(100)).divide(fileSize, 2, BigDecimal.ROUND_HALF_UP)).toInt()
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val estimatedTotalTime = (elapsedTime.toDouble() / bytesSent.toDouble() * fileSize.toDouble()).toLong()
                        val remainingTime = estimatedTotalTime - elapsedTime
                        val estimatedTime = String.format("%02d:%02d:%02d",
                            TimeUnit.MILLISECONDS.toHours(remainingTime),
                            TimeUnit.MILLISECONDS.toMinutes(remainingTime) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(remainingTime) % TimeUnit.MINUTES.toSeconds(1)
                        )

                        Log.d("FileClient", "Enviando Arquivo: $fileName $bytesSent de $fileSize")
                        Log.d("FileClient", "Progresso do envio: $progress%, Tempo Estimado: $estimatedTime")

                        // Atualizar a barra de progresso e o tempo estimado
                        (context as? Activity)?.runOnUiThread {
                            adapter.updateProgress(fileName, progress, estimatedTime)
                        }
                    }
                    Log.d("FileClient", "Arquivo enviado: $fileName")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Falha na conexão ou arquivo não encontrado", Toast.LENGTH_LONG).show()
            }
        }
    }
}
