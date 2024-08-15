package org.ferreiratechlab.sharexpress.data.network

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ServerDiscoveryManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun discoverServers(onServerFound: (String, Int) -> Unit) {
        scope.launch {
            val socket = DatagramSocket()
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            try {
                val message = "DISCOVER_SERVER"
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                packet.data = message.toByteArray()
                packet.address = broadcastAddress
                packet.port = 12345 // Porta usada para broadcast

                socket.send(packet)

                socket.soTimeout = 5000 // Timeout de 5 segundos
                while (true) {
                    try {
                        socket.receive(packet)
                        val serverMessage = String(packet.data, 0, packet.length).trim()
                        val serverAddress = packet.address.hostAddress

                        if (serverMessage.startsWith("SERVER_HERE")) {
                            val serverPort = serverMessage.split(":").getOrElse(1) { "12345" }.toInt()
                            Log.d("ServerDiscovery", "Servidor encontrado: $serverAddress:$serverPort")
                            withContext(Dispatchers.Main) {
                                onServerFound(serverAddress, serverPort)
                                Log.d("ServerDiscovery", "Servidor adicionado à lista: $serverAddress:$serverPort")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ServerDiscovery", "Erro ao receber resposta: ${e.message}")
                        break
                    }
                }
            } finally {
                socket.close()
            }
        }
    }

    fun stopDiscovery() {
        scope.cancel()
    }
}
