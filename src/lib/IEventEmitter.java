package lib;

import java.util.function.BiConsumer;

/**
 * @author Janty Azmat
 */


/**
 * The interface needed for an event-emitter with base methods that make it easier to handle listeners.
 * @author Janty Azmat
 * @param <Ev>	an enumeration type that holds the events' constants.
 */
public interface IEventEmitter<Ev extends Enum<Ev>> {

	/**
	 * Adds a specific listener for the specified event.
	 * @param theEvent			the event to add a listener to.
	 * @param eventListener		the event listener to be added.
	 * @return					'true' if 'eventListener' was not already added.
	 */
	boolean addEventListener(Ev theEvent, BiConsumer<IEventEmitter<Ev>, Object> eventListener);

	/**
	 * Removes a specific listener for the specified event.
	 * @param theEvent			the event to remove its listener.
	 * @param eventListener		the event listener to be removed.
	 * @return					'true' if the specified listener was there and was removed.
	 */
	boolean removeEventListener(Ev theEvent, BiConsumer<IEventEmitter<Ev>, Object> eventListener);

	/**
	 * Emits the specified event by/and invoking all the listeners associated with the event.
	 * @param theEvent		the event to be emitted.
	 * @param eventData		the extra data to be sent about the event (can be null).
	 */
	void emitEvent(Ev theEvent, Object eventData);

	/**
	 * Emits the specified event by/and invoking all the listeners associated with the event.
	 * @param theEvent	the event to be emitted.
	 */
	void emitEvent(Ev theEvent);
}
