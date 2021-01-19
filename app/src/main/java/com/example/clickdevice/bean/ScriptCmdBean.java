package com.example.clickdevice.bean;

public class ScriptCmdBean {
    public static int ACTION_DELAYED=1;
    public static int ACTION_CLICK=2;
    public static int ACTION_GESTURE=3;
    private int action;
    private int delayed;
    private int duration;
    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private String content;

    public static ScriptCmdBean BuildDelayedCMD(int delayed){
        ScriptCmdBean scriptCmdBean=new ScriptCmdBean();
        scriptCmdBean.setAction(ACTION_DELAYED);
        scriptCmdBean.setDelayed(delayed);
        scriptCmdBean.setContent("延时"+delayed+"ms");
        return scriptCmdBean;
    }

    public static ScriptCmdBean BuildClickCMD(int x0,int y0,int duration){
        ScriptCmdBean scriptCmdBean=new ScriptCmdBean();
        scriptCmdBean.setAction(ACTION_CLICK);
        scriptCmdBean.setX0(x0);
        scriptCmdBean.setY0(y0);
        scriptCmdBean.setDuration(duration);
        scriptCmdBean.setContent("点击坐标("+x0+","+y0+")执行时长"+duration+"ms");
        return scriptCmdBean;
    }

    public static ScriptCmdBean BuildGestureCMD(int x0,int y0,int x1,int y1,int duration){
        ScriptCmdBean scriptCmdBean=new ScriptCmdBean();
        scriptCmdBean.setAction(ACTION_GESTURE);
        scriptCmdBean.setX0(x0);
        scriptCmdBean.setY0(y0);
        scriptCmdBean.setX1(x1);
        scriptCmdBean.setY1(y1);
        scriptCmdBean.setDuration(duration);
        scriptCmdBean.setContent("滑动手势从坐标("+x0+","+y0+")到("+x1+","+y1+")执行时长"+duration+"ms");
        return scriptCmdBean;
    }

    private ScriptCmdBean(){

    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getDelayed() {
        return delayed;
    }

    public void setDelayed(int delayed) {
        this.delayed = delayed;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getX0() {
        return x0;
    }

    public void setX0(int x0) {
        this.x0 = x0;
    }

    public int getY0() {
        return y0;
    }

    public void setY0(int y0) {
        this.y0 = y0;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ScriptCmdBean{" +
                "action=" + action +
                ", delayed=" + delayed +
                ", duration=" + duration +
                ", x0=" + x0 +
                ", y0=" + y0 +
                ", x1=" + x1 +
                ", y2=" + y1 +
                ", content='" + content + '\'' +
                '}';
    }
}
