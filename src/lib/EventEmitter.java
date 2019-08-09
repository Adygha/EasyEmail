package lib;

import java.util.function.BiConsumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract class that represents an event-emitter with with base methods that make it easier to handle listeners.
 * @author Janty Azmat
 * @param <Ev>	an enumeration type that holds the events' constants.
 */
public class EventEmitter<Ev extends Enum<Ev>> implements IEventEmitter<Ev> {
	// Fields
	private Map<Ev, Set<BiConsumer<IEventEmitter<Ev>, Object>>> meListeners;

	/**
	 * Constructor that takes the enumeration class (that holds the events constants) as a parameter.
	 * @param eventsEnumType	the class of the enumeration that holds the events' constants.
	 */
	public EventEmitter(Class<Ev> eventsEnumType) {
		var tmpConsts = eventsEnumType.getEnumConstants();
		this.meListeners = new HashMap<>(tmpConsts.length);
		for (var tmpEv : tmpConsts) {
			this.meListeners.put(tmpEv, new HashSet<>());
		}
	}

	@Override
	public boolean addEventListener(Ev theEvent, BiConsumer<IEventEmitter<Ev>, Object> eventListener) {
		Objects.requireNonNull(theEvent, "The 'theEvent' argument cannot be 'null'.");
		Objects.requireNonNull(eventListener, "The 'eventListener' argument cannot be 'null'.");
		return this.meListeners.get(theEvent).add(eventListener);
	}

	@Override
	public boolean removeEventListener(Ev theEvent, BiConsumer<IEventEmitter<Ev>, Object> eventListener) {
		return this.meListeners.get(theEvent).remove(eventListener);
	}

	@Override
	public void emitEvent(Ev theEvent, Object eventData) {
		Objects.requireNonNull(theEvent, "The 'theEvent' argument cannot be 'null'.");
		this.meListeners.get(theEvent).forEach(ev -> ev.accept(this, eventData));
	}

	@Override
	public void emitEvent(Ev theEvent) {
		this.emitEvent(theEvent, null);
	}
}
