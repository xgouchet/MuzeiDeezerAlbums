package fr.xgouchet.deezer.muzei.data;


public class EditoInfo {
    
    public long id;
    public String name;
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof EditoInfo)) {
            return false;
        }
        
        EditoInfo that = (EditoInfo) o;
        
        return this.id == that.id;
    }
    
    @Override
    public int hashCode() {
        return (int) ((13 * id) & 0x7FFFFFFF);
    }
}
