package co.aospa.sense.camera.callables

import java.lang.Exception

class CallableReturn {
    @JvmField
    var exception: Exception?
    @JvmField
    var value: Any?

    constructor(obj: Any?) {
        value = obj
        exception = null
    }

    constructor(e: Exception?) {
        exception = e
        value = null
    }
}