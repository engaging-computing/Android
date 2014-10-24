package edu.uml.cs.isense.objects;

/**
 * Class that includes information about a particular project on iSENSE.
 * 
 * @author iSENSE Android Development Team
 */
public class RProject {
    //so we can check if project actually exists
    public boolean projectExist = false;
    public String serverErrorMessage = "";

    //values associated with project
    public int projectId;
    public int featured_media_id;
    public int default_read;
    public int likeCount;
    public boolean hidden;
    public boolean featured;
    public String name;
    public String url;
    public String timecreated;
    public String ownerName;
    public String ownerUrl;
}
