package net.xqhs.graphs.context;

import net.xqhs.graphs.matcher.Match;

/**
 * The interface is a general interface for classes implementing a matching process that happens in the background (in a
 * different thread).
 * <p>
 * The provided methods allow for controlling the process and adding or removing notification targets.
 *
 * @author Andrei Olaru
 */
public interface ContinuousMatchingProcess
{
	/**
	 * An implementation is able to act as a receiver for match notifications.
	 * <p>
	 * It is recommended that implementations return quickly from the receiver method and delegate any lengthy
	 * processing to a different thread.
	 *
	 * @author Andrei Olaru
	 */
	public interface MatchNotificationReceiver
	{
		/**
		 * The method is called by a {@link ContinuousMatchingProcess} when a match is detected that conforms to the
		 * notification settings.
		 *
		 * @param platform
		 *            - the platform that issued the notification.
		 * @param m
		 *            - the match for which the notification has been issued.
		 */
		public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m);
	}

	/**
	 * Registers a {@link MatchNotificationReceiver} as target for notifications for new matches below or with a
	 * specified <i>k</i> (see {@link Match}.
	 *
	 * @param thresholdK
	 *            - the threshold <i>k</i>.
	 * @param receiver
	 *            - the receiver for the notifications.
	 * @return the process itself.
	 */
	public ContinuousMatchingProcess addMatchNotificationTarget(int thresholdK, MatchNotificationReceiver receiver);

	/**
	 * Registers a {@link MatchNotificationReceiver} as target for notifications for any new matches, no matter how
	 * small.
	 *
	 * @param receiver
	 *            - the receiver for the notifications.
	 * @return the process itself.
	 */
	public ContinuousMatchingProcess addMatchNotificationTarget(MatchNotificationReceiver receiver);

	/**
	 * Removes all registrations of the specified {@link MatchNotificationReceiver}.
	 *
	 * @param receiver
	 *            - the specified notification receiver.
	 * @return the process itself.
	 */
	public ContinuousMatchingProcess removeMatchNotificationTarget(MatchNotificationReceiver receiver);

	/**
	 * Instructs the process to start or to resume (from the state in which it was stopped). It may already be ongoing.
	 *
	 * @return the process itself.
	 */
	public ContinuousMatchingProcess startContinuousMatching();

	/**
	 * Instructs the process to stop. State will be kept until the process is resumed. It may already be stopped.
	 *
	 * @return the process itself.
	 */
	public ContinuousMatchingProcess stopContinuousMatching();

	/**
	 * @return <code>true</code> if the process is currently ongoing (in a separate thread). <code>false</code>
	 *         otherwise.
	 */
	public boolean isContinuouslyMatching();

}
