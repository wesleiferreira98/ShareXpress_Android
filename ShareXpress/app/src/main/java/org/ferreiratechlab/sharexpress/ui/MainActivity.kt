package org.ferreiratechlab.sharexpress.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import org.ferreiratechlab.sharexpress.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSend: ImageButton = findViewById(R.id.btnSend)
        val btnReceive: ImageButton = findViewById(R.id.btnReceive)



        btnSend.setOnClickListener {
            startActivity(Intent(this, SendActivity::class.java))
        }

        btnReceive.setOnClickListener {
            startActivity(Intent(this, ReceiveActivity::class.java))
        }
    }
}
