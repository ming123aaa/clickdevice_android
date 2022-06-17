package com.example.clickdevice.bean;

import java.util.List;

public class RecordScriptCmd {

    public Type type;
    public List<Bean> path;
    public int delayed;
    public int duration;

    public enum  Type {
        Gesture, Delay
    }

    public static RecordScriptCmd createGestureCMD(List<Bean> path, int duration) {
        RecordScriptCmd recordScriptCmd = new RecordScriptCmd();
        recordScriptCmd.type = Type.Gesture;
        recordScriptCmd.path = path;
        recordScriptCmd.duration = duration;

        return recordScriptCmd;
    }

    public static RecordScriptCmd createDelayCMD(int Delay){
        RecordScriptCmd recordScriptCmd = new RecordScriptCmd();
        recordScriptCmd.type=Type.Delay;
        recordScriptCmd.delayed=Delay;
        return recordScriptCmd;
    }

}
