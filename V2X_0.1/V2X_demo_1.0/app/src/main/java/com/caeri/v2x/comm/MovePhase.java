package com.caeri.v2x.comm;

/**
 * Created by wind on 2017/4/7.
 */
public class MovePhase {
    public byte getFromNodeLocalID() {
        return fromNodeLocalID;
    }

    public byte getToNodeLocalID() {
        return toNodeLocalID;
    }

    public byte getPhaseID() {
        return phaseID;
    }

    byte fromNodeLocalID;//上游路段起点Node的局部ID
    byte toNodeLocalID;//下游路段重点Node局部ID
    byte phaseID;//对应相位的ID
}
