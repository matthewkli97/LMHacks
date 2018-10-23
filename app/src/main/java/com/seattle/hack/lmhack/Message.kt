package com.seattle.hack.lmhack

import android.graphics.Bitmap

class Message {
    var image: Boolean? = null
    var text: String? = null
    var libby: Boolean? = null
    var time: Long? = null
    var imageMap: Bitmap? = null

    constructor() {}  // Needed for Firebase

    constructor(text: String, libby: Boolean, image: Boolean, time:Long) {
        this.image = image
        this.text = text
        this.libby = libby
        this.time = time

        if(image) {
            imageMap = getBitmapFromURL(text);
        }
    }
}