package com.hepthst.indoorclimbingbandreader.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import java.util.LinkedList;

import static android.content.ContentValues.TAG;

/**
 * Created by Jiesean on 2017/3/1.
 */

public class CommandPool implements Runnable {

    public enum Type {
        setNotification, read, write
    }

    private Context context;
    private BluetoothGatt gatt;
    private LinkedList<Command> pool;
    private BluetoothGattCharacteristic characteristic;
    private int index = 0;
    private boolean isCompleted = false;
    private boolean isDone = false;
    private Command commandToExc;

    //CommandPool 重构函数
    public CommandPool(Context context, BluetoothGatt gatt) {
        this.gatt = gatt;
        this.context = context;
        pool = new LinkedList<>();
    }
    //添加Command函数（调用了Command Class,内部使用方法均为GET())
    public void addCommand(Type type, byte[] value, BluetoothGattCharacteristic target) {
        Command command = new Command(type, value, target);
        pool.offer(command);
    }

    @Override
    public void run() {
        while (true) {
            //Delay 1s
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (pool.peek() == null) {
                commandToExc = null;
                continue;
                // loop until the return of peek() is not NULL
            } else if (!isDone) {
                //List:pool's head is not NULL and isDone is False statu
                commandToExc = pool.peek();
                //get head element of List to commandToExc
                isDone = execute(commandToExc.getType(), commandToExc.getValue(), commandToExc.getTarget());
                //execute Method is the most important part of this page code
                System.out.println(commandToExc.getId() + "命令结果" + isDone);
            } else if (isCompleted && isDone) {
                System.out.println(commandToExc.getId() + "命令执行完成");

                pool.poll();
                //execute next element of list
                isCompleted = false;
                isDone = false;
                //return defalut value
            }
        }


    }

    private boolean execute(Type type, byte[] value, BluetoothGattCharacteristic target) {
        boolean result = false;
        switch (type) {
            case setNotification:
                result = enableNotification(true, target);
                break;
            case read:
                result = readCharacteristic(target);
                break;
            case write:
                result = writeCharacteristic(target, value);
                break;
        }
        return result;
    }

    //Characteristic Write Operation
    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] command) {
        characteristic.setValue(command);
        boolean result = gatt.writeCharacteristic(characteristic);
        return result;
    }

    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null){
            Log.e(TAG, "do not get GATT instance or characteristic is null");
            return false;
            //if do not get GATT instance or characteristic is null. ending operation
        }
        if (!gatt.setCharacteristicNotification(characteristic, enable)){
            //if System Set-operation is not available , ending
            Log.e(TAG, "System Set-operation is not available");
            return false;
        }
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(Profile.notificationDesUUID);
        //research setNotification Descriptor
        if (clientConfig == null){
            //Not found given UUID
            Log.e(TAG, "Not found given UUID");
            return false;
        }
        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return gatt.writeDescriptor(clientConfig);
        //write clientConfig value to gatt
    }

    //Characteristic Read Operation
    private boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        boolean result = gatt.readCharacteristic(characteristic);
        return result;
    }

    //Callback method after Complete
    public void onCommandCallbackComplete() {
        isCompleted = true;
    }

    private class Command {
        private int id;
        private boolean state = false;
        private byte[] value;
        private Type type;
        private BluetoothGattCharacteristic target;

        Command(Type type, byte[] value, BluetoothGattCharacteristic target) {
            this.value = value;
            this.target = target;
            this.type = type;
            id = index;
            System.out.println(index + "命令创建，UUID: " + target.getUuid().toString());
            index++;

        }

        int getId() {
            return id;
        }

        void setSsate(boolean state) {
            this.state = state;
        }

        boolean getState() {
            return state;
        }

        BluetoothGattCharacteristic getTarget() {
            return target;
        }

        byte[] getValue() {
            return value;
        }

        Type getType() {
            return type;
        }
    }

}
