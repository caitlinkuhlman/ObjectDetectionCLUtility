package detection;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import boofcv.struct.PointIndex_I32;


/**
 * A frame is a rectilinear shape used to select subsections of image when searching for candidates.
 * @author caitlinkuhlman
 *
 */
public class Frame{
	
	//permutation matrix
	private static int[][] matrix;
	static{
		
		matrix = new int[81][4];
		
		for(int i=0 ; i<81 ; i++)
		{
			matrix[i][0] = (i/27)%3;
			matrix[i][1] = (i/9)%3;
			matrix[i][2] = (i/3)%3;
			matrix[i][3] = i%3;
		}
		for(int i=0 ; i<81 ; i++)
		{
			for(int j=0 ; j<4 ; j++){
				matrix[i][j] = matrix[i][j] - 1;
			}
		}
		
	}						
	
	//step size by which to permute
	private int diff;
	//sides contains pairs of values in this order: top, bottom, left, right
	private int[] sides = new int[4];
	//matrix scaled by step size
	private int[][] permutationMatrix;
	
	/**
	 * 
	 * @param list of 4 vertices of frame.
	 * @throws Exception
	 */
	public Frame(List<PointIndex_I32> list) throws Exception{
		
		if (list.size() != 4){
			throw new Exception("Candidate box incorrect size");
		}
		
		sides[0] = list.get(0).y; 
		sides[1] = list.get(3).y;
		sides[2] = list.get(0).x;
		sides[3] = list.get(1).x;

	}
	
	/**
	 * 
	 * @return list of vertices that make up frame.
	 */
	public List<PointIndex_I32> getList(){
		List<PointIndex_I32> list = new ArrayList<PointIndex_I32>();
		
		list.add(new PointIndex_I32(sides[2], sides[0], 0));
		list.add(new PointIndex_I32(sides[3], sides[0], 1));
		list.add(new PointIndex_I32(sides[3], sides[1], 2));
		list.add(new PointIndex_I32(sides[2], sides[1], 3));
		
		return list;
	}
	
	/**
	 * 
	 * @param diff
	 */
	public void setDifference(int diff){
		
		this.diff = diff;
		
		permutationMatrix = new int[81][4];
		
		for (int j=0 ; j<81 ; j++){
			for (int i=0 ; i<4 ; i++){
				permutationMatrix[j][i] = diff * matrix[j][i];
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int getDifference(){
		return this.diff;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<List<PointIndex_I32>> permute() throws Exception{
		
		if(diff == 0){
			throw new Exception("Must set difference");
		}
		
		List<List<PointIndex_I32>> list = new ArrayList<List<PointIndex_I32>>();
		
		for (int i=0 ; i<81 ; i++){
			int[] newSides = new int[sides.length];
			for (int j=0 ; j< sides.length ; j++){
				//apply transform
				newSides[j] = sides[j] + permutationMatrix[i][j];
			}
			//create new frame
			List<PointIndex_I32> newFrame = new ArrayList<PointIndex_I32>();
			newFrame.add(new PointIndex_I32(newSides[2], newSides[0], 0) );
			newFrame.add(new PointIndex_I32(newSides[3], newSides[0], 1) );
			newFrame.add(new PointIndex_I32(newSides[3], newSides[1], 2) );
			newFrame.add(new PointIndex_I32(newSides[2], newSides[1], 3) );
			
			//add frame to list
			list.add(newFrame);
		}
		
		return list;
		
	}
	
	
	@Override
	public boolean equals(Object obj){
		
		Frame that = (Frame) obj;
		if (this.sides[0] == that.sides[0] && this.sides[1] == that.sides[1] && 
				this.sides[2] == that.sides[2] && this.sides[3] == that.sides[3])
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode(){
		return sides.hashCode();
	}
	
	public static void main(String[] args){
		System.out.println(Arrays.toString(matrix[27]));
		System.out.println(Arrays.toString(matrix[0]));
		System.out.println(Arrays.toString(matrix[80]));
	}
	
}
