package org.haox.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventHub implements Dispatcher {

    private enum BuiltInEventType implements EventType {
        STOP,
        ALL
    }

    private Map<Integer, InternalEventHandler> handlers =
            new ConcurrentHashMap<Integer, InternalEventHandler>();

    private Map<EventType, Set<Integer>> eventHandlersMap =
        new ConcurrentHashMap<EventType, Set<Integer>>();

    private Map<EventType, EventWaiter> eventWaiters =
            new ConcurrentHashMap<EventType, EventWaiter>();

    private InternalEventHandler builtInHandler;

    class BuiltInEventHandler extends AbstractEventHandler {
        public BuiltInEventHandler() {
            super();
        }

        @Override
        protected void doHandle(Event event) {

        }

        @Override
        public EventType[] getInterestedEvents() {
            return BuiltInEventType.values();
        }
    }

    public EventHub() {
        init();
    }

    private void init() {
        EventHandler eh = new BuiltInEventHandler();
        builtInHandler = new ExecutedEventHandler(eh);
        register(builtInHandler);
    }

    @Override
    public void dispatch(Event event) {
        process(event);
    }

    @Override
    public void register(EventHandler handler) {
        handler.setDispatcher(this);
        InternalEventHandler ieh = new ExecutedEventHandler(handler);
        register(ieh);
    }

    @Override
    public void register(InternalEventHandler handler) {
        handler.setDispatcher(this);
        handler.init();

        handlers.put(handler.id(), handler);

        EventType[] interestedEvents = handler.getInterestedEvents();
        Set<Integer> tmpHandlers;
        for (EventType eventType : interestedEvents) {
            if (eventHandlersMap.containsKey(eventType)) {
                tmpHandlers = eventHandlersMap.get(eventType);
            } else {
                tmpHandlers = new HashSet<Integer>();
                eventHandlersMap.put(eventType, tmpHandlers);
            }
            tmpHandlers.add(handler.id());
        }
    }

    public EventWaiter waitEvent(final EventType event) {
        return waitEvent(new EventType[] { event } );
    }

    public EventWaiter waitEvent(final EventType... events) {
        EventHandler handler = new AbstractEventHandler() {
            @Override
            protected void doHandle(Event event) throws Exception {
                // no op;
            }

            @Override
            public EventType[] getInterestedEvents() {
                return events;
            }
        };

        handler.setDispatcher(this);
        final WaitEventHandler waitEventHandler = new WaitEventHandler(handler);
        register(waitEventHandler);
        EventWaiter waiter = new EventWaiter() {
            @Override
            public Event waitEvent(EventType event) {
                return waitEventHandler.waitEvent(event);
            }

            @Override
            public Event waitEvent() {
                return waitEventHandler.waitEvent();
            }
        };

        return waiter;
    }

    private void process(Event event) {
        EventType eventType = event.getEventType();
        InternalEventHandler handler;
        Set<Integer> handlerIds;

        if (eventHandlersMap.containsKey(eventType)) {
            handlerIds = eventHandlersMap.get(eventType);
            for (Integer hid : handlerIds) {
                handler = handlers.get(hid);
                handler.handle(event);
            }
        }

        if (eventHandlersMap.containsKey(BuiltInEventType.ALL)) {
            handlerIds = eventHandlersMap.get(BuiltInEventType.ALL);
            for (Integer hid : handlerIds) {
                handler = handlers.get(hid);
                handler.handle(event);
            }
        }
    }

    public void start() {
        for (InternalEventHandler handler : handlers.values()) {
            handler.start();
        }
    }

    public void stop() {
        for (InternalEventHandler handler : handlers.values()) {
            handler.stop();
        }
    }
}