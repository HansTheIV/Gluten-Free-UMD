package com.example.umd_gluten_free.data

import android.media.Rating
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Meal(
    var location: String ?= null,
    var name: String ?= null,
    var rating: Int
    )
