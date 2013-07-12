import java.util.*;


public class Graph {
	public HashSet<Node> nodesInGraph;
	public HashSet<Edge> edgesInGraph;
	
	public PriorityQueue<Edge> importantEdgesInGraph;
	public HashSet<Edge> unimportantEdgesInGraph;
	
	public HashMap<Integer, Edge> IDsToEdges;
	public HashMap<Integer, Node> IDsToNodes;
	
	public int unimportantNodeCount = 0; 
	
	public Graph(HashSet<Node> nodesInGraph, HashSet<Edge> edgesInGraph, HashMap<Integer, Edge> IDsToEdges) {
		this.nodesInGraph = nodesInGraph;
		this.edgesInGraph = edgesInGraph;
		this.IDsToEdges = IDsToEdges;
		
		this.IDsToNodes = new HashMap<Integer, Node>();
		
		for (Node node : nodesInGraph) {
			IDsToNodes.put(node.id, node);
			
			if (!node.importance) {
				unimportantNodeCount++;
			}
			if (node.incidentEdgeIDs.size() == 0) {
				remove(node);
			}
		}
		
		importantEdgesInGraph = new PriorityQueue<Edge>(edgesInGraph.size(), new EdgeComparator());
		unimportantEdgesInGraph = new HashSet<Edge>();
		
		for (Edge edge : edgesInGraph) {
			if (edge.importance) {
				importantEdgesInGraph.offer(edge);
			}
			else {
				unimportantEdgesInGraph.add(edge);
			}
		}
	}
	
	public Graph copy() {
		HashSet<Node> newNodesInGraph = new HashSet<Node>();
		HashSet<Edge> newEdgesInGraph = new HashSet<Edge>();
		HashMap<Integer, Edge> newIDsToEdges = new HashMap<Integer, Edge>();
		
		HashMap<Integer, Node> IDsToNodes = new HashMap<Integer, Node>();
		
		Node newNode;
		
		for (Node node : nodesInGraph) {
			newNode = node.copy();
			IDsToNodes.put(node.id, newNode);
			newNodesInGraph.add(newNode);
		}
		
		Edge newEdge;
		
		HashSet<Node> newEdgeIncidentNodes;
		
		for (Edge edge : edgesInGraph) {
			newEdgeIncidentNodes = new HashSet<Node>();
			
			for (Node node : edge.incidentNodes) {
				newEdgeIncidentNodes.add(IDsToNodes.get(node.id));
			}
				
			newEdge = new Edge(edge.id, newEdgeIncidentNodes, edge.weight);
			
			newEdgesInGraph.add(newEdge);
			newIDsToEdges.put(newEdge.id, newEdge);
		}
		
		Graph graphCopy = new Graph(newNodesInGraph, newEdgesInGraph, newIDsToEdges);
		graphCopy.worshipMe();
		
		return graphCopy;
 	}
	
	public void worshipMe() {
		for (Node node : nodesInGraph) {
			node.giveMeMyGraph(this);
		}
		for (Edge edge : edgesInGraph) {
			edge.giveMeMyGraph(this);
		}
	}
	
	// Must not modify the graph
	public double approximateEdgeBonus(int edgeID, int depth) {
		HashSet<Integer> nodeIDsInGraph = new HashSet<Integer>();
		
		for (Node node : nodesInGraph) {
			nodeIDsInGraph.add(node.id);
		}	
		
		Edge relevantEdge = IDsToEdges.get(edgeID);
							
		if (relevantEdge.importance) {
			// Then we must compute the bonus stupidly, by recursing with the steiner tree.
			Graph graph1 = this.copy();
			graph1.fullRemove(graph1.IDsToEdges.get(relevantEdge.id));
			Graph graph2 = this.copy();
			
			return graph1.minimumSteinerTree() - graph2.minimumSteinerTree();
		}
		
		double bonusValue = -relevantEdge.weight;
		
		Node[] nodeArray = importanceNodeArray(relevantEdge);
		
		Node maybeImportantNode = nodeArray[0];
		Node unimportantNode = nodeArray[1];
		
		HashSet<Integer> unimportantNodeEdges = unimportantNode.incidentEdgeIDs;
		HashSet<Node> neighborsAndMe = new HashSet<Node>(); 
		
		for (int unimportantEdgeID : unimportantNodeEdges) {
			for (Node node : IDsToEdges.get(unimportantEdgeID).incidentNodes) {
				neighborsAndMe.add(node);
			}
		}
		
		if (neighborsAndMe.size() == 2) {
			return 0.0;
		}
		
		Graph mergerGraph = copy();
		bonusValue += mergerGraph.approximateMergerGraphHalfBonus(relevantEdge.id, maybeImportantNode.id,
				unimportantNode.id, depth - 1);
		
		Graph removalGraph = copy();
		bonusValue -= removalGraph.approximateRemovalGraphHalfBonus(relevantEdge.id, unimportantNode.id,
				depth - 1);
		
		return Math.max(bonusValue, 0);
	}
	
	// The part of the bonus that generally comes from the merged graph.
	public double approximateMergerGraphHalfBonus(int edgeToMergeAcross, int idOfMaybeImportantNode, 
			int idOfUnimportantNode, int depth) {
		
		double returnCounter = 0;
		
		Node unimportantNode = null;
		Node maybeImportantNode = null;
		
		for (Node node : IDsToEdges.get(edgeToMergeAcross).incidentNodes) {
			if (node.id == idOfMaybeImportantNode) 
				maybeImportantNode = node;
			if (node.id == idOfUnimportantNode) 
				unimportantNode = node;
		}
		
		HashSet<Integer> unimportantNodeEdges = unimportantNode.incidentEdgeIDs;
		
		maybeImportantNode.merge(IDsToEdges, unimportantNode);
		
		Graph graphCopy;
				
		for (int edgeID : unimportantNodeEdges) {
			if (IDsToEdges.keySet().contains(edgeID)) {		
				// Might have been an edge we merged across
				graphCopy = copy();
				returnCounter += graphCopy.approximateEdgeBonus(edgeID, depth);
				fullRemove(IDsToEdges.get(edgeID));
			}
		}
		
		return returnCounter;
	}
	
	@SuppressWarnings("unchecked")
	public double approximateRemovalGraphHalfBonus(int edgeToRemove, int idOfUnimportantNode, int depth) {
		
		double returnCounter = 0;
		
		Node unimportantNode = null;
		
		for (Node node : IDsToEdges.get(edgeToRemove).incidentNodes) {
			if (node.id == idOfUnimportantNode)
				unimportantNode = node;
		}
		
		HashSet<Integer> unimportantNodeEdges = unimportantNode.incidentEdgeIDs;
		
		fullRemove(IDsToEdges.get(edgeToRemove));
		
		Graph graphCopy;
		
		HashSet<Integer> unimportantNodeEdgesClone = (HashSet<Integer>) unimportantNodeEdges.clone();
		
		for (int edgeID : unimportantNodeEdgesClone) {
			if (!(edgeID == edgeToRemove)) {
				graphCopy = copy();
				returnCounter += graphCopy.approximateEdgeBonus(edgeID, depth);
				fullRemove(IDsToEdges.get(edgeID));
			}
		}
		
		return returnCounter;
	}
	
	public void remove(Node node) {
		nodesInGraph.remove(node);
		IDsToNodes.remove(node.id);
		
		if (!node.importance) {
			unimportantNodeCount--;
		}
	}
	
	public void remove(Edge edge) {
		edgesInGraph.remove(edge);
		unimportantEdgesInGraph.remove(edge);
				
		importantEdgesInGraph.remove(edge);
		
		IDsToEdges.remove(edge.id);
	}
	
	public void fullRemove(Edge edge) {
		remove(edge);
		for (Node node : edge.incidentNodes) {
			node.incidentEdgeIDs.remove(edge.id);
			if (node.incidentEdgeIDs.size() == 0) {
				remove(node);
			}
		}
	}
	
	public void remove(int edgeID) {
		Edge edge = IDsToEdges.get(edgeID);
		
		edgesInGraph.remove(edge);
		unimportantEdgesInGraph.remove(edge);
		importantEdgesInGraph.remove(edge);
		
		IDsToEdges.remove(edgeID);
	}
	
/*	private void add(Node node) {
		nodesInGraph.add(node);
		IDsToNodes.put(node.id, node);
	}
	
	private void add(Edge edge) {
		edgesInGraph.add(edge);
		IDsToEdges.put(edge.id, edge);
	} */
	
	public double minimumSteinerTree() {
		if (unimportantNodeCount == 0) {
			double kruskal = kruskal();
			return kruskal;
		}
		
		else {
			Edge edgeToComputeBonus = randomChoose(unimportantEdgesInGraph);
			double bonus = approximateEdgeBonus(edgeToComputeBonus.id, 10000);
			
			if (bonus > 0) {
				double weight = edgeToComputeBonus.weight;
				
				Node[] nodeArray = importanceNodeArray(edgeToComputeBonus);
				
				Node maybeImportantNode = nodeArray[0];
				Node unimportantNode = nodeArray[1];
							
				maybeImportantNode.merge(IDsToEdges, unimportantNode);
				
				return weight + minimumSteinerTree();
			}
			
			else {
				fullRemove(edgeToComputeBonus);
				
				return minimumSteinerTree();
			}
		}
	}
	
	// This method fucks with the graph! Be careful!
	// This finds the minimum spanning tree on the graph with Kruskal's algorithm.
	// Bad result if not all nodes in the graph are important.
	@SuppressWarnings("unchecked")
	public double kruskal() {
		Node node1;
		Node node2;
		
		HashSet<Integer> node1kruskalSetCopy;
		HashSet<Integer> node2kruskalSetCopy;
		
		double treeCounter = 0.0;
				
		while (!importantEdgesInGraph.isEmpty()) {
			Edge bestEdge = importantEdgesInGraph.poll();
			
			Node[] nodeArray = arbitraryNodeArray(bestEdge);
			
			node1 = nodeArray[0];
			node2 = nodeArray[1];
			if (!node1.kruskalSet.contains(node2.id)) {				
				treeCounter += bestEdge.weight;
				
				node1kruskalSetCopy = (HashSet<Integer>) node1.kruskalSet.clone();
				node2kruskalSetCopy = (HashSet<Integer>) node2.kruskalSet.clone();
				
				for (int nodeID : node1kruskalSetCopy) {
					IDsToNodes.get(nodeID).kruskalSet.addAll(node2.kruskalSet);
				}
				for (int nodeID : node2kruskalSetCopy) {
					IDsToNodes.get(nodeID).kruskalSet.addAll(node1.kruskalSet);
				}
			}
		}
		
		return treeCounter;
	}

	public <E> E randomChoose(HashSet<E> set) {
		@SuppressWarnings("unchecked")
		E[] choiceArray = (E[]) set.toArray();
		Random rand = new Random();
		return choiceArray[rand.nextInt(choiceArray.length)];
	}
	
	// Returns first the maybe important node, and then the definitely unimportant node.
	// Bad result for an edge connecting two important nodes.
	public Node[] importanceNodeArray(Edge edge) {
		
		Node[] nodeArray = new Node[2];
		
		for (Node node : edge.incidentNodes) {
			if (node.importance) {
				nodeArray[0] = node;
			}
			else {
				if (nodeArray[1] == null) {
					nodeArray[1] = node;
				}
				else {
					nodeArray[0] = node;
				}
			}
		}
		
		return nodeArray;
	}
	
	public Node[] arbitraryNodeArray(Edge edge) {
		Node[] nodeArray = new Node[2];
		
		for (Node node : edge.incidentNodes) {
			if (nodeArray[0] == null) {
				nodeArray[0] = node;
			}
			else {
				nodeArray[1] = node;
			}
		}
		
		return nodeArray;
	}
	
    public static void main(String[] args) {
    	
    	HashSet<Integer> incidentEdges = new HashSet<Integer>();
    	incidentEdges.add(1);
    	incidentEdges.add(2);
    	incidentEdges.add(4);
    	Node topNode = new Node(1, true, incidentEdges);
    	
    	incidentEdges = new HashSet<Integer>();
    	incidentEdges.add(2);
    	incidentEdges.add(3);
    	incidentEdges.add(5);
    	Node rightNode = new Node(2, true, incidentEdges);
    	
    	incidentEdges = new HashSet<Integer>();
    	incidentEdges.add(1);
    	incidentEdges.add(3);
    	incidentEdges.add(6);
    	Node leftNode = new Node(3, true, incidentEdges);
    	
    	incidentEdges = new HashSet<Integer>();
    	incidentEdges.add(4);
    	incidentEdges.add(5);
    	incidentEdges.add(6);
    	Node middleNode = new Node(4, false, incidentEdges);
    	
    	Edge edge1 = new Edge(1, topNode, leftNode, 7.0);
    	Edge edge2 = new Edge(2, topNode, rightNode, 7.0);
    	Edge edge3 = new Edge(3, leftNode, rightNode, 7.0);
    	Edge edge4 = new Edge(4, topNode, middleNode, 4.0);
    	Edge edge5 = new Edge(5, rightNode, middleNode, 4.0);
    	Edge edge6 = new Edge(6, leftNode, middleNode, 4.0);
    	
    	HashSet<Node> nodesInGraph = new HashSet<Node>();
    	nodesInGraph.add(topNode);
    	nodesInGraph.add(rightNode);
    	nodesInGraph.add(leftNode);
    	nodesInGraph.add(middleNode);
    	
    	HashSet<Edge> edgesInGraph = new HashSet<Edge>();
    	edgesInGraph.add(edge1);
    	edgesInGraph.add(edge2);
    	edgesInGraph.add(edge3);
    	edgesInGraph.add(edge4);
    	edgesInGraph.add(edge5);
    	edgesInGraph.add(edge6);
    	
    	HashMap<Integer, Edge> IDsToEdges = new HashMap<Integer, Edge>();
    	IDsToEdges.put(1, edge1);
    	IDsToEdges.put(2, edge2);
    	IDsToEdges.put(3, edge3);
    	IDsToEdges.put(4, edge4);
    	IDsToEdges.put(5, edge5);
    	IDsToEdges.put(6, edge6);
    	
    	Graph graph = new Graph(nodesInGraph, edgesInGraph, IDsToEdges);
    	graph.worshipMe();
    	System.out.println(graph.approximateEdgeBonus(6, 0));
    	System.out.println(graph.minimumSteinerTree());
    }
}
