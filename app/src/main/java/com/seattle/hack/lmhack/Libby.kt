package com.seattle.hack.lmhack

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.storage.FirebaseStorage


object Libby {
    val dbRef =  FirebaseDatabase.getInstance().reference
    var inClaimFlow:Boolean
    var convoRef = dbRef
    var claimFlow: List<String> = mutableListOf()

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

    fun runDialog() {
        
    }

    fun processText(text:String) {

        var tempRef = dbRef.child("response")

        val prepText = text.trim().toLowerCase().replace(".", "")

        if(inClaimFlow) {
            runDialog()
        }

        tempRef.child(prepText).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {

                if(p0.value == null) {
                    sendLibbyMessage("Hmmm can't seem to find an answer for that, forwarding the request to an agent")
                } else if(p0.value == "claimInit"){

                    inClaimFlow = true

                    tempRef = dbRef.child("claim")

                    claimFlow = prepClaimFlowDialog(tempRef)

                    sendLibbyMessage("Great! Would you please upload an image of your item?") // hardcode initial response
                } else {
                    sendLibbyMessage(p0.value.toString())
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                sendLibbyMessage("Poop can't find an answer to that question.")
            }
        })
    }

    fun prepClaimFlowDialog (ref : DatabaseReference) : List<String> {
        var dialogs = mutableListOf<String>()

        val referenceQuery: Query = ref.orderByChild("time")

        referenceQuery.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dialogs.add(dataSnapshot.value.toString())
                Log.i("hit1", dialogs.get(dialogs.size - 1))
            }
        })

        return dialogs
    }
}