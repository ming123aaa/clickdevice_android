package com.example.clickdevice;

import com.example.clickdevice.bean.ScriptCmdBean;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ScriptExecutorTest {

    private ScriptExecutor executor;
    private FakeScriptInterFace fakeInterFace;

    static class FakeScriptInterFace implements ScriptExecutor.ScriptInterFace {
        int delayedCallCount;
        int clickCallCount;
        int gestureCallCount;
        int totalCallCount;
        List<int[]> clickArgs = new ArrayList<>();
        List<int[]> gestureArgs = new ArrayList<>();
        List<Integer> delayedArgs = new ArrayList<>();
        boolean shouldThrowOnDelayed = false;

        void reset() {
            delayedCallCount = 0;
            clickCallCount = 0;
            gestureCallCount = 0;
            totalCallCount = 0;
            clickArgs.clear();
            gestureArgs.clear();
            delayedArgs.clear();
            shouldThrowOnDelayed = false;
        }

        @Override
        public boolean isRun() {
            return true;
        }

        @Override
        public void delayedCmd(int delayed) throws InterruptedException {
            delayedCallCount++;
            totalCallCount++;
            delayedArgs.add(delayed);
            if (shouldThrowOnDelayed) throw new InterruptedException("test");
        }

        @Override
        public void clickCMD(int x0, int y0, int duration) throws InterruptedException {
            clickCallCount++;
            totalCallCount++;
            clickArgs.add(new int[]{x0, y0, duration});
        }

        @Override
        public void gestureCMD(int x0, int y0, int x1, int y1, int duration) throws InterruptedException {
            gestureCallCount++;
            totalCallCount++;
            gestureArgs.add(new int[]{x0, y0, x1, y1, duration});
        }
    }

    @Before
    public void setUp() {
        fakeInterFace = new FakeScriptInterFace();
        executor = new ScriptExecutor(fakeInterFace);
    }

    @Test
    public void run_clickCommand_callsDelayedThenClick() throws Exception {
        ScriptCmdBean cmd = ScriptCmdBean.BuildClickCMD(100, 200, 50, 300);
        executor.run(cmd);

        assertEquals(1, fakeInterFace.delayedCallCount);
        assertEquals(300, (int) fakeInterFace.delayedArgs.get(0));
        assertEquals(1, fakeInterFace.clickCallCount);
        assertArrayEquals(new int[]{100, 200, 50}, fakeInterFace.clickArgs.get(0));
    }

    @Test
    public void run_delayedCommand_callsDelayedOnly() throws Exception {
        ScriptCmdBean cmd = ScriptCmdBean.BuildDelayedCMD(500);
        executor.run(cmd);

        assertEquals(1, fakeInterFace.delayedCallCount);
        assertEquals(0, fakeInterFace.clickCallCount);
        assertEquals(0, fakeInterFace.gestureCallCount);
    }

    @Test
    public void run_gestureCommand_callsDelayedThenGesture() throws Exception {
        ScriptCmdBean cmd = ScriptCmdBean.BuildGestureCMD(10, 20, 30, 40, 100, 200);
        executor.run(cmd);

        assertEquals(1, fakeInterFace.delayedCallCount);
        assertEquals(200, (int) fakeInterFace.delayedArgs.get(0));
        assertEquals(1, fakeInterFace.gestureCallCount);
        assertArrayEquals(new int[]{10, 20, 30, 40, 100}, fakeInterFace.gestureArgs.get(0));
    }

    @Test
    public void run_randomClickCommand_callsDelayedThenClickInRange() throws Exception {
        ScriptCmdBean cmd = ScriptCmdBean.BuildRandomClickCMD(100, 200, 300, 400, 50, 100);
        executor.run(cmd);

        assertEquals(1, fakeInterFace.delayedCallCount);
        assertEquals(1, fakeInterFace.clickCallCount);
        int[] args = fakeInterFace.clickArgs.get(0);
        int x = args[0], y = args[1];
        assertTrue("x should be >= min(x0,x1)", x >= 100);
        assertTrue("x should be <= max(x0,x1)", x <= 300);
        assertTrue("y should be >= min(y0,y1)", y >= 200);
        assertTrue("y should be <= max(y0,y1)", y <= 400);
    }

    @Test
    public void run_noneCommand_doesNothing() throws Exception {
        ScriptCmdBean cmd = ScriptCmdBean.BuildNoneCMD();
        executor.run(cmd);

        assertEquals(0, fakeInterFace.totalCallCount);
    }

    @Test
    public void run_nullBean_doesNothing() throws Exception {
        executor.run((ScriptCmdBean) null);
        assertEquals(0, fakeInterFace.totalCallCount);
    }

    @Test
    public void run_nullInterFace_doesNotCrash() throws Exception {
        executor.setScriptInterFace(null);
        ScriptCmdBean cmd = ScriptCmdBean.BuildClickCMD(1, 2, 3, 4);
        executor.run(cmd);
    }

    @Test
    public void run_list_executesAllCommands() {
        List<ScriptCmdBean> list = new ArrayList<>();
        list.add(ScriptCmdBean.BuildDelayedCMD(100));
        list.add(ScriptCmdBean.BuildClickCMD(10, 20, 30, 40));
        list.add(ScriptCmdBean.BuildDelayedCMD(200));

        executor.run(list);

        assertEquals(3, fakeInterFace.delayedCallCount);
        assertEquals(1, fakeInterFace.clickCallCount);
    }

    @Test
    public void run_forLoop_executesCorrectTimes() {
        List<ScriptCmdBean> list = new ArrayList<>();
        list.add(ScriptCmdBean.BuildForCMD(3));
        list.add(ScriptCmdBean.BuildClickCMD(1, 2, 3, 0));
        list.add(ScriptCmdBean.BuildForEndCMD());

        executor.run(list);

        assertEquals(3, fakeInterFace.clickCallCount);
    }

    @Test
    public void run_forLoop_zeroSkipsBody() {
        List<ScriptCmdBean> list = new ArrayList<>();
        list.add(ScriptCmdBean.BuildForCMD(0));
        list.add(ScriptCmdBean.BuildClickCMD(1, 2, 3, 0));
        list.add(ScriptCmdBean.BuildForEndCMD());

        executor.run(list);

        assertEquals(0, fakeInterFace.clickCallCount);
    }

    @Test
    public void run_nestedForLoop_executesCorrectTimes() {
        List<ScriptCmdBean> list = new ArrayList<>();
        list.add(ScriptCmdBean.BuildForCMD(2));
        list.add(ScriptCmdBean.BuildForCMD(3));
        list.add(ScriptCmdBean.BuildDelayedCMD(0));
        list.add(ScriptCmdBean.BuildForEndCMD());
        list.add(ScriptCmdBean.BuildForEndCMD());

        executor.run(list);

        assertEquals(6, fakeInterFace.delayedCallCount);
    }

    @Test
    public void run_interruptedInDelayed_stopsCurrentCommandButContinues() {
        fakeInterFace.shouldThrowOnDelayed = true;
        List<ScriptCmdBean> list = new ArrayList<>();
        list.add(ScriptCmdBean.BuildDelayedCMD(100));
        list.add(ScriptCmdBean.BuildClickCMD(1, 2, 3, 0));

        executor.run(list);

        assertEquals(2, fakeInterFace.delayedCallCount);
        assertEquals(0, fakeInterFace.clickCallCount);
    }

    @Test
    public void getAndSetScriptInterFace_works() {
        FakeScriptInterFace newFace = new FakeScriptInterFace();
        executor.setScriptInterFace(newFace);
        assertSame(newFace, executor.getScriptInterFace());
    }
}
