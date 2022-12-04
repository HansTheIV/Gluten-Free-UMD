package com.example.umd_gluten_free.data

data class DataOrException<T, E : Exception?>(
    var data: T? = null,
    var e: E? = null
)
