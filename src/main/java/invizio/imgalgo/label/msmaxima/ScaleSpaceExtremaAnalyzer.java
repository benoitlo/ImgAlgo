package invizio.imgalgo.label.msmaxima;


import java.util.Arrays;

import org.python.modules.math;

import Jama.Matrix;
import invizio.imgalgo.label.WindowMaxima.ExtremaType;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * @author Benoit Lombardot
 * 
 */


public class ScaleSpaceExtremaAnalyzer < T extends RealType<T> > extends ExtremaAnalyzer<T>{
	
	RandomAccessibleInterval<T> img0;
	double[] res0;
	RandomAccessibleInterval<T> img2;
	double[] res2;
	
	double scale;
	int nScalePerOctave;
	
	RandomAccess<T> imgRA0;
	RandomAccess<T> imgRA2;
	RandomAccess<Neighborhood<T>> imgNeighRA0;
	RandomAccess<Neighborhood<T>> imgNeighRA2;
	
	double[] neighData0;
	double[] neighData2;
	
	boolean canBuildScaleSpaceHessianAndGradient=true;
	
	
	/**
	 * 
	 * @param img0: image at the pyramid level below img1, same size as img1
	 * @param img1: image where maxima where detected
	 * @param img2: image at the pyramid level above img1, same size as img1
	 * @param res1: resolution of img1 pixel
	 * @param scale: level of details of img1
	 * @param nScalePerOctave:  such that the scale difference between 2 level of the pyramid is 2^1/nScalPerOctave 
	 * @param extremaType: whether minima or maxima (influence only the isExtrema method )
	 */
	public ScaleSpaceExtremaAnalyzer( 	RandomAccessibleInterval<T> img0, 
										RandomAccessibleInterval<T> img1, 
										RandomAccessibleInterval<T> img2, 
										double[] res1,
										double scale,
										int nScalePerOctave,
										ExtremaType extremaType)
	{
		super(img1, res1, extremaType);
		
		this.img0 = img0;
		this.img2 = img2;
		
		res0 = new double[nDim];
		res2 = new double[nDim];
		for(int d=0; d<nDim; d++)
		{
			res0[d] = res1[d] / Math.round( (double)img0.dimension(d) / ((double)img1.dimension(d)) );
			res2[d] = res1[d] * Math.round( (double)img2.dimension(d) / ((double)img1.dimension(d)) );
		}
		
		if (img0!=null)
		{
			this.imgRA0 = img0.randomAccess();
			int R0 = 1;
			this.imgNeighRA0 = getNeighborhoodRandomAcessible( img0, R0 );
			for(int d=0; d<nDim; d++)
				res0[d] = res1[d] / Math.round( (double)img0.dimension(d) / ((double)img1.dimension(d)) );
			
		}
		else{ canBuildScaleSpaceHessianAndGradient = false; }
		
		if (img2!=null)
		{
			this.imgRA2 = img2.randomAccess();
			int R2 = 1;
			this.imgNeighRA2 = getNeighborhoodRandomAcessible( img2, R2 );
			for(int d=0; d<nDim; d++)
				res2[d] = res1[d] * Math.round( (double)img2.dimension(d) / ((double)img1.dimension(d)) );
			
		}
		else{ canBuildScaleSpaceHessianAndGradient = false; }
		
		this.scale = scale;
		this.nScalePerOctave = nScalePerOctave;
		
		if ( canBuildScaleSpaceHessianAndGradient )
			this.size = this.nDim+1;
		
	}
	
	
	
	
	
	
	
//	/**
//	 * 
//	 */
//	public void setPosition(long[] position)
//	{
//		super.setPosition(position);
//	}
	
	
	
	/**
	 * 
	 * @return
	 */
	public Double getOptimizedScale()
	{
		if (position==null)
			return null;
		if (!canBuildScaleSpaceHessianAndGradient)
			return scale;
		
		deltaPosition = getDeltaPosition();
		double optimizedScale; 
		if( deltaPosition==null )
			optimizedScale = scale;
		else
			optimizedScale = scale * Math.pow(2,deltaPosition[nDim]/nScalePerOctave);
		
		return optimizedScale;
	}
	
	
	
	/**
	 * Calculate the eigenvalue ratio criteria in 2D or 3D according to point dimensionality, does not take the scale dimension into account 
	 * lambda1/lambda3<r <=in 2D=> tr(H)^2/Det(H) < (r+1)^2/r  from Lowe, Distinctive Image Features from Scale-Invariant Keypoints, 2004
	 * lambda1/lambda3<r <=in 3D=> tr(H)^2/Det(H) < (2r+1)^3/r^2 from Allaire, Full Orientation Invariance and Improved Feature Selectivity of 3D SIFT with Application to Medical Image Analysis, 2008
	 * @return return the eigenvalue ratio criteria
	 */
	@Override
	public void computeEigValRatioCriteria()
	{
		if (eigValRatioCriteria!=null)
			return;
			
		if(Mhessian == null)
			computeHessian();
		Matrix Mhessian_PhysicalSpace = Mhessian.getMatrix(0, nDim-1, 0, nDim-1); // remove the scale dimension from the hessian
		double det = Mhessian_PhysicalSpace.det();
		if(det==0)
			eigValRatioCriteria = Double.NaN;
		else
			eigValRatioCriteria = Math.pow(Mhessian_PhysicalSpace.trace(),nDim) / det;
		
	}
	
	
	
	
	public boolean isExtrema()
	{
		
		//System.out.println("===========");
		//System.out.println("pos: "+Arrays.toString(position));
		//System.out.println("res: "+Arrays.toString(res1));
		
		boolean isExtrema = true;

		
		if( imgRA0!=null )
		{
			// measure position0
			long[] pos0 =  adaptPosition( position, res1, res0 );
			//System.out.println("l0 neigh: "+Arrays.toString(  getNeighborhoodInImage(pos0, imgNeighRA0)  ));
			
			imgRA1.setPosition(position); 
			imgNeighRA0.setPosition(pos0); 
			Point p0 = localNeighborhoodCheck.check( imgRA1, imgNeighRA0.get() );
			if( p0==null )
				isExtrema = false;
		}
		
		//System.out.println("l1 neigh: "+Arrays.toString(  getNeighborhoodInImage(position, imgNeighRA1)  ));		
		if( ! super.isExtrema() )
			isExtrema = false;
		
		if( imgRA2!=null )
		{
			// measure position2
			long[] pos2 =  adaptPosition( position, res1, res2 );
			//System.out.println("l2 neigh: "+Arrays.toString(  getNeighborhoodInImage(pos2, imgNeighRA2)  ));
		
			imgNeighRA2.setPosition(pos2); 
			Point p2 = localNeighborhoodCheck.check( imgRA1, imgNeighRA2.get() );
			if( p2==null )
				isExtrema = false;
		}
		
		//System.out.println("Is extrema? " + isExtrema );
		
		return isExtrema;
	}
	
	// convert a position in img1 to a position in img0 or img2 depending on the new resolution passed
	protected long[] adaptPosition(long[] position, double[] oldRes, double[] newRes)
	{
		long[] pos = new long[nDim];
		for(int d=0; d<nDim; d++) {
			int factor = (int) ( oldRes[d] / newRes[d] ) ;
			pos[d] = (long) ( position[d] * factor );
			pos[d] = (long) Math.max(0, pos[d]);
			pos[d] = (long) Math.min(math.floor(img1.dimension(d)*factor)-1, pos[d]);
		}	
		return pos;
	}
	
	/**
	 * 
	 */
	protected void computeGradient()
	{
		super.computeGradient();
		if( canBuildScaleSpaceHessianAndGradient )
		{	
			if (this.neighData0 == null)
			{
				long[] pos0 =  adaptPosition( position, res1, res0 );
				this.neighData0 = getNeighborhoodInImage(pos0, imgNeighRA0);
				long[] pos2 =  adaptPosition( position, res1, res2 );
				this.neighData2 = getNeighborhoodInImage(pos2, imgNeighRA2);
			}
			
			int center = (int)(Math.pow(3,nDim)/2);
			double val =  ( neighData2[center] - neighData0[center] ) / 2;
			Mgradient.set(nDim, 0, val);
		}
	}
	
	
	/**
	 * 
	 */
	protected void computeHessian()
	{
		super.computeHessian();
		
		if( canBuildScaleSpaceHessianAndGradient )
		{
			if (this.neighData0 == null)
			{
				long[] pos0 =  adaptPosition( position, res1, res0 );
				this.neighData0 = getNeighborhoodInImage(pos0, imgNeighRA0);
				long[] pos2 =  adaptPosition( position, res1, res2 );
				this.neighData2 = getNeighborhoodInImage(pos2, imgNeighRA2);
			}
			// calculate the derivative involving the scale dimension;
			int center = (int)(Math.pow(3,nDim)/2);
			double val = -2*neighData1[center] +neighData2[center] +neighData0[center];
			Mhessian.set(nDim,nDim,val);
			for (int f=0; f<nDim; f++)
			{
				val = (  ( neighData2[center+move(1,f)] - neighData2[center+move(-1,f)] ) / 2*res2[f] 
			            -( neighData0[center+move(1,f)] - neighData0[center+move(-1,f)] ) / 2*res0[f] ) / 2 ;
				Mhessian.set(f,nDim,val);
				Mhessian.set(nDim,f,val);
			}
		}
	}
	
	
	
	public static void main(final String... args)
	{
//		new ij.ImageJ();
//		
//		int nDim=3;
//		double[] res1 = new double[] {1,1,1};
//
//		int r=5;
//		double[] cref = new double[] {r,r,r};
//		double[] c0 = new double[] {r+0.3,r+0.5,r-0.25};
//		double ds=0.4;
//		
//		Img<FloatType> img0 = getGaussAtPos( r,nDim, c0, ds+1);
//		Img<FloatType> img1 = getGaussAtPos( r,nDim, c0, ds);
//		Img<FloatType> img2 = getGaussAtPos( r,nDim, c0, ds-1);
//		
//		ImgLib2Utils.convertImgToImagePlus( img1 , "test", "Grays", new int[] {2*r+1,2*r+1,2*r+1,1,1}, null).show();
//
//		// instantiate pointN_NeighCalculation at cref
//		double scale = 2;
//		int nScalePerOctave = 2;
//		ScaleSpaceExtremaAnalyzer<FloatType> extremaAnalyzer = new ScaleSpaceExtremaAnalyzer<FloatType>( img0, img1, img2, res1, scale, nScalePerOctave, WindowedMaxima.ExtremaType.MAXIMA);
//		extremaAnalyzer.setPosition( cref );
//		if( extremaAnalyzer.isExtrema() )
//		{
//			double[] cOptimized = extremaAnalyzer.getOptimizedPosition();
//			double scaleOptimized = extremaAnalyzer.getOptimizedScale();
//			double eigValRatio = extremaAnalyzer.getEigValRatioCriteria();
//			System.out.println(Arrays.toString(cOptimized));
//			System.out.println(scaleOptimized);
//			System.out.println(  "" + ( Math.log(scaleOptimized)/Math.log(2) )  );
//			System.out.println(eigValRatio);
//		}
	}
	
	
	
}
