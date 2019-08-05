package lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * An abstract class that represents an event-emitter with with base methods that make it easier to handle listeners.
 * @author Janty Azmat
 *
 * @param <Ev>	an enumeration type that holds the events' constants.
 */
public abstract class AbsEventEmitter<Ev extends Enum<Ev>> {
	// Fields
	private Map<Ev, Set<BiConsumer<AbsEventEmitter<Ev>, Object>>> meListeners;

	/**
	 * Constructor that takes the enumeration class (that holds the events constants) as a parameter.
	 * @param eventsEnumType	the class of the enumeration that holds the events' constants.
	 */
	public AbsEventEmitter(Class<Ev> eventsEnumType) {
		var tmpConsts = eventsEnumType.getEnumConstants();
		this.meListeners = new HashMap<>(tmpConsts.length);
		for (var tmpEv : tmpConsts) {
			this.meListeners.put(tmpEv, new HashSet<>());
		}
	}

	/**
	 * Adds a specific listener for the specified event.
	 * @param theEvent		the event to add a listener to.
	 * @param eventListener	the event listener to be added.
	 * @return				'true' if 'eventListener' was not already added.
	 */
	public boolean addEventListener(Ev theEvent, BiConsumer<AbsEventEmitter<Ev>, Object> eventListener) {
		Objects.requireNonNull(theEvent, "The 'theEvent' argument cannot be 'null'.");
		Objects.requireNonNull(eventListener, "The 'eventListener' argument cannot be 'null'.");
		return this.meListeners.get(theEvent).add(eventListener);
	}

	/**
	 * Removes a specific listener for the specified event.
	 * @param theEvent		the event to remove its listener.
	 * @param eventListener	the event listener to be removed.
	 * @return				'true' if the specified listener was there and was removed.
	 */
	public boolean removeEventListener(Ev theEvent, BiConsumer<AbsEventEmitter<Ev>, Object> eventListener) {
		return this.meListeners.get(theEvent).remove(eventListener);
	}

	/**
	 * Emits the specified event by/and invoking all the listeners associated with the event.
	 * @param theEvent	the event to be emitted.
	 */
	protected void emitEvent(Ev theEvent) {
		this.emitEvent(theEvent, null);
	}

	/**
	 * Emits the specified event by/and invoking all the listeners associated with the event.
	 * @param theEvent	the event to be emitted.
	 * @param eventData	the extra data to be sent about the event (can be null).
	 */
	protected void emitEvent(Ev theEvent, Object eventData) {
		Objects.requireNonNull(theEvent, "The 'theEvent' argument cannot be 'null'.");
		this.meListeners.get(theEvent).forEach(ev -> ev.accept(this, eventData));
	}
}
