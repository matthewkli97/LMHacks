package com.seattle.hack.lmhack

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException


class MainActivity : Activity() {

    val PERMISSION_REQUEST_CODE = 1001
    val PICK_IMAGE_REQUEST = 900;

    private var mChats: ArrayList<Message>? = null
    private var reference: DatabaseReference? = null
    lateinit var mLinearLayoutManager: LinearLayoutManager
    private var chatListener:ChildEventListener? = null
    lateinit var mMessageRecyclerView: RecyclerView
    private var message = ""
    private var mStorageRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mChats = arrayListOf()
        val myAdapter = MessageAdapter(mChats!!, this)

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mLinearLayoutManager = LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView = findViewById(R.id.reyclerview_message_list) as RecyclerView
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager)
        mMessageRecyclerView.adapter = myAdapter


        reference = FirebaseDatabase.getInstance().reference.child("chats")
        chatListener = reference!!.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(p0: DataSnapshot) {}

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
            FirebaseDatabase.getInstance().getReference().child("chats").child(key!!).setValue(temp)
                    .addOnSuccessListener(OnSuccessListener<Void> {

                        Libby.processText(message)

                        mMessageRecyclerView.postDelayed(Runnable { mMessageRecyclerView.scrollToPosition(mChats!!.size - 1) }, 100)

                        et_message.setText("")
                        message = ""
                    })
                    .addOnFailureListener(OnFailureListener {
                        Toast.makeText(this, "Message Failed", Toast.LENGTH_SHORT).show()
                    })
        }

        val buttonImage = findViewById(R.id.button_chatbox_image) as ImageButton

        buttonImage.setOnClickListener {
            when {
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) -> {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
                    } else{
                        chooseFile()
                    }
                }

                else -> chooseFile()
            }
        }

        val scrollDown = findViewById(R.id.text_scroll) as TextView
        scrollDown.visibility = View.INVISIBLE

        scrollDown.setOnClickListener { view ->
            scrollDown.visibility = View.INVISIBLE
            mMessageRecyclerView.postDelayed(Runnable { mMessageRecyclerView.scrollToPosition(mChats!!.size -1) }, 100)
        }

        mMessageRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                //Log.i("track", "" + mLinearLayoutManager.findLastCompletelyVisibleItemPosition())
                if(mLinearLayoutManager.findLastCompletelyVisibleItemPosition() <= mChats!!.size - 5) {
                    scrollDown.visibility = View.VISIBLE
                } else {
                    scrollDown.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun chooseFile() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            PERMISSION_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Oops! Permission Denied!!", Toast.LENGTH_SHORT).show()
                else
                    chooseFile()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        when (requestCode){
            PICK_IMAGE_REQUEST -> {
                processFile(data!!.getData())
            }
        }
    }

    private fun labelImage(filePath:Uri) {
        var image:FirebaseVisionImage
        try {
            image = FirebaseVisionImage.fromFilePath(this, filePath);

            val detector = FirebaseVision.getInstance().getVisionLabelDetector();

            var result = detector.detectInImage(image)
                    .addOnSuccessListener {labels ->

                        labels.forEach({
                            var text = it.getLabel();
                            var entityId = it.getEntityId();
                            var confidence = it.getConfidence();

                            Log.i("imageLabel", text)
                            Log.i("imageLabel", entityId)
                            Log.i("imageLabel", confidence.toString())
                        })

                        if(Libby.inInitFlow) {
                            Libby.determineDialogFlow(labels)
                        } else {
                            Libby.processImageLabels(labels)
                        }
                    }
                    .addOnFailureListener{}
        } catch (e:IOException) {
            e.printStackTrace();
        }
    }

    private fun processFile(filePath:Uri) {
        val progress = ProgressDialog(this).apply {
            setTitle("Uploading Picture....")
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }

        val data = FirebaseStorage.getInstance()
        var value = 0.0
        val key = FirebaseDatabase.getInstance().getReference().child("chats").push().key

        var storage = data.getReference().child("pictures").child(key!!).putFile(filePath)
                .addOnProgressListener { taskSnapshot ->
                    value = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    Log.v("value","value=="+value)
                    progress.setMessage("Uploaded.. " + value.toInt() + "%")
                }
                .addOnSuccessListener { taskSnapshot -> progress.dismiss()
                    val urlRef = data.getReference().child("pictures").child(key!!).downloadUrl.addOnSuccessListener {it ->
                        var temp = mutableMapOf<Any, Any>();

                        temp.put("libby", false)
                        temp.put("image", true)
                        temp.put("time", ServerValue.TIMESTAMP)
                        temp.put("text", it.toString())

                        FirebaseDatabase.getInstance().getReference().child("chats").child(key!!).setValue(temp)
                                .addOnSuccessListener(OnSuccessListener<Void> {
                                    Log.i("MessageActivity", "Success")
                                })
                                .addOnFailureListener(OnFailureListener {
                                    Log.i("MessageActivity", "Failure")
                                })


                        mMessageRecyclerView.postDelayed(Runnable { mMessageRecyclerView.scrollToPosition(mChats!!.size - 1) }, 100)

                        labelImage(filePath) // Daisy Chaining required due to async nature of firebase :(
                    }
                }
                .addOnFailureListener{
                    exception -> exception.printStackTrace()

                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                }
    }
}


/*
    fun matchSentence() {
    double acceptanceRate = .4;
    List<String> matches = new ArrayList<String<(); // Contains the sentences that matched more than acceptanceRate %
    

    }


 */