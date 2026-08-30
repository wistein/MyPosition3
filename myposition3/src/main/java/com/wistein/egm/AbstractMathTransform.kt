package com.wistein.egm

/******************************************************************************************
 *    Derived from:
 *    GeoTools - The Open Source Java GIS Toolkit
 *    https://geotools.org
 *
 *    (C) 2001-2015, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 *
 * Provides a default implementation for most methods required by the {MathTransform}
 * interface. `AbstractMathTransform` provides a convenient base class from which other
 * transform classes can be easily derived. In addition, `AbstractMathTransform` implements
 * methods required by the {MathTransform2D} interface, but **does not**
 * implements `MathTransform2D`. Subclasses must declare `implements MathTransform2D`
 * themselves if they know to maps two-dimensional coordinate systems.
 * 
 * @author Martin Desruisseaux (IRD)
 * tutorial link: docs.codehaus.org/display/GEOTOOLS/Coordinate+Transformation+Parameters
 * @since 2.0
 * 
 * Code adaptation for use by MyPositionActivity by wistein
 * last change in Java on 2020-04-17,
 * converted to Kotlin on 2026-07-27,
 * last edited on 2026-08-30.
 */

// Constructs a math transform.
abstract class AbstractMathTransform {
    // Gets the dimension of input points.
    protected abstract val sourceDimensions: Int

    // Gets the dimension of output points.
    protected abstract val targetDimensions: Int

    // Returns a hash value for this transform.
    override fun hashCode(): Int {
        return this.sourceDimensions + 37 * this.targetDimensions
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractMathTransform

        if (sourceDimensions != other.sourceDimensions) return false
        if (targetDimensions != other.targetDimensions) return false

        return true
    }

}
