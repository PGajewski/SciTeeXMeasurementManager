package com.sciteex.ssip.sciteexmeasurementmanager;

/**
 * Created by Gajos on 12/21/2017.
 */

public class OpcUaVariable{
    private int node;
    private String path;
    private char typeChar;

    //Screen positions.
    private float xPosition;
    private float yPosition;
    private int slideNumber;

    //Setters
    public void setNode(int node)
    {
        this.node = node;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void setScreenPosition(float x, float y)
    {
        this.xPosition = x;
        this.yPosition = y;
    }

    public void setSlideNumber(int slide)
    {
        this.slideNumber = slide;
    }

    public void setType(char type)
    {
        this.typeChar = type;
    }

    public char getType()
    {
        return this.typeChar;
    }

    public int getNode()
    {
        return node;
    }

    public String getPath()
    {
        return path;
    }

    public float getXPosition()
    {
        return xPosition;
    }

    public float getYPosition()
    {
        return yPosition;
    }

    public int getSlideNumber()
    {
        return slideNumber;
    }

    public OpcUaVariable()
    {

    }

    public OpcUaVariable(int node, String path)
    {
        setNode(node);
        setPath(path);
    }

    public OpcUaVariable(int node, String path, int slide, float x, float y)
    {
        this(node,path);
        setSlideNumber(slide);
        setScreenPosition(x,y);
    }
}
