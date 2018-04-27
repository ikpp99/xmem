package com.pik.xmem;

import java.util.Arrays;

public class Part
{
    protected Block    blk;
    protected long[][] pp;     // idx
    protected Object   arr;
    protected int      arrlen;
    protected type     arrtyp;
    private   int[]    xx;     // size of part
    
    public void setIdx( long[][] idx )  throws Exception {
        long[][] tt = Xmem.realIndex( idx, blk.head );
        int len = (int)tt[0][1];
        for(int i=1;i<tt.length;i++) len *= tt[i][1];
        int arLen = type.arrLen( arrtyp, arr );
        if( len > arLen ) throw new Exception("setIdx: "+len+" > ["+arrlen+"]");
        pp = tt;
        arrlen = len;
    }

    public void setArr( Object narr )  throws Exception {
        if( !arr.getClass().equals( narr.getClass())) throw new Exception("setArr: types");
        int len = type.arrLen( arrtyp, narr );
        if( len < arrlen ) throw new Exception("setArr: "+len+" < ["+arrlen+"]");
        arr = narr;
    }
    
    public Part( Block block, long[][] idx ) throws Exception 
    {
        blk=block; pp = Xmem.realIndex( idx, blk.head );
        xx = new int[ pp.length ]; xx[0]=1; 
        arrlen=1;
        for( int i=0; i<pp.length; i++) {
            if( pp[i][0] < 1 || pp[i][1] < 1 || pp[i][0]+pp[i][1]-1 > blk.head.siz[i] )
                throw new Exception("BAD index: "+pp[i][0]+" : "+pp[i][1]);
            arrlen *= pp[i][1];
            if( i>0 ) xx[i] = xx[i-1] * (int)pp[i-1][1]; 
        }
        arrtyp = type.val( blk.head.typ );
        arr = type.creArr( arrtyp, arrlen );
    }
    
    public void get() throws Exception{ blk.part( pp, arr, false );}
    public void set() throws Exception{ blk.part( pp, arr, true  );}

    public int arrLoc( int[] ijk ) { // ijk[n] > 0 !!! 
        int loc=ijk[0]-1;
        for(int n=1;n<ijk.length;n++) loc += (ijk[n]-1) * xx[n];
        return loc;
    }
    
    public void fill( Object v ) throws Exception {
        switch( arrtyp ) {
            case DOUBLE:  Arrays.fill( (double[])arr, (double) v ); break;
            case INT   :  Arrays.fill( (int[]   )arr, (int   ) v ); break;
            case LONG  :  Arrays.fill( (long[]  )arr, (long  ) v ); break;
            case FLOAT :  Arrays.fill( (float[] )arr, (float ) v ); break;
            case SHORT :  Arrays.fill( (short[] )arr, (short ) v ); break;
            case BYTE  :  Arrays.fill( (byte[]  )arr, (byte  ) v );
        }
    }
//------------------------------------------------------------------------------
    
    public Object get( int[] ijk ){ return getObjLoc( arrLoc( ijk ));}
    public Object get( int i               ){ return get( new int[]{ i     });} 
    public Object get( int i, int j        ){ return get( new int[]{ i,j   });} 
    public Object get( int i, int j, int k ){ return get( new int[]{ i,j,k });} 

    private Object getObjLoc( int loc ){
        switch( arrtyp ) {
            case DOUBLE:  return Double .valueOf( ((double[])arr)[ loc ] );
            case INT   :  return Integer.valueOf( ((int   [])arr)[ loc ] );
            case LONG  :  return Long   .valueOf( ((long  [])arr)[ loc ] );
            case FLOAT :  return Float  .valueOf( ((float [])arr)[ loc ] );
            case SHORT :  return Short  .valueOf( ((short [])arr)[ loc ] );
            case BYTE  :  return Byte   .valueOf( ((byte  [])arr)[ loc ] );
        }
        return null;
    }
//------------------------------------------------------------------------------

    public void put( Object v, int[] ijk ){
        int loc = arrLoc( ijk );
        switch( arrtyp ) {
            case DOUBLE:  ((double[])arr)[ loc ] = (double) v ; break;
            case INT   :  ((int   [])arr)[ loc ] = (int)    v ; break;
            case LONG  :  ((long  [])arr)[ loc ] = (long)   v ; break;
            case FLOAT :  ((float [])arr)[ loc ] = (float)  v ; break;
            case SHORT :  ((short [])arr)[ loc ] = (short)  v ; break;
            case BYTE  :  ((byte  [])arr)[ loc ] = (byte)   v ;
        }
    }
    public void put( Object v, int i               ){ put( v, new int[]{ i     });} 
    public void put( Object v, int i, int j        ){ put( v, new int[]{ i,j   });} 
    public void put( Object v, int i, int j, int k ){ put( v, new int[]{ i,j,k });} 

    public String toString(){
        String s = "\nPart: "+smpInd( pp )+" of "+blk.toString(); p="";
        vv = Xmem.copyL2( pp );
        
        i1 = finDD( 0, vv );  i2 = finDD( i1+1, vv );
        if( i1 <0 )  i1=i2=0;
        if( i2 <0 ){ i2=i1; i1=0;}
        
        par2str( pp.length - 1 );
        return s+p;
    }
    private String smpInd( long[][] qq ){ return Xmem.idx2str( qq ).replaceAll(":1","");}

    private int finDD( int i, long[][] dd ){
        int ddx = dd.length;
        while( i < ddx && dd[i][1]==1 ) i++;
        return i<ddx? i: -1;
    }
    
    private long[][] vv;  String p;
    private void par2str( int pv ) {
        if( pv > i2) {
            long sav = vv[pv][0], end = sav+vv[pv][1];
            while( vv[pv][0] < end) {
                par2str( pv-1 );
                vv[pv][0]++;
            }
            vv[pv][0] = sav;
        }
        else {  // pv=i2 !!!
            p+="\n" + partIndex() + partData();
        }
    }

    private int i1, i2;
    private String partData() {
        int vx=(int)vv.length;
        int[] ijk = new int[ vx ], ij = new int[ vx ];
        for(int i=0;i<vx;i++) ijk[i] = ij[i] = (int) vv[i][0];
        
        String s="";
        for(int i=ijk[i1]; i<ijk[i1]+vv[i1][1]; i++ ){ 
            ij[i1]=i; s+="\n"; 
            for(int j=ijk[i2]; j<ijk[i2]+vv[i2][1]; j++ ){
                ij[i2]=j;
//              s+=" "; for(int t=0;t<ij.length;t++) s+=ij[t]; // usefull to debug
                s+= getPar( ij );
            }
            if( i1==i2 ) break;
        }
        return s;
    }
    
    private String getPar( int[] ijk ){
        int[] ij = new int[ ijk.length ];
        for(int i=0;i<ijk.length;i++)
            ij[i] = ijk[i] - (int)pp[i][0] +1;
        return obj2str( get( ij ));
    }
    
//+++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private String partIndex() {
        String s="[ ";
        for(int i=0; i<vv.length; i++){
            s+=vv[i][0];
            if( i==i1 || i==i2 ) s+=(pp[i][1]>1?":"+pp[i][1]:"");
            s +=", ";
        }
        return s.substring( 0, s.length()-2 )+" ]";
    }
    
    private String obj2str( Object obj ) {
        switch( arrtyp ) {
            case DOUBLE:  return normLen( dbl2str( (double) obj, 10 ), 16 );
            case INT   :  return normLen( lon2str( (int   ) obj, 10 ), 12 );
            case LONG  :  return normLen( lon2str( (long  ) obj, 10 ), 14 );
            case FLOAT :  return normLen( dbl2str( (float ) obj,  8 ), 14 );
            case SHORT :  return normLen( lon2str( (short ) obj,  5 ),  7 );
            case BYTE  :  return normLen( lon2str( (long  ) obj,  3 ),  5 );
        }
        return null;
    }
    
    static public String lon2str( long v, int m ){ 
        String s = (v<0? "":" ")+v;
        int l=s.length(), x=m+1;
        if( l>x ) s = s.substring( 0, x )+"+"+(l-x);
        return s;
    }
    
    static public String dbl2str( double v, int m ){
        int w = m+6;
        String s = (v<0? "":" ")+String.format("%"+w+"."+m+"g",v).trim().replace(',','.');
        
        int x = s.indexOf("e"), exp=0;
        if( x > 0 ) exp = Integer.parseInt( s.substring( x+1 ));
        
        if( s.substring( 1,3 ).equals("0.")) s = s.replace("0.",".");
        
        if( s.substring( 1,3 ).equals(".0")){
            int ex=-2, p=2;  x=s.length();
            while( ++p < x && s.charAt( p )=='0') ex--;
            if( p<x ){
                s = s.substring( 0, 1 )+s.charAt( p )+"." +s.substring( p+1 );
                exp += ex;
            } else s=" 0";
            x = -1;
        }
        
        if( x < 0 ) x = s.length();
        x = x < w-2? x: w-3;
        s = s.substring( 0, x );
        
        x = s.length(); while( --x > 1 && s.charAt( x )=='0'){}
        s = s.substring( 0, x+1 );
        if( exp !=0 ) s += (exp>0?"+":"")+exp;
        else if( s.charAt( s.length()-1 )=='.') s =s.substring( 0, s.length()-1 );
        
        return s;
    }

    static public String normLen( String s, int w ){
        w -= s.length();
        return w>0? s = s+sp32.substring( 0, w ): s;
    }   static private final String sp32="                                ";// 32*' '!!!
    
///* DBG: =====================================================================================================
    
    public static void main( String[] args ) throws Exception {
        new Xmem( 512+1300 + 5*7*9*8 + 30 );
        
        Block iii = new Block("iii", type.INT, new long[]{ 5,7,9 } );
        Part rrr = new Part( iii, idx("*,*,*"));
        for(int k=1;k<=9;k++)
            for(int j=1;j<=7;j++)
                for(int i=1;i<=5;i++) rrr.put( Integer.valueOf( 100*i + 10*j + k ), new int[]{i,j,k});
        rrr.set(); tt(""+rrr);
        
        Block qq = new Block("qq",type.INT, new long[]{3,4,5,6}); tt("\n"+ qq );
        rrr = new Part( qq, idx("*,*,*,*"));
        
        for(int l=1; l<= qq.head.siz[3]; l++)
            for(int k=1; k<= qq.head.siz[2]; k++)
                for(int j=1; j<= qq.head.siz[1]; j++)
                    for(int i=1; i<= qq.head.siz[0]; i++)
                        rrr.put( Integer.valueOf( 1000*i + 100*j + k*10 + l ), new int[]{i,j,k,l});
        
        rrr.set();
        tt(""+rrr);
        
        Part  q = new Part( qq, idx("1:3,2:3,2:4,3:4"));
        q.get();
        tt(""+q);
        
        Part ww =  new Part( qq, idx("3,2:3,4:2,2:5"));
//        Part ww =  new Part( qq, new Index("3,2:3,4,2:5"));
//        Part ww =  new Part( qq, new Index("3,2,4,*"));
        ww =  new Part( qq, idx("*"));      ww.get(); tt(""+ww );
        ww =  new Part( qq, idx("*,4"));    ww.get(); tt(""+ww );
        ww =  new Part( qq, idx("*,,*"));   ww.get(); tt(""+ww );
        ww =  new Part( qq, idx("3,,*"));   ww.get(); tt(""+ww );
        ww =  new Part( qq, idx(",,*,*"));  ww.get(); tt(""+ww );
        ww =  new Part( qq, idx(",,,*"));   ww.get(); tt(""+ww );
        ww =  new Part( qq, idx(",3,4,5")); ww.get(); tt(""+ww );
        
        tt( Block.cat());
        Block q3 = new Block("q3", type.INT, new long[]{5,6,4} );
        Part pq3 = new Part(  q3, idx("*,*,*"));
        
        tt("---------- type = "+ pq3.arrtyp );
        pq3.fill( -1 );
        pq3.set(); pq3.get();
        
        ww = new Part( q3, idx("2:3, 2:5, 2:3")); ww.fill( 0 ); ww.set(); 
        pq3.get(); tt( ""+pq3 );
        
        pq3.fill( 0 ); pq3.set();
tt("====================================================================");        
        
        qq.copyBB( idx("1:3, 1:4, 1:4"), q3, Xmem.idx( 2, 2, 1 ) );
        pq3.get(); tt( ""+pq3 );

        Block B = new Block( type.INT, idx("5,6"));
        Part  p = new Part( B, Xmem.idx("2:3,2:4"));
        int[] ii=new int[30]; Arrays.fill( ii, -9 );
        p.setArr( ii );
        p.setIdx( Xmem.idx( ":5,:6" ));
        tt(""+ p );
tt("____________________________________________________________________ end.");
        
    }
    static long[][] idx( String s ){ return Xmem.idx( s );}
    static void tt(String x){System.out.println( x );}static void tt(){tt("");}
 //*/        
}
