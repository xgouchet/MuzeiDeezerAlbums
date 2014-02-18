package fr.xgouchet.deezer.muzei.data;


public class AlbumInfo {
    
    public long id;
    public String title;
    public String artist;
    public String cover; 
    
    
    @Override
    public boolean equals(Object o) {
        
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof AlbumInfo)) {
            return false;
        }
        
        AlbumInfo that = (AlbumInfo) o;
        
        return this.id == that.id;
    }
    
    @Override
    public int hashCode() {
        return (int) ((13 * id) & 0x7FFFFFFF);
    }
}
