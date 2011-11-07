package org.jruby.compiler.ir.util;

/**
 *
 */
public class Edge<T extends DataInfo> {
    private Vertex<T> source;
    private Vertex<T> destination;
    private Object type;
    
    public Edge(Vertex<T> source, Vertex<T> destination, Object type) {
        this.source = source;
        this.destination = destination;
        this.type = type;

    }
    
    public Vertex<T> getDestination() {
        return destination;
    }

    public Vertex<T> getSource() {
        return source;
    }

    public Object getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "<" + source.getData().getID() + " --> " + 
                destination.getData().getID() + "> (" + type + ")";        
    }
}
