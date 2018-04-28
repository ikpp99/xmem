package com.pik.xmem;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class Head
{
    protected long    len;
    protected int     dim, typ, off, exx, nb;
    protected long[]  siz;

    protected Head(){ len=-1; dim=typ=off=exx=nb=-1; siz=null;}

    protected Head( type type, long[] size ){ this( type, size, 0 );} 
    
    protected Head( type type, long[] size, int extLen )
    {
        this();
        dim = size.length;
        siz = Arrays.copyOf( size, dim );

        typ = type.ordinal();
        nb  = type.getNb();
        len = nb; for( long d: siz ) len *=d;

        off    = 8*( 2 + dim ); exx = extLen>0? 8*(extLen/8): 0; off   += exx;
        len    = 8 * (( len + off + 7 )/8 );
    }
    
    protected void readHead( Xmem mem, long locMem ) throws Exception
    {
        isLoc( locMem );
        
        long[] hhl = { 0, 0 };
        mem.copyArr( locMem, hhl, false );
        
            getParam( hhl );

        mem.copyArr( locMem+16, siz, false );
    }
    
    protected void getParam( long[] hh )
    {
        len = hh[ 0 ];
        off = (int)(  hh[ 1 ]         & 0xFFFFFFFF );
        typ = (int)(( hh[ 1 ] >> 32 ) & 0xFFFF );
        dim = (int)(( hh[ 1 ] >> 48 ) & 0xFFFF );
        siz = new long[ dim ];
        int d0 = 8*( 2+dim ), lex = off-d0;
        exx = lex>0? lex: 0;
    }
    
    protected void writeHead( Xmem mem, long locMem ) throws Exception
    {
        isLoc( locMem );
        int dim2 = 2+dim;                                 //standard siz of header( NO ext )
        long[] hhl = new long[ dim2 ];
        hhl[0] = len;                                     //? (len >> 3) << 16 + 0xFFFE;
        hhl[1] = dim;
        hhl[1] = (((hhl[1] << 16) + typ ) << 32) + off;
        System.arraycopy( siz, 0, hhl, 2, dim );
        
        mem.copyArr( locMem, hhl, true );
    }
    
    public String toString()
    {
        String s = type.val( typ )+"[ ";
        try{ for( long ind: siz ) s+=ind+", ";}catch(Exception e){}
        return s.substring( 0, s.length()-2 )+" ] len="+len+", off="+off+", ext="+exx;
    }
    
    private void isLoc( long locMem ) throws Exception{
        if( locMem >= 0  && locMem %8 !=0 ) throw new Exception("BAD locMem: "+locMem);
    }
    
///* DBG: =====================================================================================================
    
    public static void main( String[] args ) throws Exception
    {
        Xmem mem = new Xmem( 128 );
        
        Head t = new Head( type.LONG, new long[]{2,3}, 8 ); 
        t.writeHead( mem, 0 );
        
        mem.copyArr( 40, new long[]{1,2,3,4,5,6}, true );
        dumpMem( mem );
        
        Head q = new Head();
        q.readHead(  mem, 0 );
        tt(""+q );
    }
    static void dumpMem( Xmem mm ){
        String s="### LongMem:"; String dec="dec: ";
        for( byte[] bb: mm.mem ){
            s+="\n"; for(byte b: bb ) s+=" "+(int)b;
            ByteBuffer bbu = ByteBuffer.wrap( bb );
            LongBuffer bbl =bbu.asLongBuffer();
            s+="\nhex: "; 
            int x=bb.length/8;
            for( int i=0;i<x;i++){ s+=" "+Long.toHexString( bbl.get( i )); dec+=" "+bbl.get( i );}
        }
        tt(s); tt(dec); tt("");
    }
    static void tt(String x){System.out.println( x );}static void tt(){tt("");}
//*/    
}
