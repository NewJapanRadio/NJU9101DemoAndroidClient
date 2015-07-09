package jp.co.njr.nju9101demo;

import android.util.Log;

public class NJU9101Model
{
    public static final int I2C_SLAVE_ADDRESS = 0x48;
    // Register Address
    private static final int ADR_CTRL      = 0x00;
    private static final int ADR_STATUS    = 0x01;
    private static final int ADR_AMPDATA0  = 0x02;
    private static final int ADR_AMPDATA1  = 0x03;
    private static final int ADR_AUXDATA0  = 0x04;
    private static final int ADR_AUXDATA1  = 0x05;
    private static final int ADR_TMPDATA0  = 0x06;
    private static final int ADR_TMPDATA1  = 0x07;
    private static final int ADR_ID        = 0x08;
    private static final int ADR_ROMADR0   = 0x09;
    private static final int ADR_ROMADR1   = 0x0A;
    private static final int ADR_ROMDATA   = 0x0B;
    private static final int ADR_ROMCTRL   = 0x0C;
    private static final int ADR_TEST      = 0x0D;
    private static final int ADR_ANAGAIN   = 0x0E;
    private static final int ADR_BLKCONN0  = 0x0F;
    private static final int ADR_BLKCONN1  = 0x10;
    private static final int ADR_BLKCONN2  = 0x11;
    private static final int ADR_BLKCTL    = 0x12;
    private static final int ADR_ADCCONV   = 0x13;
    private static final int ADR_SYSPRESET = 0x14;
    private static final int ADR_SCAL1A0   = 0x15;
    private static final int ADR_SCAL1A1   = 0x16;
    private static final int ADR_SCAL2A0   = 0x17;
    private static final int ADR_SCAL2A1   = 0x18;
    private static final int ADR_SCAL3A0   = 0x19;
    private static final int ADR_SCAL3A1   = 0x1A;
    private static final int ADR_SCAL4A0   = 0x1B;
    private static final int ADR_SCAL4A1   = 0x1C;
    private static final int ADR_SCAL1B0   = 0x1D;
    private static final int ADR_SCAL1B1   = 0x1E;
    private static final int ADR_SCAL2B0   = 0x1F;
    private static final int ADR_SCAL2B1   = 0x20;
    private static final int ADR_SCAL3B0   = 0x21;
    private static final int ADR_SCAL3B1   = 0x22;
    private static final int ADR_SCAL4B0   = 0x23;
    private static final int ADR_SCAL4B1   = 0x24;
    private static final int ADR_OCAL1A0   = 0x25;
    private static final int ADR_OCAL1A1   = 0x26;
    private static final int ADR_OCAL2A0   = 0x27;
    private static final int ADR_OCAL2A1   = 0x28;
    private static final int ADR_OCAL3A0   = 0x29;
    private static final int ADR_OCAL3A1   = 0x2A;
    private static final int ADR_OCAL4A0   = 0x2B;
    private static final int ADR_OCAL4A1   = 0x2C;
    private static final int ADR_OCAL1B0   = 0x2D;
    private static final int ADR_OCAL1B1   = 0x2E;
    private static final int ADR_OCAL2B0   = 0x2F;
    private static final int ADR_OCAL2B1   = 0x30;
    private static final int ADR_OCAL3B0   = 0x31;
    private static final int ADR_OCAL3B1   = 0x32;
    private static final int ADR_OCAL4B0   = 0x33;
    private static final int ADR_OCAL4B1   = 0x34;
    private static final int ADR_SCAL1     = 0x35;
    private static final int ADR_SCAL2     = 0x36;
    private static final int ADR_SCAL3     = 0x37;
    private static final int ADR_OCAL1     = 0x38;
    private static final int ADR_OCAL2     = 0x39;
    private static final int ADR_OCAL3     = 0x3A;
    private static final int ADR_AUXSCAL0  = 0x3B;
    private static final int ADR_AUXSCAL1  = 0x3C;
    private static final int ADR_AUXOCAL0  = 0x3D;
    private static final int ADR_AUXOCAL1  = 0x3E;
    private static final int ADR_CHKSUM    = 0x3F;
    // Start conversion command
    public static final int CMD_TMP = 0x08;
    public static final int CMD_AMP = 0x0A;
    public static final int CMD_AUX = 0x0C;

    private KonashiManager mKonashiManager;

    public Func<Double> onTemperatureReadListener;
    public Func<byte[]> onSensorDataReadListener;

    public NJU9101Model(KonashiManager konashiManager) {
        mKonashiManager = konashiManager;
    }

    private double calculateTemperature(int raw) {
        return raw/256.0;
    }

    private void onTemperatureRead(double temperature) {
        if (onTemperatureReadListener != null) {
            onTemperatureReadListener.execute(temperature);
        }
    }
    public void readTemperature() {
        mKonashiManager.writeRegister(I2C_SLAVE_ADDRESS, ADR_CTRL, new byte[] { CMD_TMP }, 1, new Action() {
            @Override
            public void execute() {
                mKonashiManager.readRegister(I2C_SLAVE_ADDRESS, ADR_TMPDATA0, 2, new Func<byte[]>() {
                    @Override
                    public void execute(byte[] readData) {
                        int rawData = ((readData[0] & 0xFF) << 8) + (readData[1] & 0xFF);
                        double temperature = calculateTemperature(rawData);
                        Log.d("BLE", String.valueOf(temperature) + "℃");
                        NJU9101Model.this.onTemperatureRead(temperature);
                    }
                });
            }
        });
    }

    private void onSensorDataRead(byte[] sensorData) {
        if (onSensorDataReadListener != null) {
            onSensorDataReadListener.execute(sensorData);
        }
    }
    public void readSensorData() {
        mKonashiManager.writeRegister(I2C_SLAVE_ADDRESS, ADR_CTRL, new byte[] { CMD_AMP }, 1, new Action() {
            @Override
            public void execute() {
                mKonashiManager.readRegister(I2C_SLAVE_ADDRESS, ADR_AMPDATA0, 2, new Func<byte[]>() {
                    @Override
                    public void execute(byte[] readData) {
                        Log.d("BLE", String.format("0x%04x", ((readData[0] & 0xFF) << 8) + (readData[1] & 0xFF)));
                        NJU9101Model.this.onSensorDataRead(readData);
                    }
                });
            }
        });
    }
}
