package edu.uml.cs.isense.objects;

import java.io.Serializable;

/**
 * Class that includes information about a Data Set Field on iSENSE.
 * 
 * @author iSENSE Android Development Team
 */
public class RProjectField implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = -2223711670490818827L;
	public long field_id;
    public String name="";
    public int type;
    public String unit="";  
    public String restrictions="";
    
    public static final int TYPE_TIMESTAMP = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_TEXT = 3;
    public static final int TYPE_LAT = 4;
    public static final int TYPE_LON = 5;
}
