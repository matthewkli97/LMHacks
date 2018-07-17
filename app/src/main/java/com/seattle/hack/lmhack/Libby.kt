package com.seattle.hack.lmhack

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.ml.vision.label.FirebaseVisionLabel

object Libby {
    val dbRef =  FirebaseDatabase.getInstance().reference
    var inClaimFlow:Boolean
    var inInitFlow:Boolean
    var convoRef = dbRef
    var claimFlow: List<String> = mutableListOf()
    var index = 0;

    init {
        inInitFlow = false
        inClaimFlow = false
    }

    fun processImageLabels(labels: List<FirebaseVisionLabel>) {
        if(inClaimFlow) {
            runDialog()
        } else {
            val label = labels[0].label
            convoRef = dbRef.child("label").child(label)
        }
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
        sendLibbyMessage(claimFlow.get(index));
        index++;

        if(index >= claimFlow.size) {
            inClaimFlow = false
        }
    }

    fun determineDialogFlow (labels: List<FirebaseVisionLabel>) {
        var tempRef = dbRef.child("claim").child(labels.get(0).label.toString())
        claimFlow = prepClaimFlowDialog(tempRef)
        inClaimFlow = true
        inInitFlow = false;
        runDialog()
    }

    fun processText(text:String) {

        var tempRef = dbRef.child("response")

        val prepText = text.trim().toLowerCase().replace(".", "")

        if(inClaimFlow) {
            runDialog()
        } else {
            tempRef.child(prepText).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {

                    if(p0.value == null) {
                        sendLibbyMessage("Hmmm can't seem to find an answer for that, forwarding the request to an agent")
                    } else if(p0.value.toString().contains("claimInit")){

                        inInitFlow = true;

                        tempRef = dbRef.child("claim").child(p0.value.toString())

                        sendLibbyMessage("Please upload an image to start a claim.")
                    } else {
                        sendLibbyMessage(p0.value.toString())
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    sendLibbyMessage("Poop can't find an answer to that question.")
                }
            })
        }
    }

    fun prepClaimFlowDialog (ref : DatabaseReference) : List<String> {
        var dialogs = mutableListOf<String>()

        val referenceQuery: Query = ref.orderByChild("time")
        Log.i("reference", ref.toString())

        referenceQuery.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dialogs.add(dataSnapshot.child("text").value.toString())
                Log.i("asdf", dataSnapshot.child("text").value.toString())
            }
        })

        return dialogs
    }
}