package org.ferreiratechlab.sharexpress.data.network

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ServerResponder(private val serverPort: Int) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startListening() {
        scope.launch {
            val socket = DatagramSocket(12345) // Porta para escutar o broadcast

            try {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (true) {
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)

                    if (message == "DISCOVER_SERVER") {
                        val responseMessage = "SERVER_HERE:$serverPort"
                        val responseData = responseMessage.toByteArray()
                        val responsePacket = DatagramPacket(
                            responseData,
                            responseData.size,
                            packet.address,
                            packet.port
                        )

                        socket.send(responsePacket)
                        Log.d("ServerResponder", "Resposta enviada para ${packet.address.hostAddress}:${packet.port}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerResponder", "Erro ao escutar broadcast: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }

    fun stopListening() {
        scope.cancel()
    }
}
