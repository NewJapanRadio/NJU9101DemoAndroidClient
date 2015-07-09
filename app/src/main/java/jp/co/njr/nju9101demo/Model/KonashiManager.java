package jp.co.njr.nju9101demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;

public class KonashiManager implements BluetoothAdapter.LeScanCallback
{
    private static final long SCAN_PERIOD = 5000;
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG          = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final UUID KONASHI_SERVICE_UUID                  = UUID.fromString("229BFF00-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PIO_SETTING_UUID              = UUID.fromString("229B3000-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PIO_PULLUP_UUID               = UUID.fromString("229B3001-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PIO_OUTPUT_UUID               = UUID.fromString("229B3002-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PIO_INPUT_NOTIFICATION_UUID   = UUID.fromString("229B3003-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PWM_CONFIG_UUID               = UUID.fromString("229B3004-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PWM_PARAMETER_UUID            = UUID.fromString("229B3005-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_PWM_DUTY_UUID                 = UUID.fromString("229B3006-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_ANALOG_DRIVE_UUID             = UUID.fromString("229B3007-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_ANALOG_READ_0_UUID            = UUID.fromString("229B3008-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_ANALOG_READ_1_UUID            = UUID.fromString("229B3009-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_ANALOG_READ_2_UUID            = UUID.fromString("229B300A-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_I2C_CONFIG_UUID               = UUID.fromString("229B300B-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_I2C_START_STOP_UUID           = UUID.fromString("229B300C-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_I2C_WRITE_UUID                = UUID.fromString("229B300D-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_I2C_READ_PARAMETER_UUID       = UUID.fromString("229B300E-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_I2C_READ_UUID                 = UUID.fromString("229B300F-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_UART_CONFIG_UUID              = UUID.fromString("229B3010-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_UART_BAUD_RATE_UUID           = UUID.fromString("229B3011-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_UART_TX_UUID                  = UUID.fromString("229B3012-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_UART_RX_NOTIFICATION_UUID     = UUID.fromString("229B3013-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_HARDWARE_RESET_UUID           = UUID.fromString("229B3014-03FB-40DA-98A7-B0DEF65C2D4B");
    private static final UUID KONASHI_LOW_BATTERY_NOTIFICATION_UUID = UUID.fromString("229B3015-03FB-40DA-98A7-B0DEF65C2D4B");

    private static final byte KOSHIAN_I2C_MODE_ENABLE_100K  = 0x01;
    private static final byte KONASHI_I2C_CONDITION_STOP    = 0x00;
    private static final byte KONASHI_I2C_CONDITION_START   = 0x01;
    private static final byte KONASHI_I2C_CONDITION_RESTART = 0x02;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCallback mBluetoothGattCallback;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic mCharaPioSetting;
    private BluetoothGattCharacteristic mCharaPioOutput;
    private BluetoothGattCharacteristic mCharaPioInputNotification;
    private BluetoothGattCharacteristic mCharaI2cConfig;
    private BluetoothGattCharacteristic mCharaI2cStartStop;
    private BluetoothGattCharacteristic mCharaI2cWrite;
    private BluetoothGattCharacteristic mCharaI2cReadParameter;
    private BluetoothGattCharacteristic mCharaI2cRead;
    private BluetoothGattCharacteristic mCharaHardwareReset;

    private Handler mHandler;
    private Context context;

    private enum I2C_DIRECTION {
        READ,
        WRITE
    }

    private class I2cArgument {
        public int slaveAddress;
        public int address;
        public int length;
        public I2C_DIRECTION direction;
        public byte[] writeData;
    }

    private State mI2cCurrentState;

    private State I2cStateInit;
    private State I2cStateStartCondition;
    private State I2cStateRestartCondition;
    private State I2cStateStopCondition;
    private State I2cStateAddress;
    private State I2cStateAddressAndData;
    private State I2cStateReadDataRequest;
    private State I2cStateReadData;

    public Func<byte[]> onReadListener;
    public Action onWriteListener;
    public Action onDisconnectListener;
    public Action onConnectListener;

    public KonashiManager(Context context) {
        mHandler = new Handler();
        this.context = context;

        // BLE
        mBluetoothManager = (BluetoothManager)this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                // if connected successfully
                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE", "connected");
                    gatt.discoverServices();
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE", "disconnected");
                    onDisconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
            }
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "BluetoothGatt.GATT_SUCCESS");
                    BluetoothGattService service = gatt.getService(KONASHI_SERVICE_UUID);
                    if (service != null) {
                        Log.d("BLE", "service is found.");
                        mCharaI2cConfig = service.getCharacteristic(KONASHI_I2C_CONFIG_UUID);
                        mCharaI2cStartStop = service.getCharacteristic(KONASHI_I2C_START_STOP_UUID);
                        mCharaI2cWrite = service.getCharacteristic(KONASHI_I2C_WRITE_UUID);
                        mCharaI2cReadParameter = service.getCharacteristic(KONASHI_I2C_READ_PARAMETER_UUID);
                        mCharaI2cRead = service.getCharacteristic(KONASHI_I2C_READ_UUID);
                        mCharaPioOutput = service.getCharacteristic(KONASHI_PIO_OUTPUT_UUID);
                        mCharaPioSetting = service.getCharacteristic(KONASHI_PIO_SETTING_UUID);
                        mCharaHardwareReset = service.getCharacteristic(KONASHI_HARDWARE_RESET_UUID);
                        mCharaPioInputNotification = service.getCharacteristic(KONASHI_PIO_INPUT_NOTIFICATION_UUID);
                        onConnect();

                        if (mCharaI2cConfig != null) {
                            Log.d("BLE", "KONASHI_I2C_CONFIG_UUID");
                        }
                        if (mCharaI2cStartStop != null) {
                            Log.d("BLE", "KONASHI_I2C_START_STOP_UUID");
                        }
                        if (mCharaI2cWrite != null) {
                            Log.d("BLE", "KONASHI_I2C_WRITE_UUID");
                        }
                        if (mCharaI2cReadParameter != null) {
                            Log.d("BLE", "KONASHI_I2C_READ_PARAMETER_UUID");
                        }
                        if (mCharaI2cRead != null) {
                            Log.d("BLE", "KONASHI_I2C_READ_UUID");
                        }
                        if (mCharaPioSetting != null) {
                            Log.d("BLE", "KONASHI_PIO_SETTING_UUID");
                        }
                        if (mCharaPioOutput != null) {
                            Log.d("BLE", "KONASHI_PIO_OUTPUT_UUID");
                        }
                        if (mCharaPioInputNotification != null) {
                             boolean registered = gatt.setCharacteristicNotification(mCharaPioInputNotification, true);
                             BluetoothGattDescriptor descriptor = mCharaPioInputNotification.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                             descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                             gatt.writeDescriptor(descriptor);
                             if (registered) {
                                 // Characteristics通知設定が成功
                             } else {
                                 Log.d("BLE", "Set notification is failed");
                             }
                        }
                    }
                }
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d("BLE", "onCharacteristicChanged");
                if (characteristic.getUuid().equals(KONASHI_PIO_INPUT_NOTIFICATION_UUID)) {
                    Byte value = characteristic.getValue()[0];
                    if (mCharaPioOutput != null && mCharaPioSetting != null) {
                        mCharaPioSetting.setValue(new byte[] { (byte)0x1e });
                        gatt.writeCharacteristic(mCharaPioSetting);
                        mCharaPioOutput.setValue(new byte[] { (byte)0x1e });
                        gatt.writeCharacteristic(mCharaPioOutput);
                        Log.d("BLE", "KONASHI_PIO_SETTING_UUID");
                        Log.d("BLE", "KONASHI_PIO_OUTPUT_UUID");
                    }
                    Log.d("BLE", String.valueOf(value));
                }
                else {
                    Log.d("BLE", characteristic.getUuid().toString());
                }
            }
            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d("BLE", "onCharacteristicWrite");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mI2cCurrentState = mI2cCurrentState.getNextState();
                    if (mI2cCurrentState != null) {
                        mI2cCurrentState.execute();
                    }
                    else {
                        onWrite();
                    }
                }
            }
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d("BLE", "onCharacteristicRead");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mI2cCurrentState = mI2cCurrentState.getNextState();
                    if (mI2cCurrentState != null) {
                        mI2cCurrentState.execute();
                    }
                    else {
                        onRead(characteristic.getValue());
                    }
                }
            }
        };

        I2cStateInit = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                I2cStateStartCondition.argument = this.argument;
                return I2cStateStartCondition;
            }
            @Override
            public void execute() {
                Log.d("BLE", "I2C Init");
                mCharaI2cConfig.setValue(new byte[] { KOSHIAN_I2C_MODE_ENABLE_100K });
                mBluetoothGatt.writeCharacteristic(mCharaI2cConfig);
            }
        };
        I2cStateStartCondition = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                if (this.argument.direction == I2C_DIRECTION.READ) {
                    I2cStateAddress.argument = this.argument;
                    return I2cStateAddress;
                }
                else {
                    I2cStateAddressAndData.argument = this.argument;
                    return I2cStateAddressAndData;
                }
            }
            @Override
            public void execute() {
                Log.d("BLE", "Start Condition");
                mCharaI2cStartStop.setValue(new byte[] { KONASHI_I2C_CONDITION_START });
                mBluetoothGatt.writeCharacteristic(mCharaI2cStartStop);
            }
        };
        I2cStateRestartCondition = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                I2cStateReadDataRequest.argument = this.argument;
                return I2cStateReadDataRequest;
            }
            @Override
            public void execute() {
                Log.d("BLE", "Restart Condition");
                mCharaI2cStartStop.setValue(new byte[] { KONASHI_I2C_CONDITION_RESTART });
                mBluetoothGatt.writeCharacteristic(mCharaI2cStartStop);
            }
        };
        I2cStateStopCondition = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                if (this.argument.direction == I2C_DIRECTION.READ) {
                    I2cStateReadData.argument = this.argument;
                    return I2cStateReadData;
                }
                else {
                    return null;
                }
            }
            @Override
            public void execute() {
                Log.d("BLE", "Stop Condition");
                mCharaI2cStartStop.setValue(new byte[] { KONASHI_I2C_CONDITION_STOP });
                mBluetoothGatt.writeCharacteristic(mCharaI2cStartStop);
            }
        };
        I2cStateAddress = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                I2cStateRestartCondition.argument = this.argument;
                return I2cStateRestartCondition;
            }
            @Override
            public void execute() {
                Log.d("BLE", "Regsister Address");
                mCharaI2cWrite.setValue(new byte[] { 2, (byte)((this.argument.slaveAddress << 1) & 0xFE), (byte)this.argument.address });
                mBluetoothGatt.writeCharacteristic(mCharaI2cWrite);
            }
        };
        I2cStateAddressAndData = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                I2cStateStopCondition.argument = this.argument;
                return I2cStateStopCondition;
            }
            @Override
            public void execute() {
                Log.d("BLE", "Address and Data");
                ByteBuffer buf = ByteBuffer.allocate(this.argument.length + 3);
                buf.put(new byte[] { (byte)(this.argument.length + 2), (byte)((this.argument.slaveAddress << 1) & 0xFE), (byte)this.argument.address });
                buf.put(this.argument.writeData);
                mCharaI2cWrite.setValue(buf.array());
                mBluetoothGatt.writeCharacteristic(mCharaI2cWrite);
            }
        };
        I2cStateReadDataRequest = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                I2cStateStopCondition.argument = this.argument;
                return I2cStateStopCondition;
            }
            @Override
            public void execute() {
                Log.d("BLE", "Read Data Request");
                mCharaI2cReadParameter.setValue(new byte[] { (byte)this.argument.length, (byte)(this.argument.address << 1 | 0x01) });
                mBluetoothGatt.writeCharacteristic(mCharaI2cReadParameter);
            }
        };
        I2cStateReadData = new State<I2cArgument>() {
            @Override
            public State getNextState() {
                return null;
            }
            @Override
            public void execute() {
                Log.d("BLE", "Read Data");
                mBluetoothGatt.readCharacteristic(mCharaI2cRead);
            }
        };
    }

    // BLEスキャンした際のコールバック
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.d("BLE", "RSSI=" + rssi);
        Log.d("BLE", "ADDRESS=" + device.getAddress());
        Log.d("BLE", "NAME=" + device.getName());
        if (device.getName().indexOf("Koshian") != -1) {
            mBluetoothAdapter.stopLeScan(this);
            mBluetoothGatt = device.connectGatt(this.context, false, mBluetoothGattCallback);
        }
    }

    public void startLeScan() {
        // BLEスキャンのタイムアウト
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(KonashiManager.this);
            }
        }, SCAN_PERIOD);
        mBluetoothAdapter.startLeScan(this);
    }

    public void stopLeScan() {
        mBluetoothAdapter.stopLeScan(this);
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            if (mCharaPioOutput != null) {
                mCharaPioOutput.setValue(new byte[] { (byte)0x0 });
                mBluetoothGatt.writeCharacteristic(mCharaPioOutput);
            }
            if (mCharaHardwareReset != null) {
                mCharaHardwareReset.setValue(new byte[]{ 1 });
                mBluetoothGatt.writeCharacteristic(mCharaHardwareReset);
            }
            mBluetoothGatt.disconnect();
        }
    }

    public void readRegister(int slaveAddress, int address, int length, Func<byte[]> callback) {
        I2cArgument arg = new I2cArgument();
        arg.slaveAddress = slaveAddress;
        arg.direction = I2C_DIRECTION.READ;
        arg.address = address;
        arg.length = length;
        onReadListener = callback;

        mI2cCurrentState = I2cStateInit;
        mI2cCurrentState.argument = arg;
        Log.d("BLE", "readRegister");
        mI2cCurrentState.execute();
    }

    public void writeRegister(int slaveAddress, int address, byte[] writeData, int length, Action callback) {
        I2cArgument arg = new I2cArgument();
        arg.slaveAddress = slaveAddress;
        arg.direction = I2C_DIRECTION.WRITE;
        arg.address = address;
        arg.writeData = writeData;
        arg.length = length;
        onWriteListener = callback;

        mI2cCurrentState = I2cStateInit;
        mI2cCurrentState.argument = arg;
        Log.d("BLE", "writeRegister");
        mI2cCurrentState.execute();
    }

    private void onRead(byte[] readData) {
        if (onReadListener != null) {
            onReadListener.execute(readData);
        }
    }

    private void onWrite() {
        if (onWriteListener != null) {
            onWriteListener.execute();
        }
    }

    private void onDisconnect() {
        if (onDisconnectListener != null) {
            onDisconnectListener.execute();
        }
    }

    private void onConnect() {
        if (onConnectListener != null) {
            onConnectListener.execute();
        }
    }
}
