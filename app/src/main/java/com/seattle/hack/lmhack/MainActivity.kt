package com.seattle.hack.lmhack

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    private var mChats: ArrayList<Message>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
