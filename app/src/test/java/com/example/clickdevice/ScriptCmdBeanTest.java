package com.example.clickdevice;

import com.example.clickdevice.bean.ScriptCmdBean;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScriptCmdBeanTest {

    @Test
    public void buildDelayedCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildDelayedCMD(500);
        assertEquals(ScriptCmdBean.ACTION_DELAYED, cmd.getAction());
        assertEquals(500, cmd.getDelayed());
        assertTrue(cmd.getContent().contains("500"));
    }

    @Test
    public void buildClickCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildClickCMD(100, 200, 50, 300);
        assertEquals(ScriptCmdBean.ACTION_CLICK, cmd.getAction());
        assertEquals(100, cmd.getX0());
        assertEquals(200, cmd.getY0());
        assertEquals(50, cmd.getDuration());
        assertEquals(300, cmd.getDelayed());
        assertTrue(cmd.getContent().contains("100"));
        assertTrue(cmd.getContent().contains("200"));
    }

    @Test
    public void buildGestureCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildGestureCMD(10, 20, 30, 40, 100, 200);
        assertEquals(ScriptCmdBean.ACTION_GESTURE, cmd.getAction());
        assertEquals(10, cmd.getX0());
        assertEquals(20, cmd.getY0());
        assertEquals(30, cmd.getX1());
        assertEquals(40, cmd.getY1());
        assertEquals(100, cmd.getDuration());
        assertEquals(200, cmd.getDelayed());
    }

    @Test
    public void buildRandomClickCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildRandomClickCMD(10, 20, 30, 40, 50, 60);
        assertEquals(ScriptCmdBean.ACTION_RANDOM_CLICK, cmd.getAction());
        assertEquals(10, cmd.getX0());
        assertEquals(20, cmd.getY0());
        assertEquals(30, cmd.getX1());
        assertEquals(40, cmd.getY1());
        assertEquals(50, cmd.getDuration());
        assertEquals(60, cmd.getDelayed());
    }

    @Test
    public void buildNoneCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildNoneCMD();
        assertEquals(ScriptCmdBean.ACTION_NONE, cmd.getAction());
        assertNotNull(cmd.getContent());
    }

    @Test
    public void buildForCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildForCMD(5);
        assertEquals(ScriptCmdBean.ACTION_FOR, cmd.getAction());
        assertEquals(5, cmd.getFrequency());
    }

    @Test
    public void buildForCMD_negativeFrequency_clampsTo1() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildForCMD(-1);
        assertEquals(1, cmd.getFrequency());
    }

    @Test
    public void buildForEndCMD_setsCorrectFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildForEndCMD();
        assertEquals(ScriptCmdBean.ACTION_FOR_END, cmd.getAction());
    }

    @Test
    public void getActionTypeName_returnsCorrectNames() {
        assertEquals("无命令", ScriptCmdBean.BuildNoneCMD().getActionTypeName());
        assertEquals("延时", ScriptCmdBean.BuildDelayedCMD(100).getActionTypeName());
        assertEquals("点击", ScriptCmdBean.BuildClickCMD(0, 0, 0, 0).getActionTypeName());
        assertEquals("手势", ScriptCmdBean.BuildGestureCMD(0, 0, 0, 0, 0, 0).getActionTypeName());
        assertEquals("循环开始", ScriptCmdBean.BuildForCMD(1).getActionTypeName());
        assertEquals("循环结束", ScriptCmdBean.BuildForEndCMD().getActionTypeName());
        assertEquals("随机位置点击", ScriptCmdBean.BuildRandomClickCMD(0, 0, 0, 0, 0, 0).getActionTypeName());
    }

    @Test
    public void toString_containsKeyFields() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildClickCMD(100, 200, 50, 300);
        String str = cmd.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("200"));
    }

    @Test
    public void setAndGetContent_works() {
        ScriptCmdBean cmd = ScriptCmdBean.BuildNoneCMD();
        cmd.setContent("test content");
        assertEquals("test content", cmd.getContent());
    }
}
