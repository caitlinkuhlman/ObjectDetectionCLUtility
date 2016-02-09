package kdl.classifier;
/*This class and all classes in the package kdl.classifier were written by Joseph Paul Cohen*/

import java.util.List;

import boofcv.struct.PointIndex_I32;

public class DynamicTimeWarping {


	
	
	public static double getDistance(List<PointIndex_I32> vectora, List<PointIndex_I32> vectorb){
	
		int n = vectora.size();
		int m = vectorb.size();
		
		if (n ==0 || m == 0)
			return Double.MAX_VALUE;
	  
	  double[][] DTW = new double[n][m];
	  double cost;
	  
	  for (int i = 1; i < m ; i++)
		  DTW[0][i] = Double.MAX_VALUE;
	  for (int i = 1; i < n ; i++)
		  DTW[i][0] = Double.MAX_VALUE;
		
		
	  DTW[0][0] = 0;
	  
	  for (int i = 1; i < n ; i++)
		  for (int j = 1; j < m ; j++){
			  cost = distance(vectora.get(i), vectorb.get(j));
			  DTW[i][j] = cost + minimum(DTW[i-1][j],    // insertion
					  						DTW[i][j-1],    // deletion
		                                    DTW[i-1][j-1]);    // match
			}
		
		return DTW[n-1][m-1];
		
		
		
	}
	
	

	public static double getEuclidDistance(List<PointIndex_I32> vectora, List<PointIndex_I32> vectorb){
		
		double dist = 0;
		for(int i = 0 ; i < vectora.size() ; i ++){
			
			dist+=Math.pow(distance(vectora.get(i),vectorb.get(i)),2);
		}
		
		return Math.sqrt(dist);
	}
	
	/**
	 * Distance between symbols
	 */
	public static double distance(PointIndex_I32 a, PointIndex_I32 b){
		
		return Math.sqrt(Math.pow(a.x - b.x,2) + Math.pow(a.y - b.y,2)); 
	}
	
	private static double minimum(double...ds){
		
		double min = Double.MAX_VALUE;
		
		for (double d : ds)
			min = Math.min(min, d);
		
		return min;
	}
}
