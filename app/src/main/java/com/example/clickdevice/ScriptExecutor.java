package com.example.clickdevice;

import com.example.clickdevice.bean.ScriptCmdBean;

import java.util.List;
import java.util.Stack;

public class ScriptExecutor {
    private ScriptInterFace scriptInterFace;
    private Stack<ForStart> stack = new Stack<>();

    public ScriptExecutor(ScriptInterFace scriptInterFace) {
        this.scriptInterFace = scriptInterFace;
    }

    public ScriptInterFace getScriptInterFace() {
        return scriptInterFace;
    }

    public void setScriptInterFace(ScriptInterFace scriptInterFace) {
        this.scriptInterFace = scriptInterFace;
    }


    public void run(List<ScriptCmdBean> list) {
        int size = list.size();
        boolean isJump = false;
        for (int i = 0; i < size; i++) {
            ScriptCmdBean scriptCmdBean = list.get(i);
            if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_FOR_END) {
                isJump = false;
                if (!stack.empty()) {
                    ForStart forStart = stack.pop();
                    if (forStart.num > 0) {
                        i = forStart.index;
                        forStart.num--;
                        stack.push(forStart);
                    }
                }
            } else {
                if (isJump) {
                    continue;
                }
                if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_FOR) {
                    int f = scriptCmdBean.getFrequency();
                    if (f == 0) {
                        isJump = true;
                        continue;
                    }
                    ForStart forStart = new ForStart(i, --f);
                    stack.push(forStart);
                } else {
                    try {
                        run(scriptCmdBean);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void run(ScriptCmdBean scriptCmdBean) throws InterruptedException {
        if (scriptCmdBean == null || scriptInterFace == null) {
            return;
        }
        if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_DELAYED) {
            scriptInterFace.delayedCmd(scriptCmdBean.getDelayed());
        }
        if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_CLICK) {
            scriptInterFace.delayedCmd(scriptCmdBean.getDelayed());
            scriptInterFace.clickCMD(scriptCmdBean.getX0(), scriptCmdBean.getY0(), scriptCmdBean.getDuration());
        }
        if (scriptCmdBean.getAction() == ScriptCmdBean.ACTION_GESTURE) {
            scriptInterFace.delayedCmd(scriptCmdBean.getDelayed());
            scriptInterFace.gestureCMD(scriptCmdBean.getX0(), scriptCmdBean.getY0()
                    , scriptCmdBean.getX1(), scriptCmdBean.getY1()
                    , scriptCmdBean.getDuration());
        }
    }

    private class ForStart {
        private int index;
        private int num;

        public ForStart(int index, int num) {
            this.index = index;
            this.num = num;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }
    }

    public interface ScriptInterFace {
        void delayedCmd(int delayed) throws InterruptedException;

        void clickCMD(int x0, int y0, int duration) throws InterruptedException;

        void gestureCMD(int x0, int y0, int x1, int y1, int duration) throws InterruptedException;

    }

}
