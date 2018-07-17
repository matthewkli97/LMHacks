package com.seattle.hack.lmhack

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*

class MainActivity : Activity() {

    private var mChats: ArrayList<Message>? = null
    private var reference: DatabaseReference? = null
    lateinit var mLinearLayoutManager: LinearLayoutManager
    private var chatListener:ChildEventListener? = null
    lateinit var mMessageRecyclerView: RecyclerView
    private var message = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mChats = arrayListOf()
        val myAdapter = MessageAdapter(mChats!!, this)

        mLinearLayoutManager = LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView = findViewById(R.id.reyclerview_message_list) as RecyclerView
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager)
        mMessageRecyclerView.adapter = myAdapter


        reference = FirebaseDatabase.getInstance().reference.child("chats")
        chatListener = reference!!.addChildEventListener(object : ChildEventListener {
            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}
            override fun onChildChanged(p0: DataSnapshot?, p1: String?) {}
            override fun onChildRemoved(p0: DataSnapshot?) {}
            override fun onCancelled(p0: DatabaseError?) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val model = dataSnapshot.getValue(Message::class.java)
                mChats!!.add(model!!)
                myAdapter.notifyDataSetChanged()
                mMessageRecyclerView.smoothScrollToPosition(mChats!!.size - 1)
            }
        })


        val buttonSubmit = findViewById(R.id.button_chatbox_send) as Button
        val et_message = findViewById(R.id.edittext_chatbox) as EditText

        et_message.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                message = s.toString()

                buttonSubmit.isEnabled = message.length > 0
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        buttonSubmit.isEnabled = false
        buttonSubmit.setOnClickListener { view ->
            var temp = mutableMapOf<Any, Any>();

            temp.put("libby", false)
            temp.put("image", false)
            temp.put("time", ServerValue.TIMESTAMP)
            temp.put("text", message)

            val key = FirebaseDatabase.getInstance().getReference().child("chats").push().key
            FirebaseDatabase.getInstance().getReference().child("chats").child(key).setValue(temp)
                    .addOnSuccessListener(OnSuccessListener<Void> {
                        Log.i("MessageActivity", "Success")
                    })
                    .addOnFailureListener(OnFailureListener {
                        Log.i("MessageActivity", "Failure")
                    })


            mMessageRecyclerView.postDelayed(Runnable { mMessageRecyclerView.scrollToPosition(mChats!!.size - 1) }, 100)

            et_message.setText("")
            message = ""
        }
    }
}
