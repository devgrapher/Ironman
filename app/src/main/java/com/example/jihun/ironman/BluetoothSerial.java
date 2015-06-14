package com.example.jihun.ironman;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Jihun on 2015-06-13.
 */
public class BluetoothSerial {
    static final int kMsgConnectBluetooth = 1;
    static final int kMsgReadBluetooth = 2;

    private static final String TAG = "ironman.bluetoothSerial";
    private final BluetoothAdapter bluetooth_;
    private ConnectThread connect_thread_;
    private ReadThread read_thread_;
    private Listener listener_;
    private BluetoothSocket socket_;
    private BluetoothDevice device_;
    private ReadHandler read_handler_;
    private OutputStream output_stream_ = null;

    public interface Listener {
        void onConnect(BluetoothDevice device);
        void onRead(BluetoothDevice device, byte[] data, int len);
        void onDisconnect(BluetoothDevice device);
    }

    public BluetoothSerial() {
        bluetooth_ = BluetoothAdapter.getDefaultAdapter();
    }

    public void askConnect(BluetoothDevice device, Listener listener) {
        if (connect_thread_ != null) {
            connect_thread_.cancel();
        }
        device_ = device;
        listener_ = listener;
        read_handler_ = new ReadHandler();
        connect_thread_ = new ConnectThread(device);
        connect_thread_.start();
    }

    public boolean isConnected() {
        if (read_thread_ == null) {
            return false;
        }
        return read_thread_.isAlive();
    }

    public void Write(byte[] bytes) {
        if (output_stream_ == null) {
            return;
        }
        try {
            output_stream_.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        if (connect_thread_ != null) {
            connect_thread_.cancel();
            connect_thread_ = null;
        }

        try {
            socket_.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (read_thread_ != null) {
            try {
                read_thread_.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            read_thread_ = null;
        }
    }

    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to socket_,
            // because socket_ is final
            BluetoothSocket tmp = null;
            device_ = device;

            // The uuid that I want to connect to.
            // This value of uuid is for Serial Communication.
            // http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord(java.util.UUID)
            // https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                tmp = device_.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket_ = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetooth_.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(TAG, "Connect...");
                socket_.connect();
            } catch (IOException connectException) {
                connectException.printStackTrace();
                // Unable to connect; close the socket and get out
                try {
                    socket_.close();
                } catch (IOException closeException) { }
                return;
            }

            Log.d(TAG, "Connected");

            Message msg = read_handler_.obtainMessage(kMsgConnectBluetooth);
            read_handler_.sendMessage(msg);

            // start reading thread.
            read_thread_ = new ReadThread();
            read_thread_.start();

            try {
                output_stream_ = socket_.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // release connect object
            connect_thread_ = null;
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                socket_.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadThread extends Thread {
        private final InputStream input_stream_;

        public ReadThread() {
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket_.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            input_stream_ = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = input_stream_.read(buffer);
                    // TODO: To prevent memory allocation each time,
                    // it may need a byte queue and synchronization.
                    byte[] fragment = Arrays.copyOf(buffer, bytes);

                    // Send the obtained bytes to the UI activity
                    read_handler_.obtainMessage(kMsgReadBluetooth, bytes, -1, fragment)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    protected class ReadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case kMsgConnectBluetooth:
                    listener_.onConnect(device_);
                    break;
                case kMsgReadBluetooth:
                    listener_.onRead(device_, (byte[])msg.obj, msg.arg1);
                    break;
            }
        }
    }
}