package com.cable.se;

public class CutLengthRecord {
    public String orderingTagNumber;                  //orderingTagNumber : Tag Number with suffix (e.g. -1, -2, -3)
    public String orgTagNumber;
    public double cableLength;                       // cableLength : with 2% wastage buffer
    public double diameterSize;                     //cableSize : Cable Size (Sq. mm)
    public double core;
    public String cableType;                      //cableType 
    public String orderingDrumNumber = "";       //orderingDrumNumber:  Planned Drum Number assigned during drum schedule generation
   
    public double actualLength;                 //actualLength : Actual length after drum schedule/dispatch
    public String slitStatus    = "No";        // slitStatus: Status (Slit)  : Default "No" 
    public String manufacturerDrumNumber = ""; // manufacturerDrumNo 
   
    
    public String executionStatus;
    public boolean shortageAlert = false; // shortageAlert : True if actualLength > cableLength
    public String slit = "";
    public String cutLength = "";
    public String cutFromWhichDrum = "";

    @Override
    public String toString() {
        return "CutLengthRecord{" +
                "orderingTagNumber='" + orderingTagNumber + '\'' +
                ", orgTagNumber='" + orgTagNumber + '\'' +
                ", cableLength=" + cableLength +
                ", diameterSize=" + diameterSize +
                ", core=" + core +
                ", cableType='" + cableType + '\'' +
                ", orderingDrumNumber='" + orderingDrumNumber + '\'' +
                
                 ", actualLength='" + actualLength + '\'' +
                  ", slitStatus='" + slitStatus + '\'' +
                   ", manufacturerDrumNumber='" + manufacturerDrumNumber + '\'' +
                '}';
    }
}
