package org.ferreiratechlab.sharexpress.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.ferreiratechlab.sharexpress.R
import org.ferreiratechlab.sharexpress.data.network.SocketServer
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import android.util.Log
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.IOException
import kotlin.concurrent.thread
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import org.ferreiratechlab.sharexpress.data.model.FileViewModel
import java.util.concurrent.Executors

class ReceiveActivity : AppCompatActivity(), FileTransferListener  {
    private lateinit var btnStartServer: Button
    private lateinit var tvIpAddress: TextView
    private lateinit var tvPort: TextView

    private val socketServer = SocketServer()
    private var isServerRunning = false
    private var serverThread: Thread? = null
    private lateinit var filesAdapter: FilesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileViewModel: FileViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    // Criar um ExecutorService com um thread pool
    private val executorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        btnStartServer = findViewById(R.id.btnStartServer)
        tvIpAddress = findViewById(R.id.tvIpAddress)
        tvPort = findViewById(R.id.tvPort)
        recyclerView = findViewById(R.id.rvFiles)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)



        filesAdapter = FilesAdapter(mutableListOf())
        recyclerView.adapter = filesAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileViewModel = ViewModelProvider(this).get(FileViewModel::class.java)

        // Configura a Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(navigationView) }




        checkPermissions()
         // Inicializa a variável aqui

        btnStartServer.setOnClickListener {
            if (isServerRunning) {
                stopServer()
            } else {
                startServer(12345)
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 0)
        }
    }


    // Use esse método para atualizar o progresso do arquivo
    override fun onFileProgress(fileName: String, progress: Int) {
        Handler(Looper.getMainLooper()).post {
            val fileItem = FileItem(fileName, progress)
            filesAdapter.addFile(fileItem)
            filesAdapter.updateProgress(fileName, progress)
        }
    }


    // Use esse método para adicionar um arquivo recebido
    override fun onFileReceived(fileName: String) {
        runOnUiThread {
            Log.d("UI_THREAD", "Updating UI on thread: ${Thread.currentThread().name}")
            //filesAdapter.addFile(fileName)
            //filesAdapter.notifyDataSetChanged()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            val deniedPermissions = grantResults.indices
                .filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
                .map { permissions[it] }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permissões negadas: ${deniedPermissions.joinToString()}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun startServer(port: Int) {
        Log.d("Server", "Starting server")
        val ip = getLocalIpAddress()
        Log.d("Server", "IP: $ip")
        serverThread = thread {
            try {
                // Atualize a interface para mostrar que o servidor está iniciando
                Log.d("Server", "Before starting SocketServer on port: $port")
                socketServer.startServer(port, this,this)
                socketServer.start()
                Log.d("Server", "After starting SocketServer")
                runOnUiThread {
                    Toast.makeText(this, "Iniciando servidor em $ip:$port", Toast.LENGTH_LONG).show()
                    tvIpAddress.text = "Endereço IP: N/A"
                    tvPort.text = "Porta: N/A"
                    btnStartServer.text = "Iniciar Servidor"
                }

                    runOnUiThread {
                        Toast.makeText(this, "Servidor iniciado em $ip:$port", Toast.LENGTH_LONG).show()
                        isServerRunning = true
                        tvIpAddress.text = "Endereço IP: $ip"
                        tvPort.text = "Porta: $port"
                        btnStartServer.text = "Encerrar Servidor"
                        showServerNotification(ip, port)
                    }


            } catch (e: Exception) {
                Log.e("Server", "Error starting server: ${e.message}", e)
                runOnUiThread {
                        isServerRunning = false
                        tvIpAddress.text = "Endereço IP: N/A"
                        tvPort.text = "Porta: N/A"
                        btnStartServer.text = "Iniciar Servidor"
                }



            }
        }

        // Captura de exceções não tratadas na thread do servidor
        serverThread?.setUncaughtExceptionHandler { _, e ->
            Log.e("Server", "Uncaught exception in server thread: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Falha ao iniciar o servidor: ${e.message}", Toast.LENGTH_LONG).show()
                isServerRunning = false
                tvIpAddress.text = "Endereço IP: N/A"
                tvPort.text = "Porta: N/A"
                btnStartServer.text = "Iniciar Servidor"
            }
        }
    }






    private fun stopServer() {
        try {
            serverThread?.interrupt()
            socketServer.stopServer()
            finish()
        }catch (e: Exception){
            Log.e("Server", "Error stopping server: ${e.message}", e)
        }

        runOnUiThread {
            isServerRunning = false
            tvIpAddress.text = "Endereço IP: N/A"
            tvPort.text = "Porta: N/A"
            btnStartServer.text = "Iniciar Servidor"
            Toast.makeText(this, "Servidor encerrado", Toast.LENGTH_LONG).show()
            cancelServerNotification()
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
            "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun showServerNotification(ip: String, port: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "server_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Server Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Servidor rodando")
            .setContentText("Servidor rodando em $ip:$port")
            .setSmallIcon(R.drawable.ic_server)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun cancelServerNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }
}

