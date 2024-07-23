package org.ferreiratechlab.sharexpress.data.network

import java.net.Socket

class SocketClient {

    private lateinit var serverIp: String
    private var serverPort: Int = 0

    private var socket: Socket? = null

    fun setServerDetails(ip: String, port: Int) {
        this.serverIp = ip
        this.serverPort = port
    }

    fun connect() {
        socket = Socket(serverIp, serverPort)
    }

    fun getSocket(): Socket? {
        return socket
    }

    fun closeConnection() {
        socket?.close()
    }
}