package com.example.enactusapp.Event;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class CalibrationEvent {

    private boolean startCalibration;

    public CalibrationEvent(boolean startCalibration) {
        this.startCalibration = startCalibration;
    }

    public boolean isStartCalibration() {
        return startCalibration;
    }

    public void setStartCalibration(boolean startCalibration) {
        this.startCalibration = startCalibration;
    }
}
