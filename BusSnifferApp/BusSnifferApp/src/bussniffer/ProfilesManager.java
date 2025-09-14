package bussniffer;

import java.util.ArrayList;
import java.util.List;

public class ProfilesManager {
    public static class Profile {
        public int id;
        public int height;
        public int slide;
        public int incline;
        public Profile(int id,int h,int s,int i){ this.id=id; this.height=h; this.slide=s; this.incline=i; }
        public String toString(){ return "P"+id+" H="+height+" S="+slide+" I="+incline; }
    }

    private List<Profile> profiles = new ArrayList<>();

    public ProfilesManager() {
        // create some default empty profiles
        for(int i=0;i<8;i++) profiles.add(new Profile(i,0,0,0));
    }

    public Profile getProfile(int id){ return profiles.get(id); }
    public void saveProfile(int id,int h,int s,int i){ profiles.set(id, new Profile(id,h,s,i)); }
    public List<Profile> list(){ return profiles; }
}
