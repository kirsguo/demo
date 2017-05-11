package com.caeri.v2x.ldm;

import com.caeri.v2x.comm.BSMChoosePhase;
import com.caeri.v2x.comm.BSMLane;
import com.caeri.v2x.comm.BSMLink;
import com.caeri.v2x.comm.BSMMovement;
import com.caeri.v2x.comm.BSMNode;
import com.caeri.v2x.comm.BSMVehicle;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created on 4/23/023.
 *
 * @author Benjamin
 */

public class Me {
    private BSMVehicle vehicle;
    private BSMLane lane;
    private BSMLink link;
    private BSMNode frontNode;
    private BSMNode backNode;
    private BSMChoosePhase choosePhase;
    private ArrayList<BSMMovement> movements;
    private HashMap<Byte, BSMNode> nodes;

    public BSMNode getFrontNode() {
        return frontNode;
    }

    public BSMNode getBackNode() {
        return backNode;
    }

    public HashMap<Byte, BSMNode> getNodes() {
        return nodes;
    }

    public BSMVehicle getVehicle() {
        return vehicle;
    }

    public BSMLane getLane() {
        return lane;
    }

    public BSMLink getLink() {
        return link;
    }

    public BSMChoosePhase getChoosePhase() {
        return choosePhase;
    }

    public ArrayList<BSMMovement> getMovements() {
        return movements;
    }

    public void setFrontNode(BSMNode frontNode) {
        this.frontNode = frontNode;
    }

    public void setBackNode(BSMNode backNode) {
        this.backNode = backNode;
    }

    public void setNodes(HashMap<Byte, BSMNode> nodes) {
        this.nodes = nodes;
    }

    void setVehicle(BSMVehicle vehicle) {
        this.vehicle = vehicle;
    }

    void setLane(BSMLane lane) {
        this.lane = lane;
    }

    void setLink(BSMLink link) {
        this.link = link;
    }

    void setChoosePhase(BSMChoosePhase choosePhase) {
        this.choosePhase = choosePhase;
    }

    void setMovements(ArrayList<BSMMovement> movements) {
        this.movements = movements;
    }
}
