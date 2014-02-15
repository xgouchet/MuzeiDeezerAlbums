package fr.xgouchet.deezer.muzei.data;


public class Edito {
    
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
        
        if (!(o instanceof Edito)) {
            return false;
        }
        
        Edito that = (Edito) o;
        
        return this.id == that.id;
    }
    
    @Override
    public int hashCode() {
        return (int) ((13 * id) & 0x7FFFFFFF);
    }
}
