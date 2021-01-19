package com.example.clickdevice;

import com.example.clickdevice.bean.ScriptCmdBean;

public class ScriptExecutor {
    private ScriptInterFace scriptInterFace;

    public ScriptExecutor(ScriptInterFace scriptInterFace) {
        this.scriptInterFace = scriptInterFace;
    }

    public ScriptInterFace getScriptInterFace() {
        return scriptInterFace;
    }

    public void setScriptInterFace(ScriptInterFace scriptInterFace) {
        this.scriptInterFace = scriptInterFace;
    }

    public void Run(ScriptCmdBean scriptCmdBean) throws InterruptedException {
        if (scriptCmdBean==null||scriptInterFace==null){
            return;
        }
        if (scriptCmdBean.getAction()==ScriptCmdBean.ACTION_DELAYED){
            scriptInterFace.delayedCmd(scriptCmdBean.getDelayed());
        }
        if (scriptCmdBean.getAction()==ScriptCmdBean.ACTION_CLICK){
            scriptInterFace.clickCMD(scriptCmdBean.getX0(),scriptCmdBean.getY0(),scriptCmdBean.getDuration());
        }
        if (scriptCmdBean.getAction()==ScriptCmdBean.ACTION_GESTURE){
            scriptInterFace.gestureCMD(scriptCmdBean.getX0(),scriptCmdBean.getY0()
                    ,scriptCmdBean.getX1(),scriptCmdBean.getY1()
                    ,scriptCmdBean.getDuration());
        }
    }


    public interface ScriptInterFace{
       void delayedCmd(int delayed) throws InterruptedException;
       void clickCMD(int x0,int y0,int duration) throws InterruptedException;
       void gestureCMD(int x0,int y0,int x1,int y1,int duration) throws InterruptedException;

    }

}
