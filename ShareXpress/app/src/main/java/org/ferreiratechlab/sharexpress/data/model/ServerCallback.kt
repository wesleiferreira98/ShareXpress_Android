package org.ferreiratechlab.sharexpress.data.model

import java.net.Socket

interface ServerCallback {
    fun onServerStarted()
    fun onClientConnected(clientSocket: Socket)
    fun onServerError(error: Exception)
}
