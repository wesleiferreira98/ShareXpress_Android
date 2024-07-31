package org.ferreiratechlab.sharexpress.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import org.ferreiratechlab.sharexpress.R
import org.ferreiratechlab.sharexpress.data.network.FileClient
import java.io.InputStream
import java.math.BigDecimal
import java.net.Socket
import java.util.regex.Pattern

class SendActivity : AppCompatActivity() , OnItemLongClickListener{

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var etIpAddress: EditText
    private lateinit var etPort: EditText
    private lateinit var btnTestConnection: Button
    private lateinit var rvFiles: RecyclerView
    private lateinit var btnSelectFiles: Button
    private lateinit var btnSendFiles: Button
    private var isSendingFiles = false

    private val files = mutableListOf<Uri>()
    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        etIpAddress = navigationView.findViewById(R.id.etIpAddress)
        etPort = navigationView.findViewById(R.id.etPort)
        btnTestConnection = navigationView.findViewById(R.id.btnTestConnection)
        rvFiles = findViewById(R.id.rvFiles)
        btnSelectFiles = findViewById(R.id.btnSelectFiles)
        btnSendFiles = findViewById(R.id.btnSendFiles)

        setupRecyclerView()

        btnSelectFiles.setOnClickListener { openFilePicker() }
        btnSendFiles.setOnClickListener { sendFiles() }
        btnTestConnection.setOnClickListener {
            testConnection()
            hideKeyboard() }

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(navigationView) }
    }

    private fun setupRecyclerView() {
        val fileNames: MutableList<String> = files.map { uri ->
            getFileNameFromUri(uri) ?: "unknown"
        }.toMutableList()

        fileAdapter = FileAdapter(fileNames, this) // Passa o listener para o adapter
        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = fileAdapter
    }


    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex("_display_name")
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    override fun onItemLongClick(fileName: String) {
        if (isSendingFiles) { // Verifica se o envio está em progresso
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Remover Arquivo")
            .setMessage("Você deseja remover este arquivo da lista?")
            .setPositiveButton("Sim") { _, _ ->
                files.removeAll { uri -> getFileNameFromUri(uri) == fileName }
                fileAdapter.removeItem(fileName)
                fileAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("Não", null)
            .show()
    }




    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, REQUEST_CODE_PICK_FILES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILES && resultCode == RESULT_OK) {
            data?.clipData?.let { clipData ->
                files.clear() // Limpa a lista antes de adicionar novos arquivos
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    files.add(uri)
                    // Adicione o URI original ao RecyclerViewfiles.add(uri)
                }
            } ?: data?.data?.let { uri ->
                files.clear() // Limpa a lista antes de adicionar novos arquivos
                files.add(uri)

            }
            setupRecyclerView() // Atualiza o RecyclerView com os novos arquivos
        }
    }


    private fun testConnection() {
        val ip = etIpAddress.text.toString()
        val port = etPort.text.toString().toIntOrNull()

        if (ip.isEmpty() || port == null || !isValidIP(ip)) {
            showAlertDialog("Erro", "Por favor, insira um IP válido e uma porta válida.")
            return
        }

        Thread {
            try {
                Socket(ip, port).use {
                    runOnUiThread {
                        showAlertDialog("Sucesso", "Conexão bem-sucedida!")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showAlertDialog("Falha na Conexão", "Falha na conexão: ${e.message}")
                }
            }
        }.start()
    }
    private fun getInputStreamFromUri(uri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun sendFiles() {
        val ip = etIpAddress.text.toString()
        val port = etPort.text.toString().toIntOrNull()

        if (ip.isEmpty() || port == null || !isValidIP(ip)) {
            showAlertDialog("Erro", "Por favor, insira um IP válido e uma porta válida.")
            return
        }

        if (files.isEmpty()) {
            showAlertDialog("Erro", "Nenhum arquivo selecionado.")
            return
        }

        for (uri in files) {
            val fileName = getFileNameFromUri(uri) ?: "unknown"
            val fileSize = getFileSize(uri)
            val inputStream = getInputStreamFromUri(uri)


            if (inputStream == null) {
                showAlertDialog("Erro", "Não foi possível acessar o arquivo: $fileName")
                continue
            }

            val clientThread = FileClient(ip, port, inputStream, this, fileName,fileSize,fileAdapter)
            clientThread.start()
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getFileSize(uri: Uri): BigDecimal {
        var size: BigDecimal = BigDecimal.ZERO
        val cursor = this.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = BigDecimal(it.getLong(sizeIndex))
                }
            }
        }
        return size
    }

    private fun isValidIP(ip: String): Boolean {
        val pattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$"
        )
        return pattern.matcher(ip).matches()
    }

    companion object {
        private const val REQUEST_CODE_PICK_FILES = 1
    }
}
