package algorithm;

import java.util.Comparator;

/**
 * MatchInfoComparator class
 * For heaps that use Match object
 * @author ksemer
 *
 */
public class MatchComparator implements Comparator<Match> {
	private boolean maxHeap;

	public MatchComparator(boolean b) {
		maxHeap = b;
	}

	@Override
	public int compare(Match mI1, Match mI2) {
		if (maxHeap)
			return Integer.compare(mI2.getDuration(), mI1.getDuration());

		return Integer.compare(mI1.getDuration(), mI2.getDuration());
	}
}