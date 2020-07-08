package com.example.distributedwebscraping

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.absoluteValue

class Permission (val permission: String){
    val requestCode = permission.hashCode().absoluteValue

    fun check(activity: Activity) = permission
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (activity.checkSelfPermission(permission)){
                    PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        activity.requestPermissions(arrayOf(it), requestCode)
                        false
                    }
                }
            } else true
        }
}


abstract class PermissionHandler: AppCompatActivity() {
    private val actions = mutableMapOf<Int, MutableList<() -> Any>>()
    private val handlers = mutableMapOf<Int, MutableList<() -> Unit>>()

    fun <T: Any> onGranted(permission: Permission, action: () -> T) {
        if (actions[permission.requestCode] == null)
            actions[permission.requestCode] = mutableListOf()
        actions[permission.requestCode]?.add(action)
    }

    fun  onDenied(permission: Permission, action: () -> Unit){
        if (handlers[permission.requestCode] == null)
            handlers[permission.requestCode] = mutableListOf()
        handlers[permission.requestCode]?.add(action)
    }

    fun onPermissionGranted(requestCode: Int, permissions: Array<String>) =
        actions[requestCode]?.forEach { it() }


    fun onPermissionDenied(requestCode: Int, permissions: Array<String>) =
        handlers[requestCode]?.forEach{ it() }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            this.onPermissionGranted(requestCode, permissions)
        else
            this.onPermissionDenied(requestCode, permissions)
    }

    suspend fun <R> requiring(vararg permission: Permission, action: suspend () -> R): R?{
        permission.forEach {
            if (!it.check(this)) return null
        }
        // If we are here we must have all permissions
        return action()
    }


}