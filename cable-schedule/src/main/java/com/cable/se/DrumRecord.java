package com.cable.se;

public class DrumRecord {
    public String cableCombination;     //cableCombination	
    public String drumNo;               //drumNo
    public double exactOrderedLength;   //Total sum of cuts
    public int totalPieces;             //totalPieces
    public String cutDetails;           // cutDetails
    public double maxDrumLimit; 
    
    //Display name : Actual Available Length
   // public double drumBalancedLength; chnage to actualAvailableLength
    public double actualAvailableLength;  
  
    public String manufacturerDrumNo = ""; 
    public double manufacturerActualDrumLength;
   
    //Planned Left Over Length
    public double plannedLeftOverLength;
    
    // this is for site execution purpose, to know the cable type, core and diameter size of the drum
    public String cableType;
    public double core;
    public double diameterSize;
    
    @Override
    public String toString() {
        return "DrumRecord{" +
                "cableCombination='" + cableCombination + '\'' +
                ", drumNo='" + drumNo + '\'' +
                ", exactOrderedLength=" + exactOrderedLength +
                ", totalPieces=" + totalPieces +
                ", cutDetails='" + cutDetails + '\'' +
                ", maxDrumLimit=" + maxDrumLimit +
                ", actualAvailableLength=" + actualAvailableLength +
                ", manufacturerDrumNo=" + manufacturerDrumNo +
                 ", manufacturerActualDrumLength=" + manufacturerActualDrumLength +
                  ", plannedLeftOverLength=" + plannedLeftOverLength +
                  ", cableType='" + cableType + '\'' +
                  ", core=" + core +
                  ", diameterSize=" + diameterSize +
                '}';
    }
}