package com.example.appsyncsample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_single).setOnClickListener {
            val intent = Intent(this, SingleActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_multi).setOnClickListener {
            val intent = Intent(this, MultiActivity::class.java)
            startActivity(intent)
        }
    }
}
