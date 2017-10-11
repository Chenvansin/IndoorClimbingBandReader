package com.hepthst.indoorclimbingbandreader.model;
/**
 * Created by Jiesean on 2017/3/1.
 */

public class StepParser {
    private int stepNum;
    private byte[] data;

    //recreate method
    public StepParser(byte[] data){
        this.data = data;
        parseData();
    }

    private void parseData(){
        if (data.length == 4) {
            //data is intergrity
            stepNum = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
        }
    }

    public int getStepNum(){
        return stepNum;
    }

}

