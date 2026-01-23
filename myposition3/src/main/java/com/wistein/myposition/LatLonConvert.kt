package com.wistein.myposition

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/******************************************************************************
 * Kotlin Class: LatLonConvert
 *
 * The original Java class is part of a collection of classes developed for the
 * reading and processing of oceanographic and meteorological data collected
 * since 1970 by environmental buoys and stations.  This dataset is
 * maintained by the National Oceanographic Data Center and is publicly
 * available.  These Java classes were written for the US Environmental
 * Protection Agency's National Exposure Research Laboratory under Contract
 * No. GS-10F-0073K with Neptune and Company of Los Alamos, New Mexico.
 *
 * Purpose:
 * This class contains utilities for latitude/longitude conversions.
 * Specifically, this class performs conversions from lat (or long)
 * in decimal degrees to degrees-minutes-seconds, and vice versa.
 *
 * Inputs:
 * Given a decimal degree, the equivalent degrees/minutes/seconds
 * are calculated, and vice versa, using two methods.
 *
 * Outputs:
 * The outputs are provided as public accessor methods:
 * double getDecimal()	returns decimal degrees
 * double getDegree()	returns degree part of degree/minute/second
 * double getMinute()	returns minute part of degree/minute/second
 * double getSecond()	returns second part of degree/minute/second
 *
 * Author/Company:
 * JDT: Neptune and Company
 *
 * Change log:
 * date         ver    by	  description of change
 * -----------  ----   ---    ---------------------------------------
 * 17 Dec 2001  1.40   JDT	  Version accompanying final deliverable.
 *
 * Adopted 2019 by wistein for MyPosition3,
 * last edited in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-01-23.
 */
internal class LatLonConvert {
    // declare local variables used throughout the class
    private var dfDecimal = 0.0 // decimal degrees
    private var dfDegree = 0.0 // degree part of degrees/minutes/seconds
    private var dfMinute = 0.0 // minute part of degrees/minutes/seconds
    private var dfSecond = 0.0 // second part of degrees/minutes/seconds

    /******************************************************************************
     * method:						LatLonConvert
     * ****************************************************************************
     * The two constructors for LatLonConvert class accept either
     * - a single double, which is interpreted as decimal degrees to be
     * converted to degrees/minutes/seconds, or
     * - three doubles, which are interpreted as values of degrees, minutes,
     * and seconds, respectively, to be converted to decimal degrees.
     * Member of LatLonConvert class
     * --------------------------------------------------------------------------*/
    // This constructor converts decimal degrees to degrees/minutes/seconds
    constructor(dfDecimalIn: Double) {
        // load local variables
        dfDecimal = dfDecimalIn

        // call appropriate conversion method
        fromDec2DMS()
    }

    // This constructor converts degrees/minutes/seconds to decimal degrees
    constructor(dfDegreeIn: Double, dfMinuteIn: Double, dfSecondIn: Double) {
        // load local variables
        dfDegree = dfDegreeIn
        dfMinute = dfMinuteIn
        dfSecond = dfSecondIn

        // call appropriate conversion method
        fromDMS2Dec()
    }

    /******************************************************************************
     * method:					fromDec2DMS()
     * ****************************************************************************
     * Converts decimal degrees to degrees/minutes/seconds.
     * Member of LatLonConvert class
     * --------------------------------------------------------------------------*/
    private fun fromDec2DMS() {
        val dfSec: Double // fraction converted to seconds

        // Get degrees by chopping off at the decimal
        dfDegree = floor(dfDecimal)
        // correction required since floor() is not the same as int()
        if (dfDegree < 0) dfDegree += 1

        // Get fraction after the decimal
        // define variables local to this method
        val dfFrac = abs(dfDecimal - dfDegree) // fraction after decimal

        // Convert this fraction to seconds (without minutes)
        dfSec = dfFrac * 3600

        // Determine number of whole minutes in the fraction
        dfMinute = floor(dfSec / 60)

        // Put the remainder in seconds
        dfSecond = dfSec - dfMinute * 60

        // Fix roundoff errors
        if (round(dfSecond) == 60.toDouble()) {
            dfMinute += 1
            dfSecond = 0.0
        }

        if (round(dfMinute) == 60.toDouble()) {
            dfDegree = if (dfDegree < 0) dfDegree - 1
            else  // ( dfDegree => 0 )
                dfDegree + 1

            dfMinute = 0.0
        }
    }

    /******************************************************************************
     * method:					fromDMS2Dec()
     * ****************************************************************************
     * Converts degrees/minutes/seconds to decimal degrees.
     * Member of LatLonConvert class
     * --------------------------------------------------------------------------*/
    private fun fromDMS2Dec() {
        // Determine fraction from minutes and seconds
        // define variables local to this method
        val dfFrac = dfMinute / 60 + dfSecond / 3600 // fraction after decimal

        // Be careful to get the sign right. dfDegIn is the only signed input.
        dfDecimal = if (dfDegree < 0) dfDegree - dfFrac
        else dfDegree + dfFrac
    }

        /******************************************************************************
         * method:					getDecimal()
         * ****************************************************************************
         * Gets the value in decimal degrees.
         * Member of LatLonConvert class
         * --------------------------------------------------------------------------*/
        val decimal: Double
        get() = (dfDecimal)

        /******************************************************************************
         * method:					getDegree()
         * Gets the degree part of degrees/minutes/seconds.
         * Member of LatLonConvert class
         * --------------------------------------------------------------------------*/
        val degree: Double
        get() = (dfDegree)

        /******************************************************************************
         * method:					getMinute()
         * ****************************************************************************
         * Gets the minute part of degrees/minutes/seconds.
         * Member of LatLonConvert class
         * --------------------------------------------------------------------------*/
        val minute: Double
        get() = (dfMinute)

        /******************************************************************************
         * method:					getSecond()
         * ****************************************************************************
         * Gets the second part of degrees/minutes/seconds.
         * Member of LatLonConvert class
         * --------------------------------------------------------------------------*/
        val second: Double
        get() = (dfSecond)

}
