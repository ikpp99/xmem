package com.pik.xmem;

public enum type 
{
    BYTE   ( 1 ),  // 0
    SHORT  ( 2 ),  // 1
    INT    ( 4 ),  // 2
    LONG   ( 8 ),  // 3
    FLOAT  ( 4 ),  // 4
    DOUBLE ( 8 );  // 5
    
    private type( int nbytes ){ nb=nbytes;}
    
    private int           nb;
    public  int        getNb     (){ return nb ;}
    static public type val( int i ){ return i<values().length? values()[ i ]: null;}
    
    static public Object creArr( type typ, int len ) 
    {
        switch( typ ) {
            case BYTE  :  return new byte  [ len ];
            case SHORT :  return new short [ len ];
            case INT   :  return new int   [ len ];
            case LONG  :  return new long  [ len ];
            case FLOAT :  return new float [ len ];
            case DOUBLE:  return new double[ len ];
        }
        return null;
    }

    static public int arrLen( type typ, Object arr ){
        switch( typ ) {
            case BYTE  :  return ((byte  [])arr).length;
            case SHORT :  return ((short [])arr).length;
            case INT   :  return ((int   [])arr).length;
            case LONG  :  return ((long  [])arr).length;
            case FLOAT :  return ((float [])arr).length;
            case DOUBLE:  return ((double[])arr).length;
        }
        return -1;
    }
}
