package com.wistein.egm;

import android.content.Context;

import com.wistein.myposition.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

/******************
 *    Derived from:
 *    GeoTools - The Open Source Java GIS Toolkit
 *    https://geotools.org
 * <p>
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 * <p>
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 * <p>
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 * <p>
 *    The original file is derived from NGA/NASA software available for unlimited distribution.
 *    See https://earth-info.nima.mil/GandG/wgs84/gravitymod/.
 * <p>
 * Transforms vertical coordinates using coefficients from the
 * https://earth-info.nima.mil/GandG/wgs84/gravitymod/wgs84_180/wgs84_180.html
 * Earth Gravitational Model.
 *
 * @author Pierre Cardinal
 * @author Martin Desruisseaux
 * @version $Id$
 * @since 2.3
 *
 * Code adaptation for use by MyPositionActivity by wm.stein on 2019-05-18.
 * Last edited on 2024-11-19.
 */
public final class EarthGravitationalModel extends VerticalTransform
{
    /**
     * Maximum degree and order attained.
     */
    private final int nmax;

    /**
     * WGS 84 semi-major axis.
     */
    private final double semiMajor;

    /**
     * The first Eccentricity Squared (e²) for WGS 84 ellipsoid.
     */
    private final double esq;

    /**
     * Even zonal coefficient.
     */
    private final double c2;

    /**
     * WGS 84 Earth's Gravitational Constant w/ atmosphere.
     */
    private final double rkm;

    /**
     * Theoretical (Normal) Gravity at the Equator (on the Ellipsoid).
     */
    private final double grava;

    /**
     * Theoretical (Normal) Gravity Formula Constant.
     */
    private final double star;

    /**
     * The geopotential coefficients read from the ASCII file.
     * Those arrays are filled by the load method.
     */
    private final double[] cnmGeopCoef, snmGeopCoef;

    /**
     * Cleanshaw coefficients needed for the selected gravimetric quantities that are computed.
     * Those arrays are computed by the initialize method.
     */
    private final double[] aClenshaw, bClenshaw, as;

    /**
     * Temporary buffer for use by heightOffset only. Allocated once for ever
     * for avoiding too many objects creation / destruction.
     */
    private final double[] cr, sr, s11, s12;

    /**
     * Creates a model with the specified maximum degree (180) and order.
     */
    public EarthGravitationalModel()
	{
        /*
         * WGS84 model values.
         * NOTE: The Fortran program gives 3.9860015e+14 for 'rkm' constant. This value has been
         * modified in later programs. From https://cddis.gsfc.nasa.gov/926/egm96/doc/S11.HTML :
         *
         *     "We next need to consider the determination of GM, GM0, W0, U0. The value of GM0
         *      will be that adopted for the updated GM of the WGS84 ellipsoid. This value is
         *      3.986004418e+14 m³/s², which is identical to that given in the IERS Numerical
         *      Standards [McCarthy, 1996, Table 4.1]. The best estimate of GM can be taken as
         *      the same value based on the recommendations of the IAG Special Commission SC3,
         *      Fundamental Constants [Bursa, 1995b, p. 381]."
         */
        nmax = 180;
        semiMajor = 6378137.0;
        esq = 0.00669437999013;
        c2 = 108262.9989050e-8;
        rkm = 3.986004418e+14;
        grava = 9.7803267714;
        star = 0.001931851386;
		final int cleanshawLength = locatingArray(nmax + 3);
        final int geopCoefLength = locatingArray(nmax + 1);
        aClenshaw = new double[cleanshawLength];
        bClenshaw = new double[cleanshawLength];
        cnmGeopCoef = new double[geopCoefLength];
        snmGeopCoef = new double[geopCoefLength];
        as = new double[nmax + 1];
        cr = new double[nmax + 1];
        sr = new double[nmax + 1];
        s11 = new double[nmax + 3];
        s12 = new double[nmax + 3];
    }

    /**
     * Computes the index as it would be returned by the locating array iv
     * (from the Fortran code).
     * <p>
     * Tip (used in some place in this class):
     * locatingArray(n+1) == locatingArray(n) + n + 1.
     */
    private int locatingArray(final int n)
    {
        return ((n + 1) * n) >> 1;
    }

    /**
     * Loads the coefficients from the specified ASCII file and initialize the internal
     * clenshaw arrays.
     * <p>
     * Note: ASCII may looks like an unefficient format for binary distribution.
     * A binary file with coefficient values read by java.io.DataInput readDouble would
     * be more compact than an uncompressed ASCII file. However, binary files are hard to
     * compress by the ZIP algorithm. Our experience show that a 675 kb uncompressed ASCII file
     * is only 222 kb after ZIP or JAR compression. The same data as a binary file is 257 kb
     * uncompressed and 248 kb compressed. So surprisingly, the ASCII file is more compact than
     * the binary file after compression. Since it is the primary format provided by the
     * Earth-Info web site, we use it directly in order to avoid a multiplication of formats.
     *
     * @throws IOException if the file can't be read or has an invalid content.
     */
    public void load(Context context) throws IOException
	{
        final InputStream stream = context.getResources().openRawResource(R.raw.egm180);
        final LineNumberReader in;
        in = new LineNumberReader(new InputStreamReader(stream, StandardCharsets.ISO_8859_1));
        String line;
        while ((line = in.readLine()) != null) 
		{
            final StringTokenizer tokens = new StringTokenizer(line);
            try 
			{
                /*
                 * Note: we use 'parseShort' instead of 'parseInt' as an easy way to ensure that
                 *       the values are in some reasonable range. The range is typically [0..180].
                 *       We don't check that, but at least 'parseShort' disallows values greater
                 *       than 32767. Additional note: we real all lines in all cases even if we
                 *       discard some of them, in order to check the file format.
                 */
                final int n = Short.parseShort(tokens.nextToken());
                final int m = Short.parseShort(tokens.nextToken());
                final double cbar = Double.parseDouble(tokens.nextToken());
                final double sbar = Double.parseDouble(tokens.nextToken());
                if (n <= nmax) 
				{
                    final int ll = locatingArray(n) + m;
                    cnmGeopCoef[ll] = cbar;
                    snmGeopCoef[ll] = sbar;
                }
            } catch (RuntimeException cause) 
			{
                /*
                 * Catch the following exceptions:
                 *   - NoSuchElementException      if a line has too few numbers.
                 *   - NumberFormatException       if a number can't be parsed.
                 *   - IndexOutOfBoundsException   if 'n' or 'm' values are illegal.
                 */
                throw new IOException("egm180" + in.getLineNumber(), cause);
            }
        }
        in.close();
        initialize();
    }

    /**
     * Computes the clenshaw arrays after all coefficients have been read.
     * We performs this step in a separated method than {from} in case we wish
     * to read the coefficient from an other source than an ASCII file in some future
     * version.
     */
    private void initialize() 
	{
        /*
         * MODIFY CNM EVEN ZONAL COEFFICIENTS.
         */
        final double[] c2n = new double[6];
        c2n[1] = c2;
        int sign = 1;
        double esqi = esq;
        for (int i = 2; i < c2n.length; i++) 
		{
            sign *= -1;
            esqi *= esq;
            c2n[i] = sign * (3 * esqi) / ((2 * i + 1) * (2 * i + 3)) * (1 - i + (5 * i * c2 / esq));
        }
        /* all nmax */
        double SQRT_05 = 2.2360679774997896964091736687313;
        cnmGeopCoef[3] += c2n[1] / SQRT_05;
        cnmGeopCoef[10] += c2n[2] / 3;
        double SQRT_13 = 3.6055512754639892931192212674705;
        cnmGeopCoef[21] += c2n[3] / SQRT_13;
        double SQRT_17 = 4.1231056256176605498214098559741;
        if (nmax > 6) cnmGeopCoef[36] += c2n[4] / SQRT_17;
        double SQRT_21 = 4.5825756949558400065880471937280;
        if (nmax > 9) cnmGeopCoef[55] += c2n[5] / SQRT_21;

		/*
         * BUILD ALL CLENSHAW COEFFICIENT ARRAYS.
         */
        for (int i = 0; i <= nmax; i++) 
		{
            as[i] = -Math.sqrt(1.0 + 1.0 / (2 * (i + 1)));
        }
        for (int i = 0; i <= nmax; i++) 
		{
            for (int j = i + 1; j <= nmax; j++) 
			{
                final int ll = locatingArray(j) + i;
                final int n = 2 * j + 1;
                final int ji = (j - i) * (j + i);
                aClenshaw[ll] = Math.sqrt(n * (2 * j - 1) / (double) (ji));
                bClenshaw[ll] = Math.sqrt(n * (j + i - 1) * (j - i - 1) / (double) (ji * (2 * j - 3)));
            }
        }
    }

    /**
     * Returns the value to add to a height above the ellipsoid in order to get a
     * height above the geoid for the specified geographic coordinate.
     *
     * @param longitude The geodetic longitude, in decimal degrees.
     * @param latitude  The geodetic latitude, in decimal degrees.
     * @param height    The height above the ellipsoid in metres.
     * @return The value to add in order to get the height above the geoid (in metres).
     */
    public double heightOffset(final double longitude, final double latitude, final double height)
    {
        /*
         * Note: no need to ensure that longitude is in [-180..+180°] range, because its value
         * is used only in trigonometric functions (sin / cos), which roll it as we would expect.
         * Latitude is used only in trigonometric functions as well.
         */
        final double phi = Math.toRadians(latitude);
        final double sin_phi = Math.sin(phi);
        final double sin2_phi = sin_phi * sin_phi;
        final double rni = Math.sqrt(1.0 - esq * sin2_phi);
        final double rn = semiMajor / rni;
        final double t22 = (rn + height) * Math.cos(phi);
        final double x2y2 = t22 * t22;
        final double z1 = ((rn * (1 - esq)) + height) * sin_phi;
        final double th = (Math.PI / 2.0) - Math.atan(z1 / Math.sqrt(x2y2));
        final double y = Math.sin(th);
        final double t = Math.cos(th);
        final double f1 = semiMajor / Math.sqrt(x2y2 + z1 * z1);
        final double f2 = f1 * f1;
        final double rlam = Math.toRadians(longitude);
        final double gravn;

        gravn = grava * (1.0 + star * sin2_phi) / rni;
        sr[0] = 0;
        sr[1] = Math.sin(rlam);
        cr[0] = 1;
        cr[1] = Math.cos(rlam);
        for (int j = 2; j <= nmax; j++) 
		{
            sr[j] = (2.0 * cr[1] * sr[j - 1]) - sr[j - 2];
            cr[j] = (2.0 * cr[1] * cr[j - 1]) - cr[j - 2];
        }
        double sht = 0, previousSht = 0;
        for (int i = nmax; i >= 0; i--) 
		{
            for (int j = nmax; j >= i; j--) 
			{
                final int ll = locatingArray(j) + i;
                final int ll2 = ll + j + 1;
                final int ll3 = ll2 + j + 2;
                final double ta = aClenshaw[ll2] * f1 * t;
                final double tb = bClenshaw[ll3] * f2;
                s11[j] = (ta * s11[j + 1]) - (tb * s11[j + 2]) + cnmGeopCoef[ll];
                s12[j] = (ta * s12[j + 1]) - (tb * s12[j + 2]) + snmGeopCoef[ll];
            }
            previousSht = sht;
            sht = (-as[i] * y * f1 * sht) + (s11[i] * cr[i]) + (s12[i] * sr[i]);
        }
        /*
         * Pre-computed values of some square roots.
         */
        double SQRT_03 = 1.7320508075688772935274463415059;
        return ((s11[0] + s12[0]) * f1 + (previousSht * SQRT_03 * y * f2)) * rkm /
                (semiMajor * (gravn - (height * 0.3086e-5)));
    }
	
}

