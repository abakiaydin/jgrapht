/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Lead:  Barak Naveh (barak_naveh@users.sourceforge.net)
 *
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* -------------
 * Subgraph.java
 * -------------
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 * 24-Jul-2003 : Initial revision (BN);
 * 26-Jul-2003 : Accurate constructors to avoid casting problems (BN);
 *
 */
package org._3pq.jgrapht.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.EdgeFactory;
import org._3pq.jgrapht.GraphListener;
import org._3pq.jgrapht.ListenableGraph;
import org._3pq.jgrapht.UndirectedGraph;
import org._3pq.jgrapht.WeightedElement;

/**
 * A subgraph is a graph that has a subset of vertices and subset of edges with
 * respect to some base graph. More formally, A subgraph G(V,E) that is based
 * on a graph Gb(Vb,Eb) satisfies the following <b><i>subgraph
 * property</i></b>: V is a subset of Vb and E is a subset of Eb. Other than
 * this property, a subgraph is a graph with any respect and fully complies
 * with the <code>Graph</code> interface.
 * 
 * <p>
 * A subgraph is a "live-window" on the the base graph. If edges or vertices
 * are removed from the base graph, they are automatically removed from the
 * subgraph. Subgraph listeners are informed on such removals if they also
 * result in a cascade removal from the subgraph. If edges or vertices are
 * added to the base graph, the subgraph remains unaffected.
 * </p>
 * 
 * <p>
 * Modifications to Subgraph are allowed as long as the subgraph property is
 * maintained. Addition of vertices or edges are allowed as long as they also
 * exist in the base graph. Removal of vertices or edges is always allowed.
 * The base graph is <i>never</i> affected by any modification applied to the
 * subgraph.
 * </p>
 * 
 * <p>
 * PERFORMANCE NOTE: The intention of Subgraph is to provide a "window" on a
 * base graph, so that changes made to vertices or edges in the subgraph are
 * reflected in the base graph, and vice versa. To achieve that, vertices and
 * edges of the subgraph must be reference-equal (and not only value-equal) to
 * their respective ones in the base graph. By default, this class verifies
 * this reference-equality, but at a performance cost. You can avoid this cost
 * by setting the verifyIntegrity flag off. However, you must <i>exercise
 * care</i> and make sure that all vertices or edges you add to the subgraph
 * are reference-equal to the ones contained in the base graph.
 * </p>
 * 
 * <p>
 * At the time of writing, there is no easy and performance-friendly way to
 * programmatically ensure the reference-equality requirement: there is a
 * design flaw in the <code>java.util.Set</code> interface in that it provides
 * no way of obtaining a reference to an object already contained in a Set. If
 * fixed in the future the verifyIntegrity flag could be eliminated.
 * </p>
 *
 * @author Barak Naveh
 *
 * @see Graph
 * @see java.util.Set
 * @see org._3pq.jgrapht.GraphFactory#createListenableGraph(Graph)
 * @since Jul 18, 2003
 */
public class Subgraph extends AbstractGraph {
    private static final String REF_NOT_EQUAL_TO_BASE =
        "value-equal but not reference equal to base graph";
    private static final String NO_SUCH_EDGE_IN_BASE =
        "no such edge in base graph";
    private static final String NO_SUCH_VERTEX_IN_BASE =
        "no such vertex in base graph";

    //
    Set m_edgeSet   = new HashSet(  ); // friendly to improve performance
    Set m_vertexSet = new HashSet(  ); // friendly to improve performance

    // 
    private transient Set   m_unmodifiableEdgeSet   = null;
    private transient Set   m_unmodifiableVertexSet = null;
    private ListenableGraph m_base;
    private boolean         m_verifyIntegrity       = true;

    /**
     * Creates a new Subgraph.
     *
     * @param base the base (backing) graph on which the subgraph will be
     *        based.
     * @param vertexSubset vertices to include in the subgraph. If
     *        <code>null</code> then all vertices are included.
     * @param edgeSubset edges to in include in the subgraph. If
     *        <code>null</code> then all the edges whose vertices found in the
     *        graph are included.
     */
    public Subgraph( ListenableGraph base, Set vertexSubset, Set edgeSubset ) {
        super(  );

        m_base = base;
        m_base.addGraphListener( new BaseGraphListener(  ) );

        addVerticesUsingFilter( base.vertexSet(  ), vertexSubset );
        addEdgesUsingFilter( base.edgeSet(  ), edgeSubset );
    }

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    public List getAllEdges( Object sourceVertex, Object targetVertex ) {
        List edges = null;

        if( containsVertex( sourceVertex ) && containsVertex( targetVertex ) ) {
            edges = new ArrayList(  );

            List baseEdges = m_base.getAllEdges( sourceVertex, targetVertex );

            for( Iterator iter = baseEdges.iterator(  ); iter.hasNext(  ); ) {
                Edge e = (Edge) iter.next(  );

                if( m_edgeSet.contains( e ) ) { // add if subgraph also contains it
                    edges.add( e );
                }
            }
        }

        return edges;
    }


    /**
     * @see Graph#getEdge(Object, Object)
     */
    public Edge getEdge( Object sourceVertex, Object targetVertex ) {
        List edges = getAllEdges( sourceVertex, targetVertex );

        if( edges == null || edges.isEmpty(  ) ) {
            return null;
        }
        else {
            return (Edge) edges.get( 0 );
        }
    }


    /**
     * @see Graph#getEdgeFactory()
     */
    public EdgeFactory getEdgeFactory(  ) {
        return m_base.getEdgeFactory(  );
    }


    /**
     * Sets the the check integrity flag.
     * 
     * <p>
     * WARNING: See discussion in the class description.
     * </p>
     *
     * @param verifyIntegrity
     *
     * @see Subgraph
     */
    public void setVerifyIntegrity( boolean verifyIntegrity ) {
        m_verifyIntegrity = verifyIntegrity;
    }


    /**
     * Returns the value of the verifyIntegrity flag.
     *
     * @return the value of the verifyIntegrity flag.
     */
    public boolean isVerifyIntegrity(  ) {
        return m_verifyIntegrity;
    }


    /**
     * @see Graph#addEdge(Object, Object)
     */
    public Edge addEdge( Object sourceVertex, Object targetVertex ) {
        assertVertexExist( sourceVertex );
        assertVertexExist( targetVertex );

        if( !m_base.containsEdge( sourceVertex, targetVertex ) ) {
            throw new IllegalArgumentException( NO_SUCH_EDGE_IN_BASE );
        }

        List edges = m_base.getAllEdges( sourceVertex, targetVertex );

        for( Iterator iter = edges.iterator(  ); iter.hasNext(  ); ) {
            Edge e = (Edge) iter.next(  );

            if( !containsEdge( e ) ) {
                m_edgeSet.add( e );

                return e;
            }
        }

        return null;
    }


    /**
     * Adds the specified edge to this subgraph. See performance discussion in
     * the class description.
     *
     * @param e the edge to be added.
     *
     * @return <code>true</code> if the edge was added, otherwise
     *         <code>false</code>.
     *
     * @throws NullPointerException
     * @throws IllegalArgumentException
     *
     * @see Subgraph
     * @see Graph#addEdge(Edge)
     */
    public boolean addEdge( Edge e ) {
        if( e == null ) {
            throw new NullPointerException(  );
        }

        if( !m_base.containsEdge( e ) ) {
            throw new IllegalArgumentException( NO_SUCH_EDGE_IN_BASE );
        }

        assertVertexExist( e.getSource(  ) );
        assertVertexExist( e.getTarget(  ) );
        assertBaseContainsEdgeInstance( e );

        if( containsEdge( e ) ) {
            return false;
        }
        else {
            m_edgeSet.add( e );

            return true;
        }
    }


    /**
     * @see org._3pq.jgrapht.WeightedGraph#addEdge(Object, Object, double)
     */
    public Edge addEdge( Object sourceVertex, Object targetVertex, double weight ) {
        Edge e = addEdge( sourceVertex, targetVertex );

        if( e != null ) {
            ( (WeightedElement) e ).setWeight( weight );
        }

        return e;
    }


    /**
     * Adds the specified vertex to this subgraph. See performance discussion
     * in the class description.
     *
     * @param v the vertex to be added.
     *
     * @return <code>true</code> if the vertex was added, otherwise
     *         <code>false</code>.
     *
     * @throws NullPointerException
     * @throws IllegalArgumentException
     *
     * @see Subgraph
     * @see Graph#addVertex(Object)
     */
    public boolean addVertex( Object v ) {
        if( v == null ) {
            throw new NullPointerException(  );
        }

        if( !m_base.containsVertex( v ) ) {
            throw new IllegalArgumentException( NO_SUCH_VERTEX_IN_BASE );
        }

        assertBaseContainsVertexInstance( v );

        if( containsVertex( v ) ) {
            return false;
        }
        else {
            m_vertexSet.add( v );

            return true;
        }
    }


    /**
     * @see Graph#containsEdge(Edge)
     */
    public boolean containsEdge( Edge e ) {
        return m_edgeSet.contains( e );
    }


    /**
     * @see Graph#containsVertex(Object)
     */
    public boolean containsVertex( Object v ) {
        return m_vertexSet.contains( v );
    }


    /**
     * @see UndirectedGraph#degreeOf(Object)
     */
    public int degreeOf( Object vertex ) {
        return ( (UndirectedGraph) m_base ).degreeOf( vertex );
    }


    /**
     * @see Graph#edgeSet()
     */
    public Set edgeSet(  ) {
        if( m_unmodifiableEdgeSet == null ) {
            m_unmodifiableEdgeSet = Collections.unmodifiableSet( m_edgeSet );
        }

        return m_unmodifiableEdgeSet;
    }


    /**
     * @see Graph#edgesOf(Object)
     */
    public List edgesOf( Object vertex ) {
        assertVertexExist( vertex );

        ArrayList edges     = new ArrayList(  );
        List      baseEdges = m_base.edgesOf( vertex );

        for( Iterator iter = baseEdges.iterator(  ); iter.hasNext(  ); ) {
            Edge e = (Edge) iter.next(  );

            if( containsEdge( e ) ) {
                edges.add( e );
            }
        }

        return edges;
    }


    /**
     * @see DirectedGraph#inDegreeOf(Object)
     */
    public int inDegreeOf( Object vertex ) {
        return ( (DirectedGraph) m_base ).inDegreeOf( vertex );
    }


    /**
     * @see DirectedGraph#incomingEdgesOf(Object)
     */
    public List incomingEdgesOf( Object vertex ) {
        return ( (DirectedGraph) m_base ).incomingEdgesOf( vertex );
    }


    /**
     * @see DirectedGraph#outDegreeOf(Object)
     */
    public int outDegreeOf( Object vertex ) {
        return ( (DirectedGraph) m_base ).outDegreeOf( vertex );
    }


    /**
     * @see DirectedGraph#outgoingEdgesOf(Object)
     */
    public List outgoingEdgesOf( Object vertex ) {
        return ( (DirectedGraph) m_base ).outgoingEdgesOf( vertex );
    }


    /**
     * NOTE: We allow to remove an edge by the specified "value-equal" edge
     * that denotes it, which is not necessarily "reference-equal" to the edge
     * to be removed.
     *
     * @see Graph#removeEdge(Edge)
     */
    public boolean removeEdge( Edge e ) {
        return m_edgeSet.remove( e );
    }


    /**
     * @see Graph#removeEdge(Object, Object)
     */
    public Edge removeEdge( Object sourceVertex, Object targetVertex ) {
        Edge e = getEdge( sourceVertex, targetVertex );

        return m_edgeSet.remove( e ) ? e : null;
    }


    /**
     * NOTE: We allow to remove a vertex by the specified "value-equal" vertex
     * that denotes it, which is not necessarily "reference-equal" to the
     * vertex to be removed.
     *
     * @see Graph#removeVertex(Object)
     */
    public boolean removeVertex( Object v ) {
        // if not originally removed from base we remove touching edges.
        // otherwise, base already removed them.
        if( m_base.containsVertex( v ) ) {
            removeAllEdges( edgesOf( v ) );
        }

        return m_vertexSet.remove( v );
    }


    /**
     * @see Graph#vertexSet()
     */
    public Set vertexSet(  ) {
        if( m_unmodifiableVertexSet == null ) {
            m_unmodifiableVertexSet =
                Collections.unmodifiableSet( m_vertexSet );
        }

        return m_unmodifiableVertexSet;
    }


    private void addEdgesUsingFilter( Set edgeSet, Set filter ) {
        Edge    e;
        boolean containsVertices;
        boolean edgeIncluded;

        for( Iterator iter = edgeSet.iterator(  ); iter.hasNext(  ); ) {
            e     = (Edge) iter.next(  );

            containsVertices =
                containsVertex( e.getSource(  ) )
                && containsVertex( e.getTarget(  ) );

            // note short circuit eval            
            edgeIncluded = ( filter == null ) || filter.contains( e );

            if( containsVertices && edgeIncluded ) {
                addEdge( e );
            }
        }
    }


    private void addVerticesUsingFilter( Set vertexSet, Set filter ) {
        Object v;

        for( Iterator iter = vertexSet.iterator(  ); iter.hasNext(  ); ) {
            v = iter.next(  );

            if( filter == null || filter.contains( v ) ) { // note short circuit eval
                addVertex( v );
            }
        }
    }


    private void assertBaseContainsEdgeInstance( Edge e ) {
        if( !m_verifyIntegrity ) {
            return;
        }

        List baseEdges = m_base.getAllEdges( e.getSource(  ), e.getTarget(  ) );

        for( Iterator i = baseEdges.iterator(  ); i.hasNext(  ); ) {
            Edge baseEdge = (Edge) i.next(  );

            if( e.equals( baseEdge ) ) {
                if( e != baseEdge ) {
                    throw new IllegalArgumentException( REF_NOT_EQUAL_TO_BASE );
                }

                return;
            }
        }
    }


    private void assertBaseContainsVertexInstance( Object v ) {
        if( !m_verifyIntegrity ) {
            return;
        }

        for( Iterator iter = m_base.vertexSet(  ).iterator(  );
                iter.hasNext(  ); ) {
            Object baseVertex = iter.next(  );

            if( v.equals( baseVertex ) ) {
                if( v != baseVertex ) {
                    throw new IllegalArgumentException( REF_NOT_EQUAL_TO_BASE );
                }

                return;
            }
        }
    }

    /**
     * An internal listener on the base graph.
     *
     * @author Barak Naveh
     *
     * @since Jul 20, 2003
     */
    private class BaseGraphListener implements GraphListener {
        /**
         * @see GraphListener#edgeAdded(Edge)
         */
        public void edgeAdded( Edge e ) {
            // we don't care
        }


        /**
         * @see GraphListener#edgeRemoved(Edge)
         */
        public void edgeRemoved( Edge e ) {
            if( m_edgeSet.contains( e ) ) {
                removeEdge( e );
            }
        }


        /**
         * @see VertexSetListener#vertexAdded(Object)
         */
        public void vertexAdded( Object v ) {
            // we don't care
        }


        /**
         * @see VertexSetListener#vertexRemoved(Object)
         */
        public void vertexRemoved( Object v ) {
            if( m_vertexSet.contains( v ) ) {
                removeVertex( v );
            }
        }
    }
}
