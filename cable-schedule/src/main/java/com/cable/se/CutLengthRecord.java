package com.cable.se;

public class CutLengthRecord {

	public String orderingTagNumber; // orderingTagNumber : Tag Number with suffix (e.g. -1, -2, -3)
	public String orgTagNumber;
	public double cableLength; // cableLength : with 2% wastage buffer
	public double diameterSize; // cableSize : Cable Size (Sq. mm)
	public double core;
	public String cableType; // cableType
	public String orderingDrumNumber = ""; // orderingDrumNumber: Planned Drum Number assigned during drum schedule
											// generation
	public double actualLength; // actualLength : Actual length after drum schedule/dispatch
	public String manufacturerDrumNumber = ""; // manufacturerDrumNo

	
	/*
	 * filed name - slitStatus chnages to Cutting Status => Yet to be cut (No) or
	 * Already Cut (Yes)
	 */
	public String cuttingStatus = "yetToBeCut";
	
	/*
	 * Drum Assignment Status - drumAssigned (Drum Assigned) or unAssigned
	 * (Un-Assinged/Leftover)
	 */
	public String DrumAssignmentStatus = "unAssigned"; 
	public String status = "Active";        // Status : Active or Inactive 
	public Double wastage = 0.0;           // Wastage :by User

	@Override
	public String toString() {

		return "CutLengthRecord{" + "orderingTagNumber='" + orderingTagNumber + '\'' + ", orgTagNumber='" + orgTagNumber
				+ '\'' + ", cableLength=" + cableLength + ", diameterSize=" + diameterSize + ", core=" + core
				+ ", cableType='" + cableType + '\'' + ", orderingDrumNumber='" + orderingDrumNumber + '\'' +

				", actualLength='" + actualLength + '\'' + ", CuttingStatus='" + cuttingStatus + '\''
				+ ", manufacturerDrumNumber='" + manufacturerDrumNumber + '\'' + ", DrumAssignmentStatus='"
				+ DrumAssignmentStatus + '\'' + ", status='" + status + '\'' + ", wastage='" + wastage + '\'' +

				'}';
	}
}
