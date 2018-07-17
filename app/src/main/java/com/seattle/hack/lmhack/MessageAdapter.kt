package com.seattle.hack.lmhack

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import java.text.DateFormat.getTimeInstance
import java.util.*
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.centerCrop
import com.bumptech.glide.request.RequestOptions



class MessageAdapter(private val myDataset: ArrayList<Message>, var context: Context) :
        RecyclerView.Adapter<MessageAdapter.ViewHolder>() {


    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)


    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2
    private val VIEW_TYPE_MESSAGE_SENT_IMAGE = 3


    override fun getItemViewType(position: Int): Int {
        val message = myDataset.get(position)

        // replace "position % 2 == 0"  with: message.userId.equals(FirebaseAuth.getInstance().uid)
        return if (message.libby == true) {
            // If the current user is the sender of the message
            VIEW_TYPE_MESSAGE_RECEIVED
        } else if (message.image != true) {
            // If some other user sent the message
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_SENT_IMAGE
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MessageAdapter.ViewHolder {

        Log.i("Adapter", "" + viewType)
        // create a new view

        val view: View
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
        } else if(viewType == VIEW_TYPE_MESSAGE_SENT_IMAGE){
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent_image, parent, false)
        } else {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
        }
        // set the view's size, margins, paddings and layout parameters

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val viewType = getItemViewType(position)

        if(viewType == 1 || viewType == 2) {
            val text = holder.view.findViewById(R.id.text_message_body) as TextView
            text.text = myDataset[position].text

            Log.i("view", "hit here")
        } else {
            val imageHolder = holder.view.findViewById(R.id.text_message_image) as ImageView

            val options = RequestOptions()
            options.fitCenter()

            Glide.with(this.context)
                    .load(myDataset[position].text)
                    .apply(options)
                    .into(imageHolder);


            val maxSize = imageHolder.getWidth();

            Log.i("asdfas", "image")
        }

        val time = holder.view.findViewById(R.id.text_message_time) as TextView
        time.text = getTimeDate(myDataset[position].time!!)
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

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}