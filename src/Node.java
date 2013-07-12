import java.util.*;


public class Node {
	public boolean importance;
	public HashSet<Integer> incidentEdgeIDs;
	
	public int id;
	
	public Graph graph;
	
	// This set is ONLY RELEVANT to the execution of Kruskal's algorithm.
	public HashSet<Integer> kruskalSet;
	
	public Node(int id, boolean importance, HashSet<Integer> incidentEdgeIDs) {
		this.importance = importance;
		this.incidentEdgeIDs = incidentEdgeIDs;
		this.id = id;
		
		resetKruskalSet();
	}
	
	public void giveMeMyGraph(Graph graph) {
		this.graph = graph;
	}
	
	@SuppressWarnings("unchecked")
	public Node copy() {
		return new Node(id, importance, (HashSet<Integer>) incidentEdgeIDs.clone());
	}
	
	public void add(int edgeID) {
		incidentEdgeIDs.add(edgeID);
	}
	
	public void remove(int edgeID) {
		incidentEdgeIDs.remove(edgeID);
	}
	
	public void merge(HashMap<Integer, Edge> IDsToEdges, Node otherNode) {
		importance = importance || otherNode.importance;
		
		incidentEdgeIDs.addAll(otherNode.incidentEdgeIDs);
				
		graph.remove(otherNode);
		
		HashSet<Edge> edgesToBeRemoved = new HashSet<Edge>();
		
		for (int edgeID : incidentEdgeIDs) {
			Edge relevantEdge = IDsToEdges.get(edgeID);
			
			// It must be a (duplicate?) edge between us in this case
			if (relevantEdge.incidentNodes.contains(otherNode) && 
					relevantEdge.incidentNodes.contains(this)) {
				edgesToBeRemoved.add(relevantEdge);
			}
			
			else {
				relevantEdge.incidentNodes.remove(otherNode);
				relevantEdge.updateImportance();				
				relevantEdge.incidentNodes.add(this);
			}
		}
		
		for (Edge edge : edgesToBeRemoved) {
			graph.remove(edge);
			remove(edge.id);
		}
	} 
	
	public void resetKruskalSet() {
		kruskalSet = new HashSet<Integer>();
		kruskalSet.add(id);
	}
}
