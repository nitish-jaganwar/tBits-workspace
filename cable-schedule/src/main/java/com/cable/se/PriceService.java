package com.cable.se;


/**
 * Abstraction for fetching cable unit price.
 */
public interface PriceService {
    /**
     * Returns the unit price (Rs. per meter) for a given cable combination.
     *
     * @param noOfCores     e.g., 3.0, 3.5, 4.0
     * @param cableSizeSqmm e.g., 400.0, 240.0
     * @param condType      e.g., "XLPE,Al" or "XLPE,Cu"
     * @return Unit price per meter, or 0.0 if not found
     */
    double getUnitPrice(double noOfCores, double cableSizeSqmm, String condType);
}