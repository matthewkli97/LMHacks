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
    var inClaimFlow:Boolean
    var convoRef = dbRef

    init {
        inClaimFlow = false
    }

    fun processImageLabels(labels: List<FirebaseVisionLabel>) {
        val label = labels[0].label
        val confidence = labels[0].confidence

        convoRef = dbRef.child("label").child(label)

        inClaimFlow = true

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

        var tempRef = dbRef.child("response")

        if(inClaimFlow) {
            tempRef = convoRef
        }

        Log.i("convo", inClaimFlow.toString())

        tempRef.child(text).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {

                if(p0.value == null) {
                    sendLibbyMessage("Poop can't find an answer to that question.")
                } else {
                    sendLibbyMessage(p0.value.toString())
                    Log.i("adsf", p0.toString())
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                sendLibbyMessage("Poop can't find an answer to that question.")
            }
        })

    }
}