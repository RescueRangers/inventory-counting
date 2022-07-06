package com.vestfiber.inventorycounting.DAL

import java.io.Serializable
import java.util.*

data class VfBatchNumber(val vfNumber: String, val vfCountingId: Int, var inC5: Boolean = false, var notOK: Boolean = false) : Serializable, ScannedObject(vfCountingId, vfNumber){

    override fun equals(other: Any?): Boolean {
        if (other?.javaClass != javaClass) return false

        other as VfBatchNumber
        if (other.vfNumber == vfNumber)
            return true
        return false
    }

    override fun hashCode(): Int {
        return vfNumber.hashCode()
    }

}
