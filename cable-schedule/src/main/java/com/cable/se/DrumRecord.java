package com.cable.se;

public class DrumRecord {
    public String cableCombination;     //cableCombination	
    public String drumNo;               //drumNo
    public double exactOrderedLength;   //Total sum of cuts
    public int totalPieces;             //totalPieces
    public String cutDetails;           // cutDetails
    public double maxDrumLimit; 
    
    public double drumBalancedLength;       // drumBalancedLength :Remaining buffer length = drumActualLength - exactOrderedLength
    public String manufacturerDrumNo = ""; // Vendor Drum Tag Number
    public double manufacturerActualDrumLength;
    
    @Override
    public String toString() {
        return "DrumRecord{" +
                "cableCombination='" + cableCombination + '\'' +
                ", drumNo='" + drumNo + '\'' +
                ", exactOrderedLength=" + exactOrderedLength +
                ", totalPieces=" + totalPieces +
                ", cutDetails='" + cutDetails + '\'' +
                ", maxDrumLimit=" + maxDrumLimit +
                ", drumBalancedLength=" + drumBalancedLength +
                ", manufacturerDrumNo=" + manufacturerDrumNo +
                 ", manufacturerActualDrumLength=" + manufacturerActualDrumLength +
                '}';
    }
}