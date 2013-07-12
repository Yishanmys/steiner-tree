import java.util.*;

public class Edge {
	public HashSet<Node> incidentNodes;
	
	public int id;
	public double weight;
	
	public boolean importance;
	
	public Graph graph;
		
	public Edge(int id, Node first, Node second, double weight) {
		this.incidentNodes = new HashSet<Node>();
		this.incidentNodes.add(first);
		this.incidentNodes.add(second);
				
		this.id = id;
		this.weight = weight;
		
		this.importance = first.importance && second.importance;
	}
	
	public Edge(int id, HashSet<Node> incidentNodes, double weight) {
		this.incidentNodes = incidentNodes;
		
		this.id = id;
		this.weight = weight;
		
		updateImportance();
	}
	
	public void giveMeMyGraph(Graph graph) {
		this.graph = graph;
	}
	
	public void add(Node node) {
		incidentNodes.add(node);
	}
	
	public void remove(Node node) {
		incidentNodes.add(node);
	}
	
	public void updateImportance() {
		boolean originalImportance = importance;
		
		importance = true;
		
		for (Node node : incidentNodes) {
			importance = importance && node.importance;
		}
		
		if ((!originalImportance) && importance) {
			tellTheGraphAboutYourAscentToGreatness();
		}
	}
	
	public void tellTheGraphAboutYourAscentToGreatness() {
		// The if statement is necessary because if this is called during initialization,
		// we don't need to do anything, and in fact an error will be thrown because the
		// graph hasn't gotten its pants on yet
		if (!(graph == null))
			graph.importantEdgesInGraph.offer(this);
	}
}
