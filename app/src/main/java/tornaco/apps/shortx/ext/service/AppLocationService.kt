package tornaco.apps.shortx.ext.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.Keep
import tornaco.apps.shortx.core.location.ILocationProvider
import tornaco.apps.shortx.core.os.SynchronousResultReceiver
import tornaco.apps.shortx.core.rule.action.ByteArrayWrapper
import tornaco.apps.shortx.core.util.Logger

@Keep
class AppLocationService : Service() {
    private val logger = Logger("AppLocationService")

    private val stub = object : ILocationProvider.Stub() {
        override fun getCurrentLocationInfo(
            requestId: String,
            receiver: SynchronousResultReceiver,
            requestData: ByteArrayWrapper,
        ) {
            logger.d(
                "getCurrentLocationInfo called, requestId=$requestId, callingUid=${Binder.getCallingUid()}, requestBytes=${requestData.byteData.size}"
            )
            AppLocationServiceSupport.getCurrentLocationInfo(
                service = this@AppLocationService,
                requestId = requestId,
                receiver = receiver,
                requestData = requestData,
            )
        }

        override fun getCurrentLocationAddress(
            requestId: String,
            receiver: SynchronousResultReceiver,
            requestData: ByteArrayWrapper,
        ) {
            logger.d(
                "getCurrentLocationAddress called, requestId=$requestId, callingUid=${Binder.getCallingUid()}, requestBytes=${requestData.byteData.size}"
            )
            AppLocationServiceSupport.getCurrentLocationAddress(
                service = this@AppLocationService,
                requestId = requestId,
                receiver = receiver,
                requestData = requestData,
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate")
    }

    override fun onBind(intent: Intent): IBinder {
        logger.d(
            "onBind, action=${intent.action}, component=${intent.component}, package=${intent.`package`}, extras=${intent.extras?.keySet()}"
        )
        return stub
    }

    override fun onUnbind(intent: Intent): Boolean {
        logger.d(
            "onUnbind, action=${intent.action}, component=${intent.component}, package=${intent.`package`}"
        )
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        logger.d("onDestroy")
        super.onDestroy()
    }
}
