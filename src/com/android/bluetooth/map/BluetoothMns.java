/*
 * Copyright (c) 2010-2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *          notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *          notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution.
 *        * Neither the name of Code Aurora nor
 *          the names of its contributors may be used to endorse or promote
 *          products derived from this software without specific prior written
 *          permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.bluetooth.map;

<<<<<<< HEAD


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

=======
>>>>>>> a130f0e... Bluetooth MAP (Message Access Profile) Upstream Changes (1/3)
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;

import com.android.bluetooth.map.IBluetoothMasApp.MessageNotificationListener;
import com.android.bluetooth.map.IBluetoothMasApp.MnsRegister;
import com.android.bluetooth.map.MapUtils.MapUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.obex.ObexTransport;

import static com.android.bluetooth.map.BluetoothMasService.MAS_INS_INFO;
import static com.android.bluetooth.map.BluetoothMasService.MAX_INSTANCES;
import static com.android.bluetooth.map.IBluetoothMasApp.HANDLE_OFFSET;
import static com.android.bluetooth.map.IBluetoothMasApp.MSG;
import static com.android.bluetooth.map.IBluetoothMasApp.TELECOM;

/**
 * This class run an MNS session.
 */
public class BluetoothMns implements MessageNotificationListener {
    private static final String TAG = "BtMns";

    private static final boolean V = BluetoothMasService.VERBOSE;

    public static final int RFCOMM_ERROR = 10;

    public static final int RFCOMM_CONNECTED = 11;

    public static final int MNS_CONNECT = 13;

    public static final int MNS_DISCONNECT = 14;

    public static final int MNS_SEND_EVENT = 15;

    public static final int MNS_SEND_EVENT_DONE = 16;

    public static final int MNS_SEND_TIMEOUT = 17;

    public static final int MNS_BLUETOOTH_OFF = 18;

    public static final int MNS_SEND_TIMEOUT_DURATION = 30000; // 30 secs

    private static final short MNS_UUID16 = 0x1133;

    public static final String NEW_MESSAGE = "NewMessage";

    public static final String DELIVERY_SUCCESS = "DeliverySuccess";

    public static final String SENDING_SUCCESS = "SendingSuccess";

    public static final String DELIVERY_FAILURE = "DeliveryFailure";

    public static final String SENDING_FAILURE = "SendingFailure";

    public static final String MEMORY_FULL = "MemoryFull";

    public static final String MEMORY_AVAILABLE = "MemoryAvailable";

    public static final String MESSAGE_DELETED = "MessageDeleted";

    public static final String MESSAGE_SHIFT = "MessageShift";

    private Context mContext;

    private BluetoothAdapter mAdapter;

    private BluetoothMnsObexSession mSession;

    private EventHandler mSessionHandler;

    private List<MnsClient> mMnsClients = new ArrayList<MnsClient>();
    public static final ParcelUuid BluetoothUuid_ObexMns = ParcelUuid
            .fromString("00001133-0000-1000-8000-00805F9B34FB");

    private HashSet<Integer> mWaitingMasId = new HashSet<Integer>();
    private final Queue<Pair<Integer, String>> mEventQueue = new ConcurrentLinkedQueue<Pair<Integer, String>>();
    private boolean mSendingEvent = false;

    public BluetoothMns(Context context) {
        /* check Bluetooth enable status */
        /*
         * normally it's impossible to reach here if BT is disabled. Just check
         * for safety
         */

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;

        for (int i = 0; i < MAX_INSTANCES; i ++) {
            try {
                // TODO: must be updated when Class<? extends MnsClient>'s constructor is changed
                Constructor<? extends MnsClient> constructor;
                constructor = MAS_INS_INFO[i].mMnsClientClass.getConstructor(Context.class,
                        Integer.class);
                mMnsClients.add(constructor.newInstance(mContext, i));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "The " + MAS_INS_INFO[i].mMnsClientClass.getName()
                        + "'s constructor arguments mismatch", e);
            } catch (InstantiationException e) {
                Log.e(TAG, "The " + MAS_INS_INFO[i].mMnsClientClass.getName()
                        + " cannot be instantiated", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "The " + MAS_INS_INFO[i].mMnsClientClass.getName()
                        + " cannot be instantiated", e);
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Exception during " + MAS_INS_INFO[i].mMnsClientClass.getName()
                        + "'s constructor invocation", e);
            } catch (SecurityException e) {
                Log.e(TAG, MAS_INS_INFO[i].mMnsClientClass.getName()
                        + "'s constructor is not accessible", e);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, MAS_INS_INFO[i].mMnsClientClass.getName()
                        + " has no matched constructor", e);
            }
        }

        if (!mAdapter.isEnabled()) {
            Log.e(TAG, "Can't send event when Bluetooth is disabled ");
            return;
        }

        mSessionHandler = new EventHandler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        mContext.registerReceiver(mStorageStatusReceiver, filter);
    }

    public Handler getHandler() {
        return mSessionHandler;
    }

    /**
     * Asserting masId
     * @param masId
     * @return true if MnsClient is created for masId; otherwise false.
     */
    private boolean assertMasid(final int masId) {
        final int size = mMnsClients.size();
        if (masId < 0 || masId >= size) {
            Log.e(TAG, "MAS id: " + masId + " is out of maximum number of MAS instances: " + size);
            return false;
        }
        return true;
    }

    private boolean register(final int masId) {
        if (!assertMasid(masId)) {
            Log.e(TAG, "Attempt to register MAS id: " + masId);
            return false;
        }
        final MnsClient client = mMnsClients.get(masId);
        if (!client.isRegistered()) {
            try {
                client.register(BluetoothMns.this);
            } catch (Exception e) {
                Log.e(TAG, "Exception occured while register MNS for MAS id: " + masId, e);
                return false;
            }
        }
        return true;
    }

    private synchronized boolean canDisconnect() {
        for (MnsClient client : mMnsClients) {
            if (client.isRegistered()) {
                return false;
            }
        }
        return true;
    }

    private void deregister(final int masId) {
        if (!assertMasid(masId)) {
            Log.e(TAG, "Attempt to register MAS id: " + masId);
            return;
        }
        final MnsClient client = mMnsClients.get(masId);
        if (client.isRegistered()) {
            client.register(null);
        }
    }

    private void deregisterAll() {
        for (MnsClient client : mMnsClients) {
            if (client.isRegistered()) {
                client.register(null);
            }
        }
    }

    private void mnsCleanupInstances() {
        if (V) Log.v(TAG, "MNS_BT: entered mnsCleanupInstances");
        if(mStorageStatusReceiver != null) {
            mContext.unregisterReceiver(mStorageStatusReceiver);
            mStorageStatusReceiver = null;
        }
        for (MnsClient client : mMnsClients) {
            if (V) Log.v(TAG, "MNS_BT: mnsCleanupInstances: inside for loop");
            if (client.isRegistered()) {
                if (V) Log.v(TAG, "MNS_BT: mnsCleanupInstances: Attempt to deregister MnsClient");
                client.register(null);
                client = null;
                if (V) Log.v(TAG, "MNS_BT: mnsCleanupInstances: made client = null");
            }
        }
    }

    /*
     * Receives events from mConnectThread & mSession back in the main thread.
     */
    private class EventHandler extends Handler {
        public EventHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            if (V){
                Log.v(TAG, " Handle Message " + msg.what);
            }
            switch (msg.what) {
                case MNS_CONNECT:
                {
                    final int masId = msg.arg1;
                    final BluetoothDevice device = (BluetoothDevice)msg.obj;
                    if (mSession != null) {
                        if (V) Log.v(TAG, "is MNS session connected? " + mSession.isConnected());
                        if (mSession.isConnected()) {
                            if (!register(masId)) {
                                // failed to register, disconnect
                                obtainMessage(MNS_DISCONNECT, masId, -1).sendToTarget();
                            }
                            break;
                        }
                    }
                    if (mWaitingMasId.isEmpty()) {
                        mWaitingMasId.add(masId);
                        mConnectThread = new SocketConnectThread(device);
                        mConnectThread.start();
                    } else {
                        mWaitingMasId.add(masId);
                    }
                    break;
                }
                case MNS_DISCONNECT:
                {
                    final int masId = msg.arg1;
                    new Thread(new Runnable() {
                        public void run() {
                            deregister(masId);
                            if (canDisconnect()) {
                                stop();
                            }
                        }
                    }).start();
                    break;
                }
                case MNS_BLUETOOTH_OFF:
                    if (V) Log.v(TAG, "MNS_BT: receive MNS_BLUETOOTH_OFF msg");
                    new Thread(new Runnable() {
                        public void run() {
                            if (V) Log.v(TAG, "MNS_BT: Started Deregister Thread");
                            if (canDisconnect()) {
                                stop();
                            }
                            mnsCleanupInstances();
                        }
                    }).start();
                    break;
                /*
                 * RFCOMM connect fail is for outbound share only! Mark batch
                 * failed, and all shares in batch failed
                 */
                case RFCOMM_ERROR:
                    if (V) Log.v(TAG, "receive RFCOMM_ERROR msg");
                    deregisterAll();
                    if (canDisconnect()) {
                        stop();
                    }
                    break;
                /*
                 * RFCOMM connected. Do an OBEX connect by starting the session
                 */
                case RFCOMM_CONNECTED:
                {
                    if (V) Log.v(TAG, "Transfer receive RFCOMM_CONNECTED msg");
                    ObexTransport transport = (ObexTransport) msg.obj;
                    try {
                        startObexSession(transport);
                    } catch (NullPointerException ne) {
                        sendEmptyMessage(RFCOMM_ERROR);
                        return;
                    }
                    for (int masId : mWaitingMasId) {
                        register(masId);
                    }
                    mWaitingMasId.clear();
                    break;
                }
                /* Handle the error state of an Obex session */
                case BluetoothMnsObexSession.MSG_SESSION_ERROR:
                    if (V) Log.v(TAG, "receive MSG_SESSION_ERROR");
                    deregisterAll();
                    stop();
                    break;
                case MNS_SEND_EVENT:
                {
                    final String xml = (String)msg.obj;
                    final int masId = msg.arg1;
                    if (mSendingEvent) {
                        mEventQueue.add(new Pair<Integer, String>(masId, xml));
                    } else {
                        mSendingEvent = true;
                        new Thread(new SendEventTask(xml, masId)).start();
                    }
<<<<<<< HEAD
                } catch (IOException e) {
                    Log.e(TAG, "failed to close mTransport");
=======
                    break;
                }
                case MNS_SEND_EVENT_DONE:
                    if (mEventQueue.isEmpty()) {
                        mSendingEvent = false;
                    } else {
                        final Pair<Integer, String> p = mEventQueue.remove();
                        final int masId = p.first;
                        final String xml = p.second;
                        new Thread(new SendEventTask(xml, masId)).start();
                    }
                    break;
                case MNS_SEND_TIMEOUT:
                {
                    if (V) Log.v(TAG, "MNS_SEND_TIMEOUT disconnecting.");
                    deregisterAll();
                    stop();
                    break;
>>>>>>> a130f0e... Bluetooth MAP (Message Access Profile) Upstream Changes (1/3)
                }
            }
        }

        private void setTimeout(int masId) {
            if (V) Log.v(TAG, "setTimeout MNS_SEND_TIMEOUT for instance " + masId);
            sendMessageDelayed(obtainMessage(MNS_SEND_TIMEOUT, masId, -1),
                    MNS_SEND_TIMEOUT_DURATION);
        }

        private void removeTimeout() {
            if (hasMessages(MNS_SEND_TIMEOUT)) {
                removeMessages(MNS_SEND_TIMEOUT);
                sendEventDone();
            }
        }

        private void sendEventDone() {
            if (V) Log.v(TAG, "post MNS_SEND_EVENT_DONE");
            obtainMessage(MNS_SEND_EVENT_DONE).sendToTarget();
        }

        class SendEventTask implements Runnable {
            final String mXml;
            final int mMasId;
            SendEventTask (String xml, int masId) {
                mXml = xml;
                mMasId = masId;
            }

            public void run() {
                if (V) Log.v(TAG, "MNS_SEND_EVENT started");
                setTimeout(mMasId);
                sendEvent(mXml, mMasId);
                removeTimeout();
                if (V) Log.v(TAG, "MNS_SEND_EVENT finished");
            }
        }
    }

    /*
     * Class to hold message handle for MCE Initiated operation
     */
    public class BluetoothMnsMsgHndlMceInitOp {
        public String msgHandle;
        Time time;
    }

    /*
     * Keep track of Message Handles on which the operation was
     * initiated by MCE
     */
    List<BluetoothMnsMsgHndlMceInitOp> opList = new ArrayList<BluetoothMnsMsgHndlMceInitOp>();

    /*
     * Adds the Message Handle to the list for tracking
     * MCE initiated operation
     */
    public void addMceInitiatedOperation(String msgHandle) {
        BluetoothMnsMsgHndlMceInitOp op = new BluetoothMnsMsgHndlMceInitOp();
        op.msgHandle = msgHandle;
        op.time = new Time();
        op.time.setToNow();
        opList.add(op);
    }
    /*
     * Removes the Message Handle from the list for tracking
     * MCE initiated operation
     */
    public void removeMceInitiatedOperation(int location) {
        opList.remove(location);
    }

    /*
     * Finds the location in the list of the given msgHandle, if
     * available. "+" indicates the next (any) operation
     */
    public int findLocationMceInitiatedOperation( String msgHandle) {
        int location = -1;

        Time currentTime = new Time();
        currentTime.setToNow();

        List<BluetoothMnsMsgHndlMceInitOp> staleOpList = new ArrayList<BluetoothMnsMsgHndlMceInitOp>();
        for (BluetoothMnsMsgHndlMceInitOp op: opList) {
            if (currentTime.toMillis(false) - op.time.toMillis(false) > 10000) {
                // add stale entries
                staleOpList.add(op);
            }
        }
        if (!staleOpList.isEmpty()) {
            for (BluetoothMnsMsgHndlMceInitOp op: staleOpList) {
                // Remove stale entries
                opList.remove(op);
            }
        }

        for (BluetoothMnsMsgHndlMceInitOp op: opList) {
            if (op.msgHandle.equalsIgnoreCase(msgHandle)){
                location = opList.indexOf(op);
                break;
            }
        }

        if (location == -1) {
            for (BluetoothMnsMsgHndlMceInitOp op: opList) {
                if (op.msgHandle.equalsIgnoreCase("+")) {
                    location = opList.indexOf(op);
                    break;
                }
            }
        }
        return location;
    }


    /**
     * Post a MNS Event to the MNS thread
     */
    public void sendMnsEvent(int masId, String msg, String handle, String folder,
            String old_folder, String msgType) {
        if (V) {
            Log.v(TAG, "sendMnsEvent()");
            Log.v(TAG, "msg: " + msg);
            Log.v(TAG, "handle: " + handle);
            Log.v(TAG, "folder: " + folder);
            Log.v(TAG, "old_folder: " + old_folder);
            Log.v(TAG, "msgType: " + msgType);
        }
        int location = -1;

        /* Send the notification, only if it was not initiated
         * by MCE. MEMORY_FULL and MEMORY_AVAILABLE cannot be
         * MCE initiated
         */
        if (msg.equals(MEMORY_AVAILABLE) || msg.equals(MEMORY_FULL)) {
            location = -1;
        } else {
            location = findLocationMceInitiatedOperation(handle);
        }

        if (location == -1) {
            String str = MapUtils.mapEventReportXML(msg, handle, folder, old_folder, msgType);
            if (V) Log.v(TAG, "Notification to MAS " + masId + ", msgType = " + msgType);
            mSessionHandler.obtainMessage(MNS_SEND_EVENT, masId, -1, str).sendToTarget();
        } else {
            removeMceInitiatedOperation(location);
        }
    }

    /**
     * Push the message over Obex client session
     */
    private void sendEvent(String str, int masId) {
        if (str != null && (str.length() > 0)) {
            if (V){
                Log.v(TAG, "--------------");
                Log.v(TAG, " CONTENT OF EVENT REPORT FILE: " + str);
            }

            final String FILENAME = "EventReport" + masId;
            FileOutputStream fos = null;
            File file = new File(mContext.getFilesDir() + "/" + FILENAME);
            file.delete();
            try {
                fos = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write(str.getBytes());
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File fileR = new File(mContext.getFilesDir() + "/" + FILENAME);
            if (fileR.exists() == true) {
                if (V) {
                    Log.v(TAG, " Sending event report file for Mas " + masId);
                }
                try {
                    if (mSession != null) {
                        mSession.sendEvent(fileR, (byte) masId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (V){
                    Log.v(TAG, " ERROR IN CREATING SEND EVENT OBJ FILE");
                }
            }
        } else if (V) {
            Log.v(TAG, "sendEvent(null, " + masId + ")");
        }
    }

    private BroadcastReceiver mStorageStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && mSession != null) {
                final String action = intent.getAction();
                if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                    Log.d(TAG, " Memory Full ");
                    sendMnsEventMemory(MEMORY_FULL);
                } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                    Log.d(TAG, " Memory Available ");
                    sendMnsEventMemory(MEMORY_AVAILABLE);
                }
            }
        }
    };

    /**
     * Stop the transfer
     */
    public void stop() {
        if (V) Log.v(TAG, "stop");
        if (mSession != null) {
            if (V) Log.v(TAG, "Stop mSession");
            mSession.disconnect();
            mSession = null;
        }
    }

    /**
     * Connect the MNS Obex client to remote server
     */
    private void startObexSession(ObexTransport transport) throws NullPointerException {
        if (V) Log.v(TAG, "Create Client session with transport " + transport.toString());
        mSession = new BluetoothMnsObexSession(mContext, transport);
        mSession.connect();
    }

    private SocketConnectThread mConnectThread;
    /**
     * This thread is used to establish rfcomm connection to
     * remote device
     */
    private class SocketConnectThread extends Thread {
        private final BluetoothDevice device;

        private long timestamp;

        /* create a Rfcomm Socket */
        public SocketConnectThread(BluetoothDevice device) {
            super("Socket Connect Thread");
            this.device = device;
        }

        public void interrupt() {
        }

        @Override
        public void run() {
            timestamp = System.currentTimeMillis();

            BluetoothSocket btSocket = null;
            try {
                btSocket = device.createInsecureRfcommSocketToServiceRecord(
                        BluetoothUuid_ObexMns.getUuid());
                btSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "BtSocket Connect error " + e.getMessage(), e);
                markConnectionFailed(btSocket);
                return;
            }

            if (V) Log.v(TAG, "Rfcomm socket connection attempt took "
                    + (System.currentTimeMillis() - timestamp) + " ms");
            ObexTransport transport;
            transport = new BluetoothMnsRfcommTransport(btSocket);
            if (V) Log.v(TAG, "Send transport message " + transport.toString());

            mSessionHandler.obtainMessage(RFCOMM_CONNECTED, transport).sendToTarget();
        }

        /**
         * RFCOMM connection failed
         */
        private void markConnectionFailed(BluetoothSocket s) {
            try {
                if (s != null) {
                    s.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error when close socket");
            }
            mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
            return;
        }
    }

    public void sendMnsEventMemory(String msg) {
        // Sending "MemoryFull" or "MemoryAvailable" to all registered Mas Instances
        for (MnsClient client : mMnsClients) {
            if (client.isRegistered()) {
                sendMnsEvent(client.getMasId(), msg, null, null, null, null);
            }
        }
    }

    public void onDeliveryFailure(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, DELIVERY_FAILURE, handle, folder, null, msgType);
    }

    public void onDeliverySuccess(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, DELIVERY_SUCCESS, handle, folder, null, msgType);
    }

    public void onMessageShift(int masId, String handle, String toFolder,
            String fromFolder, String msgType) {
        sendMnsEvent(masId, MESSAGE_SHIFT, handle, toFolder, fromFolder, msgType);
    }

    public void onNewMessage(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, NEW_MESSAGE, handle, folder, null, msgType);
    }

    public void onSendingFailure(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, SENDING_FAILURE, handle, folder, null, msgType);
    }

    public void onSendingSuccess(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, SENDING_SUCCESS, handle, folder, null, msgType);
    }

    public void onMessageDeleted(int masId, String handle, String folder, String msgType) {
        sendMnsEvent(masId, MESSAGE_DELETED, handle, folder, null, msgType);
    }

    public static abstract class MnsClient implements MnsRegister {
        public static final String TAG = "MnsClient";
        public static final boolean V = BluetoothMasService.VERBOSE;
        protected static final String PRE_PATH = TELECOM + "/" + MSG + "/";

        protected Context mContext;
        protected MessageNotificationListener mListener = null;
        protected int mMasId;

        protected final long OFFSET_START;
        protected final long OFFSET_END;

        public MnsClient(Context context, int masId) {
            mContext = context;
            mMasId = masId;
            OFFSET_START = HANDLE_OFFSET[masId];
            OFFSET_END = HANDLE_OFFSET[masId + 1] - 1;
        }

        public synchronized void register(MessageNotificationListener listener) {
            if (V) Log.v(TAG, "MNS_BT: register entered");
            if (listener != null) {
                mListener = listener;
                registerContentObserver();
            } else {
                if (V) Log.v(TAG, "MNS_BT: register(null)");
                unregisterContentObserver();
                mListener = null;
            }
        }

        public boolean isRegistered() {
            return mListener != null;
        }

        public int getMasId() {
            return mMasId;
        }

<<<<<<< HEAD
            Log.d(TAG, "EMAIL SENT current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crEmailSentA.moveToFirst();
                crEmailSentB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crEmailSentA,
                        new String[] { "_id" }, crEmailSentB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCREmailSent == CR_EMAIL_SENT_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM SENT ");
                            String id = crEmailSentA.getString(crEmailSentA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);

                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/SENT", null, "EMAIL");
                            } else {
                                Log.d(TAG,"Shouldn't reach here as you cannot " +
                                          "move msg from Sent to any other folder");
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCREmailSent == CR_EMAIL_SENT_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM SENT ");
                            String id = crEmailSentB.getString(crEmailSentB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);
                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/SENT", null, "EMAIL");
                            } else {
                                Log.d(TAG, "Shouldn't reach here as " +
                                                "you cannot move msg from Sent to " +
                                                "any other folder");
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCREmailSent == CR_EMAIL_SENT_A) {
                currentCREmailSent = CR_EMAIL_SENT_B;
            } else {
                currentCREmailSent = CR_EMAIL_SENT_A;
            }
        }
    }

    /**
     * This class listens for changes in Email Content Provider's Draft table
     * It acts, only when a entry gets removed from the table
     */
    private class EmailDraftContentObserverClass extends ContentObserver {

        public EmailDraftContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            if (currentCREmailDraft == CR_EMAIL_DRAFT_A) {
                currentItemCount = crEmailDraftA.getCount();
                crEmailDraftB.requery();
                newItemCount = crEmailDraftB.getCount();
            } else {
                currentItemCount = crEmailDraftB.getCount();
                crEmailDraftA.requery();
                newItemCount = crEmailDraftA.getCount();
            }

            Log.d(TAG, "EMAIL DRAFT current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crEmailDraftA.moveToFirst();
                crEmailDraftB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crEmailDraftA,
                        new String[] { "_id" }, crEmailDraftB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCREmailDraft == CR_EMAIL_DRAFT_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM DRAFT ");
                            String id = crEmailDraftA.getString(crEmailDraftA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);

                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/DRAFT", null, "EMAIL");
                            } else {
                                Cursor cr1 = null;
                                int folderId;
                                String containingFolder = null;
                                EmailUtils eu = new EmailUtils();
                                Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                                String whereClause = " _id = " + id;
                                cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                        null);

                                if (cr1.getCount() > 0) {
                                    cr1.moveToFirst();
                                    folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                    containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                                }

                                String newFolder = containingFolder;
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT",
                                        "EMAIL");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "EMAIL");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCREmailDraft == CR_EMAIL_DRAFT_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM DRAFT ");
                            String id = crEmailDraftB.getString(crEmailDraftB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);
                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/DRAFT", null, "EMAIL");
                            } else {
                                Cursor cr1 = null;
                                int folderId;
                                String containingFolder = null;
                                EmailUtils eu = new EmailUtils();
                                Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                                String whereClause = " _id = " + id;
                                cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                        null);

                                if (cr1.getCount() > 0) {
                                    cr1.moveToFirst();
                                    folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                    containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                                }

                                String newFolder = containingFolder;
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT",
                                        "EMAIL");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "EMAIL");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCREmailDraft == CR_EMAIL_DRAFT_A) {
                currentCREmailDraft = CR_EMAIL_DRAFT_B;
            } else {
                currentCREmailDraft = CR_EMAIL_DRAFT_A;
            }
        }
    }

    /**
     * This class listens for changes in Sms Content Provider's Outbox table
     * It acts only when a entry gets removed from the table
     */
    private class EmailOutboxContentObserverClass extends ContentObserver {

        public EmailOutboxContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            if (currentCREmailOutbox == CR_EMAIL_OUTBOX_A) {
                currentItemCount = crEmailOutboxA.getCount();
                crEmailOutboxB.requery();
                newItemCount = crEmailOutboxB.getCount();
            } else {
                currentItemCount = crEmailOutboxB.getCount();
                crEmailOutboxA.requery();
                newItemCount = crEmailOutboxA.getCount();
            }

            Log.d(TAG, "EMAIL OUTBOX current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crEmailOutboxA.moveToFirst();
                crEmailOutboxB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crEmailOutboxA,
                        new String[] { "_id" }, crEmailOutboxB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCREmailOutbox == CR_EMAIL_OUTBOX_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM OUTBOX ");
                            String id = crEmailOutboxA.getString(crEmailOutboxA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);
                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "EMAIL");
                            } else {
                                Cursor cr1 = null;
                                int folderId;
                                String containingFolder = null;
                                EmailUtils eu = new EmailUtils();
                                Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                                String whereClause = " _id = " + id;
                                cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                        null);

                                if (cr1.getCount() > 0) {
                                    cr1.moveToFirst();
                                    folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                    containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                                }

                                String newFolder = containingFolder;
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);

                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "EMAIL");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "EMAIL");
                                    }
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCREmailOutbox == CR_EMAIL_OUTBOX_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " EMAIL DELETED FROM OUTBOX ");
                            String id = crEmailOutboxB.getString(crEmailOutboxB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED EMAIL ID " + id);
                            int deletedFlag = getDeletedFlagEmail(id);
                            if(deletedFlag == 1){
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "EMAIL");
                            } else {
                                Cursor cr1 = null;
                                int folderId;
                                String containingFolder = null;
                                EmailUtils eu = new EmailUtils();
                                Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                                String whereClause = " _id = " + id;
                                cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                        null);

                                if (cr1.getCount() > 0) {
                                    cr1.moveToFirst();
                                    folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                    containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                                }

                                String newFolder = containingFolder;
                                id = Integer.toString(Integer.valueOf(id)
                                            + EMAIL_HDLR_CONSTANT);

                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "EMAIL");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCREmailOutbox == CR_EMAIL_OUTBOX_A) {
                currentCREmailOutbox = CR_EMAIL_OUTBOX_B;
            } else {
                currentCREmailOutbox = CR_EMAIL_OUTBOX_A;
            }
        }
    }



    /**
     * This class listens for changes in Sms Content Provider
     * It acts, only when a new entry gets added to database
     */
    private class SmsContentObserverClass extends ContentObserver {

        public SmsContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            checkMmsAdded();

            // Synchronize this?
            if (currentCRSms == CR_SMS_A) {
                currentItemCount = crSmsA.getCount();
                crSmsB.requery();
                newItemCount = crSmsB.getCount();
            } else {
                currentItemCount = crSmsB.getCount();
                crSmsA.requery();
                newItemCount = crSmsA.getCount();
            }

            Log.d(TAG, "SMS current " + currentItemCount + " new "
                    + newItemCount);

            if (newItemCount > currentItemCount) {
                crSmsA.moveToFirst();
                crSmsB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsA,
                        new String[] { "_id" }, crSmsB, new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSms == CR_SMS_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                            Log.d(TAG, " SMS ADDED TO INBOX ");
                            String body1 = crSmsA.getString(crSmsA
                                    .getColumnIndex("body"));
                            String id1 = crSmsA.getString(crSmsA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " ADDED SMS ID " + id1 + " BODY "
                                    + body1);
                            String folder = getMAPFolder(crSmsA.getInt(crSmsA
                                    .getColumnIndex("type")));
                            if ( folder != null ) {
                                sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                        + folder, null, "SMS_GSM");
                            } else {
                                Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                            }
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSms == CR_SMS_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                            Log.d(TAG, " SMS ADDED ");
                            String body1 = crSmsB.getString(crSmsB
                                    .getColumnIndex("body"));
                            String id1 = crSmsB.getString(crSmsB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " ADDED SMS ID " + id1 + " BODY "
                                    + body1);
                            String folder = getMAPFolder(crSmsB.getInt(crSmsB
                                    .getColumnIndex("type")));
                            if ( folder != null ) {
                                sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                        + folder, null, "SMS_GSM");
                            } else {
                                Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                            }
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSms == CR_SMS_A) {
                currentCRSms = CR_SMS_B;
            } else {
                currentCRSms = CR_SMS_A;
            }
        }
    }

    /**
     * This class listens for changes in Email Content Provider
     * It acts, only when a new entry gets added to database
     */
    private class EmailContentObserverClass extends ContentObserver {

        public EmailContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;
            EmailUtils eu = new EmailUtils();
            String containingFolder = null;

            // Synchronize this?
            if (currentCREmail == CR_EMAIL_A) {
                currentItemCount = crEmailA.getCount();
                crEmailB.requery();
                newItemCount = crEmailB.getCount();
            } else {
                currentItemCount = crEmailB.getCount();
                crEmailA.requery();
                newItemCount = crEmailA.getCount();
            }

            Log.d(TAG, "Email current " + currentItemCount + " new "
                    + newItemCount);

            if (newItemCount > currentItemCount) {
                crEmailA.moveToFirst();
                crEmailB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crEmailA,
                        new String[] { "_id" }, crEmailB, new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCREmail == CR_EMAIL_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                            Log.d(TAG, " EMAIL ADDED TO INBOX ");
                            String id1 = crEmailA.getString(crEmailA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " ADDED EMAIL ID " + id1);
                            Cursor cr1 = null;
                            int folderId;
                            Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                            String whereClause = " _id = " + id1;
                            cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                    null);
                            if ( cr1.moveToFirst()) {
                                        do {
                                                for(int i=0;i<cr1.getColumnCount();i++){
                                                        Log.d(TAG, " Column Name: "+ cr1.getColumnName(i) + " Value: " + cr1.getString(i));
                                                }
                                    } while ( cr1.moveToNext());
                                }

                            if (cr1.getCount() > 0) {
                                cr1.moveToFirst();
                                folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                            }

                            if ( containingFolder != null ) {
                                Log.d(TAG, " containingFolder:: "+containingFolder);
                                id1 = Integer.toString(Integer.valueOf(id1)
                                        + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                        + containingFolder, null, "EMAIL");
                            } else {
                                Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                            }
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCREmail == CR_EMAIL_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                            Log.d(TAG, " EMAIL ADDED ");
                            String id1 = crEmailB.getString(crEmailB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " ADDED EMAIL ID " + id1);
                            Cursor cr1 = null;
                            int folderId;
                            Uri uri1 = Uri.parse("content://com.android.email.provider/message");
                            String whereClause = " _id = " + id1;
                            cr1 = mContext.getContentResolver().query(uri1, null, whereClause, null,
                                    null);

                            if ( cr1.moveToFirst()) {
                                do {
                                    for(int i=0;i<cr1.getColumnCount();i++){
                                        Log.d(TAG, " Column Name: "+ cr1.getColumnName(i) +
                                                " Value: " + cr1.getString(i));
                                    }
                                } while ( cr1.moveToNext());
                            }

                            if (cr1.getCount() > 0) {
                                cr1.moveToFirst();
                                folderId = cr1.getInt(cr1.getColumnIndex("mailboxKey"));
                                containingFolder = eu.getContainingFolderEmail(folderId, mContext);
                            }
                            if ( containingFolder != null ) {
                                Log.d(TAG, " containingFolder:: "+containingFolder);
                                id1 = Integer.toString(Integer.valueOf(id1)
                                        + EMAIL_HDLR_CONSTANT);
                                sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                        + containingFolder, null, "EMAIL");
                            } else {
                                Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                            }
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCREmail == CR_EMAIL_A) {
                currentCREmail = CR_EMAIL_B;
            } else {
                currentCREmail = CR_EMAIL_A;
            }
        }
    }


    /**
     * This class listens for changes in Sms Content Provider's inbox table
     * It acts, only when a entry gets removed from the table
     */
    private class InboxContentObserverClass extends ContentObserver {

        public InboxContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            checkMmsInbox();

            if (currentCRSmsInbox == CR_SMS_INBOX_A) {
                currentItemCount = crSmsInboxA.getCount();
                crSmsInboxB.requery();
                newItemCount = crSmsInboxB.getCount();
            } else {
                currentItemCount = crSmsInboxB.getCount();
                crSmsInboxA.requery();
                newItemCount = crSmsInboxA.getCount();
            }

            Log.d(TAG, "SMS INBOX current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsInboxA.moveToFirst();
                crSmsInboxB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsInboxA,
                        new String[] { "_id" }, crSmsInboxB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsInbox == CR_SMS_INBOX_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM INBOX ");
                            String body = crSmsInboxA.getString(crSmsInboxA
                                    .getColumnIndex("body"));
                            String id = crSmsInboxA.getString(crSmsInboxA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/INBOX", null, "SMS_GSM");
                            } else {
                                Log.d(TAG, "Shouldn't reach here as you cannot " +
                                                "move msg from Inbox to any other folder");
                            }
                        } else {
                            // TODO - The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsInbox == CR_SMS_INBOX_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM INBOX ");
                            String body = crSmsInboxB.getString(crSmsInboxB
                                    .getColumnIndex("body"));
                            String id = crSmsInboxB.getString(crSmsInboxB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/INBOX", null, "SMS_GSM");
                            } else {
                                Log.d(TAG,"Shouldn't reach here as you cannot " +
                                                "move msg from Inbox to any other folder");
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsInbox == CR_SMS_INBOX_A) {
                currentCRSmsInbox = CR_SMS_INBOX_B;
            } else {
                currentCRSmsInbox = CR_SMS_INBOX_A;
            }
        }
    }



    /**
     * This class listens for changes in Sms Content Provider's Sent table
     * It acts, only when a entry gets removed from the table
     */
    private class SentContentObserverClass extends ContentObserver {

        public SentContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            checkMmsSent();

            if (currentCRSmsSent == CR_SMS_SENT_A) {
                currentItemCount = crSmsSentA.getCount();
                crSmsSentB.requery();
                newItemCount = crSmsSentB.getCount();
            } else {
                currentItemCount = crSmsSentB.getCount();
                crSmsSentA.requery();
                newItemCount = crSmsSentA.getCount();
            }

            Log.d(TAG, "SMS SENT current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsSentA.moveToFirst();
                crSmsSentB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsSentA,
                        new String[] { "_id" }, crSmsSentB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                // while((CursorJointer.Result joinerResult = joiner.next()) !=
                // null)
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsSent == CR_SMS_SENT_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM SENT ");
                            String body = crSmsSentA.getString(crSmsSentA
                                    .getColumnIndex("body"));
                            String id = crSmsSentA.getString(crSmsSentA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);

                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/SENT", null, "SMS_GSM");
                            } else {
                                Log.d(TAG,"Shouldn't reach here as you cannot " +
                                          "move msg from Sent to any other folder");
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsSent == CR_SMS_SENT_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM SENT ");
                            String body = crSmsSentB.getString(crSmsSentB
                                    .getColumnIndex("body"));
                            String id = crSmsSentB.getString(crSmsSentB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/SENT", null, "SMS_GSM");
                            } else {
                                Log.d(TAG, "Shouldn't reach here as " +
                                                "you cannot move msg from Sent to " +
                                                "any other folder");
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsSent == CR_SMS_SENT_A) {
                currentCRSmsSent = CR_SMS_SENT_B;
            } else {
                currentCRSmsSent = CR_SMS_SENT_A;
            }
        }
    }

    /**
     * This class listens for changes in Sms Content Provider's Draft table
     * It acts, only when a entry gets removed from the table
     */
    private class DraftContentObserverClass extends ContentObserver {

        public DraftContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            checkMmsDrafts();

            if (currentCRSmsDraft == CR_SMS_DRAFT_A) {
                currentItemCount = crSmsDraftA.getCount();
                crSmsDraftB.requery();
                newItemCount = crSmsDraftB.getCount();
            } else {
                currentItemCount = crSmsDraftB.getCount();
                crSmsDraftA.requery();
                newItemCount = crSmsDraftA.getCount();
            }

            Log.d(TAG, "SMS DRAFT current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsDraftA.moveToFirst();
                crSmsDraftB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsDraftA,
                        new String[] { "_id" }, crSmsDraftB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsDraft == CR_SMS_DRAFT_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM DRAFT ");
                            String body = crSmsDraftA.getString(crSmsDraftA
                                    .getColumnIndex("body"));
                            String id = crSmsDraftA.getString(crSmsDraftA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);

                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/DRAFT", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT",
                                        "SMS_GSM");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "SMS_GSM");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsDraft == CR_SMS_DRAFT_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM DRAFT ");
                            String body = crSmsDraftB.getString(crSmsDraftB
                                    .getColumnIndex("body"));
                            String id = crSmsDraftB.getString(crSmsDraftB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/DRAFT", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT",
                                        "SMS_GSM");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "SMS_GSM");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsDraft == CR_SMS_DRAFT_A) {
                currentCRSmsDraft = CR_SMS_DRAFT_B;
            } else {
                currentCRSmsDraft = CR_SMS_DRAFT_A;
            }
        }
    }

    /**
     * This class listens for changes in Sms Content Provider's Outbox table
     * It acts only when a entry gets removed from the table
     */
    private class OutboxContentObserverClass extends ContentObserver {

        public OutboxContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            // Check MMS Outbox for changes

            checkMmsOutbox();

            // Check SMS Outbox for changes

            if (currentCRSmsOutbox == CR_SMS_OUTBOX_A) {
                currentItemCount = crSmsOutboxA.getCount();
                crSmsOutboxB.requery();
                newItemCount = crSmsOutboxB.getCount();
            } else {
                currentItemCount = crSmsOutboxB.getCount();
                crSmsOutboxA.requery();
                newItemCount = crSmsOutboxA.getCount();
            }

            Log.d(TAG, "SMS OUTBOX current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsOutboxA.moveToFirst();
                crSmsOutboxB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsOutboxA,
                        new String[] { "_id" }, crSmsOutboxB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsOutbox == CR_SMS_OUTBOX_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM OUTBOX ");
                            String body = crSmsOutboxA.getString(crSmsOutboxA
                                    .getColumnIndex("body"));
                            String id = crSmsOutboxA.getString(crSmsOutboxA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "SMS_GSM");
                                    }
                                }
                                if ( (msgType == MSG_CP_QUEUED_TYPE) ||
                                        (msgType == MSG_CP_FAILED_TYPE)) {
                                    // Message moved from outbox to queue or
                                    // failed folder
                                    sendMnsEvent(SENDING_FAILURE, id,
                                            "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsOutbox == CR_SMS_OUTBOX_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM OUTBOX ");
                            String body = crSmsOutboxB.getString(crSmsOutboxB
                                    .getColumnIndex("body"));
                            String id = crSmsOutboxB.getString(crSmsOutboxB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsOutbox == CR_SMS_OUTBOX_A) {
                currentCRSmsOutbox = CR_SMS_OUTBOX_B;
            } else {
                currentCRSmsOutbox = CR_SMS_OUTBOX_A;
            }
        }
    }

    /**
     * This class listens for changes in Sms Content Provider's Failed table
     * It acts only when a entry gets removed from the table
     */
    private class FailedContentObserverClass extends ContentObserver {

        public FailedContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            // Mms doesn't have Failed type

            if (currentCRSmsFailed == CR_SMS_FAILED_A) {
                currentItemCount = crSmsFailedA.getCount();
                crSmsFailedB.requery();
                newItemCount = crSmsFailedB.getCount();
            } else {
                currentItemCount = crSmsFailedB.getCount();
                crSmsFailedA.requery();
                newItemCount = crSmsFailedA.getCount();
            }

            Log.d(TAG, "SMS FAILED current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsFailedA.moveToFirst();
                crSmsFailedB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsFailedA,
                        new String[] { "_id" }, crSmsFailedB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsFailed == CR_SMS_FAILED_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM FAILED ");
                            String body = crSmsFailedA.getString(crSmsFailedA
                                    .getColumnIndex("body"));
                            String id = crSmsFailedA.getString(crSmsFailedA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "SMS_GSM");
                                    }
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsFailed == CR_SMS_FAILED_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM FAILED ");
                            String body = crSmsFailedB.getString(crSmsFailedB
                                    .getColumnIndex("body"));
                            String id = crSmsFailedB.getString(crSmsFailedB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "SMS_GSM");
                                    }
                                }
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsFailed == CR_SMS_FAILED_A) {
                currentCRSmsFailed = CR_SMS_FAILED_B;
            } else {
                currentCRSmsFailed = CR_SMS_FAILED_A;
            }
        }
    }

    /**
     * This class listens for changes in Sms Content Provider's Queued table
     * It acts only when a entry gets removed from the table
     */
    private class QueuedContentObserverClass extends ContentObserver {

        public QueuedContentObserverClass() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int currentItemCount = 0;
            int newItemCount = 0;

            // Mms doesn't have Queued type

            if (currentCRSmsQueued == CR_SMS_QUEUED_A) {
                currentItemCount = crSmsQueuedA.getCount();
                crSmsQueuedB.requery();
                newItemCount = crSmsQueuedB.getCount();
            } else {
                currentItemCount = crSmsQueuedB.getCount();
                crSmsQueuedA.requery();
                newItemCount = crSmsQueuedA.getCount();
            }

            Log.d(TAG, "SMS QUEUED current " + currentItemCount + " new "
                    + newItemCount);

            if (currentItemCount > newItemCount) {
                crSmsQueuedA.moveToFirst();
                crSmsQueuedB.moveToFirst();

                CursorJoiner joiner = new CursorJoiner(crSmsQueuedA,
                        new String[] { "_id" }, crSmsQueuedB,
                        new String[] { "_id" });

                CursorJoiner.Result joinerResult;
                while (joiner.hasNext()) {
                    joinerResult = joiner.next();
                    switch (joinerResult) {
                    case LEFT:
                        // handle case where a row in cursor1 is unique
                        if (currentCRSmsQueued == CR_SMS_QUEUED_A) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM QUEUED ");
                            String body = crSmsQueuedA.getString(crSmsQueuedA
                                    .getColumnIndex("body"));
                            String id = crSmsQueuedA.getString(crSmsQueuedA
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "SMS_GSM");
                                    }
                                }
                            }
                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case RIGHT:
                        // handle case where a row in cursor2 is unique
                        if (currentCRSmsQueued == CR_SMS_QUEUED_B) {
                            // The new query doesn't have this row; implies it
                            // was deleted
                            Log.d(TAG, " SMS DELETED FROM QUEUED ");
                            String body = crSmsQueuedB.getString(crSmsQueuedB
                                    .getColumnIndex("body"));
                            String id = crSmsQueuedB.getString(crSmsQueuedB
                                    .getColumnIndex("_id"));
                            Log.d(TAG, " DELETED SMS ID " + id + " BODY "
                                    + body);
                            int msgType = getMessageType(id);
                            if (msgType == -1) {
                                sendMnsEvent(MESSAGE_DELETED, id,
                                        "TELECOM/MSG/OUTBOX", null, "SMS_GSM");
                            } else {
                                String newFolder = getMAPFolder(msgType);
                                if ((newFolder != null)
                                        && (!newFolder
                                                .equalsIgnoreCase("outbox"))) {
                                    // The message has moved on MAP virtual
                                    // folder representation.
                                    sendMnsEvent(MESSAGE_SHIFT, id,
                                            "TELECOM/MSG/" + newFolder,
                                            "TELECOM/MSG/OUTBOX", "SMS_GSM");
                                    if ( newFolder.equalsIgnoreCase("sent")) {
                                        sendMnsEvent(SENDING_SUCCESS, id,
                                                "TELECOM/MSG/" + newFolder,
                                                null, "SMS_GSM");
                                    }
                                }
                            }

                        } else {
                            // The current(old) query doesn't have this row;
                            // implies it was added
                        }
                        break;
                    case BOTH:
                        // handle case where a row with the same key is in both
                        // cursors
                        break;
                    }
                }
            }
            if (currentCRSmsQueued == CR_SMS_QUEUED_A) {
                currentCRSmsQueued = CR_SMS_QUEUED_B;
            } else {
                currentCRSmsQueued = CR_SMS_QUEUED_A;
            }
        }
    }

    /**
     * Start MNS connection
     */
    public void start(BluetoothDevice mRemoteDevice) {
        /* check Bluetooth enable status */
        /*
         * normally it's impossible to reach here if BT is disabled. Just check
         * for safety
         */
        if (!mAdapter.isEnabled()) {
            Log.e(TAG, "Can't send event when Bluetooth is disabled ");
            return;
        }

        mDestination = mRemoteDevice;
        int channel = -1;
        // TODO Fix this below

        if (channel != -1) {
            if (D) Log.d(TAG, "Get MNS channel " + channel + " from cache for "
                    + mDestination);
            mTimestamp = System.currentTimeMillis();
            mSessionHandler.obtainMessage(SDP_RESULT, channel, -1, mDestination)
                    .sendToTarget();
        } else {
            sendMnsSdp();
        }
    }

    /**
     * Stop the transfer
     */
    public void stop() {
        if (V)
            Log.v(TAG, "stop");
        if (mConnectThread != null) {
            try {
                mConnectThread.interrupt();
                if (V) Log.v(TAG, "waiting for connect thread to terminate");
                mConnectThread.join();
            } catch (InterruptedException e) {
                if (V) Log.v(TAG,
                        "Interrupted waiting for connect thread to join");
            }
            mConnectThread = null;
        }
        if (mSession != null) {
            if (V)
                Log.v(TAG, "Stop mSession");
            mSession.disconnect();
            mSession = null;
        }
        // TODO Do this somewhere else - Should the handler thread be gracefully closed.
    }

    /**
     * Connect the MNS Obex client to remote server
     */
    private void startObexSession() {

        if (V)
            Log.v(TAG, "Create Client session with transport "
                    + mTransport.toString());
        mSession = new BluetoothMnsObexSession(mContext, mTransport);
        mSession.connect();
    }

    /**
     * Check if local database contains remote device's info
     * Else start sdp query
     */
    private void sendMnsSdp() {
        if (V)
            Log.v(TAG, "Do Opush SDP request for address " + mDestination);

        mTimestamp = System.currentTimeMillis();

        int channel = -1;

        Method m;
        try {
            m = mDestination.getClass().getMethod("getServiceChannel",
                    new Class[] { ParcelUuid.class });
            channel = (Integer) m.invoke(mDestination, BluetoothUuid_ObexMns);
        } catch (SecurityException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        } catch (NoSuchMethodException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // TODO: channel = mDestination.getServiceChannel(BluetoothUuid_ObexMns);

        if (channel != -1) {
            if (D)
                Log.d(TAG, "Get MNS channel " + channel + " from SDP for "
                        + mDestination);

            mSessionHandler
                    .obtainMessage(SDP_RESULT, channel, -1, mDestination)
                    .sendToTarget();
            return;

        } else {

            boolean result = false;
            if (V)
                Log.v(TAG, "Remote Service channel not in cache");

            Method m2;
            try {
                m2 = mDestination.getClass().getMethod("fetchUuidsWithSdp",
                        new Class[] {});
                result = (Boolean) m2.invoke(mDestination);

            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (result == false) {
                Log.e(TAG, "Start SDP query failed");
            } else {
                // we expect framework send us Intent ACTION_UUID. otherwise we
                // will fail
                if (V)
                    Log.v(TAG, "Start new SDP, wait for result");
                IntentFilter intentFilter = new IntentFilter(
                        "android.bleutooth.device.action.UUID");
                mContext.registerReceiver(mReceiver, intentFilter);
                return;
            }
        }
        Message msg = mSessionHandler.obtainMessage(SDP_RESULT, channel, -1,
                mDestination);
        mSessionHandler.sendMessageDelayed(msg, 2000);
    }

    /**
     * Receives the response of SDP query
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, " MNS BROADCAST RECV intent: " + intent.getAction());

            if (intent.getAction().equals(
                    "android.bleutooth.device.action.UUID")) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (V)
                    Log.v(TAG, "ACTION_UUID for device " + device);
                if (device.equals(mDestination)) {
                    int channel = -1;
                    Parcelable[] uuid = intent
                            .getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
                    if (uuid != null) {
                        ParcelUuid[] uuids = new ParcelUuid[uuid.length];
                        for (int i = 0; i < uuid.length; i++) {
                            uuids[i] = (ParcelUuid) uuid[i];
                        }

                        boolean result = false;

                        // TODO Fix this error
                        result = true;

                        try {
                            Class c = Class
                                    .forName("android.bluetooth.BluetoothUuid");
                            Method m = c.getMethod("isUuidPresent",
                                    new Class[] { ParcelUuid[].class,
                                            ParcelUuid.class });

                            Boolean bool = false;
                            bool = (Boolean) m.invoke(c, uuids,
                                    BluetoothUuid_ObexMns);
                            result = bool.booleanValue();

                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        if (result) {
                            // TODO: Check if UUID IS PRESENT
                            if (V)
                                Log.v(TAG, "SDP get MNS result for device "
                                        + device);

                            // TODO: Get channel from mDestination
                            // TODO: .getServiceChannel(BluetoothUuid_ObexMns);
                            Method m1;
                            try {

                                m1 = device.getClass().getMethod(
                                        "getServiceChannel",
                                        new Class[] { ParcelUuid.class });
                                Integer chan = (Integer) m1.invoke(device,
                                        BluetoothUuid_ObexMns);

                                channel = chan.intValue();
                                Log.d(TAG, " MNS SERVER Channel no " + channel);
                                if (channel == -1) {
                                    channel = 2;
                                    Log.d(TAG, " MNS SERVER USE TEMP CHANNEL "
                                            + channel);
                                }
                            } catch (SecurityException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IllegalArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    mSessionHandler.obtainMessage(SDP_RESULT, channel, -1,
                            mDestination).sendToTarget();
                }
            }
        }
    };

    private SocketConnectThread mConnectThread;

    /**
     * This thread is used to establish rfcomm connection to
     * remote device
     */
    private class SocketConnectThread extends Thread {
        private final String host;

        private final BluetoothDevice device;

        private final int channel;

        private boolean isConnected;

        private long timestamp;

        private BluetoothSocket btSocket = null;

        /* create a Rfcomm Socket */
        public SocketConnectThread(BluetoothDevice device, int channel) {
            super("Socket Connect Thread");
            this.device = device;
            this.host = null;
            this.channel = channel;
            isConnected = false;
        }

        public void interrupt() {
        }

        @Override
        public void run() {

            timestamp = System.currentTimeMillis();

            /* Use BluetoothSocket to connect */
            try {
                try {
                    Method m = device.getClass().getMethod(
                            "createInsecureRfcommSocket",
                            new Class[] { int.class });
                    btSocket = (BluetoothSocket) m.invoke(device, channel);
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                Log.e(TAG, "Rfcomm socket create error");
                markConnectionFailed(btSocket);
                return;
            }

            try {
                btSocket.connect();
                if (V) Log.v(TAG, "Rfcomm socket connection attempt took "
                        + (System.currentTimeMillis() - timestamp) + " ms");
                BluetoothMnsRfcommTransport transport;
                transport = new BluetoothMnsRfcommTransport(btSocket);

                BluetoothMnsPreference.getInstance(mContext).setChannel(device,
                        MNS_UUID16, channel);
                BluetoothMnsPreference.getInstance(mContext).setName(device,
                        device.getName());

                if (V) Log.v(TAG, "Send transport message "
                        + transport.toString());

                mSessionHandler.obtainMessage(RFCOMM_CONNECTED, transport)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Rfcomm socket connect exception ");
                BluetoothMnsPreference.getInstance(mContext).removeChannel(
                        device, MNS_UUID16);
                markConnectionFailed(btSocket);
                return;
            }
        }

        /**
         * RFCOMM connection failed
         */
        private void markConnectionFailed(Socket s) {
            try {
                s.close();
            } catch (IOException e) {
                Log.e(TAG, "TCP socket close error");
            }
            mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
        }

        /**
         * RFCOMM connection failed
         */
        private void markConnectionFailed(BluetoothSocket s) {
            try {
                s.close();
            } catch (IOException e) {
                if (V) Log.e(TAG, "Error when close socket");
            }
            mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
            return;
        }
    }

    /**
     * Check for change in MMS outbox and send a notification if there is a
     * change
     */
    private void checkMmsOutbox() {

        int currentItemCount = 0;
        int newItemCount = 0;

        if (currentCRMmsOutbox == CR_MMS_OUTBOX_A) {
            currentItemCount = crMmsOutboxA.getCount();
            crMmsOutboxB.requery();
            newItemCount = crMmsOutboxB.getCount();
        } else {
            currentItemCount = crMmsOutboxB.getCount();
            crMmsOutboxA.requery();
            newItemCount = crMmsOutboxA.getCount();
        }

        Log.d(TAG, "MMS OUTBOX current " + currentItemCount + " new "
                + newItemCount);

        if (currentItemCount > newItemCount) {
            crMmsOutboxA.moveToFirst();
            crMmsOutboxB.moveToFirst();

            CursorJoiner joiner = new CursorJoiner(crMmsOutboxA,
                    new String[] { "_id" }, crMmsOutboxB,
                    new String[] { "_id" });

            CursorJoiner.Result joinerResult;
            while (joiner.hasNext()) {
                joinerResult = joiner.next();
                switch (joinerResult) {
                case LEFT:
                    // handle case where a row in cursor1 is unique
                    if (currentCRMmsOutbox == CR_MMS_OUTBOX_A) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM OUTBOX ");
                        String id = crMmsOutboxA.getString(crMmsOutboxA
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);

                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/OUTBOX", null, "MMS");
                        } else {
                            String newFolder = getMAPFolder(msgType);
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);

                            Log.d(TAG, " MESSAGE_SHIFT MMS ID " + id);
                            if ((newFolder != null)
                                    && (!newFolder.equalsIgnoreCase("outbox"))) {
                                // The message has moved on MAP virtual
                                // folder representation.
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/OUTBOX",
                                        "MMS");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "MMS");
                                }
                            }
                            /* Mms doesn't have failed or queued type
                             * Cannot send SENDING_FAILURE as there
                             * is no indication if Sending failed
                             */
                        }

                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case RIGHT:
                    // handle case where a row in cursor2 is unique
                    if (currentCRMmsOutbox == CR_MMS_OUTBOX_B) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM OUTBOX ");
                        String id = crMmsOutboxB.getString(crMmsOutboxB
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/OUTBOX", null, "MMS");
                        } else {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            String newFolder = getMAPFolder(msgType);
                            if ((newFolder != null)
                                    && (!newFolder.equalsIgnoreCase("outbox"))) {
                                // The message has moved on MAP virtual
                                // folder representation.
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/OUTBOX",
                                        "MMS");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "MMS");
                                }
                            }
                            /* Mms doesn't have failed or queued type
                             * Cannot send SENDING_FAILURE as there
                             * is no indication if Sending failed
                             */
                        }

                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both
                    // cursors
                    break;
                }
            }
        }
        if (currentCRMmsOutbox == CR_MMS_OUTBOX_A) {
            currentCRMmsOutbox = CR_MMS_OUTBOX_B;
        } else {
            currentCRMmsOutbox = CR_MMS_OUTBOX_A;
        }
    }

    /**
     * Check for change in MMS Drafts folder and send a notification if there is
     * a change
     */
    private void checkMmsDrafts() {

        int currentItemCount = 0;
        int newItemCount = 0;

        if (currentCRMmsDraft == CR_MMS_OUTBOX_A) {
            currentItemCount = crMmsDraftA.getCount();
            crMmsDraftA.requery();
            newItemCount = crMmsDraftB.getCount();
        } else {
            currentItemCount = crMmsDraftB.getCount();
            crMmsDraftA.requery();
            newItemCount = crMmsDraftA.getCount();
        }

        if (newItemCount != 0) {
            Log.d(TAG, "MMS DRAFT breakpoint placeholder");
        }

        Log.d(TAG, "MMS DRAFT current " + currentItemCount + " new "
                + newItemCount);

        if (currentItemCount > newItemCount) {
            crMmsDraftA.moveToFirst();
            crMmsDraftB.moveToFirst();

            CursorJoiner joiner = new CursorJoiner(crMmsDraftA,
                    new String[] { "_id" }, crMmsDraftB, new String[] { "_id" });

            CursorJoiner.Result joinerResult;
            while (joiner.hasNext()) {
                joinerResult = joiner.next();
                switch (joinerResult) {
                case LEFT:
                    // handle case where a row in cursor1 is unique
                    if (currentCRMmsDraft == CR_MMS_DRAFT_A) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM DRAFT ");
                        String id = crMmsDraftA.getString(crMmsDraftA
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/DRAFT", null, "MMS");
                        } else {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            String newFolder = getMAPFolder(msgType);
                            if ((newFolder != null)
                                    && (!newFolder.equalsIgnoreCase("draft"))) {
                                // The message has moved on MAP virtual
                                // folder representation.
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT", "MMS");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "MMS");
                                }
                            }
                        }

                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case RIGHT:
                    // handle case where a row in cursor2 is unique
                    if (currentCRMmsDraft == CR_MMS_DRAFT_B) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM DRAFT ");
                        String id = crMmsDraftB.getString(crMmsDraftB
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/DRAFT", null, "MMS");
                        } else {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            String newFolder = getMAPFolder(msgType);
                            if ((newFolder != null)
                                    && (!newFolder.equalsIgnoreCase("draft"))) {
                                // The message has moved on MAP virtual
                                // folder representation.
                                sendMnsEvent(MESSAGE_SHIFT, id, "TELECOM/MSG/"
                                        + newFolder, "TELECOM/MSG/DRAFT", "MMS");
                                if ( newFolder.equalsIgnoreCase("sent")) {
                                    sendMnsEvent(SENDING_SUCCESS, id,
                                            "TELECOM/MSG/" + newFolder,
                                            null, "MMS");
                                }
                            }
                        }

                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both
                    // cursors
                    break;
                }
            }
        }
        if (currentCRMmsDraft == CR_MMS_DRAFT_A) {
            currentCRMmsDraft = CR_MMS_DRAFT_B;
        } else {
            currentCRMmsDraft = CR_MMS_DRAFT_A;
        }
    }

    /**
     * Check for change in MMS Inbox folder and send a notification if there is
     * a change
     */
    private void checkMmsInbox() {

        int currentItemCount = 0;
        int newItemCount = 0;

        if (currentCRMmsInbox == CR_MMS_INBOX_A) {
            currentItemCount = crMmsInboxA.getCount();
            crMmsInboxB.requery();
            newItemCount = crMmsInboxB.getCount();
        } else {
            currentItemCount = crMmsInboxB.getCount();
            crMmsInboxA.requery();
            newItemCount = crMmsInboxA.getCount();
        }

        Log.d(TAG, "MMS INBOX current " + currentItemCount + " new "
                + newItemCount);

        if (currentItemCount > newItemCount) {
            crMmsInboxA.moveToFirst();
            crMmsInboxB.moveToFirst();

            CursorJoiner joiner = new CursorJoiner(crMmsInboxA,
                    new String[] { "_id" }, crMmsInboxB, new String[] { "_id" });

            CursorJoiner.Result joinerResult;
            while (joiner.hasNext()) {
                joinerResult = joiner.next();
                switch (joinerResult) {
                case LEFT:
                    // handle case where a row in cursor1 is unique
                    if (currentCRMmsInbox == CR_MMS_INBOX_A) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM INBOX ");
                        String id = crMmsInboxA.getString(crMmsInboxA
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/INBOX", null, "MMS");
                        } else {
                            Log.d(TAG, "Shouldn't reach here as you cannot "
                                    + "move msg from Inbox to any other folder");
                        }
                    } else {
                        // TODO - The current(old) query doesn't have this
                        // row;
                        // implies it was added
                    }
                    break;
                case RIGHT:
                    // handle case where a row in cursor2 is unique
                    if (currentCRMmsInbox == CR_MMS_INBOX_B) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM INBOX ");
                        String id = crMmsInboxB.getString(crMmsInboxB
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/INBOX", null, "MMS");
                        } else {
                            Log.d(TAG, "Shouldn't reach here as you cannot "
                                    + "move msg from Inbox to any other folder");
                        }
                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both
                    // cursors
                    break;
                }
            }
        }
        if (currentCRMmsInbox == CR_MMS_INBOX_A) {
            currentCRMmsInbox = CR_MMS_INBOX_B;
        } else {
            currentCRMmsInbox = CR_MMS_INBOX_A;
        }
    }
    /**
     * Check for change in MMS Sent folder and send a notification if there is
     * a change
     */
    private void checkMmsSent() {

        int currentItemCount = 0;
        int newItemCount = 0;

        if (currentCRMmsSent == CR_MMS_SENT_A) {
            currentItemCount = crMmsSentA.getCount();
            crMmsSentB.requery();
            newItemCount = crMmsSentB.getCount();
        } else {
            currentItemCount = crMmsSentB.getCount();
            crMmsSentA.requery();
            newItemCount = crMmsSentA.getCount();
        }

        Log.d(TAG, "MMS SENT current " + currentItemCount + " new "
                + newItemCount);

        if (currentItemCount > newItemCount) {
            crMmsSentA.moveToFirst();
            crMmsSentB.moveToFirst();

            CursorJoiner joiner = new CursorJoiner(crMmsSentA,
                    new String[] { "_id" }, crMmsSentB, new String[] { "_id" });

            CursorJoiner.Result joinerResult;
            while (joiner.hasNext()) {
                joinerResult = joiner.next();
                switch (joinerResult) {
                case LEFT:
                    // handle case where a row in cursor1 is unique
                    if (currentCRMmsSent == CR_MMS_SENT_A) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM SENT ");
                        String id = crMmsSentA.getString(crMmsSentA
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/SENT", null, "MMS");
                        } else {
                            Log.d(TAG, "Shouldn't reach here as you cannot "
                                    + "move msg from Sent to any other folder");
                        }
                    } else {
                        // TODO - The current(old) query doesn't have this
                        // row;
                        // implies it was added
                    }
                    break;
                case RIGHT:
                    // handle case where a row in cursor2 is unique
                    if (currentCRMmsSent == CR_MMS_SENT_B) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                        Log.d(TAG, " MMS DELETED FROM SENT ");
                        String id = crMmsSentB.getString(crMmsSentB
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id));
                        if (msgType == -1) {
                            // Convert to virtual handle for MMS
                            id = Integer.toString(Integer.valueOf(id)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " DELETED MMS ID " + id);
                            sendMnsEvent(MESSAGE_DELETED, id,
                                    "TELECOM/MSG/SENT", null, "MMS");
                        } else {
                            Log.d(TAG, "Shouldn't reach here as you cannot "
                                    + "move msg from Sent to any other folder");
                        }
                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                    }
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both
                    // cursors
                    break;
                }
            }
        }
        if (currentCRMmsSent == CR_MMS_SENT_A) {
            currentCRMmsSent = CR_MMS_SENT_B;
        } else {
            currentCRMmsSent = CR_MMS_SENT_A;
        }
    }

    /**
     * Check for MMS message being added and send a notification if there is a
     * change
     */
    private void checkMmsAdded() {

        int currentItemCount = 0;
        int newItemCount = 0;

        if (currentCRMms == CR_MMS_A) {
            currentItemCount = crMmsA.getCount();
            crMmsB.requery();
            newItemCount = crMmsB.getCount();
        } else {
            currentItemCount = crMmsB.getCount();
            crMmsA.requery();
            newItemCount = crMmsA.getCount();
        }

        Log.d(TAG, "MMS current " + currentItemCount + " new " + newItemCount);

        if (newItemCount > currentItemCount) {
            crMmsA.moveToFirst();
            crMmsB.moveToFirst();

            CursorJoiner joiner = new CursorJoiner(crMmsA,
                    new String[] { "_id" }, crMmsB, new String[] { "_id" });

            CursorJoiner.Result joinerResult;
            while (joiner.hasNext()) {
                joinerResult = joiner.next();
                switch (joinerResult) {
                case LEFT:
                    // handle case where a row in cursor1 is unique
                    if (currentCRMms == CR_MMS_A) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                        Log.d(TAG, " MMS ADDED TO INBOX ");
                        String id1 = crMmsA.getString(crMmsA
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id1));
                        String folder = getMAPFolder(msgType);
                        if (folder != null) {
                            // Convert to virtual handle for MMS
                            id1 = Integer.toString(Integer.valueOf(id1)
                                    + MMS_HDLR_CONSTANT);
                            Log.d(TAG, " ADDED MMS ID " + id1);
                            sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                    + folder, null, "MMS");
                        } else {
                            Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                        }
                    }
                    break;
                case RIGHT:
                    // handle case where a row in cursor2 is unique
                    if (currentCRMms == CR_MMS_B) {
                        // The new query doesn't have this row; implies it
                        // was deleted
                    } else {
                        // The current(old) query doesn't have this row;
                        // implies it was added
                        Log.d(TAG, " MMS ADDED ");
                        String id1 = crMmsB.getString(crMmsB
                                .getColumnIndex("_id"));
                        int msgType = getMmsContainingFolder(Integer
                                .parseInt(id1));
                        String folder = getMAPFolder(msgType);
                        if (folder != null) {
                            // Convert to virtual handle for MMS
                            id1 = Integer.toString(Integer.valueOf(id1)
                                    + MMS_HDLR_CONSTANT);

                            Log.d(TAG, " ADDED MMS ID " + id1);
                            sendMnsEvent(NEW_MESSAGE, id1, "TELECOM/MSG/"
                                    + folder, null, "MMS");
                        } else {
                            Log.d(TAG, " ADDED TO UNKNOWN FOLDER");
                        }
                    }
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both
                    // cursors
                    break;
                }
            }
        }
        if (currentCRMms == CR_MMS_A) {
            currentCRMms = CR_MMS_B;
        } else {
            currentCRMms = CR_MMS_A;
        }
    }
    /**
     * Get the folder name (MAP representation) based on the message Handle
     */
    private int getMmsContainingFolder(int msgID) {
        int folderNum = -1;
        String whereClause = " _id= " + msgID;
        Uri uri = Uri.parse("content://mms/");
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(uri, null, whereClause, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int msgboxInd = cursor.getColumnIndex("msg_box");
            folderNum = cursor.getInt(msgboxInd);
        }
        cursor.close();
        return folderNum;
=======
        protected abstract void registerContentObserver();
        protected abstract void unregisterContentObserver();
>>>>>>> a130f0e... Bluetooth MAP (Message Access Profile) Upstream Changes (1/3)
    }
}
