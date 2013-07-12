import java.util.*;

public class EdgeComparator implements Comparator<Edge> {

	@Override
	public int compare(Edge edge0, Edge edge1) {
		if (edge1.weight > edge0.weight)
			return -1;
		else if (edge0.weight > edge1.weight) 
			return 1;
		else 
			return 0;
	}
}
