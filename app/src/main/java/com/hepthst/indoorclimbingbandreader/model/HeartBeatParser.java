package com.hepthst.indoorclimbingbandreader.model;

import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by hepthSt on 2017/9/16.
 */

public class HeartBeatParser {
        private int HeartBeatNum;
        private byte[] data;

        public HeartBeatParser(byte[] data){
            this.data = data;
            HeartBeatparseData();
        }

        private void HeartBeatparseData(){
            if (data.length == 2 && data[0] == 6) {
                HeartBeatNum = data[1] & 0xFF;
//                Log.i(TAG, "HeartBeatparseData: "+  HeartBeatNum);
            }
            else
            {
                Log.e(TAG, "HeartBeatparseData: data format is wrong.");
            }
        }

        public int getHeartBeatNum(){
            return HeartBeatNum;
        }


}
