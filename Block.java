package com.pik.xmem;

import java.util.LinkedHashMap;
import java.util.TreeMap;

public class Block
{
    static protected Xmem mem;
//  static protected volatile boolean Busy=false; 

    static protected LinkedHashMap < String, Block > blockNamBlk; 
    static protected TreeMap       < Long  , Long  > freeLocLen;
    static private int Seq = 0; 
    
    protected long    loc;
    protected String  nam;
    protected Head    head;
    
    static public void iniBlocks( Xmem xmem )
    {
        mem = xmem;
        freeLocLen = new TreeMap<>();
        freeLocLen.put( 0L, mem.memLen );
        blockNamBlk = new LinkedHashMap<>();
    }
    
    public static Block getBlock( String name ) throws Exception { return blockNamBlk.get( name );}

    public  Block( type type, long[]   idx ) throws Exception { this("_"+(Seq++) ,       type, (Object)idx, 0 );}
    public  Block( type type, long[][] idx ) throws Exception { this("_"+(Seq++) ,       type, (Object)idx, 0 );}
    public  Block( String name, type type, long[]   idx ) throws Exception { this( name, type, (Object)idx, 0 );}
    public  Block( String name, type type, long[][] idx ) throws Exception { this( name, type, (Object)idx, 0 );}
    public  Block( String name, type type, long[]   idx, int extLen ) throws Exception { this( name, type, (Object)idx, extLen );}
    public  Block( String name, type type, long[][] idx, int extLen ) throws Exception { this( name, type, (Object)idx, extLen );}
    public  Block( String name, type type, Object   idx, int extLen ) throws Exception 
    {
        long[] size=null;
        if(      idx instanceof long[]   ){ size = (long[])idx;}
        else if( idx instanceof long[][] ){
            int x = ((long[][]) idx).length;
            size = new long[ x ];
            for(int i=0;i<x;i++) size[i]=((long[][])idx)[i][0];
        }
        else { throw new Exception("BAD INDEX of Class: "+idx.getClass());}
        
        head = new Head( type, size, extLen );
        long sum = writeBlock( name );
        if( sum >0 ){
            if( sum < head.len ) throw new Exception("NO MEMORY");
//          if(1==1)throw new Exception("NO Continuous MEMORY "+head.len);

            while( sum >0 ){
                if( head.len <= concatLastHoles() ) sum = writeBlock( name );
            }
        }
    }

    private long writeBlock( String name )  throws Exception
    {
        long sum=0;
        for( Long pos:   freeLocLen.keySet() ){
             Long free = freeLocLen.get( pos ); sum += free; 
             if(  free >= head.len )
             {
                if( blockNamBlk.get( name ) !=null ) throw new Exception("Block ALREADY EXISTS, "+name); 
                
                head.writeHead ( mem , pos );  nam=name;  loc = pos; 
                blockNamBlk.put( name, this );
                
                freeLocLen.remove( pos ); 
                free -= head.len;
                if( free > 0 ) freeLocLen.put( pos+head.len, free );
                return 0;
             }
        }
        return sum;
    }

    private long concatLastHoles() throws Exception {
        Object[] keys = freeLocLen.keySet().toArray();
        int xx = keys.length;
        if( xx < 2 ) throw new Exception("NO MEMORY");
        
        Long las = (Long)keys[ xx-1 ];
        Long lll = freeLocLen.get( las );
        
        Long pre = (Long)keys[ xx-2 ];
        long ppp = freeLocLen.get( pre );
        
        long dat = pre+ppp;
        long lda = las-dat;
        long fre = ppp+lll;
        
        mem.copyLeft( pre, dat, lda );
        
        freeLocLen.remove( pre );
        freeLocLen.remove( las );
        freeLocLen.put( pre+lda, fre );
        
        for( String name: blockNamBlk.keySet()){
            Block blk = blockNamBlk.get( name );
            if( dat <= blk.loc && blk.loc <=las ) blk.loc-=ppp;
        }
        return fre;
    }

    public void delete() throws Exception
    {
        long len = head.len;
        blockNamBlk.remove( nam );  long nex = loc + len;
        for( Long free: freeLocLen.keySet()){
            if( nex == free ){
                len += freeLocLen.get( free ); 
                freeLocLen.remove( free );
                break;
            }
        }
        boolean mustPut=true;  // ( loc,len )
        
        for( Long free: freeLocLen.keySet()){
             long lfree = freeLocLen.get( free );
             if( free + lfree == loc ){
                 freeLocLen.remove( free );
                 freeLocLen.put( free, lfree + len );
                 mustPut = false;
                 break;
             }
        }
        if( mustPut ) freeLocLen.put( loc, len );
    }

    static public void delete( String name ) throws Exception { getBlock( name ).delete();}
    
    public void copyExt( byte[] ext, boolean put ) throws Exception {
        if( ext !=null && head.exx >0 ){
            int xx = Math.min( ext.length, head.exx );
            if( xx >0 ) mem.copyArr( loc+8*(2+head.dim), ext, put ); 
        }
    }
    
    public String toString(){ return "Block: \""+nam+"\" "+head+", loc="+loc+", dat="+(loc+head.off);}
    
    static public String cat() throws Exception {
        String s= "\n### Catalog:"; int i=0;
        for( String name: blockNamBlk.keySet()) s+="\n    "+(++i)+".\t\t"+getBlock( name );
        s+="\n### Holes:"; i=0; long free=0;
        for( Long p: freeLocLen.keySet()){ 
            long h=freeLocLen.get( p ); free+=h;  
            s+="\n    "+(++i)+".\t\t"+p+"\t\t"+freeLocLen.get( p );
        }
        return s+"\n-------------------------------------------------\n\t\tFree:\t\t"+free;
    }
    
    public void copyALL( Object arr, boolean put) throws Exception { mem.copyArr( loc+head.off, arr, put );}
    
    static public int arrLen( long[] size ){ int xx=1; for( long l: size ) xx *= l; return xx;}
    
    public String ttBlock()  throws Exception {
        String s ="### "+this +"\n";
        Object arr = crePart( head.siz );  int xx=arrLen( head.siz );
        type typ = type.val( head.typ );
        
        mem.copyArr( loc+head.off, arr, false );
        for(int i=0;i<xx;i++){
            switch( typ ){
                case BYTE:   s+=" "+((byte  [])arr)[i]; break;
                case SHORT:  s+=" "+((short [])arr)[i]; break;
                case INT:    s+=" "+((int   [])arr)[i]; break;
                case LONG:   s+=" "+((long  [])arr)[i]; break;
                case FLOAT:  s+=" "+((float [])arr)[i]; break;
                case DOUBLE: s+=" "+((double[])arr)[i];
            }
        }
        return s;
    }

    public long[] getPartSize( long[][] idx ) throws Exception {
        long[][] reInd = Xmem.realIndex( idx, head );
        long[] partSize = new long[ reInd.length ];
        for( int i=0;i<partSize.length;i++) partSize[i] = reInd[i][1];
        return partSize;
    }
    
    public Object crePart( long[][] idx ) throws Exception { return crePart( getPartSize( idx ));}
    
    public Object crePart( long[] partSize ){
        int xx = arrLen( partSize );
        Object arr=null;
        switch( type.val( head.typ )){
            case BYTE:   arr = new byte  [ xx ]; break;
            case SHORT:  arr = new short [ xx ]; break;
            case INT:    arr = new int   [ xx ]; break;
            case LONG:   arr = new long  [ xx ]; break;
            case FLOAT:  arr = new float [ xx ]; break;
            case DOUBLE: arr = new double[ xx ];
        }
        return arr;
    }
//------------------------------------------------------------------------------ copyPart -> arr:
    
    public void part( long[][] ix, Object arr, boolean put ) throws Exception 
    {
        rel = Xmem.realIndex( ix, head );
        klen = rel.length;
        kk = new long[klen][2]; for(int i=0;i<klen;i++){ kk[i][0]=rel[i][0]; kk[i][1]=rel[i][1];}
        narr = (int)rel[0][1];
        
        kd = new long[ klen ];
        for( int i=0; i<klen; i++) {
            kd[i] = head.nb;
            for( int j=1;j<=i;j++) kd[i] *= head.siz[ j-1 ];  // 1,I,IJ,IJK...
        }
        
        pdat = loc + head.off; bufAct=put;
        sarr = arr; parr=0; partyp=type.val( head.typ );
        buf=false;  bufPos=0;  bufLen=0;

            copyRecurs( klen-1 );
            
        bufCopy( 0, 0, 0 );   
    }
    
    private Object sarr;
    private type partyp;
    private int klen, parr, narr;
    private long pdat;
    private long[][] rel, kk;
    private long[]   kd;
    private boolean buf, bufAct; long bufPos; int bufPar, bufLen;
    
    private void copyRecurs( int p ) throws Exception
    {
        if( p > 0 ) {
           long xx = kk[p][0]+kk[p][1]; 
           while( kk[p][0] < xx )
           {
               copyRecurs( p-1 );
               kk[p][0]++;
           }
           kk[p][0] = rel[p][0];  // restore index used !!!
       }
       else {  // p==0
           long pos = pdat; 
           for(int i=0;i<klen;i++) pos += (kk[i][0]-1) * kd[i];
           
//         mem.copyArr( pos, sarr, parr, narr, bufAct );       // use of BUFF now !!!
           bufCopy( pos, parr, narr );
           parr += narr;
       }
    }
    private void bufCopy( long pos, int parr, int narr ) throws Exception {
        if( buf ){
            if( bufPos + bufLen*head.nb == pos ) { bufLen += narr; return;}
            else mem.copyArr( bufPos, sarr, bufPar, bufLen, partyp, bufAct );
        }
        buf=true;  bufPos = pos;  bufPar = parr;   bufLen = narr;    
    }
//------------------------------------------------------------------------------ copyPart -> Blk:
    
    private long[][] reb,bk; long[] bd; long pdaB, iii;
    
    public void copyBB( long[][] idA, Block B, long[][] idB ) throws Exception {
        if( head.typ != B.head.typ ) throw new Exception("Blk.TYPE");
        
        rel = Xmem.realIndex( idA, head );

        kk  = Xmem.copyL2( rel ); klen = rel.length;
        kd  = new long[ klen ];
        for( int i=0;i<klen; i++) {
             kd[i] = head.nb;
             for( int j=1;j<=i;j++) kd[i] *= head.siz[ j-1 ];  // 1,I,IJ,IJK...
        }

        reb = Xmem.copyL2( rel );                                    // set ii, size  
        long[][] rb = Xmem.realIndex( idB, B.head );
        for(int i=0;i<klen;i++){
            reb[i][0] = i<rb.length? rb[i][0]: 1; // set i
            if( reb[i][0]+reb[i][1]-1 > B.head.siz[i] )
                throw new Exception("BAD Index "+(i+1)+": "+(reb[i][0]+reb[i][1]-1)+" > size");
        }

        bk  = Xmem.copyL2( reb );
        bd  = new long[ klen ];
        for( int i=0; i<klen; i++) {
             bd[i] = B.head.nb;
             for( int j=1;j<=i;j++) bd[i] *= B.head.siz[ j-1 ];  // 1,I,IJ,IJK...
        }
         
        pdat =   loc +   head.off;
        pdaB = B.loc + B.head.off;  iii = head.nb*rel[0][1];

        copyBBrecurs( klen-1 );
    }
    private void copyBBrecurs( int p ) throws Exception
    {
        if( p > 0 ) {
           long xx = kk[p][0]+kk[p][1]; 
           while( kk[p][0] < xx )
           {
               copyBBrecurs( p-1 );
               kk[p][0]++;
               bk[p][0]++;
           }
           kk[p][0] = rel[p][0];  // restore index used !!!
           bk[p][0] = reb[p][0];  // restore index used !!!
       }
       else {  // p==0
           long pos = pdat; for(int i=0;i<klen;i++) pos += (kk[i][0]-1) * kd[i];
           long bbb = pdaB; for(int i=0;i<klen;i++) bbb += (bk[i][0]-1) * bd[i]; 
           mem.copyLeft( bbb, pos, iii );
       }
    }
    
///* DBG: =====================================================================================================
                                                                static final boolean PUT=true, GET=false;
    public static void main( String[] args ) throws Exception
    {
//      Xmem mem = new Xmem( 512+1300 );
        new Xmem( 512+1300 + 5*7*9*8 + 30 );
        
        Block aa1 = new Block( "aa1", type.DOUBLE, new long[]{2,3} );
        
        Block lon2 = new Block( "lon2", type.LONG, new long[]{2,3,2}, 16 );
        lon2.copyALL(new long[]{-1,-2,-3,-4,-5,-6,-7,-8,-9,-10,-11,-12}, PUT );
        
        Block byt3 = new Block( "byt3", type.BYTE, new long[]{1,12,2}, 8 );
        Block int4 = new Block( "int4", type.INT,  new long[]{2,3,3} );
        int4.copyALL( new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18}, PUT ); 
        
        Block sh5 = new Block( type.SHORT, new long[]{4,2} ); 
        Block bb6 = new Block( type.BYTE, new long[]{8,2} );
        bb6.copyALL( new byte[]{-1,-2,-3,-4,-5,-6,-7,-8, 1,2,3,4,5,6,7,8}, PUT );
        
tt("\n_______ 0: "+ bb6.ttBlock());
tt( cat());

        sh5.delete();
        delete("byt3");
//        lon2.delete();
//        int4.delete();
        delete("aa1");
tt( cat());

tt("\n_______ 1: "+ bb6.ttBlock());
tt( int4.ttBlock());
tt( lon2.ttBlock());

        Block int7 = new Block( "int7", type.INT,  new long[]{9,2,2} );
        
tt("\n_______ 2: "+ bb6.ttBlock());
tt( int4.ttBlock());
tt( lon2.ttBlock());
        
        Block ijk = new Block("ijk", type.INT, new long[]{ 5,7,9 } );
        int[] dd = new int[ 5*7*9 ]; int p=0;
        for(int k=1;k<=9;k++)
            for(int j=1;j<=7;j++)
                for(int i=1;i<=5;i++) dd[p++] = 100*i + 10*j + k;
        ijk.copyALL( dd, PUT );
tt("[ "+dd.length+" ]: "+ijk.ttBlock());

        long[][] idx = Xmem.idx("2:3,2:4,2:5");
tt( Xmem.idx2str( idx ));

        int[] pat = (int[])ijk.crePart( idx );  String s="part["+pat.length+"];";for( int q: pat )s+=" "+q; tt( s );
        ijk.part( idx, pat, GET );                     s="part["+pat.length+"];";for( int q: pat )s+=" "+q; tt( s );
        
        idx = Xmem.idx("*,*,*");
        int[] all = (int[]) ijk.crePart( idx );
        ijk.part( idx, all, GET ); s="all["+all.length+"];";for(int q: all)s+=" "+q; tt( s );
        
tt("___________ part:");        
        Part ppp = new Part( ijk, Xmem.idx("2:3,3:4,5"));
        ppp.get();
        tt( ""+ppp);
        
        Block r579 = new Block("r579", type.DOUBLE, new long[]{ 5,7,9 } );
        Part rrr = new Part( r579, Xmem.idx("*,*,*"));
        for(int k=1;k<=9;k++)
            for(int j=1;j<=7;j++)
                for(int i=1;i<=5;i++) rrr.put( Double.valueOf( 100*i + 10*j + k ), new int[]{i,j,k});
        rrr.set();
        
        idx = Xmem.idx("2:3,2:4,2:5");        
tt("\n"+idx);        
        Part rr = new Part( r579, idx );
        rr.get();
tt("idx[][]:  "+ Xmem.idx2str( idx ));        
        tt( "############################ "+rr);
        
        tt( cat());       
        tt( "need = "+5*7*9*8);
        
tt("______________________________________________end.");
    }
    static void tt(String x){System.out.println( x );}static void tt(){tt("");}
//*/    
}
