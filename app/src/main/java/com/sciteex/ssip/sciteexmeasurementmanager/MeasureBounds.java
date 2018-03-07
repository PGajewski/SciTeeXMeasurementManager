package com.sciteex.ssip.sciteexmeasurementmanager;

/**
 * Created by Gajos on 11/10/2017.
 */

public class MeasureBounds {

    private float min;
    private float max;

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public void setMin(float min) {
        this.min = min;
    }

    @Override
    public boolean equals(Object o) {
        if(o != null && o instanceof MeasureBounds) {
            MeasureBounds bounds = (MeasureBounds) o;
            return (min == bounds.min) && (max == bounds.max);
        }
        else return false;
    }

    public MeasureBounds(float min, float max)
    {
        setMin(min);
        setMax(max);
    }

}
