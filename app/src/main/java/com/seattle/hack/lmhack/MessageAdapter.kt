package com.seattle.hack.lmhack


import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.StrictMode
import android.provider.Settings
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat.getTimeInstance
import java.util.*


class MessageAdapter(private val myDataset: ArrayList<Message>, var context: Context) :
        RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)


    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2


    override fun getItemViewType(position: Int): Int {
        val message = myDataset.get(position)

        // replace "position % 2 == 0"  with: message.userId.equals(FirebaseAuth.getInstance().uid)
        return if (message.libby == true) {
            // If the current user is the sender of the message
            VIEW_TYPE_MESSAGE_RECEIVED
        } else {
            // If some other user sent the message
            VIEW_TYPE_MESSAGE_SENT
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MessageAdapter.ViewHolder {

        Log.i("Adapter", "" + viewType)
        // create a new view

        val textView: View
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            textView = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)


        } else {
            textView = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)

        }
        // set the view's size, margins, paddings and layout parameters

        return ViewHolder(textView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element


        //loadImageFromURL("https://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg", holder.view.findViewById(R.id.image_message_profile))


        // POP UP EVENT LISTENER BEGING HERE
        // NOTE: SCROLL UP TO THE TOP OF OnBindViewHolder FUNCTION TO SEE IMPORTANT NOTE
        holder.itemView.setOnClickListener {

            if (isOnline(context)) {
                Log.i("Look!", myDataset[position].text)
                val policy = StrictMode.ThreadPolicy.Builder()
                        .permitAll().build()

                StrictMode.setThreadPolicy(policy)

                val service = ToneAnalyzer("2017-09-21")
                service.setUsernameAndPassword("5f3becf6-32ee-43ac-bbbd-2ac42ef7668c", "6fXcNSP88OWu")
                val text = myDataset[position].text

                val toneOptions = ToneOptions.Builder().text(text).build()
                val tone = service.tone(toneOptions).execute()
                val toneArray = tone.sentencesTone
                //println(toneArray[0])
                val outputArray = arrayListOf<String>()
                var result: String = "Tone(s): "
                println(tone.sentencesTone)
                if (toneArray != null) {
                    for (i in toneArray.indices) {
                        //println(tone.sentencesTone[i].tones[0].toneName)
                        val tones = toneArray[i].tones
                        if (tones.size > 0) {
                            for (i in tones.indices) {
                                val toneName = tones[i].toneName
                                if (toneName !in outputArray) {

                                    if (outputArray.size < 1) {
                                        result += toneName
                                    } else {
                                        result += ", " + toneName
                                    }
                                    outputArray.add(toneName)
                                }
                            }
                        }
                    }
                }
                if (outputArray.size < 1) {
                    result = "Can't read tone"
                }
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show()
            } else {
                displayAlert()
            }

        }


        val time = holder.view.findViewById(R.id.text_message_time) as TextView
        time.text = getTimeDate(myDataset[position].time!!)

        val text = holder.view.findViewById(R.id.text_message_body) as TextView
        text.text = myDataset[position].text

        if(getItemViewType(position) == VIEW_TYPE_MESSAGE_RECEIVED) {
            val name = holder.view.findViewById(R.id.text_message_name) as TextView
            name.text = myDataset[position].userName
        }
    }

    fun getTimeDate(timeStamp: Long): String {
        try {
            val dateFormat = getTimeInstance()
            val netDate = Date(timeStamp)
            return dateFormat.format(netDate)
        } catch (e: Exception) {
            return "date"
        }
    }

    fun displayAlert() {
        val builder = AlertDialog.Builder(context)
        builder.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
            context.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
        })
        builder.setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
            dialog.cancel()
        })
        builder.setMessage("You are not connected to the Internet. Airplane mode may be on, your wifi could be off" +
                " or you may not have service. Would you like to go to settings now to try to fix this?")
                .setTitle("Connectivity Issues")

        val dialog = builder.create()
        dialog.show()
    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        //should check null because in airplane mode it will be null
        return netInfo != null && netInfo.isConnected
    }
}