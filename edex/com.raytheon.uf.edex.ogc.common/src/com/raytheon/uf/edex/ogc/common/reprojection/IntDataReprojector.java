/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.ogc.common.reprojection;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;

/**
 * Reprojection utility wrapper for IntDataRecords
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 28, 2012            bclement     Initial creation
 * 
 * </pre>
 * 
 */
public class IntDataReprojector extends
		AbstractDataReprojector<IntegerDataRecord> {

	protected int fill = 0;

    protected int dataMaskValue = -1;

	@Override
	protected GridCoverage2D getGridCoverage(IDataRecord record,
            ReferencedEnvelope env) {
		IntegerDataRecord dataRecord = (IntegerDataRecord) record;
		int[] data = dataRecord.getIntData();
		DataBuffer buff = new DataBufferInt(data, data.length);
		int x = (int) dataRecord.getSizes()[0];
		int y = (int) dataRecord.getSizes()[1];
		CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
		return constructGridCoverage(crs.getName() + " Grid", buff, x, y, env);
	}

    @Override
    protected GridCoverage2D getMaskCoverage(IDataRecord record,
            ReferencedEnvelope env) {
        int x = (int) record.getSizes()[0];
        int y = (int) record.getSizes()[1];
        int[] mask = new int[x * y];
        Arrays.fill(mask, dataMaskValue);
        DataBufferInt buff = new DataBufferInt(mask, mask.length);
        CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
        return constructGridCoverage(crs.getName() + " Grid", buff, x, y, env);
    }

	@Override
	protected IntegerDataRecord extractData(GridCoverage2D coverage) {
		RenderedImage image = coverage.getRenderedImage();
		Raster raster;
		if (image.getNumXTiles() == 1 && image.getNumYTiles() == 1) {
			// we can directly access data
			raster = image.getTile(0, 0);
		} else {
			// need to copy data out
			raster = image.getData();
		}
		DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
		int[] data = dataBuffer.getData();
		int height = raster.getHeight();
		int width = raster.getWidth();
		return new IntegerDataRecord("", "", data, 2, new long[] { width,
				height });
	}

    @Override
    protected IntegerDataRecord extractData(GridCoverage2D coverage,
            GridCoverage2D maskCoverage) {
        RenderedImage image = coverage.getRenderedImage();
        Raster raster;
        if (image.getNumXTiles() == 1 && image.getNumYTiles() == 1) {
            // we can directly access data
            raster = image.getTile(0, 0);
        } else {
            // need to copy data out
            raster = image.getData();
        }
        DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        int[] data = dataBuffer.getData();

        // Extract mask
        image = maskCoverage.getRenderedImage();
        if (image.getNumXTiles() == 1 && image.getNumYTiles() == 1) {
            // we can directly access data
            raster = image.getTile(0, 0);
        } else {
            // need to copy data out
            raster = image.getData();
        }
        dataBuffer = (DataBufferInt) raster.getDataBuffer();
        int[] mask = dataBuffer.getData();

        if (mask.length == data.length) {
            for (int i = 0; i < data.length; ++i) {
                if (mask[i] != dataMaskValue) {
                    data[i] = fill;
                }
            }
        }

        int height = raster.getHeight();
        int width = raster.getWidth();
        return new IntegerDataRecord("", "", data, 2, new long[] { width,
                height });
    }

	@Override
	protected IntegerDataRecord getDataSlice(IDataRecord record, Request req) {
		IntegerDataRecord dataRecord = (IntegerDataRecord) record;
		int[] max = req.getMaxIndexForSlab();
		int[] min = req.getMinIndexForSlab();
		int toWidth = max[0] - min[0];
		int toHeight = max[1] - min[1];
		int[] from = dataRecord.getIntData();
		int fromWidth = (int) dataRecord.getSizes()[0];
		int[] to = new int[toWidth * toHeight];
		for (int fromY = min[1], toY = 0; fromY < max[1]; ++fromY, ++toY) {
			int toRow = toY * toWidth;
			int fromRow = fromY * fromWidth;
			for (int fromX = min[0], toX = 0; fromX < max[0]; ++fromX, ++toX) {
				to[toRow + toX] = from[fromRow + fromX];
			}
		}
		long[] sizes = { toWidth, toHeight };
		return new IntegerDataRecord("", "", to, 2, sizes);
	}

	/**
	 * @return the fill
	 */
	public int getFill() {
		return fill;
	}

	/**
	 * @param fill
	 *            the fill to set
	 */
	public void setFill(int fill) {
		this.fill = fill;
	}

	@Override
	protected boolean compatible(IDataRecord dataRecord) {
		return dataRecord instanceof IntegerDataRecord;
	}

	@Override
	protected IDataRecord getDataPoints(IDataRecord record, Request req) {
		IntegerDataRecord dataRecord = (IntegerDataRecord) record;
		int[] from = dataRecord.getIntData();
		int fromWidth = (int) dataRecord.getSizes()[0];
		Point[] points = req.getPoints();
		int[] to = new int[points.length];
		for (int i = 0; i < to.length; ++i) {
			Point p = points[i];
			to[i] = from[p.y * fromWidth + p.x];
		}
		return new IntegerDataRecord("", "", to, 1, new long[] { to.length });
	}

}
