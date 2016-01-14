package pl.maxmati.po.ftp.client.events;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by maxmati on 1/14/16
 */
public class EventDispatcher {
    private Map<Class<? extends Event>, List<EventListener>> eventListeners = new HashMap<>();
    private List<Event> queue = new LinkedList<>();
    private boolean dispatching = false;

    public synchronized void dispatch(Event event) {
        if(dispatching) {
            queue.add(event);
            return;
        }

        dispatching = true;
        System.out.println("Dispatching event: " + event);

        List<EventListener> listeners = eventListeners.get(event.getClass());
        if(listeners != null)
            listeners.forEach(eventListener -> eventListener.onEvent(event));
        dispatching = false;

        while (!queue.isEmpty()) {
            List<Event> tmp = new LinkedList<>(queue);
            queue.clear();
            tmp.forEach(this::dispatch);
        }
    }

    public void registerListener(Class<? extends Event> eventClass, EventListener listener){
        if(!eventListeners.containsKey(eventClass))
            eventListeners.put(eventClass, new LinkedList<>());

        eventListeners.get(eventClass).add(listener);
    }

    @FunctionalInterface
    public interface EventListener{
        void onEvent(Event event);
    }
}
