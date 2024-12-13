package com.example.mybraintumordetection

class BrainTumor {
    var nombre:String = ""
    var key:String = ""
    var urlBrainTumor:String = ""
    var existeBrainTumor:String = ""

    constructor(nombre: String, key: String,urlBrainTumor:String,existeBrainTumor:String) {
        this.nombre = nombre
        this.key = key
        this.urlBrainTumor = urlBrainTumor
        this.existeBrainTumor = existeBrainTumor
    }
}