package com.cable.se;

public class CutLengthRecord {
    public String orderingTagNumber;
    public String orgTagNumber;
    public double cableLength; // with 2% wastage buffer
    public double diameterSize;
    public double core;
    public String cableType;
    
    // These columns are left blank in the VBA script, but we prepare them for the Excel writer
    public String orderingDrumNumber = "";
    public String manufacturerDrumNumber = "";
    public String slit = "";
    public String cutLength = "";
    public String cutFromWhichDrum = "";

}
