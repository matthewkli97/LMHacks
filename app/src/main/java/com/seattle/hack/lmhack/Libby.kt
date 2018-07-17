package com.seattle.hack.lmhack

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener



object Libby {
    val dbRef =  FirebaseDatabase.getInstance().reference
    var inConvoFlow:Boolean

    init {
        inConvoFlow = false
    }

    fun processImageLabels(labels: List<FirebaseVisionLabel>) {
        val label = labels[0].label
        val confidence = labels[0].confidence

        sendLibbyMessage("HMMM... maybe it's a " + label)
    }

    fun sendProcessConvo(message:String) {

    }

    fun sendLibbyMessage(text:String) {

        val key = FirebaseDatabase.getInstance().getReference().child("chats").push().key

        var temp = mutableMapOf<Any, Any>();

        temp.put("libby", true)
        temp.put("image", false)
        temp.put("time", ServerValue.TIMESTAMP)
        temp.put("text", text)

        FirebaseDatabase.getInstance().getReference().child("chats").child(key!!).setValue(temp)
                .addOnSuccessListener(OnSuccessListener<Void> {
                    Log.i("MessageActivity", "Success")
                })
                .addOnFailureListener(OnFailureListener {
                    Log.i("MessageActivity", "Failure")
                })
    }

    fun processText(text:String) {
        dbRef.child("response").child(text).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                sendLibbyMessage(p0.value.toString())
                
            }

            override fun onCancelled(p0: DatabaseError) {
                sendLibbyMessage("Poop can't find an answer to that question.")
            }
        })

    }
}