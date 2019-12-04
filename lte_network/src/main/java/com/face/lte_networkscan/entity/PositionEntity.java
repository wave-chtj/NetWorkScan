package com.face.lte_networkscan.entity;

public class PositionEntity {
    private int cycleIntervalPosition;
    private int  errScanCountPosition;

    public PositionEntity() {
    }

    public PositionEntity(int cycleIntervalPosition, int errScanCountPosition) {
        this.cycleIntervalPosition = cycleIntervalPosition;
        this.errScanCountPosition = errScanCountPosition;
    }

    public int getCycleIntervalPosition() {
        return cycleIntervalPosition;
    }

    public void setCycleIntervalPosition(int cycleIntervalPosition) {
        this.cycleIntervalPosition = cycleIntervalPosition;
    }

    public int getErrScanCountPosition() {
        return errScanCountPosition;
    }

    public void setErrScanCountPosition(int errScanCountPosition) {
        this.errScanCountPosition = errScanCountPosition;
    }
}
