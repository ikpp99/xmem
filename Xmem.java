package com.pik.xmem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Xmem
{
    static protected  int MM = 64;  // 1024*1024*512 = 0.5 GB // length of byte[] arrs. 
    protected long memLen;
    protected ArrayList< byte[] > mem;
    
    protected HashMap< String, Long > blockNamLoc; 
    protected TreeMap<   Long, Long > freeLocLen;
    
    public Xmem( long len ) throws Exception { this( len, MM );} 
    
    public Xmem( long len, int lenBuf8 ) throws Exception 
    {
        MM = 8*( lenBuf8 / 8 );  // !!! must be 8 * k !!!
        mem = new ArrayList<>();
        long s=0;
        while( s < len ){
            int n = MM;
            if( s+n > len ) n = (int)( len - s );
            
            mem.add( new byte[ n ] );
            s+=n;
        }
        memLen = len;
        Block.iniBlocks( this );
    }
    
    private void copArr( byte[] bb, int pbb, Object arr, int parr, int len, type typ, boolean put )
    {
        if( typ.equals( type.BYTE ))
        {
            if( put ) System.arraycopy( (byte[]) arr, parr, bb, pbb, len ); 
            else      System.arraycopy( bb, pbb, (byte[]) arr, parr, len );        
        }
        else {
                                 ByteBuffer buf = ByteBuffer.wrap( bb, pbb, len * typ.getNb() );
            if( put ){
                switch( typ ){
                    case SHORT : buf.asShortBuffer() .put( ( short[]) arr, parr, len ); break;
                    case INT   : buf.asIntBuffer()   .put( (   int[]) arr, parr, len ); break;
                    case LONG  : buf.asLongBuffer()  .put( (  long[]) arr, parr, len ); break;
                    case FLOAT : buf.asFloatBuffer() .put( ( float[]) arr, parr, len ); break;
                    case DOUBLE: buf.asDoubleBuffer().put( (double[]) arr, parr, len );
                    default:
                }
            } else {
                switch( typ ){
                    case SHORT : buf.asShortBuffer() .get( ( short[]) arr, parr, len ); break;
                    case INT   : buf.asIntBuffer()   .get( (   int[]) arr, parr, len ); break;
                    case LONG  : buf.asLongBuffer()  .get( (  long[]) arr, parr, len ); break;
                    case FLOAT : buf.asFloatBuffer() .get( ( float[]) arr, parr, len ); break;
                    case DOUBLE: buf.asDoubleBuffer().get( (double[]) arr, parr, len );
                    default:
                }
            }
        }    
    }

    public void copyArr( long pmem, Object arr, boolean put ) throws Exception { copyArr( pmem, arr, -1, -1, put );}

    public void copyArr( long pmem, Object arr, int parr, int len, boolean put ) throws Exception
    {
        type typ = null;    
        if(      arr instanceof   byte[] ){ typ = type.BYTE;  if(parr<0) len = ((  byte[]) arr ).length;}
        else if( arr instanceof   long[] ){ typ = type.LONG;  if(parr<0) len = ((  long[]) arr ).length;}
        else if( arr instanceof    int[] ){ typ = type.INT;   if(parr<0) len = ((   int[]) arr ).length;}
        else if( arr instanceof double[] ){ typ = type.DOUBLE;if(parr<0) len = ((double[]) arr ).length;}
        else if( arr instanceof  float[] ){ typ = type.FLOAT; if(parr<0) len = (( float[]) arr ).length;}
        else if( arr instanceof  short[] ){ typ = type.SHORT; if(parr<0) len = (( short[]) arr ).length;}
                                                              if(parr<0) parr=0;     
        copyArr( pmem, arr, parr, len, typ, put );
    }
    
    public void copyArr( long pmem, Object arr, int parr, int len, type typ, boolean put ) throws Exception
    {
        int  nb = typ.getNb();
        long arlen = len*nb , x = pmem+arlen;
        if(  x > memLen ) throw new Exception("LongMem = "+memLen+" < "+pmem+"+"+arlen+" = "+x );

        int b0=buf( pmem ), p0=off( pmem ), bx=buf( x ), ee=MM;
        while( b0 <= bx )
        {
            if( b0==bx ) ee = off( x );
            int nn = ee - p0, nar=nn/nb; //numb. of bytes,
            if( nn >0 ) copArr( mem.get(b0), p0, arr, parr, nar, typ, put );
            if( ++b0 > bx ) break;
            p0=0; parr += nar; 
        }
    }
    private int buf( long p ){ return (int)( p/MM );}
    private int off( long p ){ return (int)( p - MM*buf( p ));} 

    public void copyLeft( long d, long s, long n ) throws Exception   // Left: d <= s || s+n <= d !!!
    {    
        if( d <= s || s+n <= d ){
            
            int sss = buf( s ), ssX = buf( s+n ), s0 = off( s ), sZ = off( s+n ), sx=MM; 
            int ddd = buf( d ), ddX = buf( d+n ), d0 = off( d ), dZ = off( d+n ), dx=MM;
            
            long xx=0;

            while(  xx < n ){
                if( sss==ssX ) sx=sZ;
                if( ddd==ddX ) dx=dZ;  // dd:(d0,dx) <- ss:(s0,sx)  

                int nn = Math.min( dx-d0 , sx-s0 );
                System.arraycopy( mem.get( sss ), s0, mem.get( ddd ), d0, nn ); 

                s0 += nn; if( s0==MM ){ sss++; s0=0; sx=MM;}
                d0 += nn; if( d0==MM ){ ddd++; d0=0; dx=MM;}
                xx += nn;
            }
        } 
        else throw new Exception("CrossCopy"); 
    }
    
//------------------------------------------------------------------------------------- static Utils:
    
    static private long[][] cre2l( long[] a ){
        int x = a.length;
        long[][] dd = new long[x][2];
        for(int i=0;i<x;i++){ dd[i][0]=a[i]; dd[i][1]=a[1];}
        return dd;
    }

    static public long[][] idx( long i ){ return idx( cre2l( new long[]{i}));}
    static public long[][] idx( long i, long j  ){ return idx( cre2l( new long[]{i,j}));}
    static public long[][] idx( long i, long j, long k ){ return idx( cre2l( new long[]{i,j,k}));}
    static public long[][] idx( long i, long j, long k, long l ){ return idx( cre2l( new long[]{i,j,k,l}));}

    static public long[][] idx( long[][] ind ){ return ind==null? null: copyL2( ind );}
    
    static public long[][] idx( String s ){  // ",i,j[:jj],:jj,,", *="ALL from this"
        int j;        final int DIM=7;
        long[][] tt = new long[ DIM ][2];
        for(j=0;j< DIM ;j++) tt[j][0]=tt[j][1]=1;
        
        if( s !=null && !s.isEmpty() ){
            String[] ss = s.split(","); int pss=0;
            for( String t: ss ){
                if( pss < DIM ){
                    if( !t.isEmpty()){                  int m=0, pt=0; long val=0;
                                                        StringBuilder var=new StringBuilder();
                        while( pt<t.length() && m<2 ){
                                                        char q=t.charAt( pt++ );
                            if('0'<= q && q <='9'){
                                val = val*10 + q - 48;
                            }
                            else if( q==':'){
                                if( var.length()>0 ) {
                                    Long v = vars.get( var.toString());
                                    if(  v !=null ) val = v;
                                    var = new StringBuilder();
                                }
                                tt[pss][m] = val==0? 1: val;
                                val=0; m++;
                            }
                            else if( q=='*'){
                                if( m==0){ tt[pss][m]=1;m++;}
                                tt[pss][m] = -1; 
                                break;
                            }
                            else if('A'<= q && q <='Z' || 'a'<= q && q <='z'){ var.append( q );}
                        }
                        
                        if( val !=0 ){ tt[pss][m]=val;}
                        else if( var.length()>0 ) {
                            Long v = vars.get( var.toString());
                            if(  v !=null ) tt[pss][m]=v;
                        }
                    }
                }
                pss++;
            }
        }
        int x = DIM;
        while( x-->0 && tt[x][0]==1 && tt[x][1]==1 ){}
        return copyL2( tt, ++x );
    }
    
    static public long[][] realIndex( long[][] ind, Head head ) throws Exception {

        long[][] jj = copyL2( ind );
        
        for(int i=0; i<jj.length; i++){
            if( jj[i][0] == -1 ){ jj[i][1] = -1; jj[i][0] = 1; }
            if( jj[i][1] == -1 ){ jj[i][1] = head.siz[i] - jj[i][0]+1;}

            if( jj[i][0]<1 || jj[i][0] > head.siz[i]
            ||  jj[i][1]<1 || jj[i][0] + jj[i][1] -1 > head.siz[i] )
                throw new Exception("BAD Index "+(i+1)+": "+(jj[i][0] + jj[i][1]-1)+" > size");
        }
        return jj;
    }
    
    static public long[][] copyL2( long[][] ss ){ return copyL2( ss, ss.length );}
    static public long[][] copyL2( long[][] ss, int x ){
        long[][] dd = new long[ x ][2];
        while( x-->0 ){ dd[x][0] = ss[x][0]; dd[x][1] = ss[x][1];} 
        return dd;
    }
    
    static public void var( String eq ){
        int i=eq.indexOf('=');
        if( i>0 ) var( eq.substring( 0,i ).trim(), Long.parseLong( eq.substring( i+1 ).trim()));
    }
    static public void var( String name, long value ){
        if( vars==null ) vars = new HashMap<>();
        vars.put( name, value );
    }
    static private HashMap<String,Long> vars;
    
    public static String idx2str( long[][] pp ){
        String s="[";
        for(int i=0;i<pp.length;i++) s+=" "+pp[i][0]+":"+pp[i][1]+",";
        s = s.substring( 0, s.length()-1 )+" ]";
        return s;
    }
    static void tt(String x){System.out.println( x );}static void tt(){tt("");}
   
// DBG: ===============================================================================================
                                                              static final boolean PUT=true, GET=false;
    public static void main( String[] args ) throws Exception
    {
        int NN=1111; 
        Xmem mem = new Xmem( 12*NN );

        long[] ddd = new long[ NN ]; for(int i=0;i<NN;i++) ddd[i]=i;
        long[] rrr = new long[ ddd.length ]; 

        long aa=48, bb=120, cc=8;
        mem.copyArr( aa, ddd, 2, NN-2, PUT );
        mem.copyArr( aa, rrr,          GET );
        String s="[]: "; for( long q: rrr ) s+=q+", ";  tt( s );

        double[] dd = new double[ NN ]; for(int i=0;i<NN;i++) dd[i]=i;
        double[] rr = new double[ dd.length ]; 

        mem.copyArr( bb, dd, 2, NN-2, PUT );
        mem.copyArr( bb, rr,          GET );
        s="[]: "; for( double r: rr ) s+=r+", ";  tt( s );

        mem.copyLeft( cc, bb, NN*8 );
        mem.copyArr(  cc, rr, GET  );
        s="[]: "; for( double r: rr ) s+=r+", ";  tt( s );
    }
//    
}
