/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.entity;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexField {
    
    private static final Pattern P = Pattern.compile( "(\\d+):(\\d+)" );
    
    private static final String DELIMITER = ",";
    private static final String SEPARATOR = ":";
    
    private String indexContent;
    private List<Integer> indexes;
    
    public IndexField() {
        this( null );
    }
    
    public IndexField(String indexContent) {
        parseIndexContent( indexContent );
    }
    
    public void setIndexContent( String indexContent ) {
        parseIndexContent( indexContent );
    }
    
    public String getIndexContent() {
        if( indexes == null || indexes.isEmpty() ) {
            return null;
        }
        if( StringUtils.isEmpty( indexContent ) ) {
            StringBuilder sbd = new StringBuilder();
            for( int i = 0; i < indexes.size(); i += 2 ) {
                if( sbd.length() > 0 ) {
                    sbd.append( DELIMITER );
                }
                sbd.append( String.valueOf( indexes.get(i) ) )
                   .append( SEPARATOR )
                   .append( String.valueOf( indexes.get( i+1 ) ) );
            }
            this.indexContent = sbd.toString();
        }
        return indexContent;
    }
    
    public int getAt( int indexNo ) {
        if( this.indexes != null ) {
            for( int i = 0; i < this.indexes.size(); i += 2 ) {
                if( this.indexes.get( i ) == indexNo ) {
                    return this.indexes.get( i + 1 );
                }
            }
        }
        return 0;
    }
    
    public void increase( int indexNo ) {
        increase( indexNo, 1 );
    }
    
    public void decrease( int indexNo ) {
        decrease( indexNo, 1 );
    }
    
    public void decrease( int indexNo, int n ) {
        increase( indexNo, -n );
    }
    
    public void increase( int indexNo, int n ) {
        if( this.indexes == null ) {
            this.indexes = new ArrayList<Integer>();
        }
        
        // since the indexes had been changed, so the indexContent need to be rebuilt
        this.indexContent = null;
        
        for( int i = 0; i < this.indexes.size(); i += 2 ) {
            if( this.indexes.get( i ) == indexNo ) {
                int x = this.indexes.get( i + 1 ) + n;
                if( x > 0 ) {
                    this.indexes.set( i + 1, x );
                }else {
                    this.indexes.remove( i ); // i
                    this.indexes.remove( i ); // i + 1
                }
                return;
            }
        }
        
        if( n > 0 ) {
            addAt( indexNo, n );
        }
    }

    /**
     * 获取分段序号及每段个数
     * 
     * @param i 总个数的开始索引，从0开始；正数表示从左至右，负数则从右至左
     * @param n 从i开始的n个数
     * @return Integer[] [0]为第一个分段的数字索引；
     * [1]为第一个分段的开始序号；
     * [2]为第一个分段的个数；
     * 后面依次为其余分段的开始序号及其分段的个数
     */
    public Integer[] range( int i, int n ) {
        List<Integer> tempIndexes;
        if( i >= 0 ) {
            tempIndexes = this.indexes;
        }else {
            tempIndexes = new ArrayList<Integer>();
            for( int z = this.indexes.size() - 1; z > 0; z -= 2 ) {
                tempIndexes.add( this.indexes.get( z - 1 ) );
                tempIndexes.add( this.indexes.get( z ) );
            }
            i = -1 - i;
        }
        
        List<Integer> idx = new ArrayList<Integer>();
        int t = 0;
        for( int k = 0; k < tempIndexes.size() - 1; k += 2 ) {
            int m = tempIndexes.get( k+1 );
            if( i < m ) {
                idx.add( tempIndexes.get( k ) );
                if(idx.size() == 1) {
                    idx.add( i );
                }
                int d = m - i;
                if( (t + d) >= n ) {
                    idx.add( n - t );
                    break;
                }else {
                    idx.add( d );
                    t += d;
                }
                i = 0;
                continue;
            }
            i -= m;
        }
        return idx.toArray( new Integer[]{} );
    }
    
    @Override
    public String toString() {
        return getIndexContent();
    }
    
    public int count() {
        int n = 0;
        if( indexes != null ) {
            for( int i = 0; i < indexes.size(); i += 2 ) {
                n += indexes.get( i + 1 );
            }
        }
        return n;
    }
    
    private void parseIndexContent( String indexContent ) {
        
        this.indexContent = null;
        this.indexes = null;
        
        if( indexContent != null ) {
            indexContent = indexContent.trim();
        }
        
        if( StringUtils.isEmpty( indexContent ) ) {
            return;
        }
        
        String[] pairs = indexContent.split( DELIMITER );
        if( pairs != null ) {
            this.indexes = new ArrayList<Integer>( pairs.length * 2 );
            int i = 0;
            for( String pair : pairs ) {
                Matcher m = P.matcher( pair );
                if( ! m.matches() ) {
                    throw new RuntimeException( "Illegal index pair \"" 
                            + pair + "\" of \"" + this.indexContent + "\"" );
                }
                
                addAt( Integer.parseInt( m.group( 1 ) ),
                       Integer.parseInt( m.group( 2 ) ) );
            }
        }
    }

    private void addAt( int indexNo, int n ) {
        List<Integer> tempList = new ArrayList<Integer>();
        tempList.add( indexNo );
        tempList.add( n );
        int k = tempList.get( 0 );
        int j = this.indexes.size() - 2;
        for( ; j >= 0; j -= 2 ) {
            if( k >= this.indexes.get( j ) ) {
                this.indexes.addAll( j+2, tempList );
                break;
            }
        }
        if( j < 0 ) {
            this.indexes.addAll( 0, tempList );
        }
    }
    
}