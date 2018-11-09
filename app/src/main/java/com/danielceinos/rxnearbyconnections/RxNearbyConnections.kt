package com.danielceinos.rxnearbyconnections

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


/**
 * Created by Daniel S on 06/11/2018.
 */
class RxNearbyConnections {

    data class Success(val msg: String = "Success")

    /**
     *  ADVERTISING
     */
    data class ConnectionInitiated(val endpointId: String, val connectionInfo: ConnectionInfo)

    data class ConnectionResult(val endpointId: String, val result: ConnectionResolution)
    data class ConnectionDisconnected(val endpointId: String)

    val onConnectionInitiated: PublishSubject<ConnectionInitiated> = PublishSubject.create()
    val onConnectionResult: PublishSubject<ConnectionResult> = PublishSubject.create()
    val onConnectionDisconnected: PublishSubject<ConnectionDisconnected> = PublishSubject.create()

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            onConnectionInitiated.onNext(ConnectionInitiated(endpointId, connectionInfo))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            onConnectionResult.onNext(ConnectionResult(endpointId, result))
        }

        override fun onDisconnected(endpointId: String) {
            onConnectionDisconnected.onNext(ConnectionDisconnected(endpointId))
        }
    }

    fun startAdvertising(
            context: Context,
            name: String,
            serviceId: String,
            strategy: Strategy) =
            Maybe.create<Success> { emitter ->
                val strategyBuilder = AdvertisingOptions.Builder()
                strategyBuilder.setStrategy(strategy)

                Nearby.getConnectionsClient(context)
                        .startAdvertising(
                                name,
                                serviceId,
                                mConnectionLifecycleCallback,
                                strategyBuilder.build()
                        )
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }

    fun stopAdvertising(context: Context) {
        Nearby.getConnectionsClient(context).stopAdvertising()
    }

    /**
     *  DISCOVERING
     */
    data class Endpoint(val endpointId: String, val discoveredEndpointInfo: DiscoveredEndpointInfo?)

    val onEndpointDiscovered: PublishSubject<Endpoint> = PublishSubject.create()
    val onEndpointLost: PublishSubject<Endpoint> = PublishSubject.create()

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            onEndpointDiscovered.onNext(Endpoint(endpointId, discoveredEndpointInfo))
        }

        override fun onEndpointLost(endpointId: String) {
            onEndpointLost.onNext(Endpoint(endpointId, null))
        }
    }

    fun startDiscovery(
            context: Context,
            serviceId: String,
            strategy: Strategy) =
            Maybe.create<Success> { emitter ->
                val strategyBuilder = DiscoveryOptions.Builder()
                strategyBuilder.setStrategy(strategy)

                Nearby.getConnectionsClient(context).startDiscovery(
                        serviceId,
                        mEndpointDiscoveryCallback,
                        strategyBuilder.build())
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }

    fun stopDiscovery(context: Context) {
        Nearby.getConnectionsClient(context).stopDiscovery()
    }

    /**
     *  REQUEST CONNECTION
     */

    fun requestConnection(
            context: Context,
            endpointId: String,
            username: String) =
            Maybe.create<Success> { emitter ->
                Nearby.getConnectionsClient(context).requestConnection(
                        username,
                        endpointId,
                        mConnectionLifecycleCallback)
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }

    /**
     *  ACCEPT CONNECTION
     */

    data class EndpointPayloadReceived(val endpointId: String, val payload: Payload)

    data class EndpointPayloadTransferUpdate(val endpointId: String, val payload: PayloadTransferUpdate)

    val onPayloadReceived: PublishSubject<EndpointPayloadReceived> = PublishSubject.create()
    val onPayloadTransferUpdate: PublishSubject<EndpointPayloadTransferUpdate> = PublishSubject.create()

    private val mPayloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            onPayloadReceived.onNext(EndpointPayloadReceived(endpointId, payload))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            onPayloadTransferUpdate.onNext(EndpointPayloadTransferUpdate(endpointId, update))
        }
    }

    fun acceptConnection(context: Context, endpointId: String) =
            Maybe.create<Success> { emitter ->
                Nearby.getConnectionsClient(context)
                        .acceptConnection(endpointId, mPayloadCallback)
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }

    fun rejectConnection(context: Context, endpointId: String) {
        Nearby.getConnectionsClient(context).rejectConnection(endpointId)
    }

    fun disconnect(context: Context, endpointId: String) {
        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId)
    }

    /**
     *  SEND PAYLOAD
     */

    fun sendPayload(context: Context, endpointId: String, payload: Payload) =
            Maybe.create<Success> { emitter ->
                Nearby.getConnectionsClient(context)
                        .sendPayload(endpointId, payload)
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }

    fun sendPayload(context: Context, endpointsId: List<String>, payload: Payload) =
            Maybe.create<Success> { emitter ->
                Nearby.getConnectionsClient(context)
                        .sendPayload(endpointsId, payload)
                        .addOnSuccessListener { emitter.onSuccess(Success()) }
                        .addOnFailureListener { emitter.onError(it) }
            }
}

fun Maybe<RxNearbyConnections.Success>.log(title: String = ""): Disposable =
        subscribe({
            Timber.i("$title ${it.msg}")
        }, {
            Timber.e("$title ${it.message}")
        })

